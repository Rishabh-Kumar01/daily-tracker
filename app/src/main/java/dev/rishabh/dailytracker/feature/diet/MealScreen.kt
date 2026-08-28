package dev.rishabh.dailytracker.feature.diet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rishabh.dailytracker.core.camera.PhotoCaptureCamera
import dev.rishabh.dailytracker.core.designsystem.AccentColors
import dev.rishabh.dailytracker.core.designsystem.ActivityKey
import dev.rishabh.dailytracker.core.designsystem.DailyTrackerTheme
import dev.rishabh.dailytracker.core.designsystem.DisabledOpacity
import dev.rishabh.dailytracker.core.designsystem.Dimens
import dev.rishabh.dailytracker.core.designsystem.OnSurface
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceFaint
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceVariant
import dev.rishabh.dailytracker.core.designsystem.OutlineVariant
import dev.rishabh.dailytracker.core.designsystem.ProvideActivityAccent
import dev.rishabh.dailytracker.core.designsystem.Radius
import dev.rishabh.dailytracker.core.designsystem.Scrim
import dev.rishabh.dailytracker.core.designsystem.Spacing
import dev.rishabh.dailytracker.core.designsystem.Surface2
import dev.rishabh.dailytracker.core.designsystem.Surface3
import dev.rishabh.dailytracker.core.designsystem.TypeNumeric
import dev.rishabh.dailytracker.core.designsystem.component.AccentButton
import dev.rishabh.dailytracker.core.designsystem.component.BackTopBar
import dev.rishabh.dailytracker.core.designsystem.component.BrandPickerRow
import dev.rishabh.dailytracker.core.designsystem.component.ConfirmField
import dev.rishabh.dailytracker.core.designsystem.component.ConfirmSheet
import dev.rishabh.dailytracker.core.designsystem.component.FrontPhotoRow
import dev.rishabh.dailytracker.core.designsystem.component.ItemRow
import dev.rishabh.dailytracker.core.designsystem.component.Per100g
import dev.rishabh.dailytracker.core.designsystem.component.QuantitySheet
import dev.rishabh.dailytracker.core.designsystem.component.ServingUnit
import dev.rishabh.dailytracker.core.db.NutrientKeys
import dev.rishabh.dailytracker.core.db.dao.MealTemplateSummary
import dev.rishabh.dailytracker.core.network.UsdaFood
import dev.rishabh.dailytracker.core.nutrition.NutrientTotals
import dev.rishabh.dailytracker.core.nutrition.kcalLabel
import dev.rishabh.dailytracker.core.nutrition.macroSummaryLine

/**
 * The meal screen: pick a food, pick its brand, set the portion, see the meal total.
 *
 * Implements the three states of the Lunch Screen design — default list, inline brand
 * expansion, and the portion sheet over a scrim. Every macro on screen is computed at read
 * time from product_nutrients; none of it is stored.
 */
