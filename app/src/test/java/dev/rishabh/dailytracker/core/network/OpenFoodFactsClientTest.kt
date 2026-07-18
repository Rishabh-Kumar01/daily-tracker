package dev.rishabh.dailytracker.core.network

import com.google.common.truth.Truth.assertThat
import dev.rishabh.dailytracker.core.db.NutrientKeys
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The lookup is the one place untrusted remote data enters the app, so the whole client is
 * driven end-to-end against the shapes Open Food Facts actually returns — including the
 * partial and malformed ones, which are common in crowd-sourced entries.
 */
class OpenFoodFactsClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OpenFoodFactsClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OpenFoodFactsClient(
            callFactory = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true; coerceInputValues = true },
            baseUrl = server.url("/").toString().trimEnd('/'),
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun respond(body: String, code: Int = 200) {
        server.enqueue(MockResponse(code = code, body = body))
    }

    private suspend fun found(body: String): ScannedProduct {
        respond(body)
        return (client.lookup("8901262010207") as LookupResult.Found).product
    }

    @Test
    fun `maps a real response to per-100g nutrients`() = runTest {
        // Trimmed from the live response for Amul Butter School Pack.
        val product = found(
            """
            {"code":"8901262010207","status":1,"product":{
              "brands":"Amul","product_name":"Butter School Pack","quantity":"100 g",
              "image_front_small_url":"https://images.openfoodfacts.org/front_en.jpg",
              "nutriments":{"energy-kcal":724,"energy-kcal_100g":724,"proteins_100g":1,
                "carbohydrates_100g":0,"fat_100g":80,"salt_100g":2.3875}}}
            """.trimIndent(),
        )

        assertThat(product.barcode).isEqualTo("8901262010207")
        assertThat(product.brand).isEqualTo("Amul")
        assertThat(product.productName).isEqualTo("Butter School Pack")
        assertThat(product.quantity).isEqualTo("100 g")
        assertThat(product.nutrients).containsExactly(
            NutrientKeys.ENERGY_KCAL, 724.0,
            NutrientKeys.PROTEIN_G, 1.0,
            NutrientKeys.CARBS_G, 0.0,
            NutrientKeys.FAT_G, 80.0,
        )
    }

    @Test
    fun `sends the identifying User-Agent OFF requires`() = runTest {
        respond("""{"status":0}""")
        client.lookup("123")

        val request = server.takeRequest()
        assertThat(request.headers["User-Agent"]).isEqualTo(OpenFoodFactsClient.USER_AGENT)
        assertThat(request.target).contains("/api/v2/product/123.json")
    }

    @Test
    fun `status 0 is a miss, not an error`() = runTest {
        respond("""{"code":"00000000","status":0,"status_verbose":"not found"}""")
        assertThat(client.lookup("00000000")).isEqualTo(LookupResult.NotFound)
    }

    @Test
    fun `reads only the _100g keys, ignoring per-serving figures`() = runTest {
        // OFF keeps the as-entered value alongside its normalised one. Reading the wrong
        // key here would store per-serving numbers as if they were per-100g.
        val product = found(
            """
            {"status":1,"product":{"product_name":"Biscuit","nutrition_data_per":"serving",
              "nutriments":{"energy-kcal":150,"energy-kcal_serving":150,"energy-kcal_100g":480,
                "proteins":2,"proteins_100g":6.4}}}
            """.trimIndent(),
        )

        assertThat(product.nutrients[NutrientKeys.ENERGY_KCAL]).isEqualTo(480.0)
        assertThat(product.nutrients[NutrientKeys.PROTEIN_G]).isEqualTo(6.4)
    }

    @Test
    fun `a hit with no nutriments still maps, with no nutrients`() = runTest {
        val product = found("""{"status":1,"product":{"product_name":"Mystery","brands":"X"}}""")

        assertThat(product.productName).isEqualTo("Mystery")
        assertThat(product.nutrients).isEmpty()
    }

    @Test
    fun `numeric strings are read as numbers`() = runTest {
        // Contributor-entered values sometimes arrive quoted.
        val product = found(
            """{"status":1,"product":{"product_name":"X","nutriments":{"energy-kcal_100g":"296"}}}""",
        )
        assertThat(product.nutrients[NutrientKeys.ENERGY_KCAL]).isEqualTo(296.0)
    }

    @Test
    fun `non-numeric and negative values are dropped rather than stored`() = runTest {
        val product = found(
            """
            {"status":1,"product":{"product_name":"X","nutriments":{
              "energy-kcal_100g":"unknown","proteins_100g":-5,"fat_100g":12}}}
            """.trimIndent(),
        )

        assertThat(product.nutrients.keys).containsExactly(NutrientKeys.FAT_G)
    }

    @Test
    fun `only the first of several brands is taken`() = runTest {
        val product = found(
            """{"status":1,"product":{"product_name":"X","brands":"Amul, Amul Dairy, GCMMF"}}""",
        )
        assertThat(product.brand).isEqualTo("Amul")
    }

    @Test
    fun `blank brand becomes null`() = runTest {
        val product = found("""{"status":1,"product":{"product_name":"X","brands":""}}""")
        assertThat(product.brand).isNull()
    }

    @Test
    fun `a server error is an Error, not a miss`() = runTest {
        // A 500 must not be mistaken for "this product does not exist" — that would send
        // the user to manual entry when a retry would have worked.
        respond("", code = 500)
        assertThat(client.lookup("123")).isEqualTo(LookupResult.Error(offline = false))
    }

    @Test
    fun `unparseable body is an Error rather than a crash`() = runTest {
        respond("<html>maintenance</html>")
        assertThat(client.lookup("123")).isEqualTo(LookupResult.Error(offline = false))
    }

    @Test
    fun `an unreachable server reports offline so the UI can offer manual entry`() = runTest {
        server.close()
        assertThat(client.lookup("123")).isEqualTo(LookupResult.Error(offline = true))
    }

    @Test
    fun `unknown fields in the response do not fail the parse`() = runTest {
        // OFF adds fields continually; the DTOs name only what is used.
        val product = found(
            """
            {"status":1,"unexpected_top":42,"product":{"product_name":"X","ecoscore_data":{"a":1},
              "nutriments":{"energy-kcal_100g":100}}}
            """.trimIndent(),
        )
        assertThat(product.productName).isEqualTo("X")
    }
}
