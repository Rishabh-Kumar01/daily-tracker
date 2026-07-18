package dev.rishabh.dailytracker.core.nutrition

import java.util.Locale

/*
 * The macro strings the Diet UI shows, in one place.
 *
 * These format numbers that were computed by MacroCalculator at read time — they never
 * cache or persist a result. Locale.US throughout so the decimal separator matches the
 * mono, value-forward look the design asks for regardless of device locale.
 */

private fun oneDp(n: Double): String =
    if (n % 1.0 == 0.0) n.toLong().toString() else String.format(Locale.US, "%.1f", n)

private fun kcal(n: Double): String = Math.round(n).toString()

/**
 * The BrandPickerRow subtitle: "per 100g · 296 kcal · 18.5P · 5.4C · 22.7F".
 *
 * Takes per-100g amounts directly, which is the basis every product is stored in.
 */
fun per100gLine(totals: NutrientTotals): String = buildString {
    append("per 100g · ")
    append(kcal(totals.energyKcal)).append(" kcal · ")
    append(oneDp(totals.proteinG)).append("P · ")
    append(oneDp(totals.carbsG)).append("C · ")
    append(oneDp(totals.fatG)).append("F")
}

/**
 * The sticky summary bar: "386 kcal · 14g P · 62g C · 7g F".
 *
 * Whole grams, as in the design. A decimal per macro is precision nobody acts on at meal
 * level and it pushes the line past the width the bar has next to the Done button; the
 * per-portion readouts keep their decimal.
 */
fun macroSummaryLine(totals: NutrientTotals): String = buildString {
    append(kcal(totals.energyKcal)).append(" kcal · ")
    append(wholeGrams(totals.proteinG)).append("g P · ")
    append(wholeGrams(totals.carbsG)).append("g C · ")
    append(wholeGrams(totals.fatG)).append("g F")
}

private fun wholeGrams(n: Double): String = Math.round(n).toString()

/** The trailing value on a logged ItemRow: "265 kcal". */
fun kcalLabel(totals: NutrientTotals): String = "${kcal(totals.energyKcal)} kcal"

/**
 * Home's Diet card summary: "1240 kcal · 68g P".
 *
 * Shorter than the meal bar because the card gives it one line next to the activity name.
 */
fun homeMacroSummary(totals: NutrientTotals): String =
    "${kcal(totals.energyKcal)} kcal · ${wholeGrams(totals.proteinG)}g P"
