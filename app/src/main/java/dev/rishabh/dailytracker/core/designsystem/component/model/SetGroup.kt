package dev.rishabh.dailytracker.core.designsystem.component.model

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * One row of a `set_group` field: some reps at a weight.
 *
 * Matches the seeded shape `{"fields":["reps","weight"],"weight_unit":"kg"}` — the value is a
 * JSON array of `{"reps":Int,"weight":Double}`. Parsing and formatting live here so the
 * editor (FieldRenderer) and the previous-session recall read the exact same representation.
 */
data class SetRow(val reps: Int = 0, val weight: Double = 0.0)

private val setJson = Json { ignoreUnknownKeys = true; isLenient = true }

/** Parses a set_group `value_json` into rows; malformed or empty json yields no rows. */
fun parseSetRows(json: String?): List<SetRow> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
        setJson.decodeFromString(ListSerializer(RawSet.serializer()), json)
            .map { SetRow(reps = it.reps, weight = it.weight) }
    }.getOrDefault(emptyList())
}

/** Encodes rows back to the `value_json` stored in log_values. */
fun encodeSetRows(rows: List<SetRow>): String =
    setJson.encodeToString(ListSerializer(RawSet.serializer()), rows.map { RawSet(it.reps, it.weight) })

/**
 * A compact recall of a past session: "3 × 8 @ 60 kg" when every set matches, otherwise the
 * sets listed as reps@weight ("8@60 · 8@60 · 6@62 kg"). Null when there are no sets.
 */
fun formatSetsRecall(rows: List<SetRow>): String? {
    if (rows.isEmpty()) return null
    val first = rows.first()
    val uniform = rows.all { it.reps == first.reps && it.weight == first.weight }
    return if (uniform) {
        "${rows.size} × ${first.reps} @ ${formatWeight(first.weight)} kg"
    } else {
        rows.joinToString(" · ") { "${it.reps}@${formatWeight(it.weight)}" } + " kg"
    }
}

/** Drops a needless trailing ".0" so 60.0 kg reads as "60". */
fun formatWeight(weight: Double): String =
    if (weight % 1.0 == 0.0) weight.toLong().toString() else weight.toString()

/** kotlinx.serialization surface; kept private so callers only ever see [SetRow]. */
@kotlinx.serialization.Serializable
private data class RawSet(val reps: Int = 0, val weight: Double = 0.0)
