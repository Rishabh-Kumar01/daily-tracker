package dev.rishabh.dailytracker.feature.diet

import dev.rishabh.dailytracker.core.designsystem.ActivityKey
import dev.rishabh.dailytracker.core.designsystem.component.Per100g
import dev.rishabh.dailytracker.core.nutrition.NutrientTotals

/**
 * One brand option under a food, i.e. one row of the inline expansion.
 *
 * [per100g] is carried as the display struct the QuantitySheet readout wants; the totals
 * actually written to the log still go through MacroCalculator against the stored
 * product_nutrients rows.
 */
data class BrandOption(
    val productId: String,
    val brand: String?,
    val productName: String,
    val per100g: Per100g,
    val per100gLine: String,
    /** Preparation state, e.g. "cooked", "raw", "roasted". Null for branded products. */
    val variant: String? = null,
    /** True for bundled generic foods (brand == null, source == BUNDLED_GENERIC). */
    val isGeneric: Boolean = false,
    /** True when the value is a typical composite rather than a lab analysis. */
    val isApprox: Boolean = false,
    /** Default serving size from the generic food asset. Null for branded products. */
    val defaultServingG: Double? = null,
    /**
     * How this food is logged: "count" | "household" | "grams" (or null → grams). Drives the
     * QuantitySheet's input unit; the amount is still converted to and stored as grams.
     */
    val servingUnit: String? = null,
    /** The unit noun, e.g. "egg", "katori", "tsp". Null for grams-logged foods. */
    val unitLabel: String? = null,
    /** Grams in one [unitLabel]; the input-to-grams conversion factor. */
    val gramsPerUnit: Double? = null,
)

/**
 * A portion already logged for this item today.
 *
 * [totals] is recomputed on every emission from the product's current nutrients — that is
 * what makes correcting a product retroactively correct the meal.
 */
data class LoggedPortion(
    val entryId: String,
    val productId: String,
    val grams: Double,
    val totals: NutrientTotals,
)

/** One food row in the meal, with its brands and whatever is logged against it. */
data class MealItem(
    val itemId: String,
    val name: String,
    /** The products grouping key, e.g. item "Paneer" -> "paneer". */
    val genericName: String,
    /** item_fields.field_key of the quantity field this item logs into. */
    val quantityFieldKey: String?,
    /** item_fields.field_key of the item_variant field, if any. */
    val variantFieldKey: String?,
    val brands: List<BrandOption>,
    val logged: LoggedPortion?,
)

/** Everything the meal screen renders. */
data class MealDetail(
    val subMenuId: String,
    val templateId: String,
    val name: String,
    val accent: ActivityKey,
    val items: List<MealItem>,
    /** Sum across every logged portion in this meal, computed at read time. */
    val totals: NutrientTotals,
)