@Composable
fun MealScreen(
    onBack: () -> Unit,
    onScanClick: (itemId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MealViewModel = hiltViewModel(),
    pendingScanLogItemId: String? = null,
    pendingScanLogProductId: String? = null,
    onScanLogConsumed: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // "Log it now" from a re-scan: the saved product's portion sheet opens directly, then
    // the hand-off is consumed so it doesn't re-fire on recomposition.
    LaunchedEffect(pendingScanLogItemId, pendingScanLogProductId) {
        val itemId = pendingScanLogItemId
        val productId = pendingScanLogProductId
        if (itemId != null && productId != null) {
            viewModel.onScanLogRequest(itemId, productId)
            onScanLogConsumed()
        }
    }

    // Camera overlay over everything else; the captured temp file waits on the manual
    // sheet until the product is saved.
    var cameraOpen by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize()) {
        MealContent(
            state = state,
            onBack = onBack,
            onItemClick = viewModel::onItemClick,
            onBrandClick = viewModel::onBrandClick,
            onAddBrandClick = viewModel::onAddBrandClick,
            onScanClick = onScanClick,
            onToggleSearch = viewModel::onToggleSearch,
            onQueryChange = viewModel::onQueryChange,
            onConfirmQuantity = viewModel::onConfirmQuantity,
            onRemovePortion = viewModel::onRemovePortion,
            onLogTemplate = viewModel::onLogTemplate,
            onSaveAsTemplate = viewModel::onSaveMealAsTemplateClick,
            onSaveTemplateNameChange = viewModel::onSaveTemplateNameChange,
            onConfirmSaveTemplate = viewModel::onConfirmSaveTemplate,
            onDismissSaveTemplate = viewModel::onDismissSaveTemplate,
            onManualFieldChange = viewModel::onManualFieldChange,
            onConfirmManualProduct = viewModel::onConfirmManualProduct,
            onDismissSheet = viewModel::onDismissSheet,
            onUsdaQueryChange = viewModel::onUsdaQueryChange,
            onUsdaSearch = viewModel::onUsdaSearch,
            onUsdaPick = viewModel::onUsdaPick,
            onUsdaKeyInputChange = viewModel::onUsdaKeyInputChange,
            onUsdaSaveKey = viewModel::onUsdaSaveKey,
            onUsdaDismissKeyPrompt = viewModel::onUsdaDismissKeyPrompt,
            onAddPhoto = { cameraOpen = true },
            modifier = modifier,
        )
        if (cameraOpen) {
            PhotoCaptureCamera(
                onCaptured = { file ->
                    cameraOpen = false
                    viewModel.onManualPhotoCaptured(file.absolutePath)
                },
                onCancel = { cameraOpen = false },
            )
        }
    }
}

