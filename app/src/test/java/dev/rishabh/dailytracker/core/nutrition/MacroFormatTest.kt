package dev.rishabh.dailytracker.core.nutrition

import com.google.common.truth.Truth.assertThat
import dev.rishabh.dailytracker.core.db.NutrientKeys
import org.junit.Test

/**
 * The macro strings are load-bearing: they are what the design's states are checked
 * against, and the summary bar has a fixed width to live within.
 */
class MacroFormatTest {

    private fun totals(kcal: Double, p: Double, c: Double, f: Double) = NutrientTotals(
        mapOf(
            NutrientKeys.ENERGY_KCAL to kcal,
            NutrientKeys.PROTEIN_G to p,
            NutrientKeys.CARBS_G to c,
            NutrientKeys.FAT_G to f,
        ),
    )

    @Test
    fun `summary bar matches the design's line`() {
        assertThat(macroSummaryLine(totals(386.0, 14.0, 62.0, 7.0)))
            .isEqualTo("386 kcal · 14g P · 62g C · 7g F")
    }

    @Test
    fun `summary bar rounds grams to whole numbers`() {
        // 150g of the design's Amul paneer.
        assertThat(macroSummaryLine(totals(444.0, 27.75, 8.1, 34.05)))
            .isEqualTo("444 kcal · 28g P · 8g C · 34g F")
    }

    @Test
    fun `per-100g line keeps one decimal and matches the design`() {
        assertThat(per100gLine(totals(296.0, 18.5, 5.4, 22.7)))
            .isEqualTo("per 100g · 296 kcal · 18.5P · 5.4C · 22.7F")
    }

    @Test
    fun `kcal is always whole`() {
        assertThat(kcalLabel(totals(443.99, 0.0, 0.0, 0.0))).isEqualTo("444 kcal")
    }

    @Test
    fun `home summary is kcal and protein only`() {
        assertThat(homeMacroSummary(totals(1240.0, 68.4, 100.0, 40.0)))
            .isEqualTo("1240 kcal · 68g P")
    }

    @Test
    fun `empty totals render as zeroes rather than blanks`() {
        assertThat(macroSummaryLine(NutrientTotals.EMPTY))
            .isEqualTo("0 kcal · 0g P · 0g C · 0g F")
    }
}
