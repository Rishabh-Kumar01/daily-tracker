package dev.rishabh.dailytracker.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Companion metadata for bundled generic foods.
 *
 * Sits alongside [ProductEntity] in the products table — every row here references exactly one
 * product. The extra columns (slug, category, source provenance, is_approx) live here rather than
 * being nullable columns on products, keeping the core product table clean for branded and
 * manually entered foods.
 *
 * Created by [dev.rishabh.dailytracker.core.db.seed.GenericFoodSeeder] and read by the meal
 * screen for category-based grouping.
 */
@Entity(
    tableName = "generic_food_meta",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["product_id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["product_id"], unique = true),
        Index(value = ["slug"], unique = true),
        Index(value = ["category"]),
        Index(value = ["dataset_version"]),
    ],
)
data class GenericFoodMetaEntity(
    @PrimaryKey @ColumnInfo(name = "product_id") val productId: String,
    /** URL-style identifier, e.g. "toor-dal-cooked". */
    @ColumnInfo(name = "slug") val slug: String,
    /**
     * Nullable category for group matching.
     *
     * "dal", "vegetables", "fruit", "oil" — foods with category=null are specific (matched
     * only by exact generic_name).
     */
    @ColumnInfo(name = "category") val category: String?,
    /** Preparation state shown to the user, e.g. "cooked", "raw", "roasted". */
    @ColumnInfo(name = "prep") val prep: String?,
    /** USDA form used ("cooked", "raw", etc.). */
    @ColumnInfo(name = "source_form") val sourceForm: String?,
    /** Source database: "usda_fdc", "ifct", etc. */
    @ColumnInfo(name = "source_db") val sourceDb: String?,
    /** Human-readable source reference, e.g. "FDC 173757". */
    @ColumnInfo(name = "source_ref") val sourceRef: String?,
    /** True when the value is a typical composite rather than a lab analysis. */
    @ColumnInfo(name = "is_approx") val isApprox: Boolean = false,
    /**
     * How this food is naturally logged: "grams" | "count" | "household". Drives which unit
     * the QuantitySheet opens in — the input is converted to grams for storage, so nutrients
     * and the read-time macro math are untouched. Nullable so an ALTER-added column and a
     * yet-to-be-backfilled row degrade to plain grams rather than crashing.
     */
    @ColumnInfo(name = "serving_unit") val servingUnit: String? = null,
    /** The unit noun shown in the picker, e.g. "egg", "katori", "tsp". Null for grams. */
    @ColumnInfo(name = "unit_label") val unitLabel: String? = null,
    /** Grams in one [unitLabel]; the input-to-storage conversion factor. Null for grams. */
    @ColumnInfo(name = "grams_per_unit") val gramsPerUnit: Double? = null,
    /** Dataset version this row was seeded from; used for idempotent re-seeding. */
    @ColumnInfo(name = "dataset_version") val datasetVersion: Int,
)
