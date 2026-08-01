package dev.rishabh.dailytracker.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.rishabh.dailytracker.core.db.entity.GenericFoodMetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GenericFoodMetaDao {

    // --- Reads ---

    /** All generic food metadata, for category-based meal grouping. */
    @Query("SELECT * FROM generic_food_meta")
    fun observeAll(): Flow<List<GenericFoodMetaEntity>>

    /** All generic food metadata, suspend variant. */
    @Query("SELECT * FROM generic_food_meta")
    suspend fun getAll(): List<GenericFoodMetaEntity>

    /** Metadata for a specific product. */
    @Query("SELECT * FROM generic_food_meta WHERE product_id = :productId")
    suspend fun getByProductId(productId: String): GenericFoodMetaEntity?

    /** Check whether a slug has already been seeded. */
    @Query("SELECT EXISTS(SELECT 1 FROM generic_food_meta WHERE slug = :slug LIMIT 1)")
    suspend fun hasSlug(slug: String): Boolean

    /** Highest dataset_version currently in the table. 0 if empty. */
    @Query("SELECT COALESCE(MAX(dataset_version), 0) FROM generic_food_meta")
    suspend fun currentDatasetVersion(): Int

    /** Rows seeded before serving units existed (Migration 2→3 added the columns as null). */
    @Query("SELECT COUNT(*) FROM generic_food_meta WHERE serving_unit IS NULL")
    suspend fun countMissingServing(): Int

    // --- Writes ---

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(meta: GenericFoodMetaEntity)

    /**
     * Backfills serving metadata for a slug, but only where it is still null — so it fills
     * rows seeded before the feature and never overwrites one already set. Serving unit is
     * seed metadata, not user-edited content, so this is safe under the no-clobber rule.
     */
    @Query(
        """
        UPDATE generic_food_meta
        SET serving_unit = :servingUnit, unit_label = :unitLabel, grams_per_unit = :gramsPerUnit
        WHERE slug = :slug AND serving_unit IS NULL
        """,
    )
    suspend fun backfillServing(slug: String, servingUnit: String, unitLabel: String?, gramsPerUnit: Double?)

    /** Delete all metadata rows. Does NOT cascade to products — caller must clean those up. */
    @Query("DELETE FROM generic_food_meta")
    suspend fun deleteAll()
}
