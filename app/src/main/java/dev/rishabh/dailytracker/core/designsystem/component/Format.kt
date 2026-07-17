package dev.rishabh.dailytracker.core.designsystem.component

import java.util.Locale

/**
 * Number formatting shared by the components.
 *
 * The design's rule: whole numbers show no decimal, fractional ones show one. Locale.US so
 * the decimal separator matches the mono, value-forward look regardless of device locale.
 */
internal fun formatAmount(n: Double): String =
    if (n % 1.0 == 0.0) n.toLong().toString() else String.format(Locale.US, "%.1f", n)

/** "265" for kcal — always whole. */
internal fun formatKcal(n: Double): String = Math.round(n).toString()

/** "18.5g" style macro value. */
internal fun formatGrams(n: Double): String = "${formatAmount(n)}g"
