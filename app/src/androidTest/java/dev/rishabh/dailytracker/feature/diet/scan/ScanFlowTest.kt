package dev.rishabh.dailytracker.feature.diet.scan

import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.rishabh.dailytracker.core.db.CreatedBy
import dev.rishabh.dailytracker.core.db.DailyTrackerDatabase
import dev.rishabh.dailytracker.core.db.FakeIdGenerator
import dev.rishabh.dailytracker.core.db.FakeTimeSource
import dev.rishabh.dailytracker.core.db.FieldType
import dev.rishabh.dailytracker.core.db.NutrientKeys
import dev.rishabh.dailytracker.core.db.ProductSource
import dev.rishabh.dailytracker.core.db.VariantSource
import dev.rishabh.dailytracker.core.db.entity.ActivityTemplateEntity
import dev.rishabh.dailytracker.core.db.entity.ItemEntity
import dev.rishabh.dailytracker.core.db.entity.ItemFieldEntity
import dev.rishabh.dailytracker.core.db.entity.SubMenuEntity
import dev.rishabh.dailytracker.core.network.OpenFoodFactsClient
import dev.rishabh.dailytracker.feature.diet.MealRepository
import dev.rishabh.dailytracker.navigation.Routes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The barcode lane end to end, minus the camera: a detected code goes to the lookup, the
 * result lands in the editable confirmation, and saving writes a real product row.
 *
 * ML Kit's decoding of an actual printed barcode is the one link this cannot cover — that
 * needs a physical device and a real packet.
 */
@RunWith(AndroidJUnit4::class)
class ScanFlowTest {

    private lateinit var db: DailyTrackerDatabase
    private lateinit var server: MockWebServer
    private lateinit var repository: MealRepository
    private lateinit var client: OpenFoodFactsClient

