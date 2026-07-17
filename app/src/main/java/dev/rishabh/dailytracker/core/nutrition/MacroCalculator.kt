package dev.rishabh.dailytracker.core.nutrition

import dev.rishabh.dailytracker.core.db.NutrientKeys
import dev.rishabh.dailytracker.core.db.entity.ProductNutrientEntity

/**
 * Nutrient amounts for some quantity of food.
 *
 * Long-format like the table it comes from, so a micronutrient the app has never heard of
 * still totals correctly. The named accessors are conveniences for the four the Diet UI
 * shows, not a schema.
 */
@JvmInline
value class NutrientTotals(val byKey: Map<String, Double>) {

    val energyKcal: Double get() = byKey[NutrientKeys.ENERGY_KCAL] ?: 0.0
    val proteinG: Double get() = byKey[NutrientKeys.PROTEIN_G] ?: 0.0
    val carbsG: Double get() = byKey[NutrientKeys.CARBS_G] ?: 0.0
    val fatG: Double get() = byKey[NutrientKeys.FAT_G] ?: 0.0

    operator fun get(key: String): Double = byKey[key] ?: 0.0

    /** Summing totals is how a meal or a day is built up from its entries. */
    operator fun plus(other: NutrientTotals): NutrientTotals {
        if (byKey.isEmpty()) return other
        if (other.byKey.isEmpty()) return this
        val merged = HashMap<String, Double>(byKey.size + other.byKey.size)
        merged.putAll(byKey)
        other.byKey.forEach { (key, value) -> merged[key] = (merged[key] ?: 0.0) + value }
        return NutrientTotals(merged)
    }

    companion object {
        val EMPTY = NutrientTotals(emptyMap())
    }
}

/**
 * The one place macros are computed.
 *
 * Core rule: `amount_per_100g × grams / 100`, evaluated at read time and never written back
 * into a log. That is what makes fixing a product's nutrients retroactively fix every meal
 * that ever referenced it.
 */
object MacroCalculator {

    /** Basis divisor: every stored amount is per 100g after ingestion normalisation. */
    private const val BASIS_GRAMS = 100.0

    /**
     * Scales one product's nutrients to [grams].
     *
     * A null or non-finite [grams] yields empty totals rather than throwing: an entry can
     * legitimately exist before its quantity is filled in, and a day summary must still render.
     */
    fun forQuantity(nutrients: List<ProductNutrientEntity>, grams: Double?): NutrientTotals {
        if (grams == null || !grams.isFinite() || nutrients.isEmpty()) return NutrientTotals.EMPTY
        val factor = grams / BASIS_GRAMS
        val out = HashMap<String, Double>(nutrients.size)
        for (n in nutrients) {
            // Long format allows repeats of a key in principle; summing is the safe read.
            out[n.nutrientKey] = (out[n.nutrientKey] ?: 0.0) + n.amountPer100g * factor
        }
        return NutrientTotals(out)
    }

    /**
     * Totals across several logged quantities.
     *
     * [nutrientsByProduct] is the product -> nutrients map the caller already loaded; a
     * quantity whose product is missing contributes nothing rather than failing the summary.
     */
    fun total(
        quantities: List<Quantity>,
        nutrientsByProduct: Map<String, List<ProductNutrientEntity>>,
    ): NutrientTotals {
        var acc = NutrientTotals.EMPTY
        for (q in quantities) {
            val nutrients = nutrientsByProduct[q.productId] ?: continue
            acc += forQuantity(nutrients, q.grams)
        }
        return acc
    }

    /** One logged amount of one product. */
    data class Quantity(val productId: String, val grams: Double?)
}
