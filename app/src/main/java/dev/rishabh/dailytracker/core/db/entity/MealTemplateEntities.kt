package dev.rishabh.dailytracker.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Meal templates: a saved set of logged foods ("my usual breakfast") for one-tap logging.
 *
 * Deliberately a thin shortcut, not a second log: a template records which product and how
 * many grams for each item slot, and logging one writes ordinary log_entries/log_values so
 * calendar, totals and export never learn a new shape. Nutrition is still computed at read
 * time from the products the template points at, so a template auto-tracks a corrected label.
 */

@Entity(
    tableName = "meal_templates",
    foreignKeys = [
        ForeignKey(
            entity = SubMenuEntity::class,
            parentColumns = ["sub_menu_id"],
            childColumns = ["sub_menu_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sub_menu_id")],
)
data class MealTemplateEntity(
    @PrimaryKey @ColumnInfo(name = "meal_template_id") val mealTemplateId: String,
    /** The meal this template belongs to — breakfast templates show on the breakfast screen. */
    @ColumnInfo(name = "sub_menu_id") val subMenuId: String,
    /** User-given label, e.g. "My usual breakfast". */
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/**
 * One food line of a template: the item slot, the product picked for it, and the portion.
 *
 * Grams is the stored basis exactly as in the log — the input-unit layer is a UI concern.
 * The product FK is NO_ACTION because products are archived, never hard-deleted, so the
 * reference always resolves.
 */
@Entity(
    tableName = "meal_template_items",
    foreignKeys = [
        ForeignKey(
            entity = MealTemplateEntity::class,
            parentColumns = ["meal_template_id"],
            childColumns = ["meal_template_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["item_id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["product_id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index("meal_template_id"), Index("item_id"), Index("product_id")],
)
data class MealTemplateItemEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "meal_template_id") val mealTemplateId: String,
    @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "grams") val grams: Double,
)