    private val amulButter = """
        {"code":"8901262010207","status":1,"product":{
          "brands":"Amul","product_name":"Butter School Pack","quantity":"100 g",
          "nutriments":{"energy-kcal_100g":724,"proteins_100g":1,
            "carbohydrates_100g":0,"fat_100g":80}}}
    """.trimIndent()

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DailyTrackerDatabase::class.java,
        ).build()
        server = MockWebServer()
        server.start()

        repository = MealRepository(
            db.templateDao(), db.logDao(), db.productDao(), db.genericFoodMetaDao(), FakeIdGenerator(), FakeTimeSource(),
        )
        client = OpenFoodFactsClient(
            callFactory = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true; coerceInputValues = true },
            baseUrl = server.url("/").toString().trimEnd('/'),
        )

        db.templateDao().insertFullTemplate(
            ActivityTemplateEntity("t1", "Diet", "restaurant", "#75D78D", CreatedBy.SYSTEM, "sum_field", "kcal", 1, false, 0, 1L),
            listOf(SubMenuEntity("s1", "t1", "Breakfast", 0, null)),
            listOf(ItemEntity("i1", "s1", "Butter", true, VariantSource.USER_LIBRARY, 0)),
            listOf(
                ItemFieldEntity("f1", "i1", "variant", FieldType.ITEM_VARIANT.wire, "Brand", null, true, 0, null),
                ItemFieldEntity("f2", "i1", "amount", FieldType.QUANTITY.wire, "Amount", "g", true, 1, null),
            ),
        )
    }

    @After
    fun tearDown() {
        if (::server.isInitialized) server.close()
        if (::db.isInitialized) db.close()
    }

    private fun viewModel() = ScanViewModel(
        repository = repository,
        client = client,
        savedStateHandle = SavedStateHandle(mapOf(Routes.ARG_ITEM_ID to "i1")),
    )

    /**
     * The lookup runs on viewModelScope (the main looper) and hits a real socket, so tests
     * wait for the state they expect instead of reading it straight back.
     *
     * These tests use runBlocking rather than runTest deliberately: the whole point is to
     * exercise the real dispatchers and a real HTTP round trip, which a virtual clock and a
     * test main dispatcher would short-circuit.
     */
    private suspend inline fun <reified T : ScanState> ScanViewModel.await(): T =
        withTimeout(10_000) { state.first { it is T } as T }

    private suspend fun ScanViewModel.awaitSavedProductId(): String =
        withTimeout(10_000) { savedProductId.first { it != null }!! }

    @Test
    fun a_hit_prefills_the_confirmation_from_the_lookup() = runBlocking {
        server.enqueue(MockResponse(code = 200, body = amulButter))
        val vm = viewModel()

        vm.onBarcodeDetected("8901262010207")

        val state = vm.await<ScanState.Confirm>()
        assertThat(state.barcode).isEqualTo("8901262010207")
        assertThat(state.source).isEqualTo(ProductSource.OFF)
        assertThat(state.input.brand).isEqualTo("Amul")
        assertThat(state.input.productName).isEqualTo("Butter School Pack")
        assertThat(state.input.kcal).isEqualTo("724")
        assertThat(state.input.fat).isEqualTo("80")
    }

    @Test
    fun saving_writes_the_product_under_the_scanned_food() = runBlocking {
        server.enqueue(MockResponse(code = 200, body = amulButter))
        val vm = viewModel()
        vm.onBarcodeDetected("8901262010207")
        vm.await<ScanState.Confirm>()

        vm.onSave()

        val productId = vm.awaitSavedProductId()
        val product = checkNotNull(db.productDao().getProduct(productId))
        assertThat(product.genericName).isEqualTo("butter")
        assertThat(product.barcode).isEqualTo("8901262010207")
        assertThat(product.source).isEqualTo(ProductSource.OFF)

        val nutrients = db.productDao().getNutrients(productId).associate { it.nutrientKey to it.amountPer100g }
        assertThat(nutrients[NutrientKeys.ENERGY_KCAL]).isEqualTo(724.0)
        assertThat(nutrients[NutrientKeys.FAT_G]).isEqualTo(80.0)
    }

    @Test
    fun a_correction_in_the_sheet_wins_over_the_lookup() = runBlocking {
        // The lookup only proposes; whatever the user leaves in the field is what is stored.
        server.enqueue(MockResponse(code = 200, body = amulButter))
        val vm = viewModel()
        vm.onBarcodeDetected("8901262010207")
        vm.await<ScanState.Confirm>()

        vm.onFieldChange(2, "700")
        vm.onSave()

        val productId = vm.awaitSavedProductId()
        val nutrients = db.productDao().getNutrients(productId).associate { it.nutrientKey to it.amountPer100g }
        assertThat(nutrients[NutrientKeys.ENERGY_KCAL]).isEqualTo(700.0)
    }

    @Test
    fun a_miss_still_offers_confirmation_and_keeps_the_barcode() = runBlocking {
        server.enqueue(MockResponse(code = 200, body = """{"status":0}"""))
        val vm = viewModel()

        vm.onBarcodeDetected("8901262010207")

        val state = vm.await<ScanState.Confirm>()
        assertThat(state.barcode).isEqualTo("8901262010207")
        assertThat(state.source).isEqualTo(ProductSource.MANUAL)
        assertThat(state.input.productName).isEmpty()
        assertThat(state.notice).contains("Not in Open Food Facts")
    }

    @Test
    fun a_product_typed_after_a_miss_still_carries_the_barcode() = runBlocking {
        server.enqueue(MockResponse(code = 200, body = """{"status":0}"""))
        val vm = viewModel()
        vm.onBarcodeDetected("8901262010207")
        vm.await<ScanState.Confirm>()

        vm.onFieldChange(1, "Local Butter")
        vm.onFieldChange(2, "700")
        vm.onSave()

        val product = checkNotNull(db.productDao().getProduct(vm.awaitSavedProductId()))
        assertThat(product.productName).isEqualTo("Local Butter")
        assertThat(product.barcode).isEqualTo("8901262010207")
        assertThat(product.source).isEqualTo(ProductSource.MANUAL)
    }

    @Test
    fun a_network_failure_is_retryable_rather_than_a_dead_end() = runBlocking {
        server.close()
        val vm = viewModel()

        vm.onBarcodeDetected("8901262010207")

        val state = vm.await<ScanState.LookupFailed>()
        assertThat(state.offline).isTrue()
        assertThat(state.barcode).isEqualTo("8901262010207")
    }

    @Test
    fun a_hit_without_nutrition_says_so_rather_than_saving_an_empty_product() = runBlocking {
        server.enqueue(
            MockResponse(code = 200, body = """{"status":1,"product":{"product_name":"Mystery"}}"""),
        )
        val vm = viewModel()
        vm.onBarcodeDetected("8901262010207")
        vm.await<ScanState.Confirm>()

        vm.onSave()

        // Energy is required, so this cannot become a product until the user supplies it.
        val state = vm.await<ScanState.Confirm>()
        assertThat(state.notice).contains("without nutrition")
        assertThat(state.error).isEqualTo("Energy (kcal) is required")
        assertThat(vm.savedProductId.value).isNull()
    }

    @Test
    fun repeated_detections_of_the_same_frame_do_not_stack_lookups() = runBlocking {
        // The analyser fires per frame; only the first may start a lookup.
        server.enqueue(MockResponse(code = 200, body = amulButter))
        val vm = viewModel()

        vm.onBarcodeDetected("8901262010207")
        vm.onBarcodeDetected("8901262010207")
        vm.onBarcodeDetected("8901262010207")

        vm.await<ScanState.Confirm>()
        assertThat(server.requestCount).isEqualTo(1)
    }
}
