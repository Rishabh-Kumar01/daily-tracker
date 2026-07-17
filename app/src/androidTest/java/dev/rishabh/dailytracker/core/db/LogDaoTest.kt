package dev.rishabh.dailytracker.core.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.rishabh.dailytracker.core.db.dao.LogDao
import dev.rishabh.dailytracker.core.db.dao.ProductDao
import dev.rishabh.dailytracker.core.db.dao.TemplateDao
import dev.rishabh.dailytracker.core.db.entity.ActivityTemplateEntity
import dev.rishabh.dailytracker.core.db.entity.ItemEntity
import dev.rishabh.dailytracker.core.db.entity.LogEntryEntity
import dev.rishabh.dailytracker.core.db.entity.LogValueEntity
import dev.rishabh.dailytracker.core.db.entity.ProductEntity
import dev.rishabh.dailytracker.core.db.entity.ProductNutrientEntity
import dev.rishabh.dailytracker.core.db.entity.SubMenuEntity
import dev.rishabh.dailytracker.core.nutrition.MacroCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LogDaoTest {

    private lateinit var db: DailyTrackerDatabase
    private lateinit var dao: LogDao
    private lateinit var templateDao: TemplateDao
    private lateinit var productDao: ProductDao

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DailyTrackerDatabase::class.java,
        ).build()
        dao = db.logDao()
        templateDao = db.templateDao()
        productDao = db.productDao()

        // Minimal template scaffolding the log FKs require.
        templateDao.insertFullTemplate(
            ActivityTemplateEntity("t1", "Diet", "restaurant", "#75D78D", CreatedBy.SYSTEM, null, null, 1, false, 0, 1L),
            listOf(SubMenuEntity("s1", "t1", "Lunch", 0, null)),
            listOf(ItemEntity("i1", "s1", "Paneer", true, VariantSource.USER_LIBRARY, 0)),
            emptyList(),
        )
        productDao.insertProductWithNutrients(
            ProductEntity(
                productId = "p1", genericName = "paneer", brand = "Amul", productName = "Malai Paneer",
                source = ProductSource.MANUAL, createdAt = 1L,
            ),
            listOf(
                ProductNutrientEntity("n1", "p1", NutrientKeys.ENERGY_KCAL, 296.0),
                ProductNutrientEntity("n2", "p1", NutrientKeys.PROTEIN_G, 18.5),
            ),
        )
    }

    @After
    fun tearDown() = db.close()

    private fun entry(id: String, date: String, product: String? = "p1", at: Long = 1L) =
        LogEntryEntity(id, "t1", "s1", "i1", at, date, product)

    @Test
    fun insertLog_writesEntryAndValuesTogether() = runTest {
        val e = entry("e1", TODAY)
        val values = listOf(
            LogValueEntity("v1", "e1", "amount", valueNumber = 150.0),
            LogValueEntity("v2", "e1", "note", valueText = "extra spicy"),
        )

        dao.insertLog(e, values)

        assertThat(dao.getEntry("e1")).isEqualTo(e)
        assertThat(dao.getValues("e1")).containsExactlyElementsIn(values)
    }

    @Test
    fun sparseValueColumnsRoundTripByType() = runTest {
        dao.insertLog(
            entry("e1", TODAY),
            listOf(
                LogValueEntity("v1", "e1", "amount", valueNumber = 150.5),
                LogValueEntity("v2", "e1", "note", valueText = "hello"),
                LogValueEntity("v3", "e1", "done", valueBool = true),
                LogValueEntity("v4", "e1", "sets", valueJson = """[{"reps":8,"weight":60}]"""),
            ),
        )

        val byKey = dao.getValues("e1").associateBy { it.fieldKey }

        assertThat(byKey["amount"]!!.valueNumber).isEqualTo(150.5)
        assertThat(byKey["note"]!!.valueText).isEqualTo("hello")
        assertThat(byKey["done"]!!.valueBool).isTrue()
        assertThat(byKey["sets"]!!.valueJson).isEqualTo("""[{"reps":8,"weight":60}]""")
        // Sparse really means sparse: untouched columns stay null.
        assertThat(byKey["note"]!!.valueNumber).isNull()
    }

    @Test
    fun observeEntriesForDay_filtersByTemplateAndDate() = runTest {
        dao.insertLog(entry("e1", TODAY), emptyList())
        dao.insertLog(entry("e2", TODAY, at = 2L), emptyList())
        dao.insertLog(entry("e3", YESTERDAY), emptyList())

        val today = dao.observeEntriesForDay("t1", TODAY).first()

        assertThat(today.map { it.entryId }).containsExactly("e1", "e2").inOrder()
    }

    @Test
    fun deletingEntryCascadesToValues() = runTest {
        dao.insertLog(entry("e1", TODAY), listOf(LogValueEntity("v1", "e1", "amount", valueNumber = 100.0)))

        dao.deleteEntry("e1")

        assertThat(dao.getEntry("e1")).isNull()
        assertThat(dao.getValues("e1")).isEmpty()
    }

    @Test
    fun replaceLog_swapsValuesWithoutLeavingStragglers() = runTest {
        dao.insertLog(entry("e1", TODAY), listOf(LogValueEntity("v1", "e1", "amount", valueNumber = 100.0)))

        dao.replaceLog(entry("e1", TODAY), listOf(LogValueEntity("v2", "e1", "amount", valueNumber = 250.0)))

        val values = dao.getValues("e1")
        assertThat(values).hasSize(1)
        assertThat(values.single().valueNumber).isEqualTo(250.0)
    }

    @Test
    fun observeLoggedQuantities_joinsGramsToItsProduct() = runTest {
        dao.insertLog(entry("e1", TODAY), listOf(LogValueEntity("v1", "e1", "amount", valueNumber = 150.0)))

        val quantities = dao.observeLoggedQuantities("s1", TODAY, "amount").first()

        val q = quantities.single()
        assertThat(q.productId).isEqualTo("p1")
        assertThat(q.grams).isEqualTo(150.0)
    }

    @Test
    fun macrosAreComputedFromTheJoinRatherThanStored() = runTest {
        dao.insertLog(entry("e1", TODAY), listOf(LogValueEntity("v1", "e1", "amount", valueNumber = 150.0)))

        val quantities = dao.observeLoggedQuantities("s1", TODAY, "amount").first()
        val nutrients = productDao.getNutrientsFor(quantities.mapNotNull { it.productId })
        val totals = MacroCalculator.total(
            quantities.map { MacroCalculator.Quantity(it.productId!!, it.grams) },
            nutrients.groupBy { it.productId },
        )

        assertThat(totals.energyKcal).isWithin(1e-9).of(444.0) // 296 * 1.5

        // The point of computing at read time: fixing the product fixes history.
        productDao.upsertNutrients(listOf(ProductNutrientEntity("n1", "p1", NutrientKeys.ENERGY_KCAL, 300.0)))
        val corrected = MacroCalculator.total(
            quantities.map { MacroCalculator.Quantity(it.productId!!, it.grams) },
            productDao.getNutrientsFor(listOf("p1")).groupBy { it.productId },
        )
        assertThat(corrected.energyKcal).isWithin(1e-9).of(450.0)
        // and the log itself never changed
        assertThat(dao.getValues("e1").single().valueNumber).isEqualTo(150.0)
    }

    @Test
    fun entryWithoutVariantIsAllowed() = runTest {
        // Not every activity references a product — only item_variant fields do.
        dao.insertLog(entry("e1", TODAY, product = null), emptyList())
        assertThat(dao.getEntry("e1")!!.variantRef).isNull()
    }

    @Test
    fun getLoggedDates_returnsDistinctDaysNewestFirst() = runTest {
        dao.insertLog(entry("e1", TODAY), emptyList())
        dao.insertLog(entry("e2", TODAY, at = 2L), emptyList())
        dao.insertLog(entry("e3", YESTERDAY), emptyList())

        assertThat(dao.getLoggedDates("t1")).containsExactly(TODAY, YESTERDAY).inOrder()
    }

    private companion object {
        const val TODAY = "2026-07-17"
        const val YESTERDAY = "2026-07-16"
    }
}
