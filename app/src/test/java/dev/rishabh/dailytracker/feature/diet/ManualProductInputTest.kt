package dev.rishabh.dailytracker.feature.diet

import com.google.common.truth.Truth.assertThat
import dev.rishabh.dailytracker.core.db.NutrientKeys
import org.junit.Test

/**
 * Tier-3 entry is the last thing standing between a typo and the nutrition history, so the
 * rules are pinned here rather than left to the UI.
 */
class ManualProductInputTest {

    private fun input(
        brand: String = "Amul",
        name: String = "Malai Paneer",
        kcal: String = "296",
        protein: String = "18.5",
        carbs: String = "5.4",
        fat: String = "22.7",
    ) = ManualProductInput(brand, name, kcal, protein, carbs, fat)

    private fun valid(i: ManualProductInput) =
        (validateManualProduct(i) as ProductValidation.Valid).product

    private fun invalidMessage(i: ManualProductInput) =
        (validateManualProduct(i) as ProductValidation.Invalid).message

    @Test
    fun `full label parses into per-100g nutrients`() {
        val product = valid(input())
        assertThat(product.brand).isEqualTo("Amul")
        assertThat(product.productName).isEqualTo("Malai Paneer")
        assertThat(product.nutrients).containsExactly(
            NutrientKeys.ENERGY_KCAL, 296.0,
            NutrientKeys.PROTEIN_G, 18.5,
            NutrientKeys.CARBS_G, 5.4,
            NutrientKeys.FAT_G, 22.7,
        )
    }

    @Test
    fun `blank macro is absent, not zero`() {
        // A label that omits fibre must not claim the food contains none of it.
        val product = valid(input(carbs = "", fat = "  "))
        assertThat(product.nutrients.keys)
            .containsExactly(NutrientKeys.ENERGY_KCAL, NutrientKeys.PROTEIN_G)
    }

    @Test
    fun `zero is kept when actually typed`() {
        val product = valid(input(fat = "0"))
        assertThat(product.nutrients[NutrientKeys.FAT_G]).isEqualTo(0.0)
    }

    @Test
    fun `name is required`() {
        assertThat(invalidMessage(input(name = "   "))).isEqualTo("Product needs a name")
    }

    @Test
    fun `energy is required`() {
        assertThat(invalidMessage(input(kcal = ""))).isEqualTo("Energy (kcal) is required")
    }

    @Test
    fun `non-numeric macro is rejected`() {
        assertThat(invalidMessage(input(protein = "lots"))).contains("Protein")
    }

    @Test
    fun `negative macro is rejected`() {
        assertThat(invalidMessage(input(carbs = "-1"))).contains("Carbs")
    }

    @Test
    fun `blank brand becomes null rather than an empty string`() {
        // Generic foods have no brand; storing "" would split them from real nulls on dedupe.
        assertThat(valid(input(brand = "  ")).brand).isNull()
    }

    @Test
    fun `surrounding whitespace is trimmed`() {
        val product = valid(input(brand = " Amul ", name = " Malai Paneer ", kcal = " 296 "))
        assertThat(product.brand).isEqualTo("Amul")
        assertThat(product.productName).isEqualTo("Malai Paneer")
        assertThat(product.nutrients[NutrientKeys.ENERGY_KCAL]).isEqualTo(296.0)
    }

    @Test
    fun `field editing addresses the right slot`() {
        val edited = ManualProductInput()
            .withFieldAt(0, "Amul")
            .withFieldAt(1, "Malai Paneer")
            .withFieldAt(2, "296")
        assertThat(edited.brand).isEqualTo("Amul")
        assertThat(edited.productName).isEqualTo("Malai Paneer")
        assertThat(edited.kcal).isEqualTo("296")
        assertThat(edited.fieldAt(1)).isEqualTo("Malai Paneer")
    }
}
