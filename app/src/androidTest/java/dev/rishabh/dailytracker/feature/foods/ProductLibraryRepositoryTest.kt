package dev.rishabh.dailytracker.feature.foods

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.rishabh.dailytracker.core.db.DailyTrackerDatabase
import dev.rishabh.dailytracker.core.db.MediaType
import dev.rishabh.dailytracker.core.db.NutrientKeys
import dev.rishabh.dailytracker.core.db.ProductSource
import dev.rishabh.dailytracker.core.db.dao.ProductDao
import dev.rishabh.dailytracker.core.db.entity.MediaEntity
import dev.rishabh.dailytracker.core.db.entity.ProductEntity
import dev.rishabh.dailytracker.core.db.entity.ProductNutrientEntity
import dev.rishabh.dailytracker.feature.diet.ValidatedProduct
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The My Foods lifecycle against a real database, verifying the three rules that bite:
 * an edit corrects in place (never duplicates), archiving hides a product from every picker
 * yet keeps it resolvable for logged history, and correcting nutrients is read-time.
 */
@RunWith(AndroidJUnit4::class)
class ProductLibraryRepositoryTest {

    private lateinit var db: DailyTrackerDatabase
    private lateinit var dao: ProductDao
    private lateinit var repo: ProductLibraryRepository

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DailyTrackerDatabase::class.java,
        ).build()
        dao = db.productDao()
        repo = ProductLibraryRepository(dao, db.mediaDao())
        dao.insertProductWithNutrients(
            ProductEntity(
                productId = "p1", genericName = "paneer", brand = "Amul", productName = "Malai Paneer",
                source = ProductSource.BARCODE_LOOKUP, createdAt = 1L, lastUsedAt = 10L,
            ),
            listOf(
                ProductNutrientEntity("p1:energy_kcal", "p1", NutrientKeys.ENERGY_KCAL, 296.0),
                ProductNutrientEntity("p1:protein_g", "p1", NutrientKeys.PROTEIN_G, 18.5),
                ProductNutrientEntity("p1:calcium_mg", "p1", NutrientKeys.CALCIUM_MG, 480.0),
            ),
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun libraryShowsProductWithItsMacroLine() = runTest {
        val cards = repo.observeLibrary("").first()
        assertThat(cards).hasSize(1)
        val card = cards.single()
        assertThat(card.brand).isEqualTo("Amul")
        assertThat(card.isGeneric).isFalse()
        assertThat(card.per100g[NutrientKeys.ENERGY_KCAL]).isEqualTo(296.0)
    }

    @Test
    fun searchMatchesNameBrandAndGeneric() = runTest {
        assertThat(repo.observeLibrary("malai").first()).hasSize(1)
        assertThat(repo.observeLibrary("amul").first()).hasSize(1)
        assertThat(repo.observeLibrary("paneer").first()).hasSize(1)
        assertThat(repo.observeLibrary("zzz").first()).isEmpty()
    }

    @Test
    fun editCorrectsInPlaceAndNeverDuplicates() = runTest {
        repo.updateProduct(
            "p1",
            ValidatedProduct(
                brand = "Amul",
                productName = "Malai Paneer",
                nutrients = mapOf(NutrientKeys.ENERGY_KCAL to 250.0, NutrientKeys.PROTEIN_G to 20.0),
            ),
        )

        // Still one product, same id — a correction, not a new row.
        assertThat(dao.observeAllProducts().first()).hasSize(1)
        val nutrients = dao.getNutrients("p1").associate { it.nutrientKey to it.amountPer100g }
        assertThat(nutrients[NutrientKeys.ENERGY_KCAL]).isEqualTo(250.0)
        assertThat(nutrients[NutrientKeys.PROTEIN_G]).isEqualTo(20.0)
        // A micronutrient the form doesn't show is preserved across the edit.
        assertThat(nutrients[NutrientKeys.CALCIUM_MG]).isEqualTo(480.0)
    }

    @Test
    fun blankingAMacroRemovesItButKeepsMicros() = runTest {
        // Edit omits protein (blanked in the form) — it should be deleted, calcium untouched.
        repo.updateProduct(
            "p1",
            ValidatedProduct(
                brand = "Amul", productName = "Malai Paneer",
                nutrients = mapOf(NutrientKeys.ENERGY_KCAL to 296.0),
            ),
        )
        val keys = dao.getNutrients("p1").map { it.nutrientKey }
        assertThat(keys).doesNotContain(NutrientKeys.PROTEIN_G)
        assertThat(keys).contains(NutrientKeys.CALCIUM_MG)
    }

    @Test
    fun archiveHidesFromPickersButKeepsHistoryResolvable() = runTest {
        repo.archive("p1")

        // Gone from the library, search, and the generic-name picker...
        assertThat(repo.observeLibrary("").first()).isEmpty()
        assertThat(dao.searchProducts("paneer").first()).isEmpty()
        assertThat(dao.observeByGenericName("paneer").first()).isEmpty()
        // ...but a logged day can still resolve it by id — never orphaned.
        assertThat(dao.getProduct("p1")).isNotNull()
    }

    @Test
    fun libraryCardsCarryTheirPhotoPath() = runTest {
        db.mediaDao().insert(MediaEntity("m1", "/photos/front.jpg", MediaType.PRODUCT_FRONT, createdAt = 1L))
        dao.setFrontPhoto("p1", "m1")

        assertThat(repo.observeLibrary("").first().single().photoPath).isEqualTo("/photos/front.jpg")
    }
}
