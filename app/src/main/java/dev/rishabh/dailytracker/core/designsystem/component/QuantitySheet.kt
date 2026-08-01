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
 * A food's natural logging unit — "2 eggs", "1 katori", "1 tsp" — with the grams it converts
 * to. Purely an input layer: the sheet still emits grams, so product_nutrients and the macro
 * math are untouched. Null means the food is logged in plain grams.
 */
data class ServingUnit(
    val label: String,
    val gramsPerUnit: Double,
    /** Step in units: 1 for countable things (eggs), 0.5 for household measures (katori). */
    val step: Double,
) {
    /** Rounds grams to the nearest whole step of this unit, never below one step. */
    fun snap(grams: Double): Double {
        val units = grams / gramsPerUnit
        val snapped = (kotlin.math.round(units / step) * step).coerceAtLeast(step)
        return snapped * gramsPerUnit
    }

    /** True when [grams] lands on a clean step boundary (so it can open in unit mode). */
    fun isCleanMultiple(grams: Double): Boolean {
        val units = grams / gramsPerUnit
        val nearest = kotlin.math.round(units / step) * step
        return kotlin.math.abs(units - nearest) < 0.01
    }

    /** Naive English plural for the count readout; singular for tsp and at exactly one. */
    fun plural(count: Double): String = when {
        kotlin.math.abs(count - 1.0) < 0.001 -> label
        label == "tsp" -> "tsp"
        label.endsWith("s") || label.endsWith("sh") || label.endsWith("ch") || label.endsWith("x") -> "${label}es"
        else -> "${label}s"
    }

    companion object {
        /** Builds a spec from generic-food metadata, or null when the food is grams-logged. */
        fun from(servingUnit: String?, label: String?, gramsPerUnit: Double?): ServingUnit? {
            if (label.isNullOrBlank() || gramsPerUnit == null || gramsPerUnit <= 0.0) return null
            val step = if (servingUnit == "count") 1.0 else 0.5
            return ServingUnit(label = label, gramsPerUnit = gramsPerUnit, step = step)
        }
    }
}

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
    /** The food's natural unit (eggs, katori, tsp); null logs in plain grams. */
    serving: ServingUnit? = null,
    accent: AccentColors = DailyTrackerTheme.accent,
    edited: Boolean = false,
    disabled: Boolean = false,
    confirmLabel: String = "Add to log",
    /** Non-null when an existing portion is being edited; renders the unlog action. */
    onRemove: (() -> Unit)? = null,
    onAdd: (grams: Double) -> Unit = {},
    onCancel: () -> Unit = {},
) {
    // A new portion opens snapped to a whole unit ("1 katori"); an edit keeps its exact
    // logged grams, and drops to grams mode if that is not a clean multiple of the unit.
    val isNew = onRemove == null
    val startGrams = if (serving != null && isNew) serving.snap(initialGrams) else initialGrams
    var grams by remember(product, startGrams) { mutableStateOf(startGrams) }
    var byUnit by remember(product) {
        mutableStateOf(serving != null && (isNew || serving.isCleanMultiple(initialGrams)))
    }
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

        // In unit mode the stepper moves whole/half units and shows "2 eggs"; the grams line
        // sits beneath as the conversion. In grams mode it is the plain grams stepper.
        val inUnitMode = byUnit && serving != null
        val stepGrams = if (inUnitMode) serving!!.gramsPerUnit * serving.step else step

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sp4),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sp6, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepButton(Icons.Rounded.Remove, "less", enabled = !disabled) {
                grams = (grams - stepGrams).coerceAtLeast(0.0)
            }
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.widthIn(min = 88.dp)) {
                if (inUnitMode) {
                    val count = grams / serving!!.gramsPerUnit
                    Text(
                        formatAmount(count),
                        style = TypeNumericLarge,
                        color = if (changed) accent.base else OnSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(" ${serving.plural(count)}", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                } else {
                    Text(
                        formatAmount(grams),
                        style = TypeNumericLarge,
                        color = if (changed) accent.base else OnSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(" g", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                }
            }
            StepButton(Icons.Rounded.Add, "more", enabled = !disabled) {
                grams += stepGrams
            }
        }

        // The unit is the default, never a cage: a toggle always exposes the raw grams, and
        // grams-logged foods simply never show it.
        if (serving != null) {
            val toGrams = inUnitMode
            Text(
                text = if (toGrams) "Switch to grams" else "Switch to ${serving.label}",
                style = MaterialTheme.typography.labelSmall,
                color = accent.base,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = Spacing.sp2)
                    .clip(ShapeFull)
                    .clickable(enabled = !disabled, role = Role.Button) { byUnit = !byUnit }
                    .padding(horizontal = Spacing.sp3, vertical = Spacing.sp1),
            )
            if (inUnitMode) {
                Text(
                    "${formatAmount(grams)} g",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceFaint,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sp3, bottom = Spacing.sp4),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sp2),
        ) {
            MacroTile("kcal", formatKcal(per100g.kcal * factor), accent.base, Modifier.weight(1f))
            MacroTile("protein", formatGrams(per100g.protein * factor), OnSurface, Modifier.weight(1f))
            MacroTile("carbs", formatGrams(per100g.carbs * factor), OnSurface, Modifier.weight(1f))
            MacroTile("fat", formatGrams(per100g.fat * factor), OnSurface, Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sp3)) {
            if (onRemove != null) {
                OutlineButton("Remove", enabled = !disabled, onClick = onRemove)
            }
            OutlineButton("Cancel", enabled = !disabled, onClick = onCancel)
            AccentButton(confirmLabel, accent = accent.base, enabled = !disabled, modifier = Modifier.weight(1f)) {
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
