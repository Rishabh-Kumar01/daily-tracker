package dev.rishabh.dailytracker.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.rishabh.dailytracker.core.db.FieldType
import dev.rishabh.dailytracker.core.db.entity.ItemFieldEntity
import dev.rishabh.dailytracker.core.designsystem.AccentColors
import dev.rishabh.dailytracker.core.designsystem.DailyTrackerTheme
import dev.rishabh.dailytracker.core.designsystem.Dimens
import dev.rishabh.dailytracker.core.designsystem.OnSurface
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceFaint
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceVariant
import dev.rishabh.dailytracker.core.designsystem.Outline
import dev.rishabh.dailytracker.core.designsystem.Radius
import dev.rishabh.dailytracker.core.designsystem.ShapeFull
import dev.rishabh.dailytracker.core.designsystem.Spacing
import dev.rishabh.dailytracker.core.designsystem.Surface2
import dev.rishabh.dailytracker.core.designsystem.Surface3
import dev.rishabh.dailytracker.core.designsystem.TypeNumeric
import dev.rishabh.dailytracker.core.designsystem.component.model.LogValueDraft
import dev.rishabh.dailytracker.core.designsystem.component.model.SetRow
import dev.rishabh.dailytracker.core.designsystem.component.model.coerceToBounds
import dev.rishabh.dailytracker.core.designsystem.component.model.durationOptions
import dev.rishabh.dailytracker.core.designsystem.component.model.encodeSetRows
import dev.rishabh.dailytracker.core.designsystem.component.model.formatWeight
import dev.rishabh.dailytracker.core.designsystem.component.model.parseSetRows
import dev.rishabh.dailytracker.core.designsystem.component.model.quantityOptions
import dev.rishabh.dailytracker.core.designsystem.component.model.scaleOptions
import dev.rishabh.dailytracker.core.designsystem.component.model.selectOptions
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

/**
 * The one Composable that renders any field of any activity, driven by an [ItemFieldEntity].
 *
 * This is the heart of "activities are data": adding an activity adds rows, and they render
 * here with no new screen. The field type is resolved through [FieldType.fromWire], so a
 * template carrying a type this build doesn't know renders as a read-only "unsupported"
 * card — never a crash.
 *
 * @param draft the current value; the renderer is stateless and reports edits via [onChange].
 */
@Composable
fun FieldRenderer(
    field: ItemFieldEntity,
    draft: LogValueDraft,
    onChange: (LogValueDraft) -> Unit,
    modifier: Modifier = Modifier,
    accent: AccentColors = DailyTrackerTheme.accent,
    enabled: Boolean = true,
) {
    Box(modifier = modifier) {
        when (FieldType.fromWire(field.type)) {
            FieldType.CHECKBOX -> CheckboxField(field, draft, onChange, accent, enabled)
            FieldType.QUANTITY -> QuantityField(field, draft, onChange, accent, enabled)
            FieldType.ITEM_VARIANT -> VariantPickerField(field, draft, accent, enabled)
            FieldType.PHOTO -> PhotoField(field, draft, accent, enabled)
            FieldType.SCALE -> ScaleField(field, draft, onChange, accent, enabled)
            FieldType.DURATION -> DurationField(field, draft, onChange, accent, enabled)
            FieldType.NOTE -> NoteField(field, draft, onChange, accent, enabled)
            FieldType.SET_GROUP -> SetGroupField(field, draft, onChange, accent, enabled)
            FieldType.TIME -> TimeField(field, draft, onChange, accent, enabled)
            FieldType.SINGLE_SELECT -> SelectField(field, draft, onChange, accent, enabled, multi = false)
            FieldType.MULTI_SELECT -> SelectField(field, draft, onChange, accent, enabled, multi = true)
            null -> UnsupportedField(field)
        }
    }
}