@Composable
internal fun MealContent(
    state: MealUiState,
    onBack: () -> Unit,
    onItemClick: (MealItem) -> Unit,
    onBrandClick: (itemId: String, productId: String) -> Unit,
    onAddBrandClick: (itemId: String) -> Unit,
    onScanClick: (itemId: String) -> Unit,
    onToggleSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onConfirmQuantity: (itemId: String, productId: String, grams: Double) -> Unit,
    onRemovePortion: (itemId: String) -> Unit,
    onLogTemplate: (mealTemplateId: String) -> Unit = {},
    onSaveAsTemplate: () -> Unit = {},
    onSaveTemplateNameChange: (String) -> Unit = {},
    onConfirmSaveTemplate: () -> Unit = {},
    onDismissSaveTemplate: () -> Unit = {},
    onManualFieldChange: (Int, String) -> Unit,
    onConfirmManualProduct: () -> Unit,
    onDismissSheet: () -> Unit,
    onUsdaQueryChange: (String) -> Unit = {},
    onUsdaSearch: () -> Unit = {},
    onUsdaPick: (UsdaFood) -> Unit = {},
    onUsdaKeyInputChange: (String) -> Unit = {},
    onUsdaSaveKey: () -> Unit = {},
    onUsdaDismissKeyPrompt: () -> Unit = {},
    onAddPhoto: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val detail = state.detail
    val visibleItems = detail?.items.orEmpty().filter {
        state.query.isBlank() || it.name.contains(state.query, ignoreCase = true)
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        ProvideActivityAccent(detail?.accent ?: ActivityKey.DIET) {
            val accent = DailyTrackerTheme.accent
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    BackTopBar(title = detail?.name ?: "", onBack = onBack)

                    ActionRow(
                        accent = accent,
                        searchOpen = state.searchOpen,
                        onSearch = onToggleSearch,
                        // The design puts Scan at meal level, but a product needs to know
                        // which food it belongs to, so the tile acts on the expanded food
                        // and stays disabled until one is open.
                        scanItemId = state.expandedItemId,
                        onScan = onScanClick,
                    )

                    if (state.searchOpen) {
                        SearchField(query = state.query, accent = accent, onQueryChange = onQueryChange)
                    }

                    UsualMealsRow(
                        templates = state.templates,
                        canSave = detail?.items?.any { it.logged != null } == true,
                        accent = accent,
                        onLogTemplate = onLogTemplate,
                        onSaveAsTemplate = onSaveAsTemplate,
                    )

                    SectionLabel("Frequent")

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = Spacing.sp2, vertical = Spacing.sp1),
                    ) {
                        for (item in visibleItems) {
                            item(key = item.itemId) {
                                MealItemRow(
                                    item = item,
                                    expanded = state.expandedItemId == item.itemId,
                                    accent = accent,
                                    onClick = { onItemClick(item) },
                                )
                            }
                            if (state.expandedItemId == item.itemId) {
                                item(key = "${item.itemId}-brands") {
                                    BrandExpansion(
                                        item = item,
                                        accent = accent,
                                        onBrandClick = { productId -> onBrandClick(item.itemId, productId) },
                                        onAddBrandClick = { onAddBrandClick(item.itemId) },
                                        onScanClick = { onScanClick(item.itemId) },
                                    )
                                }
                            }
                        }
                    }

                    MealSummaryBar(
                        mealName = detail?.name ?: "",
                        totals = detail?.totals ?: NutrientTotals.EMPTY,
                        accent = accent,
                        onDone = onBack,
                    )
                }

                val sheet = state.sheet
                if (sheet != null && detail != null) {
                    // Scrim first so it sits under the sheet and swallows taps on the list.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Scrim)
                            .clickable(role = Role.Button, onClick = onDismissSheet),
                    )
                    // imePadding so the sheet rides above the keyboard: the manual-entry
                    // fields sit at the bottom of the sheet and are otherwise unreachable
                    // the moment the IME opens.
                    Box(modifier = Modifier.align(Alignment.BottomCenter).imePadding()) {
                        when (sheet) {
                            is MealSheet.Quantity -> QuantitySheetHost(
                                sheet = sheet,
                                detail = detail,
                                accent = accent,
                                onConfirm = onConfirmQuantity,
                                onRemove = onRemovePortion,
                                onCancel = onDismissSheet,
                            )

                            is MealSheet.ManualProduct -> ManualProductSheet(
                                sheet = sheet,
                                accent = accent,
                                onFieldChange = onManualFieldChange,
                                onConfirm = onConfirmManualProduct,
                                onCancel = onDismissSheet,
                                onUsdaQueryChange = onUsdaQueryChange,
                                onUsdaSearch = onUsdaSearch,
                                onUsdaPick = onUsdaPick,
                                onUsdaKeyInputChange = onUsdaKeyInputChange,
                                onUsdaSaveKey = onUsdaSaveKey,
                                onUsdaDismissKeyPrompt = onUsdaDismissKeyPrompt,
                                onAddPhoto = onAddPhoto,
                            )
                        }
                    }
                }

                val saveName = state.saveTemplateName
                if (saveName != null) {
                    SaveTemplateDialog(
                        name = saveName,
                        accent = accent,
                        onNameChange = onSaveTemplateNameChange,
                        onConfirm = onConfirmSaveTemplate,
                        onCancel = onDismissSaveTemplate,
                    )
                }
            }
        }
    }
}

/**
 * Search / Scan barcode / Photo of label.
 *
 * Barcode is M7 and the label-photo lane is Phase 3, so both render in the design's layout
 * but are visibly disabled rather than silently doing nothing.
 */
