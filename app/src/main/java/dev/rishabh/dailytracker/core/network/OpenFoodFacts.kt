package dev.rishabh.dailytracker.core.network

import dev.rishabh.dailytracker.core.db.NutrientKeys
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Request
import java.io.IOException

/**
 * Tier-1 nutrition lookup: barcode -> Open Food Facts.
 *
 * Deterministic, no AI — the whole point of the tier order is that the cheapest, most
 * reliable lane runs first. The result is still a proposal: it lands in an editable
 * confirmation sheet before it becomes a product, exactly like an AI-extracted label
 * would.
 */
sealed interface LookupResult {
    /** A hit. Fields may still be blank — OFF entries are crowd-sourced and often partial. */
    data class Found(val product: ScannedProduct) : LookupResult

    /** Valid response, no such product. The user is offered manual entry with the barcode kept. */
    data object NotFound : LookupResult

    /** Network or server problem — distinct from NotFound so the UI can offer a retry. */
    data class Error(val offline: Boolean) : LookupResult
}

/**
 * What a lookup yields, already normalised to the per-100g basis the schema stores.
 *
 * Deliberately a plain value type rather than the DTO: nothing from the wire reaches the
 * database without passing through this shape and then the confirmation sheet.
 */
data class ScannedProduct(
    val barcode: String,
    val brand: String?,
    val productName: String,
    val quantity: String?,
    val imageUrl: String?,
    /** Per-100g amounts keyed by NutrientKeys; absent keys were absent upstream. */
    val nutrients: Map<String, Double>,
)

/**
 * Constructed by NetworkModule rather than field-injected, so [baseUrl] can be pointed at a
 * local server in tests without Dagger needing a binding for a bare String.
 */
class OpenFoodFactsClient(
    private val callFactory: Call.Factory,
    private val json: Json,
    private val baseUrl: String = DEFAULT_BASE_URL,
) {

    suspend fun lookup(barcode: String): LookupResult = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/v2/product/$barcode.json?fields=$FIELDS"
        val request = Request.Builder()
            .url(url)
            // OFF's usage policy requires an identifying User-Agent; anonymous clients get
            // rate-limited or blocked. It carries no user identity, only the app's.
            .header("User-Agent", USER_AGENT)
            .build()

        val body = try {
            callFactory.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext LookupResult.Error(offline = false)
                response.body?.string() ?: return@withContext LookupResult.Error(offline = false)
            }
        } catch (e: IOException) {
            // Offline is the expected case for an offline-first app, not an exception path.
            return@withContext LookupResult.Error(offline = true)
        }

        val dto = try {
            json.decodeFromString<OffResponse>(body)
        } catch (e: Exception) {
            return@withContext LookupResult.Error(offline = false)
        }

        val product = dto.product
        if (dto.status != 1 || product == null) return@withContext LookupResult.NotFound
        LookupResult.Found(product.toScannedProduct(barcode))
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://world.openfoodfacts.org"
        private const val FIELDS =
            "code,product_name,brands,quantity,nutriments,image_front_small_url"
        const val USER_AGENT = "DailyTracker/0.1 (personal offline tracker; github.com/rishabh)"
    }
}

/* --- Wire DTOs. Everything is nullable: OFF entries are crowd-sourced and often partial. --- */

@Serializable
internal data class OffResponse(
    val status: Int = 0,
    val product: OffProduct? = null,
)

@Serializable
internal data class OffProduct(
    @SerialName("product_name") val productName: String? = null,
    val brands: String? = null,
    val quantity: String? = null,
    @SerialName("image_front_small_url") val imageUrl: String? = null,
    val nutriments: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
)

/**
 * OFF exposes a `_100g` variant of every nutrient regardless of the basis the label was
 * entered in, which is exactly the basis the schema stores — so reading only those keys is
 * the normalisation step. A product whose figures OFF could not normalise simply has no
 * `_100g` key and contributes nothing, rather than being silently misread at the wrong basis.
 */
internal fun OffProduct.toScannedProduct(barcode: String): ScannedProduct {
    val nutrients = buildMap {
        putIfPresent(NutrientKeys.ENERGY_KCAL, nutriments.number("energy-kcal_100g"))
        putIfPresent(NutrientKeys.PROTEIN_G, nutriments.number("proteins_100g"))
        putIfPresent(NutrientKeys.CARBS_G, nutriments.number("carbohydrates_100g"))
        putIfPresent(NutrientKeys.FAT_G, nutriments.number("fat_100g"))
        putIfPresent(NutrientKeys.FIBER_G, nutriments.number("fiber_100g"))
    }
    return ScannedProduct(
        barcode = barcode,
        // OFF stores brands as a comma-separated list; the first is the primary one.
        brand = brands?.split(",")?.firstOrNull()?.trim()?.ifEmpty { null },
        productName = productName?.trim().orEmpty(),
        quantity = quantity?.trim()?.ifEmpty { null },
        imageUrl = imageUrl?.ifEmpty { null },
        nutrients = nutrients,
    )
}

private fun MutableMap<String, Double>.putIfPresent(key: String, value: Double?) {
    if (value != null && value.isFinite() && value >= 0.0) put(key, value)
}

/**
 * Nutriment values arrive as either JSON numbers or numeric strings depending on how the
 * entry was contributed, so both are read rather than trusting one shape.
 */
private fun Map<String, kotlinx.serialization.json.JsonElement>.number(key: String): Double? {
    val element = this[key] ?: return null
    val primitive = element as? kotlinx.serialization.json.JsonPrimitive ?: return null
    return primitive.content.toDoubleOrNull()
}
