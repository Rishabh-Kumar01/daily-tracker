package dev.rishabh.dailytracker.core.network

import dev.rishabh.dailytracker.core.db.NutrientKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.io.IOException

/**
 * Tier-1 long tail: a typed food that is neither in the bundled generic set nor the user's
 * library is looked up in USDA FoodData Central.
 *
 * Same contract as [OpenFoodFactsClient]: deterministic, no AI, and every hit is a *proposal*
 * that lands in the editable confirmation sheet before it becomes a product. The one extra
 * concern is the API key — FDC requires one, it is the caller's secret, and it is passed in
 * per call rather than baked into the client so it can live in encrypted storage and never
 * in code.
 */
sealed interface UsdaResult {
    /** One or more matches, best-scored first, already normalised to per-100g. */
    data class Found(val foods: List<UsdaFood>) : UsdaResult

    /** Valid response, nothing matched the query. */
    data object NotFound : UsdaResult

    /**
     * Network/server problem. [offline] distinguishes "no connection" (retryable) from a
     * bad response; [unauthorized] flags a missing or rejected API key so the UI can prompt
     * for one rather than offering a pointless retry.
     */
    data class Error(val offline: Boolean, val unauthorized: Boolean = false) : UsdaResult
}

/**
 * A single FDC match, reduced to what the confirmation sheet needs. Amounts are per-100g —
 * FDC's `foodNutrients` are already on that basis — keyed by [NutrientKeys].
 */
data class UsdaFood(
    val fdcId: Long,
    val description: String,
    /** Present only for `Branded` foods; null for Foundation / SR Legacy generics. */
    val brand: String?,
    val dataType: String?,
    val servingSizeG: Double?,
    val nutrients: Map<String, Double>,
) {
    /** Source reference recorded on the seeded/saved product, mirroring the bundled set. */
    val sourceRef: String get() = "FDC $fdcId"
}

/**
 * Constructed by NetworkModule (not field-injected) so [baseUrl] can point at a local
 * server in tests without Dagger needing a binding for a bare String — same reasoning as
 * [OpenFoodFactsClient].
 */
class UsdaClient(
    private val callFactory: Call.Factory,
    private val json: Json,
    private val baseUrl: String = DEFAULT_BASE_URL,
) {

    /**
     * @param apiKey the caller's FDC key, read from encrypted storage. Blank is treated as
     *   an [UsdaResult.Error] with `unauthorized = true` rather than sent — FDC would 403,
     *   and the UI should prompt for a key instead of showing a network error.
     */
    suspend fun search(query: String, apiKey: String, pageSize: Int = DEFAULT_PAGE_SIZE): UsdaResult =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext UsdaResult.Error(offline = false, unauthorized = true)
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return@withContext UsdaResult.NotFound

            // Key travels in the header, never the URL, so it stays out of any request log.
            val url = "$baseUrl/fdc/v1/foods/search".toHttpUrl().newBuilder()
                .addQueryParameter("query", trimmed)
                .addQueryParameter("pageSize", pageSize.toString())
                .build()
            val request = Request.Builder()
                .url(url)
                .header("X-Api-Key", apiKey)
                .build()

            val response = try {
                callFactory.newCall(request).execute()
            } catch (e: IOException) {
                return@withContext UsdaResult.Error(offline = true)
            }

            val body = response.use {
                when {
                    it.code == 401 || it.code == 403 ->
                        return@withContext UsdaResult.Error(offline = false, unauthorized = true)
                    !it.isSuccessful -> return@withContext UsdaResult.Error(offline = false)
                    else -> it.body?.string() ?: return@withContext UsdaResult.Error(offline = false)
                }
            }

            val dto = try {
                json.decodeFromString<FdcSearchResponse>(body)
            } catch (e: Exception) {
                return@withContext UsdaResult.Error(offline = false)
            }

            val foods = dto.foods.mapNotNull { it.toUsdaFoodOrNull() }
            if (foods.isEmpty()) UsdaResult.NotFound else UsdaResult.Found(foods)
        }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.nal.usda.gov"
        private const val DEFAULT_PAGE_SIZE = 10

        /** FDC nutrient numbers, mapped to the app's keys. */
        private val NUTRIENT_NUMBERS: Map<String, String> = mapOf(
            "208" to NutrientKeys.ENERGY_KCAL,
            "203" to NutrientKeys.PROTEIN_G,
            "205" to NutrientKeys.CARBS_G,
            "204" to NutrientKeys.FAT_G,
            "291" to NutrientKeys.FIBER_G,
            "301" to NutrientKeys.CALCIUM_MG,
            "303" to NutrientKeys.IRON_MG,
            "401" to NutrientKeys.VITAMIN_C_MG,
        )

        /** Only these keys are read; everything else FDC returns is ignored. */
        internal val NUMBER_TO_KEY: Map<String, String> get() = NUTRIENT_NUMBERS
    }
}

/* --- Wire DTOs. FDC returns a large object; only the named fields are read. --- */

@Serializable
internal data class FdcSearchResponse(
    val foods: List<FdcFood> = emptyList(),
)

@Serializable
internal data class FdcFood(
    val fdcId: Long = 0,
    val description: String = "",
    val dataType: String? = null,
    val brandOwner: String? = null,
    val brandName: String? = null,
    val servingSize: Double? = null,
    val servingSizeUnit: String? = null,
    val foodNutrients: List<FdcNutrient> = emptyList(),
)

@Serializable
internal data class FdcNutrient(
    val nutrientNumber: String? = null,
    val value: Double? = null,
    @SerialName("unitName") val unitName: String? = null,
)

/**
 * Maps an FDC food to the reduced [UsdaFood], reading only the recognised nutrient numbers
 * and dropping non-finite or negative values. A food with no readable energy is discarded —
 * it would render as an empty proposal.
 */
internal fun FdcFood.toUsdaFoodOrNull(): UsdaFood? {
    if (fdcId == 0L || description.isBlank()) return null
    val nutrients = buildMap {
        for (n in foodNutrients) {
            val key = UsdaClient.NUMBER_TO_KEY[n.nutrientNumber] ?: continue
            val v = n.value ?: continue
            if (v.isFinite() && v >= 0.0) put(key, v)
        }
    }
    if (!nutrients.containsKey(NutrientKeys.ENERGY_KCAL)) return null
    // Only Branded foods carry a brand; Foundation / SR Legacy rows are generic.
    val brand = (brandName ?: brandOwner)?.trim()?.ifEmpty { null }?.takeIf { dataType == "Branded" }
    return UsdaFood(
        fdcId = fdcId,
        description = description.trim(),
        brand = brand,
        dataType = dataType,
        servingSizeG = servingSize?.takeIf { servingSizeUnit.equals("g", ignoreCase = true) },
        nutrients = nutrients,
    )
}