@Composable
private fun ActionRow(
    accent: AccentColors,
    searchOpen: Boolean,
    onSearch: () -> Unit,
    scanItemId: String?,
    onScan: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.sp4, end = Spacing.sp4, top = Spacing.sp2, bottom = Spacing.sp4),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sp2),
    ) {
        ActionTile("Search", Icons.Rounded.Search, accent, active = searchOpen, enabled = true, modifier = Modifier.weight(1f), onClick = onSearch)
        ActionTile(
            "Scan barcode", Icons.Rounded.QrCodeScanner, accent,
            active = false, enabled = scanItemId != null, modifier = Modifier.weight(1f),
            onClick = { scanItemId?.let(onScan) },
        )
        ActionTile("Photo of label", Icons.Rounded.CameraAlt, accent, active = false, enabled = false, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ActionTile(
    label: String,
    icon: ImageVector,
    accent: AccentColors,
    active: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.lg))
            .background(if (active) accent.container else Surface2)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else DisabledOpacity)
            .padding(vertical = Spacing.sp3),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sp1 + 2.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) accent.base else OnSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) accent.base else OnSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun SearchField(query: String, accent: AccentColors, onQueryChange: (String) -> Unit) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = OnSurface),
        cursorBrush = SolidColor(accent.base),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sp4)
            .clip(RoundedCornerShape(Radius.md))
            .background(Surface2)
            .padding(horizontal = Spacing.sp3, vertical = Spacing.sp3),
        decorationBox = { field ->
            if (query.isEmpty()) {
                Text("Filter foods", style = MaterialTheme.typography.bodyLarge, color = OnSurfaceFaint)
            }
            field()
        },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = OnSurfaceFaint,
        modifier = Modifier.padding(start = Spacing.sp5, end = Spacing.sp5, top = Spacing.sp1, bottom = Spacing.sp2),
    )
}

/**
 * A food row. Logged foods read as checked with their kcal; unlogged ones show a chevron
 * because tapping expands rather than toggles.
 */
@Composable
private fun MealItemRow(item: MealItem, expanded: Boolean, accent: AccentColors, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ItemRow(
            name = item.name,
            value = item.logged?.let { kcalLabel(it.totals) },
            checked = item.logged != null,
            accent = accent,
            onCheckedChange = { onClick() },
            modifier = Modifier.weight(1f),
        )
        if (item.logged == null) {
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = OnSurfaceFaint,
                modifier = Modifier.padding(end = Spacing.sp4).size(20.dp),
            )
        }
    }
}

/** The inline brand list, indented under its food. */
@Composable
private fun BrandExpansion(
    item: MealItem,
    accent: AccentColors,
    onBrandClick: (String) -> Unit,
    onAddBrandClick: () -> Unit,
    onScanClick: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(start = Spacing.sp6, top = Spacing.sp1, bottom = Spacing.sp2),
        verticalArrangement = Arrangement.spacedBy(Spacing.sp1),
    ) {
        for (brand in item.brands) {
            BrandPickerRow(
                brand = brand.brand.orEmpty(),
                product = brand.productName,
                per100g = brand.per100gLine,
                thumbnailUrl = brand.photoPath,
                isGeneric = brand.isGeneric,
                isApprox = brand.isApprox,
                variant = brand.variant,
                accent = accent,
                onClick = { onBrandClick(brand.productId) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // Always available, and on a fresh library it is the entire state: there is no
        // barcode or label lane yet, so typing the figures is the only way in.
        AddBrandRow(empty = item.brands.isEmpty(), accent = accent, onClick = onAddBrandClick)
        // Tier 1 before Tier 3: scanning is less work and more accurate than typing, so it
        // sits right beside the manual route rather than being buried at meal level.
        ExpansionAction("Scan a barcode", Icons.Rounded.QrCodeScanner, accent, onScanClick)
    }
}

@Composable
private fun AddBrandRow(empty: Boolean, accent: AccentColors, onClick: () -> Unit) {
    Column {
        if (empty) {
            Text(
                "No brands saved for this food yet",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceFaint,
                modifier = Modifier.padding(horizontal = Spacing.sp3, vertical = Spacing.sp2),
            )
        }
        ExpansionAction("Add a brand", Icons.Rounded.Add, accent, onClick)
    }
}

/** An accent-coloured action row inside a food's expansion. */
@Composable
private fun ExpansionAction(
    label: String,
    icon: ImageVector,
    accent: AccentColors,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.sp3, vertical = Spacing.sp3),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sp3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = accent.base, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = accent.base)
    }
}

