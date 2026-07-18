package dev.rishabh.dailytracker.feature.diet.scan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rishabh.dailytracker.core.db.NutrientKeys
import dev.rishabh.dailytracker.core.db.ProductSource
import dev.rishabh.dailytracker.core.network.LookupResult
import dev.rishabh.dailytracker.core.network.OpenFoodFactsClient
import dev.rishabh.dailytracker.core.network.ScannedProduct
import dev.rishabh.dailytracker.feature.diet.ManualProductInput
import dev.rishabh.dailytracker.feature.diet.MealRepository
import dev.rishabh.dailytracker.feature.diet.ProductValidation
import dev.rishabh.dailytracker.feature.diet.validateManualProduct
import dev.rishabh.dailytracker.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Where the scan flow is: camera, lookup, confirmation, or a failed lookup. */
sealed interface ScanState {
    data object Scanning : ScanState

    data class LookingUp(val barcode: String) : ScanState

    /**
     * The editable confirmation step every product passes through, hit or miss.
     *
     * [notice] explains why the fields look the way they do — prefilled from Open Food
     * Facts, or blank because nothing was found.
     */
    data class Confirm(
        val barcode: String,
        val input: ManualProductInput,
        val source: ProductSource,
        val notice: String?,
        val error: String? = null,
    ) : ScanState

    /** Network failure, as opposed to a valid "no such product". Offers retry. */
    data class LookupFailed(val barcode: String, val offline: Boolean) : ScanState
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val repository: MealRepository,
    private val client: OpenFoodFactsClient,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val itemId: String = checkNotNull(savedStateHandle[Routes.ARG_ITEM_ID])

    private val _state = MutableStateFlow<ScanState>(ScanState.Scanning)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    /** Set once the product is written; the screen navigates back on it. */
    private val _savedProductId = MutableStateFlow<String?>(null)
    val savedProductId: StateFlow<String?> = _savedProductId.asStateFlow()

    /**
     * Called for every frame the analyser finds a code in, so it must be idempotent: only
     * the first barcode while actually scanning starts a lookup.
     */
    fun onBarcodeDetected(barcode: String) {
        if (_state.value !is ScanState.Scanning) return
        if (barcode.isBlank()) return
        lookup(barcode)
    }

    fun onRetry(barcode: String) = lookup(barcode)

    private fun lookup(barcode: String) {
        _state.value = ScanState.LookingUp(barcode)
        viewModelScope.launch {
            _state.value = when (val result = client.lookup(barcode)) {
                is LookupResult.Found -> ScanState.Confirm(
                    barcode = barcode,
                    input = result.product.toInput(),
                    source = ProductSource.OFF,
                    notice = noticeFor(result.product),
                )

                LookupResult.NotFound -> ScanState.Confirm(
                    barcode = barcode,
                    input = ManualProductInput(),
                    source = ProductSource.MANUAL,
                    notice = "Not in Open Food Facts — enter it from the label",
                )

                is LookupResult.Error -> ScanState.LookupFailed(barcode, result.offline)
            }
        }
    }

    /** OFF entries are crowd-sourced; a hit with no numbers still needs the label read. */
    private fun noticeFor(product: ScannedProduct): String? =
        if (product.nutrients.containsKey(NutrientKeys.ENERGY_KCAL)) {
            "From Open Food Facts — check it against the label"
        } else {
            "Found in Open Food Facts, but without nutrition — add it from the label"
        }

    /** Skips the lookup entirely and types the label, keeping the scanned barcode. */
    fun onEnterManually(barcode: String) {
        _state.value = ScanState.Confirm(
            barcode = barcode,
            input = ManualProductInput(),
            source = ProductSource.MANUAL,
            notice = null,
        )
    }

    fun onRescan() {
        _state.value = ScanState.Scanning
    }

    fun onFieldChange(index: Int, value: String) {
        _state.update { current ->
            val confirm = current as? ScanState.Confirm ?: return@update current
            confirm.copy(input = confirm.input.withFieldAt(index, value), error = null)
        }
    }

    /**
     * Same validator as Tier-3 manual entry: whatever the lookup proposed, only figures that
     * survive validation reach the database.
     */
    fun onSave() {
        val confirm = _state.value as? ScanState.Confirm ?: return
        when (val result = validateManualProduct(confirm.input)) {
            is ProductValidation.Invalid ->
                _state.value = confirm.copy(error = result.message)

            is ProductValidation.Valid -> viewModelScope.launch {
                val genericName = repository.genericNameForItem(itemId) ?: return@launch
                _savedProductId.value = repository.createManualProduct(
                    genericName = genericName,
                    product = result.product,
                    barcode = confirm.barcode,
                    source = confirm.source,
                )
            }
        }
    }
}

/** Prefills the confirmation fields, formatting numbers the way the sheet displays them. */
private fun ScannedProduct.toInput() = ManualProductInput(
    brand = brand.orEmpty(),
    productName = productName,
    kcal = nutrients.field(NutrientKeys.ENERGY_KCAL),
    protein = nutrients.field(NutrientKeys.PROTEIN_G),
    carbs = nutrients.field(NutrientKeys.CARBS_G),
    fat = nutrients.field(NutrientKeys.FAT_G),
)

private fun Map<String, Double>.field(key: String): String {
    val value = this[key] ?: return ""
    return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
