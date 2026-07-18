package dev.rishabh.dailytracker.feature.diet

import dev.rishabh.dailytracker.core.db.NutrientKeys

/**
 * Tier-3 nutrition entry: the user types the label themselves.
 *
 * Tier 1 is the barcode lookup (M7) and Tier 2 is the label photo (Phase 3); this is the
 * floor that always works, including offline and for unpackaged food. It is deliberately a
 * pure value type with a pure validator so the rules are table-testable without a database
 * or a UI — the same shape the AI lanes will validate into later.
 */
data class ManualProductInput(
    val brand: String = "",
    val productName: String = "",
    val kcal: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
) {
    companion object {
        /** Field order shown in the ConfirmSheet; index-addressed by the editor. */
        val LABELS = listOf("Brand", "Product", "kcal", "Protein", "Carbs", "Fat")
    }

    fun withFieldAt(index: Int, value: String): ManualProductInput = when (index) {
        0 -> copy(brand = value)
        1 -> copy(productName = value)
        2 -> copy(kcal = value)
        3 -> copy(protein = value)
        4 -> copy(carbs = value)
        5 -> copy(fat = value)
        else -> this
    }

    fun fieldAt(index: Int): String = when (index) {
        0 -> brand
        1 -> productName
        2 -> kcal
        3 -> protein
        4 -> carbs
        5 -> fat
        else -> ""
    }
}

/** A validated manual product, ready to become rows. Amounts are per 100g. */
data class ValidatedProduct(
    val brand: String?,
    val productName: String,
    val nutrients: Map<String, Double>,
)

sealed interface ProductValidation {
    data class Valid(val product: ValidatedProduct) : ProductValidation
    data class Invalid(val message: String) : ProductValidation
}

/**
 * Validates typed label figures before anything reaches the database.
 *
 * Rules: a product needs a name; every macro that is present must parse as a finite,
 * non-negative number; a blank macro is genuinely absent rather than zero, so it is
 * omitted from the nutrient rows instead of being stored as 0. Energy is required —
 * without it a logged portion could not contribute to any summary.
 */
fun validateManualProduct(input: ManualProductInput): ProductValidation {
    val name = input.productName.trim()
    if (name.isEmpty()) return ProductValidation.Invalid("Product needs a name")

    val nutrients = LinkedHashMap<String, Double>()
    val fields = listOf(
        NutrientKeys.ENERGY_KCAL to input.kcal,
        NutrientKeys.PROTEIN_G to input.protein,
        NutrientKeys.CARBS_G to input.carbs,
        NutrientKeys.FAT_G to input.fat,
    )
    for ((key, raw) in fields) {
        val text = raw.trim()
        if (text.isEmpty()) continue
        val value = text.toDoubleOrNull()
        if (value == null || !value.isFinite() || value < 0.0) {
            return ProductValidation.Invalid("${labelFor(key)} must be a number of 0 or more")
        }
        nutrients[key] = value
    }
    if (!nutrients.containsKey(NutrientKeys.ENERGY_KCAL)) {
        return ProductValidation.Invalid("Energy (kcal) is required")
    }

    return ProductValidation.Valid(
        ValidatedProduct(
            brand = input.brand.trim().ifEmpty { null },
            productName = name,
            nutrients = nutrients,
        ),
    )
}

private fun labelFor(nutrientKey: String): String = when (nutrientKey) {
    NutrientKeys.ENERGY_KCAL -> "Energy"
    NutrientKeys.PROTEIN_G -> "Protein"
    NutrientKeys.CARBS_G -> "Carbs"
    NutrientKeys.FAT_G -> "Fat"
    else -> nutrientKey
}