/** The sticky bar: running meal total on the left, Done on the right. */
@Composable
private fun MealSummaryBar(
    mealName: String,
    totals: NutrientTotals,
    accent: AccentColors,
    onDone: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface2)
            .padding(horizontal = Spacing.sp4, vertical = Spacing.sp3),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sp2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Restaurant, contentDescription = null, tint = accent.base, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "$mealName so far".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
            )
            // The full four-macro line is the point of the bar, and it only gets wider as
            // the day fills up (four-digit kcal, three-digit carbs). Shrinking to fit keeps
            // it whole instead of clipping the fat off the end.
            BasicText(
                text = macroSummaryLine(totals),
                style = TypeNumeric.copy(color = OnSurface),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp),
            )
        }
        AccentButton("Done", accent = accent.base, onClick = onDone)
    }
}

@Composable
private fun QuantitySheetHost(
    sheet: MealSheet.Quantity,
    detail: MealDetail,
    accent: AccentColors,
    onConfirm: (String, String, Double) -> Unit,
    onRemove: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val item = detail.items.firstOrNull { it.itemId == sheet.itemId } ?: return
    val brand = item.brands.firstOrNull { it.productId == sheet.productId } ?: return
    val logged = item.logged?.takeIf { it.productId == sheet.productId }

    QuantitySheet(
        brand = brand.brand,
        product = brand.productName,
        per100g = brand.per100g,
        initialGrams = logged?.grams ?: brand.defaultServingG ?: DEFAULT_PORTION_GRAMS,
        serving = ServingUnit.from(brand.servingUnit, brand.unitLabel, brand.gramsPerUnit),
        accent = accent,
        confirmLabel = if (logged != null) "Save" else "Add to log",
        onRemove = if (logged != null) ({ onRemove(item.itemId) }) else null,
        onAdd = { grams -> onConfirm(item.itemId, brand.productId, grams) },
        onCancel = onCancel,
    )
}

@Composable
private fun ManualProductSheet(
    sheet: MealSheet.ManualProduct,
    accent: AccentColors,
    onFieldChange: (Int, String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onUsdaQueryChange: (String) -> Unit,
    onUsdaSearch: () -> Unit,
    onUsdaPick: (UsdaFood) -> Unit,
    onUsdaKeyInputChange: (String) -> Unit,
    onUsdaSaveKey: () -> Unit,
    onUsdaDismissKeyPrompt: () -> Unit,
    onAddPhoto: () -> Unit,
) {
    // Six fields plus the actions can outgrow what is left above the keyboard on a short
    // window, so the sheet scrolls rather than clipping its own Save button.
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        if (sheet.error != null) {
            Text(
                sheet.error,
                style = MaterialTheme.typography.bodyMedium,
                color = accent.base,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface2)
                    .padding(horizontal = Spacing.sp4, vertical = Spacing.sp2),
            )
        }
        // The USDA long-tail lookup: fills the fields below rather than saving directly, so
        // every hit still passes through the same editable confirmation the manual path uses.
        UsdaLookup(
            usda = sheet.usda,
            accent = accent,
            onQueryChange = onUsdaQueryChange,
            onSearch = onUsdaSearch,
            onPick = onUsdaPick,
            onKeyInputChange = onUsdaKeyInputChange,
            onSaveKey = onUsdaSaveKey,
            onDismissKeyPrompt = onUsdaDismissKeyPrompt,
        )
        ConfirmSheet(
            title = "Add a brand",
            fields = ManualProductInput.LABELS.mapIndexed { index, label ->
                ConfirmField(
                    label = label,
                    value = sheet.input.fieldAt(index),
                    suffix = suffixFor(label),
                )
            },
            accent = accent,
            confirmLabel = "Save product",
            headerContent = {
                FrontPhotoRow(photoPath = sheet.pendingPhotoPath, accent = accent, onClick = onAddPhoto)
            },
            onFieldChange = onFieldChange,
            onConfirm = onConfirm,
            onCancel = onCancel,
        )
    }
}

/**
 * USDA FoodData Central lookup, embedded above the manual fields.
 *
 * A typed food that is in neither the bundled set nor the library is searched here; a picked
 * result prefills the fields below. The API key is the user's own — it is prompted for inline
 * on first use and stored encrypted — so it never ships in the app and never sits in state
 * longer than the prompt.
 */
