package dev.rishabh.dailytracker.feature.builder

import dev.rishabh.dailytracker.core.db.FieldType

/**
 * The field types a custom activity can use in v1: everything the generic logging screen
 * already renders and writes without a product library or a later-phase capture.
 *
 * item_variant (needs the food library), photo (needs capture), set_group and the selects
 * are intentionally left out of the manual builder for now.
 */
enum class BuilderFieldType(
    val fieldType: FieldType,
    val display: String,
    val needsUnit: Boolean,
) {
    CHECKBOX(FieldType.CHECKBOX, "Checkbox", false),
    QUANTITY(FieldType.QUANTITY, "Amount", true),
    SCALE(FieldType.SCALE, "Scale 1–5", false),
    DURATION(FieldType.DURATION, "Duration", false),
    TIME(FieldType.TIME, "Time", false),
    NOTE(FieldType.NOTE, "Note", false),
}

/** The four authored accents a custom activity picks its colour from. */
val builderColorHexes: List<String> = listOf("#75D78D", "#FFA460", "#7BC3FF", "#D3A6FF")

// --- Draft models (UI state). Local ids are stable list keys only; real ids are generated
// app-side on save, so a draft can never smuggle an id into the database. ---

data class FieldDraft(
    val id: String,
    val label: String = "",
    val type: BuilderFieldType = BuilderFieldType.CHECKBOX,
    val unit: String = "",
)

data class ItemDraft(
    val id: String,
    val name: String = "",
    val fields: List<FieldDraft> = emptyList(),
)

data class SectionDraft(
    val id: String,
    val name: String = "",
    val items: List<ItemDraft> = emptyList(),
)

data class ActivityDraft(
    val name: String = "",
    val iconKey: String = "category",
    val colorHex: String = builderColorHexes.first(),
    val sections: List<SectionDraft> = emptyList(),
)

/** Result of validating + writing a custom activity. */
sealed interface CreateResult {
    data class Created(val templateId: String) : CreateResult
    data class Invalid(val message: String) : CreateResult
}
