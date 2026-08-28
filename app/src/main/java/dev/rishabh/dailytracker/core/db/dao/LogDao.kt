package dev.rishabh.dailytracker.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.rishabh.dailytracker.core.db.entity.LogEntryEntity
import dev.rishabh.dailytracker.core.db.entity.LogValueEntity
import kotlinx.coroutines.flow.Flow

/**
 * A logged quantity plus the product it referred to.
 *
 * This is the read-time join the macro computation runs on: grams come from log_values,
 * the nutrients come from the product, and the product of the two is never stored.
 */
data class LoggedQuantity(
    val entryId: String,
    val itemId: String,
    val productId: String?,
    val fieldKey: String,
    val grams: Double?,
)

@Dao
interface LogDao {

    // --- Reads ---

    @Query("SELECT * FROM log_entries WHERE entry_id = :entryId")
    suspend fun getEntry(entryId: String): LogEntryEntity?

    /** Day view for one activity — hits the (template_id, local_date) composite index. */
    @Query(
        """
        SELECT * FROM log_entries
        WHERE template_id = :templateId AND local_date = :localDate
        ORDER BY logged_at
        """,
    )
    fun observeEntriesForDay(templateId: String, localDate: String): Flow<List<LogEntryEntity>>

    @Query(
        """
        SELECT * FROM log_entries
        WHERE template_id = :templateId AND local_date = :localDate
        ORDER BY logged_at
        """,
    )
    suspend fun getEntriesForDay(templateId: String, localDate: String): List<LogEntryEntity>

    /** Whole-day view across every activity — powers Home's today summaries. */
    @Query("SELECT * FROM log_entries WHERE local_date = :localDate ORDER BY logged_at")
    fun observeEntriesForDayAllActivities(localDate: String): Flow<List<LogEntryEntity>>

    @Query(
        """
        SELECT * FROM log_entries
        WHERE sub_menu_id = :subMenuId AND local_date = :localDate
        ORDER BY logged_at
        """,
    )
    fun observeEntriesForSubMenuDay(subMenuId: String, localDate: String): Flow<List<LogEntryEntity>>

    @Query(
        """
        SELECT * FROM log_entries
        WHERE sub_menu_id = :subMenuId AND local_date = :localDate
        ORDER BY logged_at
        """,
    )
    suspend fun getEntriesForSubMenuDay(subMenuId: String, localDate: String): List<LogEntryEntity>

    @Query("SELECT * FROM log_values WHERE entry_id = :entryId")
    suspend fun getValues(entryId: String): List<LogValueEntity>

    @Query("SELECT * FROM log_values WHERE entry_id IN (:entryIds)")
    suspend fun getValuesFor(entryIds: List<String>): List<LogValueEntity>

    @Query("SELECT * FROM log_values WHERE entry_id IN (:entryIds)")
    fun observeValuesFor(entryIds: List<String>): Flow<List<LogValueEntity>>

    /**
     * Every quantity logged for a sub-menu on a day, with its product reference.
     *
     * Deliberately returns grams rather than macros — see LoggedQuantity.
     */
    @Query(
        """
        SELECT e.entry_id AS entryId,
               e.item_id AS itemId,
               e.variant_ref AS productId,
               v.field_key AS fieldKey,
               v.value_number AS grams
        FROM log_entries e
        INNER JOIN log_values v ON v.entry_id = e.entry_id
        WHERE e.sub_menu_id = :subMenuId
          AND e.local_date = :localDate
          AND v.field_key = :fieldKey
        """,
    )
    fun observeLoggedQuantities(
        subMenuId: String,
        localDate: String,
        fieldKey: String,
    ): Flow<List<LoggedQuantity>>

    @Query(
        """
        SELECT e.entry_id AS entryId,
               e.item_id AS itemId,
               e.variant_ref AS productId,
               v.field_key AS fieldKey,
               v.value_number AS grams
        FROM log_entries e
        INNER JOIN log_values v ON v.entry_id = e.entry_id
        WHERE e.template_id = :templateId
          AND e.local_date = :localDate
          AND v.field_key = :fieldKey
        """,
    )
    fun observeLoggedQuantitiesForDay(
        templateId: String,
        localDate: String,
        fieldKey: String,
    ): Flow<List<LoggedQuantity>>