@Composable
private fun UsdaLookup(
    usda: UsdaSearchState,
    accent: AccentColors,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPick: (UsdaFood) -> Unit,
    onKeyInputChange: (String) -> Unit,
    onSaveKey: () -> Unit,
    onDismissKeyPrompt: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sp4, vertical = Spacing.sp2),
        verticalArrangement = Arrangement.spacedBy(Spacing.sp2),
    ) {
        Text(
            "Not listed? Search USDA".uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant,
        )

        if (usda.promptForKey) {
            // First-use gate: FDC needs a free API key. Kept inline rather than a separate
            // screen, and stored encrypted the moment it is saved.
            Text(
                "Paste your free USDA FoodData Central API key (fdc.nal.usda.gov).",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceFaint,
            )
            SheetInput(usda.keyInput, "FDC API key", accent, onKeyInputChange)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sp2)) {
                AccentButton("Save key", accent = accent.base, onClick = onSaveKey)
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.md))
                        .clickable(role = Role.Button, onClick = onDismissKeyPrompt)
                        .padding(horizontal = Spacing.sp4, vertical = Spacing.sp3),
                )
            }
            return@Column
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sp2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SheetInput(usda.query, "e.g. quinoa, ragi", accent, onQueryChange)
            }
            AccentButton("Search", accent = accent.base, onClick = onSearch)
        }

        when (usda.status) {
            UsdaStatus.Loading -> UsdaNote("Searching USDA…")
            UsdaStatus.Empty -> UsdaNote("No USDA match for “${usda.query}”.")
            UsdaStatus.Offline -> UsdaNote("Offline — connect to search USDA.")
            UsdaStatus.Error -> UsdaNote("USDA lookup failed. Try again.")
            UsdaStatus.Idle -> Unit
        }

        for (food in usda.results) {
            BrandPickerRow(
                brand = food.brand.orEmpty(),
                product = food.description,
                per100g = usdaLine(food),
                isGeneric = food.brand == null,
                accent = accent,
                onClick = { onPick(food) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun UsdaNote(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceFaint)
}

/** A single-line sheet input styled like the meal filter field. */
@Composable
private fun SheetInput(
    value: String,
    placeholder: String,
    accent: AccentColors,
    onValueChange: (String) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = OnSurface),
        cursorBrush = SolidColor(accent.base),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Surface2)
            .padding(horizontal = Spacing.sp3, vertical = Spacing.sp3),
        decorationBox = { field ->
            if (value.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = OnSurfaceFaint)
            }
            field()
        },
    )
}

/** Compact per-100g readout for a USDA result row. */
private fun usdaLine(food: UsdaFood): String {
    fun g(key: String) = food.nutrients[key]?.let { "%.1f".format(it) } ?: "—"
    val kcal = food.nutrients[NutrientKeys.ENERGY_KCAL]?.let { "%.0f".format(it) } ?: "—"
    return "per 100g · $kcal kcal · ${g(NutrientKeys.PROTEIN_G)}P · " +
        "${g(NutrientKeys.CARBS_G)}C · ${g(NutrientKeys.FAT_G)}F"
}

/** Everything nutritional is typed per 100g, which is the basis products are stored in. */
private fun suffixFor(label: String): String? = when (label) {
    "kcal" -> "/100g"
    "Protein", "Carbs", "Fat" -> "g/100g"
    else -> null
}

/**
 * The saved "usual meals" for this meal, as tappable chips, plus a save-current affordance.
 *
 * One tap on a chip re-logs that whole template for today. "Save as usual" only appears once
 * something is logged — there is nothing to snapshot from an empty meal. The whole row hides
 * when there is neither a saved template nor anything to save.
 */
@Composable
private fun UsualMealsRow(
    templates: List<MealTemplateSummary>,
    canSave: Boolean,
    accent: AccentColors,
    onLogTemplate: (String) -> Unit,
    onSaveAsTemplate: () -> Unit,
) {
    if (templates.isEmpty() && !canSave) return
    Column {
        SectionLabel("Usual meals")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Spacing.sp4),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sp2),
        ) {
            for (template in templates) {
                UsualMealChip(
                    label = template.name,
                    icon = Icons.Rounded.Bookmark,
                    filled = true,
                    accent = accent,
                    onClick = { onLogTemplate(template.mealTemplateId) },
                )
            }
            if (canSave) {
                UsualMealChip(
                    label = "Save as usual",
                    icon = Icons.Rounded.BookmarkAdd,
                    filled = false,
                    accent = accent,
                    onClick = onSaveAsTemplate,
                )
            }
        }
    }
}

