package dev.rishabh.dailytracker.core.designsystem.component.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/*
 * Typed views over item_fields.options_json.
 *
 * options_json is deliberately a free-form column, so parsing is defensive: malformed or
 * partial options fall back to sensible defaults rather than throwing. A broken template
 * must still render its fields.
 */

internal val LenientJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

/** quantity: number + unit with optional bounds and a starting value. */
@Serializable
data class QuantityOptions(
    val min: Double? = null,
    val max: Double? = null,
    val step: Double = 1.0,
    val default: Double? = null,
)

/** scale: an integer range, e.g. 1..5. */
@Serializable
data class ScaleOptions(
    val min: Int = 1,
    val max: Int = 5,
)

/** duration: seconds, with an optional timer affordance. */
@Serializable
data class DurationOptions(
    val timer_ui: Boolean = false,
)

/** photo: how the capture behaves. */
@Serializable
data class PhotoOptions(
    val comparison_series: Boolean = false,
    val overlay_ghost_of_last: Boolean = false,
)

/** set_group: which columns each set row carries. */
@Serializable
data class SetGroupOptions(
    val fields: List<String> = listOf("reps", "weight"),
    val weight_unit: String = "kg",
)

/** single_select / multi_select choices. */
@Serializable
data class SelectOption(val id: String, val label: String)

@Serializable
data class SelectOptions(
    val options: List<SelectOption> = emptyList(),
)

/**
 * Decodes [optionsJson] into [T], returning [default] for null, blank, or malformed input.
 * The whole point is that a bad options blob degrades gracefully instead of crashing.
 */
internal inline fun <reified T> decodeOptions(optionsJson: String?, default: T): T {
    if (optionsJson.isNullOrBlank()) return default
    return runCatching { LenientJson.decodeFromString<T>(optionsJson) }.getOrDefault(default)
}

fun quantityOptions(optionsJson: String?) = decodeOptions(optionsJson, QuantityOptions())
fun scaleOptions(optionsJson: String?) = decodeOptions(optionsJson, ScaleOptions())
fun durationOptions(optionsJson: String?) = decodeOptions(optionsJson, DurationOptions())
fun photoOptions(optionsJson: String?) = decodeOptions(optionsJson, PhotoOptions())
fun setGroupOptions(optionsJson: String?) = decodeOptions(optionsJson, SetGroupOptions())
fun selectOptions(optionsJson: String?) = decodeOptions(optionsJson, SelectOptions())

/** Clamps a quantity/scale value into its configured bounds. */
fun Double.coerceToBounds(min: Double?, max: Double?): Double {
    var v = this
    if (min != null && v < min) v = min
    if (max != null && v > max) v = max
    return v
}
