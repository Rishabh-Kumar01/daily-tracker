package dev.rishabh.dailytracker.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.rishabh.dailytracker.core.db.ProductSource

/** The "My Foods" library. */
@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = MediaEntity::class,
            parentColumns = ["media_id"],
            childColumns = ["front_photo_ref"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = MediaEntity::class,
            parentColumns = ["media_id"],
            childColumns = ["back_photo_ref"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        // Groups brand variants under one item ("paneer" -> Amul / Mother Dairy / ...).
        Index("generic_name"),
        // Unique only where present: most manual products have no barcode, and SQLite
        // treats NULLs as distinct, so many null-barcode rows coexist fine.
        Index(value = ["barcode"], unique = true),
        Index("last_used_at"),
        Index("front_photo_ref"),
        Index("back_photo_ref"),
    ],
)
data class ProductEntity(
    @PrimaryKey @ColumnInfo(name = "product_id") val productId: String,
    /** "paneer" — the grouping key that ties brand variants to an item. */
    @ColumnInfo(name = "generic_name") val genericName: String,
    /** "Amul" */
    @ColumnInfo(name = "brand") val brand: String?,
    /** "Malai Paneer" */
    @ColumnInfo(name = "product_name") val productName: String,
    /** Pack size / flavour. */
    @ColumnInfo(name = "variant") val variant: String? = null,
    @ColumnInfo(name = "barcode") val barcode: String? = null,
    @ColumnInfo(name = "source") val source: ProductSource,
    @ColumnInfo(name = "front_photo_ref") val frontPhotoRef: String? = null,
    @ColumnInfo(name = "back_photo_ref") val backPhotoRef: String? = null,
    /**
     * Always "per_100g" after ingestion normalisation. Labels read per-serving or
     * per-pack are converted on the way in, and the original basis is recorded so the
     * conversion stays auditable.
     */
    @ColumnInfo(name = "basis") val basis: String = BASIS_PER_100G,
    @ColumnInfo(name = "serving_size_g") val servingSizeG: Double? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    /** Drives frequency ranking in the meal screen. */
    @ColumnInfo(name = "last_used_at") val lastUsedAt: Long? = null,
) {
    companion object {
        const val BASIS_PER_100G = "per_100g"
    }
}

/**
 * Long format so any micronutrient is storable without a migration.
 *
 * Amounts are always per 100g (see ProductEntity.basis). The Diet UI multiplies by
 * grams/100 at read time; it never writes the product of that back.
 */
@Entity(
    tableName = "product_nutrients",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["product_id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("product_id"),
        // One row per nutrient per product; upserts replace rather than duplicate.
        Index(value = ["product_id", "nutrient_key"], unique = true),
    ],
)
data class ProductNutrientEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "product_id") val productId: String,
    /** energy_kcal, protein_g, carbs_g, fat_g, fiber_g, iron_mg, ... (open set). */
    @ColumnInfo(name = "nutrient_key") val nutrientKey: String,
    @ColumnInfo(name = "amount_per_100g") val amountPer100g: Double,
    /** Extraction confidence 0-1; the UI flags anything below 0.8. */
    @ColumnInfo(name = "confidence") val confidence: Double? = null,
)