@Composable
private fun UsualMealChip(
    label: String,
    icon: ImageVector,
    filled: Boolean,
    accent: AccentColors,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.lg))
            .background(if (filled) accent.container else Surface2)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.sp3, vertical = Spacing.sp2),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sp1 + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (filled) accent.base else OnSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (filled) accent.base else OnSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** Names the current meal so it can be re-logged with one tap later. */
@Composable
private fun SaveTemplateDialog(
    name: String,
    accent: AccentColors,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Scrim)
            .clickable(role = Role.Button, onClick = onCancel),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(Spacing.sp6)
                .clip(RoundedCornerShape(Radius.md))
                .background(Surface3)
                // Swallow taps so a press inside the card doesn't dismiss via the scrim.
                .clickable(enabled = false) {}
                .padding(Spacing.sp5),
            verticalArrangement = Arrangement.spacedBy(Spacing.sp3),
        ) {
            Text("Save as a usual meal", style = MaterialTheme.typography.titleMedium, color = OnSurface)
            Text(
                "Log this exact set of foods again with one tap.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
            )
            SheetInput(name, "e.g. My usual breakfast", accent, onNameChange)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sp2),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sp3, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = OnSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.md))
                        .clickable(role = Role.Button, onClick = onCancel)
                        .padding(horizontal = Spacing.sp4, vertical = Spacing.sp2),
                )
                AccentButton("Save", accent = accent.base, onClick = onConfirm)
            }
        }
    }
}

private const val DEFAULT_PORTION_GRAMS = 100.0

@Preview(name = "Meal", showBackground = true, backgroundColor = 0xFF0E1013, widthDp = 412, heightDp = 860)
@Composable
private fun MealPreview() {
    DailyTrackerTheme {
        MealContent(
            state = MealUiState(detail = previewDetail(), expandedItemId = "1"),
            onBack = {}, onItemClick = {}, onBrandClick = { _, _ -> }, onAddBrandClick = {},
            onScanClick = {}, onToggleSearch = {}, onQueryChange = {}, onConfirmQuantity = { _, _, _ -> },
            onRemovePortion = {}, onManualFieldChange = { _, _ -> }, onConfirmManualProduct = {},
            onDismissSheet = {},
        )
    }
}

private fun previewDetail() = MealDetail(
    subMenuId = "b", templateId = "t", name = "Lunch", accent = ActivityKey.DIET,
    items = listOf(
        MealItem(
            itemId = "1", name = "Paneer", genericName = "paneer",
            quantityFieldKey = "amount", variantFieldKey = "variant",
            brands = listOf(
                BrandOption("p1", "Amul", "Malai Paneer", Per100g(296.0, 18.5, 5.4, 22.7), "per 100g · 296 kcal · 18.5P · 5.4C · 22.7F"),
                BrandOption("p2", "Mother Dairy", "Paneer", Per100g(265.0, 18.9, 3.3, 20.0), "per 100g · 265 kcal · 18.9P · 3.3C · 20.0F"),
            ),
            logged = null,
        ),
        MealItem(
            itemId = "2", name = "Dal", genericName = "dal",
            quantityFieldKey = "amount", variantFieldKey = "variant", brands = emptyList(),
            logged = LoggedPortion("e1", "p3", 200.0, NutrientTotals(mapOf("energy_kcal" to 180.0))),
        ),
        MealItem("3", "Rice", "rice", "amount", "variant", emptyList(), null),
    ),
    totals = NutrientTotals(
        mapOf("energy_kcal" to 386.0, "protein_g" to 14.0, "carbs_g" to 62.0, "fat_g" to 7.0),
    ),
)
