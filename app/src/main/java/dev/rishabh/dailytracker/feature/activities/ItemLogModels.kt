package dev.rishabh.dailytracker.feature.activities

import dev.rishabh.dailytracker.core.db.entity.ItemFieldEntity
import dev.rishabh.dailytracker.core.designsystem.ActivityKey
import dev.rishabh.dailytracker.core.designsystem.component.model.LogValueDraft

/**
 * One loggable item on the generic logging screen: its fields, the values committed for
 * today, whether it is already logged, and a recall of the last session.
 */
data class ItemLog(
    val itemId: String,
    val name: String,
    val fields: List<ItemFieldEntity>,
    /** Today's committed values, one draft per field; empty drafts when nothing is logged. */
    val committed: List<LogValueDraft>,
    /** Non-null once logged today — the card offers Update/Clear instead of Log. */
    val loggedEntryId: String?,
    /** "3 × 8 @ 60 kg" from the most recent prior session, or null when there is none. */
    val recall: String?,
)

/** A whole sub-menu rendered as a generic logging screen (Workout, and later others). */
data class SubMenuLog(
    val subMenuId: String,
    val templateId: String,
    val name: String,
    val accent: ActivityKey,
    val items: List<ItemLog>,
)
