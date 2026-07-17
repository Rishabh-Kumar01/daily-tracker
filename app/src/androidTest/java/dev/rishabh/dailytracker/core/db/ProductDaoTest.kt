package dev.rishabh.dailytracker.core.db

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
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
class ProductDaoTest {

    private lateinit var db: DailyTrackerDatabase
    private lateinit var dao: ProductDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DailyTrackerDatabase::class.java,
        ).build()
        dao = db.productDao()
    }

    @After
    fun tearDown() = db.close()

    private fun product(
        id: String,
        generic: String = "paneer",
        brand: String? = "Amul",
        name: String = "Malai Paneer",
        barcode: String? = null,
        lastUsed: Long? = null,
    ) = ProductEntity(
        productId = id,
        genericName = generic,
        brand = brand,
        productName = name,
        barcode = barcode,
        source = ProductSource.MANUAL,
        createdAt = 1L,
        lastUsedAt = lastUsed,
    )

    @Test
    fun insertProductWithNutrients_roundTrips() = runTest {
        val p = product("p1")
        val nutrients = listOf(
            ProductNutrientEntity("n1", "p1", NutrientKeys.ENERGY_KCAL, 296.0, confidence = 0.95),
            ProductNutrientEntity("n2", "p1", NutrientKeys.PROTEIN_G, 18.5, confidence = 0.95),
        )

        dao.insertProductWithNutrients(p, nutrients)

        assertThat(dao.getProduct("p1")).isEqualTo(p)
        assertThat(dao.getNutrients("p1")).containsExactlyElementsIn(nutrients)
        // Ingestion normalises everything to per-100g.
        assertThat(dao.getProduct("p1")!!.basis).isEqualTo(ProductEntity.BASIS_PER_100G)
    }

    @Test
    fun observeByGenericName_ranksByRecentUseSoTheBrandYouBuyFloatsUp() = runTest {
        dao.insertProduct(product("p1", brand = "Amul", name = "Malai Paneer", lastUsed = 100L))
        dao.insertProduct(product("p2", brand = "Mother Dairy", name = "Paneer", lastUsed = 300L))
        dao.insertProduct(product("p3", brand = "iD Fresh", name = "Paneer", lastUsed = 200L))
        dao.insertProduct(product("p4", generic = "dal", brand = "X", name = "Toor Dal", lastUsed = 999L))

        val paneers = dao.observeByGenericName("paneer").first()

        assertThat(paneers.map { it.brand }).containsExactly("Mother Dairy", "iD Fresh", "Amul").inOrder()
    }

    @Test
    fun touch_promotesAProductInTheRanking() = runTest {
        dao.insertProduct(product("p1", brand = "Amul", lastUsed = 100L))
        dao.insertProduct(product("p2", brand = "Mother Dairy", name = "Paneer", lastUsed = 300L))

        dao.touch("p1", 500L)

        assertThat(dao.observeByGenericName("paneer").first().map { it.brand })
            .containsExactly("Amul", "Mother Dairy").inOrder()
    }

    @Test
    fun neverUsedProductsSortAfterUsedOnes() = runTest {
        dao.insertProduct(product("p1", brand = "Never", name = "Unused Paneer", lastUsed = null))
        dao.insertProduct(product("p2", brand = "Amul", lastUsed = 1L))

        assertThat(dao.observeByGenericName("paneer").first().first().brand).isEqualTo("Amul")
    }

    @Test
    fun findByBarcode_locatesTheProduct() = runTest {
        dao.insertProduct(product("p1", barcode = "8901262010177"))

        assertThat(dao.findByBarcode("8901262010177")!!.productId).isEqualTo("p1")
        assertThat(dao.findByBarcode("0000000000000")).isNull()
    }

    @Test(expected = SQLiteConstraintException::class)
    fun barcodeIsUniqueWhenPresent() = runTest {
        dao.insertProduct(product("p1", barcode = "8901262010177"))
        dao.insertProduct(product("p2", name = "Other", barcode = "8901262010177"))
    }

    @Test
    fun manyProductsMayHaveNoBarcode() = runTest {
        // SQLite treats NULLs as distinct, so the unique index must not collide manual entries.
        dao.insertProduct(product("p1", name = "Home Paneer", barcode = null))
        dao.insertProduct(product("p2", name = "Other Paneer", barcode = null))
        dao.insertProduct(product("p3", name = "Third Paneer", barcode = null))

        assertThat(dao.observeAllProducts().first()).hasSize(3)
    }

    @Test
    fun findDuplicate_ignoresCaseAndSurroundingWhitespace() = runTest {
        dao.insertProduct(product("p1", brand = "Amul", name = "Malai Paneer"))

        assertThat(dao.findDuplicate("  amul ", "malai paneer")!!.productId).isEqualTo("p1")
        assertThat(dao.findDuplicate("AMUL", "MALAI PANEER")!!.productId).isEqualTo("p1")
        assertThat(dao.findDuplicate("Amul", "Tofu")).isNull()
        assertThat(dao.findDuplicate("Britannia", "Malai Paneer")).isNull()
    }

    @Test
    fun findDuplicate_handlesBrandlessProducts() = runTest {
        dao.insertProduct(product("p1", brand = null, name = "Home Paneer"))

        // COALESCE keeps null brands comparable rather than silently never matching.
        assertThat(dao.findDuplicate(null, "home paneer")!!.productId).isEqualTo("p1")
    }

    @Test
    fun upsertNutrients_correctsInPlaceRatherThanDuplicating() = runTest {
        dao.insertProductWithNutrients(
            product("p1"),
            listOf(ProductNutrientEntity("n1", "p1", NutrientKeys.ENERGY_KCAL, 296.0)),
        )

        dao.upsertNutrients(listOf(ProductNutrientEntity("n1", "p1", NutrientKeys.ENERGY_KCAL, 300.0)))

        val nutrients = dao.getNutrients("p1")
        assertThat(nutrients).hasSize(1)
        assertThat(nutrients.single().amountPer100g).isEqualTo(300.0)
    }

    @Test
    fun searchProducts_matchesBrandNameOrGeneric() = runTest {
        dao.insertProduct(product("p1", brand = "Amul", name = "Malai Paneer", generic = "paneer"))
        dao.insertProduct(product("p2", brand = "Britannia", name = "Cheese Slices", generic = "cheese"))

        assertThat(dao.searchProducts("Amul").first().map { it.productId }).containsExactly("p1")
        assertThat(dao.searchProducts("Cheese").first().map { it.productId }).containsExactly("p2")
        assertThat(dao.searchProducts("paneer").first().map { it.productId }).containsExactly("p1")
    }

    @Test
    fun deletingProductCascadesToItsNutrients() = runTest {
        dao.insertProductWithNutrients(
            product("p1"),
            listOf(ProductNutrientEntity("n1", "p1", NutrientKeys.ENERGY_KCAL, 296.0)),
        )

        db.openHelper.writableDatabase.execSQL("DELETE FROM products WHERE product_id = 'p1'")

        assertThat(dao.getNutrients("p1")).isEmpty()
    }

    @Test
    fun lowConfidenceNutrientsRoundTripForTheUiToFlag() = runTest {
        dao.insertProductWithNutrients(
            product("p1"),
            listOf(ProductNutrientEntity("n1", "p1", NutrientKeys.ENERGY_KCAL, 296.0, confidence = 0.42)),
        )

        assertThat(dao.getNutrients("p1").single().confidence).isEqualTo(0.42)
    }
}
