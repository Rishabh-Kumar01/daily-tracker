package dev.rishabh.dailytracker.feature.activities

import dev.rishabh.dailytracker.core.common.IdGenerator
import dev.rishabh.dailytracker.core.common.TimeSource
import dev.rishabh.dailytracker.core.db.FieldType
import dev.rishabh.dailytracker.core.db.dao.LogDao
import dev.rishabh.dailytracker.core.db.dao.TemplateDao
import dev.rishabh.dailytracker.core.db.entity.ItemFieldEntity
import dev.rishabh.dailytracker.core.db.entity.LogEntryEntity
import dev.rishabh.dailytracker.core.db.entity.LogValueEntity
import dev.rishabh.dailytracker.core.designsystem.accentKeyForColor
import dev.rishabh.dailytracker.core.designsystem.component.model.LogValueDraft
import dev.rishabh.dailytracker.core.designsystem.component.model.formatSetsRecall
import dev.rishabh.dailytracker.core.designsystem.component.model.parseSetRows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generic logging for a non-variant sub-menu: read the items and their fields, what has been
 * logged today, and a recall of the last session — then write it back as ordinary
 * log_entries/log_values.
 *
 * This is the "activities are data" write path for everything that isn't the Diet meal flow.
 * Workout is its first user; Study and Sleep join as their milestones land, with no new
 * tables or screens.
 */
@Singleton
class ItemLogRepository @Inject constructor(
    private val templateDao: TemplateDao,
    private val logDao: LogDao,
    private val ids: IdGenerator,
    private val time: TimeSource,
) {

    /**
     * The sub-menu's items with today's values and last-session recall, live off the log.
     *
     * Re-emits whenever today's entries change, so a save elsewhere (or a fresh day) is
     * reflected without the screen polling.
     */
    fun observeSubMenuLog(subMenuId: String): Flow<SubMenuLog?> {
        val today = time.today()
        return logDao.observeEntriesForSubMenuDay(subMenuId, today).map { todaysEntries ->
            val subMenu = templateDao.getSubMenu(subMenuId) ?: return@map null
            val template = templateDao.getTemplate(subMenu.templateId)
            val entryByItem = todaysEntries.associateBy { it.itemId }
            val items = templateDao.getItems(subMenuId).map { item ->
                val fields = templateDao.getFields(item.itemId)
                val todayEntry = entryByItem[item.itemId]
                val committed = if (todayEntry != null) {
                    draftsFrom(fields, logDao.getValues(todayEntry.entryId))
                } else {
                    fields.map { LogValueDraft.empty(it.fieldKey) }
                }
                val recall = logDao.getLatestEntryForItemBefore(item.itemId, today)
                    ?.let { recallLine(fields, logDao.getValues(it.entryId)) }
                ItemLog(
                    itemId = item.itemId,
                    name = item.name,
                    fields = fields,
                    committed = committed,
                    loggedEntryId = todayEntry?.entryId,
                    recall = recall,
                )
            }
            SubMenuLog(
                subMenuId = subMenu.subMenuId,
                templateId = subMenu.templateId,
                name = subMenu.name,
                accent = accentKeyForColor(template?.color),
                items = items,
            )
        }
    }

    /**
     * Logs one item's fields for today, replacing whatever it already has logged.
     *
     * Entry and values commit together, and a re-log reuses the existing entry id, so an
     * item never leaves two entries behind for the same day. Empty drafts are dropped so a
     * blank optional field writes no row.
     */
    suspend fun logItem(
        templateId: String,
        subMenuId: String,
        itemId: String,
        drafts: List<LogValueDraft>,
    ) {
        val now = time.nowMillis()
        val existing = logDao.getEntriesForSubMenuDay(subMenuId, time.today())
            .firstOrNull { it.itemId == itemId }
        val entryId = existing?.entryId ?: ids.newId()
        val entry = LogEntryEntity(
            entryId = entryId,
            templateId = templateId,
            subMenuId = subMenuId,
            itemId = itemId,
            loggedAt = now,
            localDate = time.localDateOf(now),
            variantRef = null,
        )
        val values = drafts.filter { it.hasValue }.map { draft ->
            LogValueEntity(
                valueId = ids.newId(),
                entryId = entryId,
                fieldKey = draft.fieldKey,
                valueNumber = draft.number,
                valueText = draft.text,
                valueBool = draft.bool,
                valueJson = draft.json,
            )
        }
        if (existing == null) logDao.insertLog(entry, values) else logDao.replaceLog(entry, values)
    }

    /** Removes today's log for an item (its values cascade). History is untouched. */
    suspend fun clearItem(entryId: String) = logDao.deleteEntry(entryId)

    private fun draftsFrom(
        fields: List<ItemFieldEntity>,
        values: List<LogValueEntity>,
    ): List<LogValueDraft> {
        val byKey = values.associateBy { it.fieldKey }
        return fields.map { field ->
            val value = byKey[field.fieldKey] ?: return@map LogValueDraft.empty(field.fieldKey)
            LogValueDraft(
                fieldKey = field.fieldKey,
                number = value.valueNumber,
                text = value.valueText,
                bool = value.valueBool,
                json = value.valueJson,
            )
        }
    }

    /** Previous-session recall from a set_group field, e.g. "3 × 8 @ 60 kg". */
    private fun recallLine(
        fields: List<ItemFieldEntity>,
        values: List<LogValueEntity>,
    ): String? {
        val setField = fields.firstOrNull { it.type == FieldType.SET_GROUP.wire } ?: return null
        val json = values.firstOrNull { it.fieldKey == setField.fieldKey }?.valueJson ?: return null
        return formatSetsRecall(parseSetRows(json))
    }
}

/** True when a draft carries something worth a log_values row. */
private val LogValueDraft.hasValue: Boolean
    get() = number != null ||
        !text.isNullOrEmpty() ||
        bool != null ||
        (!json.isNullOrBlank() && json != "[]")
