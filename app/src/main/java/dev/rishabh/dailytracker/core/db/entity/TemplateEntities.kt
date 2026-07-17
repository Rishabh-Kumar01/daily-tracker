package dev.rishabh.dailytracker.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.rishabh.dailytracker.core.db.CreatedBy
import dev.rishabh.dailytracker.core.db.VariantSource

/*
 * Template side: the definitions that make activities data rather than code.
 *
 * activity_template -> sub_menus -> items -> item_fields. A new activity is new rows here
 * and nothing else: no new table, no new screen.
 */

@Entity(tableName = "activity_templates")
data class ActivityTemplateEntity(
    @PrimaryKey @ColumnInfo(name = "template_id") val templateId: String,
    @ColumnInfo(name = "name") val name: String,
    /** Icon key, resolved by the UI (design system uses Material Symbols names). */
    @ColumnInfo(name = "icon") val icon: String,
    /** Hex colour. The UI maps built-ins onto the four activity accents. */
    @ColumnInfo(name = "color") val color: String,
    @ColumnInfo(name = "created_by") val createdBy: CreatedBy,
    /** Free string, not a closed enum — see SummaryMetricTypes. */
    @ColumnInfo(name = "summary_metric_type") val summaryMetricType: String?,
    @ColumnInfo(name = "summary_metric_label") val summaryMetricLabel: String?,
    /** Template format version, starts at 1. */
    @ColumnInfo(name = "schema_version") val schemaVersion: Int = 1,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "sub_menus",
    foreignKeys = [
        ForeignKey(
            entity = ActivityTemplateEntity::class,
            parentColumns = ["template_id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("template_id")],
)
data class SubMenuEntity(
    @PrimaryKey @ColumnInfo(name = "sub_menu_id") val subMenuId: String,
    @ColumnInfo(name = "template_id") val templateId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    /**
     * `{type: daily/interval_days/weekly/none, every?, day?, reminder_time?}`.
     * Stays JSON: schedules are irregular and normalising them buys nothing.
     */
    @ColumnInfo(name = "schedule_json") val scheduleJson: String?,
)

@Entity(
    tableName = "items",
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
data class ItemEntity(
    @PrimaryKey @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "sub_menu_id") val subMenuId: String,
    @ColumnInfo(name = "name") val name: String,
    /** The food -> brand -> product pattern; drives inline BrandPickerRow expansion. */
    @ColumnInfo(name = "has_variants") val hasVariants: Boolean = false,
    @ColumnInfo(name = "variant_source") val variantSource: VariantSource? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
)

@Entity(
    tableName = "item_fields",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["item_id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("item_id")],
)
data class ItemFieldEntity(
    @PrimaryKey @ColumnInfo(name = "field_id") val fieldId: String,
    @ColumnInfo(name = "item_id") val itemId: String,
    /** Stable key referenced by log_values.field_key. */
    @ColumnInfo(name = "field_key") val fieldKey: String,
    /**
     * Raw wire value of the closed field-type vocabulary, resolved via FieldType.fromWire.
     * Stored as TEXT so an imported template carrying an unknown type renders as an
     * "unsupported field" card instead of crashing the query.
     */
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "label") val label: String,
    /** From the unit whitelist (g, ml, kg, mg, min, drops, reps, ...). */
    @ColumnInfo(name = "unit") val unit: String?,
    @ColumnInfo(name = "required") val required: Boolean = false,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    /**
     * Type-specific options: min/max/step/default, select options, comparison_series,
     * overlay_ghost_of_last, timer_ui. JSON per the schema — not normalised.
     */
    @ColumnInfo(name = "options_json") val optionsJson: String?,
)
