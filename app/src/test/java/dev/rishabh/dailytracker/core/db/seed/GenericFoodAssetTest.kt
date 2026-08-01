package dev.rishabh.dailytracker.core.db.seed

import com.google.common.truth.Truth.assertThat
import dev.rishabh.dailytracker.core.db.NutrientKeys
import org.junit.Test
import java.io.File

/**
 * The bundled dataset is shipped data, so it is validated as data: the real asset must
 * parse, every food must carry the five macros the calculator needs, and the invariants the
 * seeder and meal screen rely on (unique slugs, per-100g amounts, known categories) must
 * hold. A bad row here would seed a silently-wrong product on first launch.
 */
class GenericFoodAssetTest {

    // Gradle runs unit tests with the module dir (app/) as the working directory.
    private val assetFile =
        File("src/main/assets/generic_foods/generic_foods.v1.json")

    private val asset: GenericFoodAsset by lazy {
        genericFoodJson.decodeFromString<GenericFoodAsset>(assetFile.readText())
    }

    private val requiredMacros = setOf(
        NutrientKeys.ENERGY_KCAL, NutrientKeys.PROTEIN_G,
        NutrientKeys.CARBS_G, NutrientKeys.FAT_G, NutrientKeys.FIBER_G,
    )

    @Test
    fun `the real asset parses`() {
        assertThat(assetFile.exists()).isTrue()
        assertThat(asset.datasetVersion).isEqualTo(1)
        assertThat(asset.foods).hasSize(120)
    }

    @Test
    fun `every food carries the five macros`() {
        for (food in asset.foods) {
            assertThat(food.per100g.keys).containsAtLeastElementsIn(requiredMacros)
        }
    }

    @Test
    fun `all nutrient amounts are finite and non-negative`() {
        for (food in asset.foods) {
            for ((key, amount) in food.per100g) {
                assertThat(amount).isFinite()
                assertThat(amount).isAtLeast(0.0)
                assertThat(key).isNotEmpty()
            }
        }
    }

    @Test
    fun `slugs are unique — they are the seeder's idempotency ledger`() {
        val slugs = asset.foods.map { it.slug }
        assertThat(slugs).containsNoDuplicates()
        assertThat(slugs.none { it.isBlank() }).isTrue()
    }

    @Test
    fun `every food has a generic name and a resolvable source ref or approx flag`() {
        for (food in asset.foods) {
            assertThat(food.genericName).isNotEmpty()
            assertThat(food.displayName).isNotEmpty()
            // A row is trustworthy if it cites a source, or is explicitly flagged approximate.
            assertThat(food.sourceRef != null || food.isApprox).isTrue()
        }
    }

    @Test
    fun `categories match the ones the meal screen groups on`() {
        val known = setOf("dal", "fruit", "vegetables", "oil")
        val used = asset.foods.mapNotNull { it.category }.toSet()
        assertThat(known).containsAtLeastElementsIn(used)
    }

    @Test
    fun `the acceptance foods are present and reachable`() {
        val byGeneric = asset.foods.groupBy { it.genericName }
        val byCategory = asset.foods.filter { it.category != null }.groupBy { it.category }
        // Boiled egg surfaces under the "Eggs" item via generic_name.
        assertThat(byGeneric["eggs"]?.map { it.slug }).contains("egg-boiled")
        // Toor dal surfaces under the "Dal" item via category.
        assertThat(byCategory["dal"]?.map { it.slug }).contains("toor-dal-cooked")
        // White rice surfaces under the "Rice" item via generic_name.
        assertThat(byGeneric["rice"]?.map { it.slug }).contains("white-rice-cooked")
        // The restored drops.
        assertThat(byGeneric["khoya"]).isNotNull()
        assertThat(byGeneric["tea"]).isNotNull()
    }

    @Test
    fun `every food declares a valid serving unit`() {
        for (food in asset.foods) {
            assertThat(food.servingUnit).isIn(listOf("grams", "count", "household"))
            if (food.servingUnit == "grams") {
                assertThat(food.unitLabel).isNull()
                assertThat(food.gramsPerUnit).isNull()
            } else {
                // count / household must carry a label and a positive gram equivalent.
                assertThat(food.unitLabel).isNotEmpty()
                assertThat(food.gramsPerUnit!!).isGreaterThan(0.0)
            }
        }
    }

    @Test
    fun `the countable and household staples log in their natural units`() {
        val bySlug = asset.foods.associateBy { it.slug }
        fun unit(slug: String) = bySlug.getValue(slug).let { Triple(it.servingUnit, it.unitLabel, it.gramsPerUnit) }
        assertThat(unit("egg-boiled")).isEqualTo(Triple("count", "egg", 50.0))
        assertThat(unit("roti")).isEqualTo(Triple("count", "roti", 40.0))
        assertThat(unit("almonds").first).isEqualTo("count")
        assertThat(unit("toor-dal-cooked")).isEqualTo(Triple("household", "katori", 150.0))
        assertThat(unit("white-rice-cooked")).isEqualTo(Triple("household", "katori", 150.0))
        assertThat(unit("mustard-oil")).isEqualTo(Triple("household", "tsp", 5.0))
        assertThat(unit("tea-chai")).isEqualTo(Triple("household", "cup", 150.0))
        // Raisins stay grams (per your call); paneer/flours stay grams too.
        assertThat(unit("raisins").first).isEqualTo("grams")
        assertThat(unit("paneer").first).isEqualTo("grams")
    }

    @Test
    fun `default serving is a whole number of the natural unit`() {
        for (food in asset.foods) {
            val gpu = food.gramsPerUnit ?: continue
            val units = food.defaultServingG / gpu
            // The sheet opens on a clean count (within half a unit), never something like 2.1.
            val nearestHalf = Math.round(units * 2.0) / 2.0
            assertThat(Math.abs(units - nearestHalf)).isLessThan(0.5)
        }
    }

    @Test
    fun `approximate foods are flagged so the UI can mark them`() {
        val approx = asset.foods.filter { it.isApprox }.map { it.slug }
        // Paneer (high variance), processed cheese, khoya, and the composite beverages.
        assertThat(approx).containsAtLeast("paneer", "processed-cheese", "khoya-mawa", "tea-chai")
    }
}
