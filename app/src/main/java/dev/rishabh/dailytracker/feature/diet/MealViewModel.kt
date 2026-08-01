package dev.rishabh.dailytracker.feature.diet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rishabh.dailytracker.core.db.NutrientKeys
import dev.rishabh.dailytracker.core.db.ProductSource
import dev.rishabh.dailytracker.core.network.UsdaClient
import dev.rishabh.dailytracker.core.network.UsdaFood
import dev.rishabh.dailytracker.core.network.UsdaResult
import dev.rishabh.dailytracker.core.settings.UsdaKeyStore
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

    /** Tier-3 manual product creation for [itemId], with an optional USDA lookup to prefill it. */
    data class ManualProduct(
        val itemId: String,
        val input: ManualProductInput = ManualProductInput(),
        val error: String? = null,
        val usda: UsdaSearchState = UsdaSearchState(),
        /** True when [input] came from a picked USDA food — the save is then tagged source=usda. */
        val fromUsda: Boolean = false,
    ) : MealSheet
}

/** State of the USDA long-tail lookup embedded in the manual-add sheet. */
data class UsdaSearchState(
    val query: String = "",
    val status: UsdaStatus = UsdaStatus.Idle,
    val results: List<UsdaFood> = emptyList(),
    /** When true, the sheet prompts for the FDC API key before it can search. */
    val promptForKey: Boolean = false,
    val keyInput: String = "",
)

enum class UsdaStatus { Idle, Loading, Empty, Offline, Error }

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
    private val usdaClient: UsdaClient,
    private val usdaKeyStore: UsdaKeyStore,
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
                val source = if (sheet.fromUsda) ProductSource.USDA else ProductSource.MANUAL
                val productId = repository.createManualProduct(item.genericName, result.product, source = source)
                uiState.update { it.copy(sheet = MealSheet.Quantity(item.itemId, productId)) }
            }
        }
    }

    // --- USDA long-tail lookup, embedded in the manual-add sheet ---

    private fun updateManual(transform: (MealSheet.ManualProduct) -> MealSheet.ManualProduct) {
        uiState.update { ui ->
            val sheet = ui.sheet as? MealSheet.ManualProduct ?: return@update ui
            ui.copy(sheet = transform(sheet))
        }
    }

    fun onUsdaQueryChange(query: String) =
        updateManual { it.copy(usda = it.usda.copy(query = query)) }

    /**
     * Runs the FDC search, prompting for the API key first if none is stored. The key is read
     * from [UsdaKeyStore] (encrypted) only at call time and never held in UI state.
     */
    fun onUsdaSearch() {
        val sheet = state.value.sheet as? MealSheet.ManualProduct ?: return
        val query = sheet.usda.query.trim()
        if (query.isEmpty()) return
        viewModelScope.launch {
            val key = usdaKeyStore.getApiKey()
            if (key.isNullOrBlank()) {
                updateManual { it.copy(usda = it.usda.copy(promptForKey = true)) }
                return@launch
            }
            updateManual { it.copy(usda = it.usda.copy(status = UsdaStatus.Loading, results = emptyList())) }
            val result = usdaClient.search(query, key)
            updateManual { it.copy(usda = it.usda.applyResult(result)) }
        }
    }

    fun onUsdaKeyInputChange(value: String) =
        updateManual { it.copy(usda = it.usda.copy(keyInput = value)) }

    /** Persists the entered key (encrypted) and immediately retries the pending search. */
    fun onUsdaSaveKey() {
        val sheet = state.value.sheet as? MealSheet.ManualProduct ?: return
        val key = sheet.usda.keyInput.trim()
        if (key.isEmpty()) return
        viewModelScope.launch {
            usdaKeyStore.setApiKey(key)
            updateManual { it.copy(usda = it.usda.copy(promptForKey = false, keyInput = "")) }
            onUsdaSearch()
        }
    }

    fun onUsdaDismissKeyPrompt() =
        updateManual { it.copy(usda = it.usda.copy(promptForKey = false, keyInput = "")) }

    /**
     * A picked FDC food prefills the editable label fields — the deterministic-lookup version
     * of "AI proposes, user disposes": nothing is saved until the user confirms the sheet.
     */
    fun onUsdaPick(food: UsdaFood) = updateManual { sheet ->
        sheet.copy(
            input = food.toManualInput(),
            error = null,
            usda = sheet.usda.copy(status = UsdaStatus.Idle, results = emptyList(), query = ""),
            fromUsda = true,
        )
    }
}

private fun UsdaSearchState.applyResult(result: UsdaResult): UsdaSearchState = when (result) {
    is UsdaResult.Found -> copy(status = UsdaStatus.Idle, results = result.foods)
    UsdaResult.NotFound -> copy(status = UsdaStatus.Empty, results = emptyList())
    is UsdaResult.Error -> copy(
        status = if (result.offline) UsdaStatus.Offline else UsdaStatus.Error,
        results = emptyList(),
        // A rejected/missing key re-opens the prompt rather than showing a dead error.
        promptForKey = result.unauthorized,
    )
}

private fun UsdaFood.toManualInput(): ManualProductInput = ManualProductInput(
    brand = brand.orEmpty(),
    productName = description,
    kcal = nutrients[NutrientKeys.ENERGY_KCAL].asField(),
    protein = nutrients[NutrientKeys.PROTEIN_G].asField(),
    carbs = nutrients[NutrientKeys.CARBS_G].asField(),
    fat = nutrients[NutrientKeys.FAT_G].asField(),
)

/** Formats a per-100g amount for a text field, dropping a needless trailing ".0". */
private fun Double?.asField(): String = when {
    this == null -> ""
    this % 1.0 == 0.0 -> toLong().toString()
    else -> toString()
}
