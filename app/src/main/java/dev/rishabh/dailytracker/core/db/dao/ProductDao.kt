package dev.rishabh.dailytracker.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.rishabh.dailytracker.core.db.entity.ProductEntity
import dev.rishabh.dailytracker.core.db.entity.ProductNutrientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    // --- Reads ---

    @Query("SELECT * FROM products WHERE product_id = :productId")
    suspend fun getProduct(productId: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): ProductEntity?

    /**
     * Brand variants for an item, most recently used first.
     *
     * This is the frequency ranking the meal screen's inline expansion shows: the brand you
     * actually buy floats to the top without any explicit "favourite" concept.
     */
    @Query(
        """
        SELECT * FROM products
        WHERE generic_name = :genericName
        ORDER BY last_used_at DESC, product_name
        """,
    )
    fun observeByGenericName(genericName: String): Flow<List<ProductEntity>>

    @Query(
        """
        SELECT * FROM products
        WHERE generic_name = :genericName
        ORDER BY last_used_at DESC, product_name
        """,
    )
    suspend fun getByGenericName(genericName: String): List<ProductEntity>

    /**
     * Dedupe probe: normalised brand + product name.
     *
     * LOWER/TRIM here matches the fuzzy-match-on-save rule at the SQL level; anything
     * subtler than case and whitespace is the repository's job.
     */
    @Query(
        """
        SELECT * FROM products
        WHERE LOWER(TRIM(COALESCE(brand, ''))) = LOWER(TRIM(COALESCE(:brand, '')))
          AND LOWER(TRIM(product_name)) = LOWER(TRIM(:productName))
        LIMIT 1
        """,
    )
    suspend fun findDuplicate(brand: String?, productName: String): ProductEntity?

    /** My Foods, most recently used first. */
    @Query("SELECT * FROM products ORDER BY last_used_at DESC, product_name")
    fun observeAllProducts(): Flow<List<ProductEntity>>

    @Query(
        """
        SELECT * FROM products
        WHERE product_name LIKE '%' || :query || '%'
           OR brand LIKE '%' || :query || '%'
           OR generic_name LIKE '%' || :query || '%'
        ORDER BY last_used_at DESC, product_name
        """,
    )
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM product_nutrients WHERE product_id = :productId")
    suspend fun getNutrients(productId: String): List<ProductNutrientEntity>

    @Query("SELECT * FROM product_nutrients WHERE product_id = :productId")
    fun observeNutrients(productId: String): Flow<List<ProductNutrientEntity>>

    @Query("SELECT * FROM product_nutrients WHERE product_id IN (:productIds)")
    suspend fun getNutrientsFor(productIds: List<String>): List<ProductNutrientEntity>

    @Query("SELECT * FROM product_nutrients WHERE product_id IN (:productIds)")
    fun observeNutrientsFor(productIds: List<String>): Flow<List<ProductNutrientEntity>>

    /**
     * The whole nutrient table.
     *
     * The meal screen needs nutrients for an open-ended set of products (every brand of
     * every food in the meal, plus whatever is already logged). This is a single-user
     * library of hand-curated foods, so loading it whole is cheaper than fanning out a
     * per-product Flow and recombining.
     */
    @Query("SELECT * FROM product_nutrients")
    fun observeAllNutrients(): Flow<List<ProductNutrientEntity>>

    // --- Writes ---

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(product: ProductEntity)

    @Upsert
    suspend fun upsertProduct(product: ProductEntity)

    @Upsert
    suspend fun upsertNutrients(nutrients: List<ProductNutrientEntity>)

    /** Frequency ranking input; called when a product is actually logged. */
    @Query("UPDATE products SET last_used_at = :usedAt WHERE product_id = :productId")
    suspend fun touch(productId: String, usedAt: Long)

    /** A product without its nutrients is useless, so they commit together. */
    @Transaction
    suspend fun insertProductWithNutrients(
        product: ProductEntity,
        nutrients: List<ProductNutrientEntity>,
    ) {
        insertProduct(product)
        upsertNutrients(nutrients)
    }
}