    /**
     * Every quantity logged against a product for a sub-menu on a day.
     *
     * Which value is "the quantity" comes from item_fields.type, not a hardcoded field key,
     * so this keeps working for any template that follows the variant + quantity pattern.
     * Entries without a product contribute no nutrients, so they are filtered out here
     * rather than being carried to the calculator to be dropped.
     */
    @Query(
        """
        SELECT e.entry_id AS entryId,
               e.item_id AS itemId,
               e.variant_ref AS productId,
               v.field_key AS fieldKey,
               v.value_number AS grams
        FROM log_entries e
        INNER JOIN log_values v ON v.entry_id = e.entry_id
        INNER JOIN item_fields f ON f.item_id = e.item_id AND f.field_key = v.field_key
        WHERE e.sub_menu_id = :subMenuId
          AND e.local_date = :localDate
          AND f.type = 'quantity'
          AND e.variant_ref IS NOT NULL
        """,
    )
    fun observeProductQuantitiesForSubMenuDay(
        subMenuId: String,
        localDate: String,
    ): Flow<List<LoggedQuantity>>

    /** The one-shot snapshot version, used when saving the day's meal as a template. */
    @Query(
        """
        SELECT e.entry_id AS entryId,
               e.item_id AS itemId,
               e.variant_ref AS productId,
               v.field_key AS fieldKey,
               v.value_number AS grams
        FROM log_entries e
        INNER JOIN log_values v ON v.entry_id = e.entry_id
        INNER JOIN item_fields f ON f.item_id = e.item_id AND f.field_key = v.field_key
        WHERE e.sub_menu_id = :subMenuId
          AND e.local_date = :localDate
          AND f.type = 'quantity'
          AND e.variant_ref IS NOT NULL
        """,
    )
    suspend fun getProductQuantitiesForSubMenuDay(
        subMenuId: String,
        localDate: String,
    ): List<LoggedQuantity>

    /** The whole-activity equivalent — powers Home's Diet summary. */
    @Query(
        """
        SELECT e.entry_id AS entryId,
               e.item_id AS itemId,
               e.variant_ref AS productId,
               v.field_key AS fieldKey,
               v.value_number AS grams
        FROM log_entries e
        INNER JOIN log_values v ON v.entry_id = e.entry_id
        INNER JOIN item_fields f ON f.item_id = e.item_id AND f.field_key = v.field_key
        WHERE e.template_id = :templateId
          AND e.local_date = :localDate
          AND f.type = 'quantity'
          AND e.variant_ref IS NOT NULL
        """,
    )
    fun observeProductQuantitiesForDay(
        templateId: String,
        localDate: String,
    ): Flow<List<LoggedQuantity>>

    @Query("SELECT COUNT(*) FROM log_entries WHERE template_id = :templateId AND local_date = :localDate")
    fun observeEntryCountForDay(templateId: String, localDate: String): Flow<Int>

    /** Distinct days with any entry — calendar and streaks read this. */
    @Query(
        """
        SELECT DISTINCT local_date FROM log_entries
        WHERE template_id = :templateId
        ORDER BY local_date DESC
        """,
    )
    suspend fun getLoggedDates(templateId: String): List<String>

    // --- Writes ---

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntry(entry: LogEntryEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertValues(values: List<LogValueEntity>)

    /** An entry and its field values are one user action, so they commit together. */
    @Transaction
    suspend fun insertLog(entry: LogEntryEntity, values: List<LogValueEntity>) {
        insertEntry(entry)
        insertValues(values)
    }

    /** Cascades to log_values via the foreign key. */
    @Query("DELETE FROM log_entries WHERE entry_id = :entryId")
    suspend fun deleteEntry(entryId: String)

    @Transaction
    suspend fun replaceLog(entry: LogEntryEntity, values: List<LogValueEntity>) {
        deleteEntry(entry.entryId)
        insertEntry(entry)
        insertValues(values)
    }

    /**
     * Logs a whole meal in one transaction: the portions it replaces are removed and every
     * new entry with its values is inserted together.
     *
     * Used by one-tap template logging — a template must land as all-or-nothing, never as a
     * half-logged meal if the app is killed mid-write.
     */
    @Transaction
    suspend fun replaceLogs(
        entryIdsToDelete: List<String>,
        entries: List<LogEntryEntity>,
        values: List<LogValueEntity>,
    ) {
        entryIdsToDelete.forEach { deleteEntry(it) }
        entries.forEach { insertEntry(it) }
        insertValues(values)
    }
}
