package dev.rishabh.dailytracker.core.nutrition

import com.google.common.truth.Truth.assertThat
import dev.rishabh.dailytracker.core.db.NutrientKeys
import dev.rishabh.dailytracker.core.db.entity.ProductNutrientEntity
import org.junit.Test

/**
 * Table-driven tests for the read-time macro rule: amount_per_100g x grams / 100.
 *
 * Uses the real per-100g figures from the Lunch Screen design so the numbers here can be
 * checked against the design by eye.
 */
class MacroCalculatorTest {

    private fun nutrient(key: String, per100g: Double) =
        ProductNutrientEntity(id = "n-$key", productId = "p1", nutrientKey = key, amountPer100g = per100g)

    /** Amul Malai Paneer, per the Lunch Screen design. */
    private val amulPaneer = listOf(
        nutrient(NutrientKeys.ENERGY_KCAL, 296.0),
        nutrient(NutrientKeys.PROTEIN_G, 18.5),
        nutrient(NutrientKeys.CARBS_G, 5.4),
        nutrient(NutrientKeys.FAT_G, 22.7),
    )

    private data class Case(
        val name: String,
        val grams: Double?,
        val expectedKcal: Double,
        val expectedProtein: Double,
        val expectedCarbs: Double,
        val expectedFat: Double,
    )

    @Test
    fun `scales nutrients linearly with grams`() {
        val cases = listOf(
            // The basis itself: 100g must reproduce the label exactly.
            Case("100g is identity", 100.0, 296.0, 18.5, 5.4, 22.7),
            // The design's stepper default and its neighbours.
            Case("150g is 1.5x", 150.0, 444.0, 27.75, 8.1, 34.05),
            Case("50g is half", 50.0, 148.0, 9.25, 2.7, 11.35),
            Case("10g is a tenth", 10.0, 29.6, 1.85, 0.54, 2.27),
            Case("250g is 2.5x", 250.0, 740.0, 46.25, 13.5, 56.75),
            // Zero is reachable: the design's stepper floors at 0.
            Case("0g is zero", 0.0, 0.0, 0.0, 0.0, 0.0),
            // Fractional grams must not be rounded by the calculator.
            Case("fractional grams", 12.5, 37.0, 2.3125, 0.675, 2.8375),
        )

        for (case in cases) {
            val totals = MacroCalculator.forQuantity(amulPaneer, case.grams)
            assertThat(totals.energyKcal).isWithin(TOLERANCE).of(case.expectedKcal)
            assertThat(totals.proteinG).isWithin(TOLERANCE).of(case.expectedProtein)
            assertThat(totals.carbsG).isWithin(TOLERANCE).of(case.expectedCarbs)
            assertThat(totals.fatG).isWithin(TOLERANCE).of(case.expectedFat)
        }
    }

    @Test
    fun `missing or unusable quantity yields empty totals rather than throwing`() {
        // An entry can exist before its quantity is filled in; a day summary must still render.
        val badInputs = listOf(null, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
        for (grams in badInputs) {
            val totals = MacroCalculator.forQuantity(amulPaneer, grams)
            assertThat(totals.byKey).isEmpty()
            assertThat(totals.energyKcal).isEqualTo(0.0)
        }
    }

    @Test
    fun `product with no nutrients contributes nothing`() {
        val totals = MacroCalculator.forQuantity(emptyList(), 100.0)
        assertThat(totals.byKey).isEmpty()
    }

    @Test
    fun `unknown nutrient keys survive the computation`() {
        // product_nutrients is long-format so any micronutrient works without a migration.
        val withIron = amulPaneer + nutrient("iron_mg", 0.4)
        val totals = MacroCalculator.forQuantity(withIron, 50.0)
        assertThat(totals["iron_mg"]).isWithin(TOLERANCE).of(0.2)
    }

    @Test
    fun `absent nutrient reads as zero not an error`() {
        val totals = MacroCalculator.forQuantity(amulPaneer, 100.0)
        assertThat(totals[NutrientKeys.FIBER_G]).isEqualTo(0.0)
    }

    @Test
    fun `totals sum across products in a meal`() {
        val dal = listOf(
            nutrient(NutrientKeys.ENERGY_KCAL, 120.0),
            nutrient(NutrientKeys.PROTEIN_G, 9.0),
        )
        val nutrientsByProduct = mapOf("paneer" to amulPaneer, "dal" to dal)
        val quantities = listOf(
            MacroCalculator.Quantity("paneer", 100.0), // 296 kcal, 18.5P
            MacroCalculator.Quantity("dal", 200.0), // 240 kcal, 18.0P
        )

        val totals = MacroCalculator.total(quantities, nutrientsByProduct)

        assertThat(totals.energyKcal).isWithin(TOLERANCE).of(536.0)
        assertThat(totals.proteinG).isWithin(TOLERANCE).of(36.5)
        // Only paneer carries carbs/fat; dal's absence must not zero them out.
        assertThat(totals.carbsG).isWithin(TOLERANCE).of(5.4)
        assertThat(totals.fatG).isWithin(TOLERANCE).of(22.7)
    }

    @Test
    fun `quantity whose product is missing is skipped rather than failing the summary`() {
        val quantities = listOf(
            MacroCalculator.Quantity("paneer", 100.0),
            MacroCalculator.Quantity("deleted-product", 100.0),
        )
        val totals = MacroCalculator.total(quantities, mapOf("paneer" to amulPaneer))
        assertThat(totals.energyKcal).isWithin(TOLERANCE).of(296.0)
    }

    @Test
    fun `empty meal totals to zero`() {
        val totals = MacroCalculator.total(emptyList(), emptyMap())
        assertThat(totals.byKey).isEmpty()
        assertThat(totals.energyKcal).isEqualTo(0.0)
    }

    @Test
    fun `adding totals is commutative and identity-preserving`() {
        val a = MacroCalculator.forQuantity(amulPaneer, 100.0)
        val b = MacroCalculator.forQuantity(amulPaneer, 50.0)

        assertThat((a + b).energyKcal).isWithin(TOLERANCE).of((b + a).energyKcal)
        assertThat((a + NutrientTotals.EMPTY).energyKcal).isWithin(TOLERANCE).of(a.energyKcal)
        assertThat((NutrientTotals.EMPTY + a).energyKcal).isWithin(TOLERANCE).of(a.energyKcal)
    }

    @Test
    fun `repeated nutrient keys for one product are summed not overwritten`() {
        val duplicated = listOf(
            nutrient(NutrientKeys.ENERGY_KCAL, 100.0).copy(id = "a"),
            nutrient(NutrientKeys.ENERGY_KCAL, 50.0).copy(id = "b"),
        )
        val totals = MacroCalculator.forQuantity(duplicated, 100.0)
        assertThat(totals.energyKcal).isWithin(TOLERANCE).of(150.0)
    }

    private companion object {
        const val TOLERANCE = 1e-9
    }
}
