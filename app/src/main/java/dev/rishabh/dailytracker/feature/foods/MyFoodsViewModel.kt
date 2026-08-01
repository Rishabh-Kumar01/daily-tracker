package dev.rishabh.dailytracker.feature.foods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rishabh.dailytracker.core.db.NutrientKeys
import dev.rishabh.dailytracker.core.nutrition.NutrientTotals
import dev.rishabh.dailytracker.feature.diet.ManualProductInput
import dev.rishabh.dailytracker.feature.diet.ProductValidation
import dev.rishabh.dailytracker.feature.diet.validateManualProduct
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyFoodsViewModel @Inject constructor(
    private val repository: ProductLibraryRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(LibraryFilter.ALL)
    private val edit = MutableStateFlow<EditSheet?>(null)

    private val products = query.flatMapLatest { repository.observeLibrary(it) }

    val state: StateFlow<MyFoodsUiState> =
        combine(query, filter, products, edit) { q, f, list, e ->
            MyFoodsUiState(
                query = q,
                filter = f,
                products = list.filter { f.matches(it) },
                loading = false,
                edit = e,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyFoodsUiState())

    fun onQueryChange(value: String) = query.update { value }

    fun onFilterChange(value: LibraryFilter) = filter.update { value }

    /** Opens the edit sheet prefilled from the product's current read-time values. */
    fun onEdit(card: ProductCard) {
        edit.value = EditSheet(productId = card.productId, input = card.toInput())
    }

    fun onEditFieldChange(index: Int, value: String) {
        edit.update { it?.copy(input = it.input.withFieldAt(index, value), error = null) }
    }

    fun onDismissEdit() = edit.update { null }

    /** Validates the edit, then upserts it onto the same row — a correction, not a new food. */
    fun onSaveEdit() {
        val sheet = edit.value ?: return
        when (val result = validateManualProduct(sheet.input)) {
            is ProductValidation.Invalid -> edit.update { it?.copy(error = result.message) }
            is ProductValidation.Valid -> viewModelScope.launch {
                repository.updateProduct(sheet.productId, result.product)
                edit.update { null }
            }
        }
    }

    /** Soft-delete: the row leaves the list (and every picker) but stays in logged history. */
    fun onArchive(productId: String) {
        viewModelScope.launch { repository.archive(productId) }
        // If the archived product was open for editing, close the now-stale sheet.
        edit.update { if (it?.productId == productId) null else it }
    }
}

private fun LibraryFilter.matches(card: ProductCard): Boolean = when (this) {
    LibraryFilter.ALL -> true
    LibraryFilter.GENERIC -> card.isGeneric
    LibraryFilter.BRANDED -> !card.isGeneric
}

/** Prefills the manual form from a card, leaving genuinely-absent macros blank (not "0"). */
private fun ProductCard.toInput(): ManualProductInput = ManualProductInput(
    brand = brand.orEmpty(),
    productName = name,
    kcal = per100g.field(NutrientKeys.ENERGY_KCAL),
    protein = per100g.field(NutrientKeys.PROTEIN_G),
    carbs = per100g.field(NutrientKeys.CARBS_G),
    fat = per100g.field(NutrientKeys.FAT_G),
)

private fun NutrientTotals.field(key: String): String {
    val value = byKey[key] ?: return ""
    return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