/** Read-only card for a field type outside the closed vocabulary — the never-crash path. */
@Composable
private fun UnsupportedField(field: ItemFieldEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Surface2)
            .border(1.dp, Outline, RoundedCornerShape(Radius.md))
            .padding(Spacing.sp3),
    ) {
        Text(field.label, style = MaterialTheme.typography.titleMedium, color = OnSurface)
        Text(
            "Unsupported field (${field.type})",
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceFaint,
        )
    }
}

@Composable
private fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant, modifier = modifier)
}

@Composable
private fun CheckboxField(
    field: ItemFieldEntity,
    draft: LogValueDraft,
    onChange: (LogValueDraft) -> Unit,
    accent: AccentColors,
    enabled: Boolean,
) {
    ItemRow(
        name = field.label,
        checked = draft.bool == true,
        disabled = !enabled,
        accent = accent,
        onCheckedChange = { onChange(draft.withBool(it)) },
    )
}

@Composable
private fun QuantityField(
    field: ItemFieldEntity,
    draft: LogValueDraft,
    onChange: (LogValueDraft) -> Unit,
    accent: AccentColors,
    enabled: Boolean,
) {
    val opts = quantityOptions(field.optionsJson)
    val current = draft.number ?: opts.default ?: opts.min ?: 0.0
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.hitMin),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sp3),
    ) {
        FieldLabel(field.label, Modifier.weight(1f))
        RoundIconButton(Icons.Rounded.Remove, "less", enabled) {
            onChange(draft.withNumber((current - opts.step).coerceToBounds(opts.min, opts.max)))
        }
        Text(
            "${formatAmount(current)}${field.unit?.let { " $it" } ?: ""}",
            style = TypeNumeric,
            color = OnSurface,
        )
        RoundIconButton(Icons.Rounded.Add, "more", enabled) {
            onChange(draft.withNumber((current + opts.step).coerceToBounds(opts.min, opts.max)))
        }
    }
}

@Composable
private fun VariantPickerField(
    field: ItemFieldEntity,
    draft: LogValueDraft,
    accent: AccentColors,
    enabled: Boolean,
) {
    // The Diet meal screen (M6) drives variant selection with inline BrandPickerRow +
    // QuantitySheet. In the generic renderer this shows the chosen product, or a prompt.
    val selected = draft.text
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.hitMin)
            .clip(RoundedCornerShape(Radius.md))
            .background(Surface2)
            .padding(horizontal = Spacing.sp3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sp3),
    ) {
        FieldLabel(field.label, Modifier.weight(1f))
        Text(
            selected ?: "Select…",
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected != null) OnSurface else OnSurfaceFaint,
        )
    }
}

@Composable
private fun PhotoField(
    field: ItemFieldEntity,
    draft: LogValueDraft,
    accent: AccentColors,
    enabled: Boolean,
) {
    // Capture arrives with CameraX in a later phase; this renders the affordance.
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sp3)) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(Radius.sm))
                .background(Surface3),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.PhotoCamera, contentDescription = null, tint = accent.base, modifier = Modifier.size(24.dp))
        }
        FieldLabel(field.label)
    }
}

@Composable
private fun ScaleField(
    field: ItemFieldEntity,
    draft: LogValueDraft,
    onChange: (LogValueDraft) -> Unit,
    accent: AccentColors,
    enabled: Boolean,
) {
    val opts = scaleOptions(field.optionsJson)
    val selected = draft.number?.toInt()
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sp2)) {
        FieldLabel(field.label)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sp2)) {
            for (n in opts.min..opts.max) {
                val on = n == selected
                Box(
                    modifier = Modifier
                        .size(Dimens.hitMin)
                        .clip(RoundedCornerShape(Radius.md))
                        .background(if (on) accent.container else Surface3)
                        .then(if (on) Modifier.border(1.5.dp, accent.base, RoundedCornerShape(Radius.md)) else Modifier)
                        .clickable(enabled = enabled, role = Role.RadioButton) { onChange(draft.withNumber(n.toDouble())) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(n.toString(), style = TypeNumeric, color = if (on) accent.base else OnSurface)
                }
            }
        }
    }
}

