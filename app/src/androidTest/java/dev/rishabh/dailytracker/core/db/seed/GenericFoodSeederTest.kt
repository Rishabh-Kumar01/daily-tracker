package dev.rishabh.dailytracker.core.db.seed

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.rishabh.dailytracker.core.db.DailyTrackerDatabase
import dev.rishabh.dailytracker.core.db.FakeIdGenerator
import dev.rishabh.dailytracker.core.db.FakeTimeSource
import dev.rishabh.dailytracker.core.db.ProductSource
import dev.rishabh.dailytracker.core.db.dao.GenericFoodMetaDao
import dev.rishabh.dailytracker.core.db.dao.ProductDao
import dev.rishabh.dailytracker.core.db.entity.ProductEntity
import dev.rishabh.dailytracker.core.db.entity.ProductNutrientEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GenericFoodSeederTest {

    private lateinit var db: DailyTrackerDatabase
    private lateinit var productDao: ProductDao
    private lateinit var metaDao: GenericFoodMetaDao
    private lateinit var ids: FakeIdGenerator
    private lateinit var time: FakeTimeSource
    private lateinit var seeder: GenericFoodSeeder

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, DailyTrackerDatabase::class.java).build()
        productDao = db.productDao()
        metaDao = db.genericFoodMetaDao()
        ids = FakeIdGenerator()
        time = FakeTimeSource()
        seeder = GenericFoodSeeder(context, productDao, metaDao, ids, time)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun seedsTheBundledDatasetOnFirstRun() = runTest {
        val inserted = seeder.seedIfNeeded()

        assertThat(inserted).isGreaterThan(100)
        assertThat(productDao.observeAllProducts().first()).hasSize(inserted)
        assertThat(metaDao.getAll()).hasSize(inserted)
    }

    @Test
    fun seededFoodsAreGenericPer100gAndFromTheBundle() = runTest {
        seeder.seedIfNeeded()

        val products = productDao.observeAllProducts().first()
        // The core distinction: generic == brand null, never a separate column.
        assertThat(products.all { it.brand == null }).isTrue()
        assertThat(products.map { it.source }.toSet()).containsExactly(ProductSource.BUNDLED_GENERIC)
        assertThat(products.map { it.basis }.toSet()).containsExactly(ProductEntity.BASIS_PER_100G)
    }

    @Test
    fun aKnownStapleHasItsMacros() = runTest {
        seeder.seedIfNeeded()

        val egg = productDao.getByGenericName("eggs").first { it.productName.contains("boil", true) }
        val nutrients = productDao.getNutrients(egg.productId).associate { it.nutrientKey to it.amountPer100g }
        // Boiled egg is one of the acceptance foods; it must resolve energy at read time.
        assertThat(nutrients).containsKey("energy_kcal")
        assertThat(nutrients.getValue("energy_kcal")).isGreaterThan(0.0)
    }

    @Test
    fun seedingTwiceInsertsNothingTheSecondTime() = runTest {
        val first = seeder.seedIfNeeded()
        val idsAfterFirst = ids.issued()

        val second = seeder.seedIfNeeded()

        assertThat(second).isEqualTo(0)
        assertThat(productDao.observeAllProducts().first()).hasSize(first)
        // Not one ID burned on the idempotent pass.
        assertThat(ids.issued()).isEqualTo(idsAfterFirst)
    }

    @Test
    fun reSeedingNeverClobbersAUserEditToASeededFood() = runTest {
        seeder.seedIfNeeded()

        // Simulate the user correcting a seeded product's energy (as My Foods edit will).
        val paneer = productDao.getByGenericName("paneer").first()
        productDao.upsertNutrients(
            listOf(
                ProductNutrientEntity(
                    id = "${paneer.productId}:energy_kcal",
                    productId = paneer.productId,
                    nutrientKey = "energy_kcal",
                    amountPer100g = 111.0,
                ),
            ),
        )

        // A later launch re-runs the seeder; the edit must survive untouched.
        seeder.seedIfNeeded()

        val energy = productDao.getNutrients(paneer.productId)
            .single { it.nutrientKey == "energy_kcal" }.amountPer100g
        assertThat(energy).isEqualTo(111.0)
    }

    @Test
    fun seededFoodsCarryTheirServingUnit() = runTest {
        seeder.seedIfNeeded()

        val egg = metaDao.getAll().first { it.slug == "egg-boiled" }
        assertThat(egg.servingUnit).isEqualTo("count")
        assertThat(egg.unitLabel).isEqualTo("egg")
        assertThat(egg.gramsPerUnit).isEqualTo(50.0)

        val dal = metaDao.getAll().first { it.slug == "toor-dal-cooked" }
        assertThat(dal.servingUnit).isEqualTo("household")
        assertThat(dal.unitLabel).isEqualTo("katori")

        val paneer = metaDao.getAll().first { it.slug == "paneer" }
        assertThat(paneer.servingUnit).isEqualTo("grams")
        assertThat(paneer.unitLabel).isNull()
    }

    @Test
    fun backfillFillsRowsSeededBeforeServingUnitsExisted() = runTest {
        seeder.seedIfNeeded()

        // Simulate a pre-serving-unit row (what Migration 2→3 leaves until backfill runs).
        db.openHelper.writableDatabase.execSQL(
            "UPDATE generic_food_meta SET serving_unit = NULL, unit_label = NULL, grams_per_unit = NULL WHERE slug = 'egg-boiled'",
        )
        assertThat(metaDao.countMissingServing()).isEqualTo(1)

        // A later launch backfills it from the asset — metadata only, nutrients untouched.
        seeder.seedIfNeeded()

        val egg = metaDao.getAll().first { it.slug == "egg-boiled" }
        assertThat(egg.servingUnit).isEqualTo("count")
        assertThat(egg.gramsPerUnit).isEqualTo(50.0)
        assertThat(metaDao.countMissingServing()).isEqualTo(0)
    }

    @Test
    fun everySeededProductHasAMatchingMetaRow() = runTest {
        seeder.seedIfNeeded()

        val productIds = productDao.observeAllProducts().first().map { it.productId }.toSet()
        val metaProductIds = metaDao.getAll().map { it.productId }.toSet()
        assertThat(metaProductIds).isEqualTo(productIds)
        // Slugs are unique — the idempotency ledger.
        val slugs = metaDao.getAll().map { it.slug }
        assertThat(slugs).containsNoDuplicates()
    }
}
