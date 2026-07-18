package dev.rishabh.dailytracker.feature.diet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rishabh.dailytracker.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Which sheet, if any, is over the meal list. */
sealed interface MealSheet {
    /** Portion editor for [productId] under [itemId]. */
    data class Quantity(val itemId: String, val productId: String) : MealSheet

    /** Tier-3 manual product creation for [itemId]. */
    data class ManualProduct(
        val itemId: String,
        val input: ManualProductInput = ManualProductInput(),
        val error: String? = null,
    ) : MealSheet
}

/** UI-only state, kept separate from the Room-backed [MealDetail]. */
data class MealUiState(
    val detail: MealDetail? = null,
    val expandedItemId: String? = null,
    val searchOpen: Boolean = false,
    val query: String = "",
    val sheet: MealSheet? = null,
)

@HiltViewModel
class MealViewModel @Inject constructor(
    private val repository: MealRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val subMenuId: String = checkNotNull(savedStateHandle[Routes.ARG_SUB_MENU_ID])

    private val uiState = MutableStateFlow(MealUiState())

    val state: StateFlow<MealUiState> =
        combine(repository.observeMeal(subMenuId), uiState) { detail, ui ->
            ui.copy(detail = detail)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MealUiState())

    /**
     * Tapping a food: an already-logged one reopens its portion for editing, an unlogged one
     * expands to its brands. Editing rather than unlogging on tap means a mistap costs a
     * dismiss, not a logged meal.
     */
    fun onItemClick(item: MealItem) {
        val logged = item.logged
        if (logged != null) {
            uiState.update { it.copy(sheet = MealSheet.Quantity(item.itemId, logged.productId)) }
        } else {
            uiState.update {
                it.copy(expandedItemId = if (it.expandedItemId == item.itemId) null else item.itemId)
            }
        }
    }

    fun onBrandClick(itemId: String, productId: String) {
        uiState.update { it.copy(sheet = MealSheet.Quantity(itemId, productId)) }
    }

    fun onAddBrandClick(itemId: String) {
        uiState.update { it.copy(sheet = MealSheet.ManualProduct(itemId)) }
    }

    fun onDismissSheet() {
        uiState.update { it.copy(sheet = null) }
    }

    fun onToggleSearch() {
        uiState.update { it.copy(searchOpen = !it.searchOpen, query = "") }
    }

    fun onQueryChange(query: String) {
        uiState.update { it.copy(query = query) }
    }

    /** Commits one portion immediately, so nothing staged is lost if the app is killed. */
    fun onConfirmQuantity(itemId: String, productId: String, grams: Double) {
        val detail = state.value.detail ?: return
        val item = detail.items.firstOrNull { it.itemId == itemId } ?: return
        viewModelScope.launch {
            repository.logPortion(
                templateId = detail.templateId,
                subMenuId = detail.subMenuId,
                item = item,
                productId = productId,
                grams = grams,
            )
            uiState.update { it.copy(sheet = null, expandedItemId = null) }
        }
    }

    fun onRemovePortion(itemId: String) {
        val entryId = state.value.detail?.items
            ?.firstOrNull { it.itemId == itemId }?.logged?.entryId ?: return
        viewModelScope.launch {
            repository.removePortion(entryId)
            uiState.update { it.copy(sheet = null) }
        }
    }

    fun onManualFieldChange(index: Int, value: String) {
        uiState.update { ui ->
            val sheet = ui.sheet as? MealSheet.ManualProduct ?: return@update ui
            ui.copy(sheet = sheet.copy(input = sheet.input.withFieldAt(index, value), error = null))
        }
    }

    /**
     * Validates typed figures before they reach the database, then moves straight into the
     * portion sheet — creating a brand is only ever a step towards logging one.
     */
    fun onConfirmManualProduct() {
        val sheet = state.value.sheet as? MealSheet.ManualProduct ?: return
        val item = state.value.detail?.items?.firstOrNull { it.itemId == sheet.itemId } ?: return
        when (val result = validateManualProduct(sheet.input)) {
            is ProductValidation.Invalid ->
                uiState.update { it.copy(sheet = sheet.copy(error = result.message)) }

            is ProductValidation.Valid -> viewModelScope.launch {
                val productId = repository.createManualProduct(item.genericName, result.product)
                uiState.update { it.copy(sheet = MealSheet.Quantity(item.itemId, productId)) }
            }
        }
    }
}
