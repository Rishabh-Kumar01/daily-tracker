package dev.rishabh.dailytracker.feature.foods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rishabh.dailytracker.core.db.NutrientKeys
import dev.rishabh.dailytracker.core.media.ProductPhotoStore
import dev.rishabh.dailytracker.core.nutrition.NutrientTotals
import dev.rishabh.dailytracker.feature.diet.ManualProductInput
import dev.rishabh.dailytracker.feature.diet.ProductValidation
import dev.rishabh.dailytracker.feature.diet.validateManualProduct
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyFoodsViewModel @Inject constructor(
    private val repository: ProductLibraryRepository,
    private val photoStore: ProductPhotoStore,
) : ViewModel() {

    /** Everything the UI drives directly; the product list is derived from [LocalState.query]. */
    private data class LocalState(
        val query: String = "",
        val filter: LibraryFilter = LibraryFilter.ALL,
        val edit: EditSheet? = null,
        val pendingDelete: ProductCard? = null,
        val toast: String? = null,
    )

    private val local = MutableStateFlow(LocalState())
    private var toastJob: Job? = null

    private val products = local
        .map { it.query }
        .distinctUntilChanged()
        .flatMapLatest { repository.observeLibrary(it) }

    val state: StateFlow<MyFoodsUiState> =
        combine(local, products) { l, list ->
            MyFoodsUiState(
                query = l.query,
                filter = l.filter,
                products = list.filter { l.filter.matches(it) },
                loading = false,
                edit = l.edit,
                pendingDelete = l.pendingDelete,
                toast = l.toast,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyFoodsUiState())

    fun onQueryChange(value: String) = local.update { it.copy(query = value) }

    fun onFilterChange(value: LibraryFilter) = local.update { it.copy(filter = value) }

    // --- Edit ---

    fun onEdit(card: ProductCard) = local.update {
        it.copy(edit = EditSheet(card.productId, card.toInput(), photoPath = card.photoPath))
    }

    /**
     * The product already exists here, so a captured photo attaches immediately — the sheet
     * just swaps to showing the stored copy.
     */
    fun onEditPhotoCaptured(capturePath: String) {
        val sheet = local.value.edit ?: return
        viewModelScope.launch {
            val stored = photoStore.attachCapture(sheet.productId, File(capturePath)) ?: return@launch
            local.update { it.copy(edit = it.edit?.copy(photoPath = stored)) }
        }
    }

    fun onEditFieldChange(index: Int, value: String) = local.update {
        it.copy(edit = it.edit?.copy(input = it.edit.input.withFieldAt(index, value), error = null))
    }

    fun onDismissEdit() = local.update { it.copy(edit = null) }

    fun onSaveEdit() {
        val sheet = local.value.edit ?: return
        when (val result = validateManualProduct(sheet.input)) {
            is ProductValidation.Invalid -> local.update { it.copy(edit = it.edit?.copy(error = result.message)) }
            is ProductValidation.Valid -> viewModelScope.launch {
                repository.updateProduct(sheet.productId, result.product)
                local.update { it.copy(edit = null) }
            }
        }
    }

    // --- Delete (soft-delete/archive), with confirm before and a toast after ---

    /** Deleting is destructive, so the icon only opens a confirmation — it never archives directly. */
    fun onDeleteClick(card: ProductCard) = local.update { it.copy(pendingDelete = card) }

    fun onCancelDelete() = local.update { it.copy(pendingDelete = null) }

    fun onConfirmDelete() {
        val card = local.value.pendingDelete ?: return
        viewModelScope.launch {
            repository.archive(card.productId)
            local.update { it.copy(pendingDelete = null, toast = "Deleted ${card.name}") }
            toastJob?.cancel()
            toastJob = launch {
                delay(TOAST_MS)
                local.update { it.copy(toast = null) }
            }
        }
    }

    fun onToastShown() = local.update { it.copy(toast = null) }

    private companion object {
        const val TOAST_MS = 2500L
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