@Composable
private fun DurationField(
    field: ItemFieldEntity,
    draft: LogValueDraft,
    onChange: (LogValueDraft) -> Unit,
    accent: AccentColors,
    enabled: Boolean,
) {
    val opts = durationOptions(field.optionsJson)
    val minutes = draft.number ?: 0.0
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.hitMin),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sp3),
    ) {
        FieldLabel(field.label, Modifier.weight(1f))
        if (opts.timer_ui) {
            RoundIconButton(Icons.Rounded.Timer, "timer", enabled) { /* timer UI wired later */ }
        }
        RoundIconButton(Icons.Rounded.Remove, "less", enabled) {
            onChange(draft.withNumber((minutes - 5).coerceAtLeast(0.0)))
        }
        Text("${formatAmount(minutes)} min", style = TypeNumeric, color = OnSurface)
        RoundIconButton(Icons.Rounded.Add, "more", enabled) {
            onChange(draft.withNumber(minutes + 5))
        }
    }
}

@Composable
private fun NoteField(
    field: ItemFieldEntity,
    draft: LogValueDraft,
    onChange: (LogValueDraft) -> Unit,
    accent: AccentColors,
    enabled: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sp2)) {
        FieldLabel(field.label)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.hitMin)
                .clip(RoundedCornerShape(Radius.md))
                .background(Surface3)
                .padding(Spacing.sp3),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = draft.text.orEmpty(),
                onValueChange = { onChange(draft.withText(it)) },
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = OnSurface),
                cursorBrush = SolidColor(accent.base),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SetGroupField(
    field: ItemFieldEntity,
    draft: LogValueDraft,
    onChange: (LogValueDraft) -> Unit,
    accent: AccentColors,
    enabled: Boolean,
) {
    val sets = parseSetRows(draft.json)
    fun update(next: List<SetRow>) = onChange(draft.withJson(encodeSetRows(next)))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sp2)) {
        FieldLabel(field.label)
        sets.forEachIndexed { index, set ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.hitMin)
                    .clip(RoundedCornerShape(Radius.md))
                    .background(Surface3)
                    .padding(horizontal = Spacing.sp3, vertical = Spacing.sp1),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sp2),
            ) {
                Text(
                    "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                )
                // Reps: whole steps of 1, never below 0.
                MiniStepper(
                    value = "${set.reps}",
                    enabled = enabled,
                    onMinus = { update(sets.mapAt(index) { it.copy(reps = (it.reps - 1).coerceAtLeast(0)) }) },
                    onPlus = { update(sets.mapAt(index) { it.copy(reps = it.reps + 1) }) },
                )
                Text("×", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceFaint)
                // Weight: 2.5 kg steps, the smallest common plate increment, never below 0.
                MiniStepper(
                    value = "${formatWeight(set.weight)} kg",
                    enabled = enabled,
                    onMinus = { update(sets.mapAt(index) { it.copy(weight = (it.weight - WEIGHT_STEP).coerceAtLeast(0.0)) }) },
                    onPlus = { update(sets.mapAt(index) { it.copy(weight = it.weight + WEIGHT_STEP) }) },
                )
                RoundIconButton(Icons.Rounded.Close, "remove set", enabled) {
                    update(sets.filterIndexed { i, _ -> i != index })
                }
            }
        }
        Row(
            modifier = Modifier
                .heightIn(min = Dimens.hitMin)
                .clip(ShapeFull)
                .clickable(enabled = enabled, role = Role.Button) {
                    // A new set copies the last one's load — you rarely change weight between
                    // sets, so this is the fewest taps to log another set.
                    update(sets + (sets.lastOrNull() ?: SetRow(reps = 8, weight = 0.0)))
                }
                .padding(horizontal = Spacing.sp3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sp2),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = accent.base, modifier = Modifier.size(20.dp))
            Text("Add set", style = MaterialTheme.typography.labelMedium, color = accent.base)
        }
    }
}

