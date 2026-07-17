package dev.rishabh.dailytracker.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Log side: the universal event shape.
 *
 * Invariant: every user action in every activity becomes log_entries + log_values.
 * Calendar, streaks, export and the correlation engine are written once against this.
 */

@Entity(
    tableName = "log_entries",
    foreignKeys = [
        ForeignKey(
            entity = ActivityTemplateEntity::class,
            parentColumns = ["template_id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = SubMenuEntity::class,
            parentColumns = ["sub_menu_id"],
            childColumns = ["sub_menu_id"],
            onDelete = ForeignKey.NO_ACTION,
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
            childColumns = ["variant_ref"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        // Day views query by (template, date); this composite is the hot path.
        Index("template_id", "local_date"),
        Index("local_date"),
        Index("sub_menu_id"),
        Index("item_id"),
        Index("variant_ref"),
    ],
)
data class LogEntryEntity(
    @PrimaryKey @ColumnInfo(name = "entry_id") val entryId: String,
    @ColumnInfo(name = "template_id") val templateId: String,
    @ColumnInfo(name = "sub_menu_id") val subMenuId: String,
    @ColumnInfo(name = "item_id") val itemId: String,
    /** Epoch millis UTC. */
    @ColumnInfo(name = "logged_at") val loggedAt: Long,
    /** Denormalised YYYY-MM-DD in the device timezone, indexed for day queries. */
    @ColumnInfo(name = "local_date") val localDate: String,
    /** Which product was actually logged, for item_variant items. */
    @ColumnInfo(name = "variant_ref") val variantRef: String? = null,
)

/**
 * One row per field per entry, with sparse value columns by type.
 *
 * Nutrition is never stored here: a Diet entry records grams, and macros are computed at
 * read time from product_nutrients so correcting a product retroactively fixes history.
 */
@Entity(
    tableName = "log_values",
    foreignKeys = [
        ForeignKey(
            entity = LogEntryEntity::class,
            parentColumns = ["entry_id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("entry_id")],
)
data class LogValueEntity(
    @PrimaryKey @ColumnInfo(name = "value_id") val valueId: String,
    @ColumnInfo(name = "entry_id") val entryId: String,
    /** Matches item_fields.field_key. */
    @ColumnInfo(name = "field_key") val fieldKey: String,
    /** quantity / scale / duration / time-as-minutes */
    @ColumnInfo(name = "value_number") val valueNumber: Double? = null,
    /** note / select ids */
    @ColumnInfo(name = "value_text") val valueText: String? = null,
    /** checkbox */
    @ColumnInfo(name = "value_bool") val valueBool: Boolean? = null,
    /** set_group arrays, multi_select */
    @ColumnInfo(name = "value_json") val valueJson: String? = null,
)
