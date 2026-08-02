package dev.rishabh.dailytracker.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.rishabh.dailytracker.core.designsystem.AccentColors
import dev.rishabh.dailytracker.core.designsystem.DailyTrackerTheme
import dev.rishabh.dailytracker.core.designsystem.DisabledOpacity
import dev.rishabh.dailytracker.core.designsystem.Dimens
import dev.rishabh.dailytracker.core.designsystem.OnSurface
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceFaint
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceVariant
import dev.rishabh.dailytracker.core.designsystem.Radius
import dev.rishabh.dailytracker.core.designsystem.ShapeSheet
import dev.rishabh.dailytracker.core.designsystem.Spacing
import dev.rishabh.dailytracker.core.designsystem.Surface2
import dev.rishabh.dailytracker.core.designsystem.Surface3
import dev.rishabh.dailytracker.core.designsystem.TypeNumeric

/** One editable field in a ConfirmSheet: label left, mono value right, optional unit suffix. */
data class ConfirmField(
    val label: String,
    val value: String,
    val suffix: String? = null,
)

/**
 * Confirmation bottom sheet: title, a list of editable fields, Cancel / Confirm actions.
 *
 * This is the "AI proposes, user disposes" surface — every AI-extracted value lands here as
 * an editable field before it's saved, and the same component serves manual confirm flows.
 *
 * @param focusedField index shown with an accent border, or -1 for none.
 * @param headerContent optional slot between the title and the fields (e.g. a photo row).
 */
@Composable
fun ConfirmSheet(
    title: String,
    fields: List<ConfirmField>,
    modifier: Modifier = Modifier,
    accent: AccentColors = DailyTrackerTheme.accent,
    focusedField: Int = -1,
    disabled: Boolean = false,
    confirmLabel: String = "Confirm",
    cancelLabel: String = "Cancel",
    headerContent: (@Composable () -> Unit)? = null,
    onConfirm: () -> Unit = {},
    onCancel: () -> Unit = {},
    onFieldChange: (index: Int, value: String) -> Unit = { _, _ -> },
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ShapeSheet)
            .background(Surface2)
            .alpha(if (disabled) DisabledOpacity else 1f)
            .padding(start = Spacing.sp4, end = Spacing.sp4, top = Spacing.sp2, bottom = Spacing.sp4),
    ) {
        SheetHandle(Modifier.align(Alignment.CenterHorizontally).padding(bottom = Spacing.sp3))
        Text(title, style = MaterialTheme.typography.titleLarge, color = OnSurface)

        if (headerContent != null) {
            Box(modifier = Modifier.padding(top = Spacing.sp2)) { headerContent() }
        }

        Column(
            modifier = Modifier.padding(vertical = Spacing.sp4),
            verticalArrangement = Arrangement.spacedBy(Spacing.sp2),
        ) {
            fields.forEachIndexed { index, field ->
                FieldRowEditor(
                    field = field,
                    focused = index == focusedField,
                    accent = accent.base,
                    enabled = !disabled,
                    onValueChange = { onFieldChange(index, it) },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sp3)) {
            OutlineButton(cancelLabel, enabled = !disabled, onClick = onCancel)
            AccentButton(confirmLabel, accent = accent.base, enabled = !disabled, modifier = Modifier.weight(1f), onClick = onConfirm)
        }
    }
}

@Composable
private fun FieldRowEditor(
    field: ConfirmField,
    focused: Boolean,
    accent: Color,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    val valueColor = if (focused) accent else OnSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.hitMin)
            .clip(RoundedCornerShape(Radius.md))
            .background(Surface3)
            .then(if (focused) Modifier.border(1.5.dp, accent, RoundedCornerShape(Radius.md)) else Modifier)
            .padding(horizontal = Spacing.sp3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sp3),
    ) {
        Text(
            field.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        val selectionColors = TextSelectionColors(handleColor = accent, backgroundColor = accent.copy(alpha = 0.3f))
        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
            BasicTextField(
                value = field.value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = TypeNumeric.copy(color = valueColor, textAlign = TextAlign.End),
                cursorBrush = SolidColor(accent),
            )
        }
        if (field.suffix != null) {
            Text(field.suffix, style = MaterialTheme.typography.labelSmall, color = OnSurfaceFaint)
        }
    }
}

@Preview(name = "ConfirmSheet", showBackground = true, backgroundColor = 0xFF0E1013, widthDp = 380)
@Composable
private fun ConfirmSheetPreview() {
    DailyTrackerTheme {
        ConfirmSheet(
            title = "Log workout",
            accent = DailyTrackerTheme.accents.workout,
            focusedField = 2,
            fields = listOf(
                ConfirmField("Exercise", "Bench press"),
                ConfirmField("Sets × reps", "4 × 8"),
                ConfirmField("Weight", "60", suffix = "kg"),
            ),
        )
    }
}