private const val WEIGHT_STEP = 2.5

/** Applies [transform] to the row at [index], leaving the rest of the list unchanged. */
private inline fun List<SetRow>.mapAt(index: Int, transform: (SetRow) -> SetRow): List<SetRow> =
    mapIndexed { i, row -> if (i == index) transform(row) else row }

/** A compact −/value/+ control that keeps a set row on one line. */
@Composable
private fun MiniStepper(value: String, enabled: Boolean, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sp1)) {
        RoundIconButton(Icons.Rounded.Remove, "less", enabled, onMinus)
        Text(value, style = TypeNumeric, color = OnSurface, textAlign = TextAlign.Center)
        RoundIconButton(Icons.Rounded.Add, "more", enabled, onPlus)
    }
}

@Composable
private fun TimeField(
    field: ItemFieldEntity,
    draft: LogValueDraft,
    onChange: (LogValueDraft) -> Unit,
    accent: AccentColors,
    enabled: Boolean,
) {
    // Stored as minutes since midnight (value_number). A proper picker replaces the steppers
    // later; steppers keep it interactive and testable now.
    val minutes = (draft.number ?: (22 * 60).toDouble()).toInt().coerceIn(0, 24 * 60 - 1)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.hitMin),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sp3),
    ) {
        FieldLabel(field.label, Modifier.weight(1f))
        RoundIconButton(Icons.Rounded.Remove, "earlier", enabled) {
            onChange(draft.withNumber(((minutes - 15 + 24 * 60) % (24 * 60)).toDouble()))
        }
        Text(formatTime(minutes), style = TypeNumeric, color = OnSurface)
        RoundIconButton(Icons.Rounded.Add, "later", enabled) {
            onChange(draft.withNumber(((minutes + 15) % (24 * 60)).toDouble()))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectField(
    field: ItemFieldEntity,
    draft: LogValueDraft,
    onChange: (LogValueDraft) -> Unit,
    accent: AccentColors,
    enabled: Boolean,
    multi: Boolean,
) {
    val options = selectOptions(field.optionsJson).options
    val chosen = if (multi) parseIdList(draft.json).toSet() else setOfNotNull(draft.text)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sp2)) {
        FieldLabel(field.label)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sp2), verticalArrangement = Arrangement.spacedBy(Spacing.sp2)) {
            options.forEach { option ->
                val on = option.id in chosen
                Box(
                    modifier = Modifier
                        .heightIn(min = Dimens.hitMin)
                        .clip(ShapeFull)
                        .background(if (on) accent.container else Surface3)
                        .then(if (on) Modifier.border(1.5.dp, accent.base, ShapeFull) else Modifier)
                        .clickable(enabled = enabled, role = Role.Button) {
                            if (multi) {
                                val next = chosen.toMutableSet().apply { if (!add(option.id)) remove(option.id) }
                                onChange(draft.withJson(encodeIdList(next)))
                            } else {
                                onChange(draft.withText(option.id))
                            }
                        }
                        .padding(horizontal = Spacing.sp4),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(option.label, style = MaterialTheme.typography.labelMedium, color = if (on) accent.base else OnSurface)
                }
            }
        }
    }
}

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(Dimens.hitMin)
            .clip(ShapeFull)
            .background(Surface3)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = OnSurface, modifier = Modifier.size(20.dp))
    }
}

// --- small pure helpers (kept here so the field composables stay declarative) ---

private val setJson = Json { ignoreUnknownKeys = true; isLenient = true }

private fun parseIdList(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
        (setJson.parseToJsonElement(json) as JsonArray).mapNotNull { (it as? JsonPrimitive)?.content }
    }.getOrDefault(emptyList())
}

private fun encodeIdList(ids: Collection<String>): String =
    ids.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }

private fun formatTime(minutesOfDay: Int): String {
    val h = minutesOfDay / 60
    val m = minutesOfDay % 60
    return "%02d:%02d".format(h, m)
}
