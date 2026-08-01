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
 * USDA is the second place untrusted remote data enters the app, so — like the OFF client —
 * the whole thing is driven end-to-end against the shapes FoodData Central actually returns,
 * including the auth and empty cases the UI branches on.
 */
class UsdaClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: UsdaClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = UsdaClient(
            callFactory = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true; coerceInputValues = true },
            baseUrl = server.url("/").toString().trimEnd('/'),
        )
    }

    @After
    fun tearDown() = server.close()

    private fun respond(body: String, code: Int = 200) =
        server.enqueue(MockResponse(code = code, body = body))

    @Test
    fun `maps a real search hit to per-100g nutrients`() = runTest {
        // Trimmed from the live foods/search response for "paneer".
        respond(
            """
            {"totalHits":46,"foods":[{
              "fdcId":2103038,"description":"PANEER","dataType":"Branded",
              "brandOwner":"Karoun Dairies Inc. GOPI","brandName":"GOPI",
              "servingSize":28.0,"servingSizeUnit":"g",
              "foodNutrients":[
                {"nutrientNumber":"203","value":25.0,"unitName":"G"},
                {"nutrientNumber":"204","value":25.0,"unitName":"G"},
                {"nutrientNumber":"205","value":3.57,"unitName":"G"},
                {"nutrientNumber":"208","value":321.0,"unitName":"KCAL"},
                {"nutrientNumber":"291","value":0.0,"unitName":"G"},
                {"nutrientNumber":"301","value":407.0,"unitName":"MG"},
                {"nutrientNumber":"303","value":0.0,"unitName":"MG"}]}]}
            """.trimIndent(),
        )

        val foods = (client.search("paneer", apiKey = "k") as UsdaResult.Found).foods
        assertThat(foods).hasSize(1)
        val f = foods.single()
        assertThat(f.fdcId).isEqualTo(2103038)
        assertThat(f.description).isEqualTo("PANEER")
        assertThat(f.brand).isEqualTo("GOPI")
        assertThat(f.sourceRef).isEqualTo("FDC 2103038")
        assertThat(f.servingSizeG).isEqualTo(28.0)
        assertThat(f.nutrients).containsExactly(
            NutrientKeys.PROTEIN_G, 25.0,
            NutrientKeys.FAT_G, 25.0,
            NutrientKeys.CARBS_G, 3.57,
            NutrientKeys.ENERGY_KCAL, 321.0,
            NutrientKeys.FIBER_G, 0.0,
            NutrientKeys.CALCIUM_MG, 407.0,
            NutrientKeys.IRON_MG, 0.0,
        )
    }

    @Test
    fun `generic Foundation foods carry no brand`() = runTest {
        respond(
            """
            {"foods":[{"fdcId":170379,"description":"Rice, white, cooked","dataType":"SR Legacy",
              "brandOwner":"USDA",
              "foodNutrients":[{"nutrientNumber":"208","value":130.0,"unitName":"KCAL"}]}]}
            """.trimIndent(),
        )
        val f = (client.search("rice", apiKey = "k") as UsdaResult.Found).foods.single()
        assertThat(f.brand).isNull()
    }

    @Test
    fun `a food with no energy is dropped rather than shown as an empty proposal`() = runTest {
        respond(
            """
            {"foods":[
              {"fdcId":1,"description":"No energy","dataType":"Branded",
               "foodNutrients":[{"nutrientNumber":"203","value":5.0,"unitName":"G"}]},
              {"fdcId":2,"description":"Has energy","dataType":"Branded",
               "foodNutrients":[{"nutrientNumber":"208","value":50.0,"unitName":"KCAL"}]}]}
            """.trimIndent(),
        )
        val foods = (client.search("x", apiKey = "k") as UsdaResult.Found).foods
        assertThat(foods.map { it.fdcId }).containsExactly(2L)
    }

    @Test
    fun `an empty result list is NotFound`() = runTest {
        respond("""{"totalHits":0,"foods":[]}""")
        assertThat(client.search("zzzzz", apiKey = "k")).isEqualTo(UsdaResult.NotFound)
    }

    @Test
    fun `a rejected key is unauthorized, not a generic error`() = runTest {
        respond("""{"error":{"code":"API_KEY_INVALID"}}""", code = 403)
        val result = client.search("paneer", apiKey = "bad") as UsdaResult.Error
        assertThat(result.unauthorized).isTrue()
    }

    @Test
    fun `a blank key is unauthorized and never hits the network`() = runTest {
        // No response enqueued: a request would hang the MockWebServer read, so reaching
        // here at all proves the client short-circuited before calling out.
        val result = client.search("paneer", apiKey = "  ") as UsdaResult.Error
        assertThat(result.unauthorized).isTrue()
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `a server error is an Error, not a miss`() = runTest {
        respond("", code = 500)
        val result = client.search("paneer", apiKey = "k")
        assertThat(result).isInstanceOf(UsdaResult.Error::class.java)
        assertThat((result as UsdaResult.Error).unauthorized).isFalse()
    }

    @Test
    fun `unparseable body is an Error rather than a crash`() = runTest {
        respond("not json")
        assertThat(client.search("paneer", apiKey = "k")).isInstanceOf(UsdaResult.Error::class.java)
    }

    @Test
    fun `the key is sent as a header, never in the query string`() = runTest {
        respond("""{"foods":[]}""")
        client.search("paneer", apiKey = "secret-key")
        val request = server.takeRequest()
        assertThat(request.headers["X-Api-Key"]).isEqualTo("secret-key")
        assertThat(request.target).doesNotContain("secret-key")
        assertThat(request.target).contains("query=paneer")
    }
}
