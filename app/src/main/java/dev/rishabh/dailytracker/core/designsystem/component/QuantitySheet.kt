package dev.rishabh.dailytracker.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
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
import dev.rishabh.dailytracker.core.designsystem.ShapeFull
import dev.rishabh.dailytracker.core.designsystem.ShapeSheet
import dev.rishabh.dailytracker.core.designsystem.Spacing
import dev.rishabh.dailytracker.core.designsystem.Surface2
import dev.rishabh.dailytracker.core.designsystem.Surface3
import dev.rishabh.dailytracker.core.designsystem.TypeNumeric
import dev.rishabh.dailytracker.core.designsystem.TypeNumericLarge

/** Macros per 100g, the basis every product is normalised to. */
data class Per100g(
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
)

/**
 * Bottom-sheet content: a grams stepper with a live 4-up macro readout (kcal in accent)
 * and Cancel / Add actions. Renders the sheet surface itself (rounded top, handle); the
 * host places it over a scrim.
 *
 * The readout uses the same read-time rule as everything else — per-100g × grams / 100 —
 * on a display struct. The authoritative computation for the saved log still runs through
 * MacroCalculator when the entry is written.
 */
@Composable
fun QuantitySheet(
    product: String,
    per100g: Per100g,
    modifier: Modifier = Modifier,
    brand: String? = null,
    initialGrams: Double = 100.0,
    step: Double = 10.0,
    accent: AccentColors = DailyTrackerTheme.accent,
    edited: Boolean = false,
    disabled: Boolean = false,
    onAdd: (grams: Double) -> Unit = {},
    onCancel: () -> Unit = {},
) {
    var grams by remember(product, initialGrams) { mutableStateOf(initialGrams) }
    val factor = grams / 100.0
    val changed = edited || grams != initialGrams

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ShapeSheet)
            .background(Surface2)
            .alpha(if (disabled) DisabledOpacity else 1f)
            .padding(start = Spacing.sp4, end = Spacing.sp4, top = Spacing.sp2, bottom = Spacing.sp4),
    ) {
        SheetHandle(Modifier.align(Alignment.CenterHorizontally).padding(bottom = Spacing.sp3))

        if (brand != null) {
            Text(brand.uppercase(), style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
        }
        Text(product, style = MaterialTheme.typography.titleLarge, color = OnSurface)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.sp4),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sp6, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepButton(Icons.Rounded.Remove, "less", enabled = !disabled) {
                grams = (grams - step).coerceAtLeast(0.0)
            }
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.widthIn(min = 88.dp)) {
                Text(
                    formatAmount(grams),
                    style = TypeNumericLarge,
                    color = if (changed) accent.base else OnSurface,
                    textAlign = TextAlign.Center,
                )
                Text(" g", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            }
            StepButton(Icons.Rounded.Add, "more", enabled = !disabled) {
                grams += step
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sp4),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sp2),
        ) {
            MacroTile("kcal", formatKcal(per100g.kcal * factor), accent.base, Modifier.weight(1f))
            MacroTile("protein", formatGrams(per100g.protein * factor), OnSurface, Modifier.weight(1f))
            MacroTile("carbs", formatGrams(per100g.carbs * factor), OnSurface, Modifier.weight(1f))
            MacroTile("fat", formatGrams(per100g.fat * factor), OnSurface, Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sp3)) {
            OutlineButton("Cancel", enabled = !disabled, onClick = onCancel)
            AccentButton("Add to log", accent = accent.base, enabled = !disabled, modifier = Modifier.weight(1f)) {
                onAdd(grams)
            }
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(Dimens.hitMin)
            .clip(ShapeFull)
            .background(Surface3)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = OnSurface, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun MacroTile(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.md))
            .background(Surface3)
            .padding(Spacing.sp2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = TypeNumeric, color = valueColor)
        Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceFaint)
    }
}

@Preview(name = "QuantitySheet", showBackground = true, backgroundColor = 0xFF0E1013, widthDp = 380)
@Composable
private fun QuantitySheetPreview() {
    DailyTrackerTheme {
        QuantitySheet(
            brand = "Amul",
            product = "Malai Paneer",
            per100g = Per100g(kcal = 296.0, protein = 18.5, carbs = 5.4, fat = 22.7),
            initialGrams = 150.0,
            edited = true,
            accent = DailyTrackerTheme.accents.diet,
        )
    }
}
