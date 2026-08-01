package dev.rishabh.dailytracker.feature.diet

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
import dev.rishabh.dailytracker.core.db.entity.ProductEntity
import dev.rishabh.dailytracker.core.db.entity.ProductNutrientEntity
import dev.rishabh.dailytracker.core.db.entity.SubMenuEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The end-to-end Diet slice against a real database: what the meal screen reads, what
 * logging writes, and the invariant that macros are always derived and never stored.
 */
@RunWith(AndroidJUnit4::class)
class MealRepositoryTest {

    private lateinit var db: DailyTrackerDatabase
    private lateinit var repository: MealRepository
    private lateinit var ids: FakeIdGenerator
    private lateinit var time: FakeTimeSource

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DailyTrackerDatabase::class.java,
        ).build()
        ids = FakeIdGenerator()
        time = FakeTimeSource()
        repository = MealRepository(db.templateDao(), db.logDao(), db.productDao(), db.genericFoodMetaDao(), ids, time)

        db.templateDao().insertFullTemplate(
            ActivityTemplateEntity("t1", "Diet", "restaurant", "#75D78D", CreatedBy.SYSTEM, "sum_field", "kcal", 1, false, 0, 1L),
            listOf(SubMenuEntity("s1", "t1", "Lunch", 0, null)),
            listOf(
                ItemEntity("i1", "s1", "Paneer", true, VariantSource.USER_LIBRARY, 0),
                ItemEntity("i2", "s1", "Rice", true, VariantSource.USER_LIBRARY, 1),
            ),
            listOf(
                ItemFieldEntity("f1", "i1", "variant", FieldType.ITEM_VARIANT.wire, "Brand", null, true, 0, null),
                ItemFieldEntity("f2", "i1", "amount", FieldType.QUANTITY.wire, "Amount", "g", true, 1, null),
                ItemFieldEntity("f3", "i2", "variant", FieldType.ITEM_VARIANT.wire, "Brand", null, true, 0, null),
                ItemFieldEntity("f4", "i2", "amount", FieldType.QUANTITY.wire, "Amount", "g", true, 1, null),
            ),
        )
        db.productDao().insertProductWithNutrients(
            ProductEntity(
                productId = "p1", genericName = "paneer", brand = "Amul",
                productName = "Malai Paneer", source = ProductSource.MANUAL, createdAt = 1L,
            ),
            listOf(
                ProductNutrientEntity("n1", "p1", NutrientKeys.ENERGY_KCAL, 296.0),
                ProductNutrientEntity("n2", "p1", NutrientKeys.PROTEIN_G, 18.5),
            ),
        )
    }

    @After
    fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    private suspend fun meal() = checkNotNull(repository.observeMeal("s1").first())

    private suspend fun item(id: String) = meal().items.first { it.itemId == id }

    @Test
    fun meal_exposes_items_with_their_brands_and_field_keys() = runTest {
        val paneer = item("i1")
        assertThat(paneer.genericName).isEqualTo("paneer")
        assertThat(paneer.quantityFieldKey).isEqualTo("amount")
        assertThat(paneer.variantFieldKey).isEqualTo("variant")
        assertThat(paneer.brands.map { it.productId }).containsExactly("p1")
        assertThat(paneer.brands.single().per100g.kcal).isEqualTo(296.0)
        // Rice has no products saved yet, which is the empty-expansion state.
        assertThat(item("i2").brands).isEmpty()
        assertThat(paneer.logged).isNull()
    }

    @Test
    fun logging_a_portion_derives_macros_at_read_time() = runTest {
        repository.logPortion("t1", "s1", item("i1"), "p1", grams = 150.0)

        val logged = checkNotNull(item("i1").logged)
        assertThat(logged.grams).isEqualTo(150.0)
        assertThat(logged.productId).isEqualTo("p1")
        // 296 kcal/100g x 150g
        assertThat(logged.totals.energyKcal).isWithin(0.001).of(444.0)
        assertThat(logged.totals.proteinG).isWithin(0.001).of(27.75)
        assertThat(meal().totals.energyKcal).isWithin(0.001).of(444.0)
    }

    @Test
    fun correcting_a_product_retroactively_corrects_the_logged_meal() = runTest {
        repository.logPortion("t1", "s1", item("i1"), "p1", grams = 100.0)
        assertThat(meal().totals.energyKcal).isWithin(0.001).of(296.0)

        // The label was misread; fix the product, not the history.
        db.productDao().upsertNutrients(
            listOf(ProductNutrientEntity("n1", "p1", NutrientKeys.ENERGY_KCAL, 250.0)),
        )

        assertThat(meal().totals.energyKcal).isWithin(0.001).of(250.0)
    }

    @Test
    fun writes_entry_and_values_together() = runTest {
        repository.logPortion("t1", "s1", item("i1"), "p1", grams = 150.0)

        val entryId = checkNotNull(item("i1").logged).entryId
        val entry = checkNotNull(db.logDao().getEntry(entryId))
        assertThat(entry.variantRef).isEqualTo("p1")
        assertThat(entry.templateId).isEqualTo("t1")
        assertThat(entry.localDate).isEqualTo(time.today())

        val values = db.logDao().getValues(entryId)
        assertThat(values.first { it.fieldKey == "amount" }.valueNumber).isEqualTo(150.0)
        assertThat(values.first { it.fieldKey == "variant" }.valueText).isEqualTo("p1")
        // Nothing computed is persisted: no value row carries a macro.
        assertThat(values.map { it.valueNumber }).containsExactly(150.0, null)
    }

    @Test
    fun re_logging_replaces_the_portion_instead_of_adding_a_second() = runTest {
        repository.logPortion("t1", "s1", item("i1"), "p1", grams = 100.0)
        val firstEntry = checkNotNull(item("i1").logged).entryId

        repository.logPortion("t1", "s1", item("i1"), "p1", grams = 200.0)

        val logged = checkNotNull(item("i1").logged)
        assertThat(logged.entryId).isEqualTo(firstEntry)
        assertThat(logged.grams).isEqualTo(200.0)
        assertThat(db.logDao().getEntriesForDay("t1", time.today())).hasSize(1)
        assertThat(db.logDao().getValues(firstEntry)).hasSize(2)
    }

    @Test
    fun removing_a_portion_clears_it_and_its_values() = runTest {
        repository.logPortion("t1", "s1", item("i1"), "p1", grams = 100.0)
        val entryId = checkNotNull(item("i1").logged).entryId

        repository.removePortion(entryId)

        assertThat(item("i1").logged).isNull()
        assertThat(meal().totals.energyKcal).isEqualTo(0.0)
        assertThat(db.logDao().getValues(entryId)).isEmpty()
    }

    @Test
    fun logging_touches_the_product_for_frequency_ranking() = runTest {
        time.now = FakeTimeSource.FIXED_NOW + 5_000
        repository.logPortion("t1", "s1", item("i1"), "p1", grams = 100.0)

        assertThat(db.productDao().getProduct("p1")?.lastUsedAt).isEqualTo(time.now)
    }

    @Test
    fun manual_product_becomes_a_brand_under_its_food() = runTest {
        val productId = repository.createManualProduct(
            genericName = "rice",
            product = ValidatedProduct(
                brand = "India Gate",
                productName = "Basmati",
                nutrients = mapOf(NutrientKeys.ENERGY_KCAL to 350.0, NutrientKeys.CARBS_G to 78.0),
            ),
        )

        val rice = item("i2")
        assertThat(rice.brands.map { it.productId }).containsExactly(productId)
        assertThat(rice.brands.single().per100g.carbs).isEqualTo(78.0)
        assertThat(db.productDao().getProduct(productId)?.source).isEqualTo(ProductSource.MANUAL)
    }

    @Test
    fun a_scanned_product_keeps_its_barcode_and_source() = runTest {
        val productId = repository.createManualProduct(
            genericName = "rice",
            product = ValidatedProduct("India Gate", "Basmati", mapOf(NutrientKeys.ENERGY_KCAL to 350.0)),
            barcode = "8901262010207",
            source = ProductSource.OFF,
        )

        val saved = checkNotNull(db.productDao().getProduct(productId))
        assertThat(saved.barcode).isEqualTo("8901262010207")
        assertThat(saved.source).isEqualTo(ProductSource.OFF)
        assertThat(item("i2").brands.map { it.productId }).containsExactly(productId)
    }

    @Test
    fun rescanning_the_same_barcode_updates_the_product_even_if_renamed() = runTest {
        val first = repository.createManualProduct(
            "rice",
            ValidatedProduct("India Gate", "Basmati", mapOf(NutrientKeys.ENERGY_KCAL to 350.0)),
            barcode = "8901262010207",
            source = ProductSource.OFF,
        )
        // Same packet, different name from the lookup. The barcode identifies it exactly,
        // so a fuzzy name match must not be what decides this.
        val second = repository.createManualProduct(
            "rice",
            ValidatedProduct("India Gate", "Basmati Rice 1kg", mapOf(NutrientKeys.ENERGY_KCAL to 345.0)),
            barcode = "8901262010207",
            source = ProductSource.OFF,
        )

        assertThat(second).isEqualTo(first)
        assertThat(item("i2").brands).hasSize(1)
        assertThat(item("i2").brands.single().productName).isEqualTo("Basmati Rice 1kg")
    }

    @Test
    fun a_barcode_is_not_erased_by_a_later_manual_edit() = runTest {
        val productId = repository.createManualProduct(
            "rice",
            ValidatedProduct("India Gate", "Basmati", mapOf(NutrientKeys.ENERGY_KCAL to 350.0)),
            barcode = "8901262010207",
            source = ProductSource.OFF,
        )
        // Corrected by hand afterwards, with no barcode in play.
        repository.createManualProduct(
            "rice",
            ValidatedProduct("India Gate", "Basmati", mapOf(NutrientKeys.ENERGY_KCAL to 348.0)),
        )

        assertThat(db.productDao().getProduct(productId)?.barcode).isEqualTo("8901262010207")
    }

    @Test
    fun generic_name_for_an_item_is_its_food_name_normalised() = runTest {
        assertThat(repository.genericNameForItem("i1")).isEqualTo("paneer")
        assertThat(repository.genericNameForItem("nope")).isNull()
    }

    @Test
    fun saving_the_same_brand_twice_updates_it_rather_than_duplicating() = runTest {
        val first = repository.createManualProduct(
            "rice",
            ValidatedProduct("India Gate", "Basmati", mapOf(NutrientKeys.ENERGY_KCAL to 350.0)),
        )
        // Same product typed again in a different case, with a corrected figure.
        val second = repository.createManualProduct(
            "rice",
            ValidatedProduct("india gate", "basmati", mapOf(NutrientKeys.ENERGY_KCAL to 345.0)),
        )

        assertThat(second).isEqualTo(first)
        assertThat(item("i2").brands).hasSize(1)
        assertThat(item("i2").brands.single().per100g.kcal).isEqualTo(345.0)
    }
}
