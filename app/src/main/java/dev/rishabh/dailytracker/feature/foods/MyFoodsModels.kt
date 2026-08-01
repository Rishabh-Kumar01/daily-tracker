package dev.rishabh.dailytracker.feature.foods

import dev.rishabh.dailytracker.core.nutrition.NutrientTotals
import dev.rishabh.dailytracker.feature.diet.ManualProductInput

/** How the library list is filtered by kind. Generic == brand is null. */
enum class LibraryFilter { ALL, GENERIC, BRANDED }

/**
 * One product in the My Foods list. [per100g] is carried so the edit sheet can be prefilled
 * from the same read-time values the row shows — nothing here is stored computed.
 */
data class ProductCard(
    val productId: String,
    val brand: String?,
    val name: String,
    /** True for bundled generic and other brand-less foods. */
    val isGeneric: Boolean,
    val per100gLine: String,
    val per100g: NutrientTotals,
)

/** The edit sheet over the library: the confirmation form, prefilled from a product. */
data class EditSheet(
    val productId: String,
    val input: ManualProductInput,
    val error: String? = null,
)

data class MyFoodsUiState(
    val query: String = "",
    val filter: LibraryFilter = LibraryFilter.ALL,
    val products: List<ProductCard> = emptyList(),
    val loading: Boolean = true,
    val edit: EditSheet? = null,
)
