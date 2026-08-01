package dev.rishabh.dailytracker.feature.foods

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.draw.clip
import dev.rishabh.dailytracker.core.designsystem.AccentColors
import dev.rishabh.dailytracker.core.designsystem.DailyTrackerTheme
import dev.rishabh.dailytracker.core.designsystem.FontMono
import dev.rishabh.dailytracker.core.designsystem.OnAccent
import dev.rishabh.dailytracker.core.designsystem.OnSurface
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceFaint
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceVariant
import dev.rishabh.dailytracker.core.designsystem.Radius
import dev.rishabh.dailytracker.core.designsystem.Scrim
import dev.rishabh.dailytracker.core.designsystem.ShapeFull
import dev.rishabh.dailytracker.core.designsystem.Spacing
import dev.rishabh.dailytracker.core.designsystem.Surface2
import dev.rishabh.dailytracker.core.designsystem.Surface3
import dev.rishabh.dailytracker.core.designsystem.component.BackTopBar
import dev.rishabh.dailytracker.core.designsystem.component.ConfirmField
import dev.rishabh.dailytracker.core.designsystem.component.ConfirmSheet
import dev.rishabh.dailytracker.feature.diet.ManualProductInput

@Composable
fun MyFoodsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyFoodsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MyFoodsContent(
        state = state,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onEdit = viewModel::onEdit,
        onArchive = viewModel::onArchive,
        onEditFieldChange = viewModel::onEditFieldChange,
        onSaveEdit = viewModel::onSaveEdit,
        onDismissEdit = viewModel::onDismissEdit,
        modifier = modifier,
    )
}

@Composable
internal fun MyFoodsContent(
    state: MyFoodsUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFilterChange: (LibraryFilter) -> Unit,
    onEdit: (ProductCard) -> Unit,
    onArchive: (String) -> Unit,
    onEditFieldChange: (Int, String) -> Unit,
    onSaveEdit: () -> Unit,
    onDismissEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = DailyTrackerTheme.accents.diet
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                BackTopBar(title = "My Foods", onBack = onBack)
                SearchField(state.query, accent, onQueryChange)
                FilterRow(state.filter, accent, onFilterChange)

                if (!state.loading && state.products.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (state.query.isBlank()) "No foods yet" else "No match for “${state.query}”",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = Spacing.sp2, vertical = Spacing.sp2),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sp1),
                    ) {
                        items(state.products, key = { it.productId }) { card ->
                            ProductRow(
                                card = card,
                                accent = accent,
                                onClick = { onEdit(card) },
                                onArchive = { onArchive(card.productId) },
                            )
                        }
                    }
                }
            }

            val edit = state.edit
            if (edit != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Scrim)
                        .clickable(role = Role.Button, onClick = onDismissEdit),
                )
                Box(modifier = Modifier.align(Alignment.BottomCenter).imePadding()) {
                    EditSheetContent(edit, accent, onEditFieldChange, onSaveEdit, onDismissEdit)
                }
            }
        }
    }
}

/** One product: brand/name + mono macro line, tap to edit, trailing archive. */
@Composable
private fun ProductRow(
    card: ProductCard,
    accent: AccentColors,
    onClick: () -> Unit,
    onArchive: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Surface2)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.sp3, vertical = Spacing.sp3),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sp3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (!card.isGeneric && card.brand != null) {
                Text(card.brand.uppercase(), style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
            }
            Text(
                card.name,
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                card.per100gLine,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontMono),
                color = OnSurfaceFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(Radius.md))
                .background(Surface3)
                .clickable(role = Role.Button, onClick = onArchive),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Inventory2,
                contentDescription = "Archive ${card.name}",
                tint = OnSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun FilterRow(selected: LibraryFilter, accent: AccentColors, onFilterChange: (LibraryFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sp4, vertical = Spacing.sp2),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sp2),
    ) {
        for (f in LibraryFilter.entries) {
            val on = f == selected
            Text(
                text = when (f) {
                    LibraryFilter.ALL -> "All"
                    LibraryFilter.GENERIC -> "Generic"
                    LibraryFilter.BRANDED -> "Branded"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (on) OnAccent else OnSurfaceVariant,
                modifier = Modifier
                    .clip(ShapeFull)
                    .background(if (on) accent.base else Surface2)
                    .clickable(role = Role.Button) { onFilterChange(f) }
                    .padding(horizontal = Spacing.sp4, vertical = Spacing.sp2),
            )
        }
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
            .padding(horizontal = Spacing.sp4, vertical = Spacing.sp1)
            .clip(RoundedCornerShape(Radius.md))
            .background(Surface2)
            .padding(horizontal = Spacing.sp3, vertical = Spacing.sp3),
        decorationBox = { field ->
            if (query.isEmpty()) {
                Text("Search foods", style = MaterialTheme.typography.bodyLarge, color = OnSurfaceFaint)
            }
            field()
        },
    )
}

@Composable
private fun EditSheetContent(
    edit: EditSheet,
    accent: AccentColors,
    onFieldChange: (Int, String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Column {
        if (edit.error != null) {
            Text(
                edit.error,
                style = MaterialTheme.typography.bodyMedium,
                color = accent.base,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface2)
                    .padding(horizontal = Spacing.sp4, vertical = Spacing.sp2),
            )
        }
        ConfirmSheet(
            title = "Edit food",
            fields = ManualProductInput.LABELS.mapIndexed { index, label ->
                ConfirmField(label = label, value = edit.input.fieldAt(index), suffix = suffixFor(label))
            },
            accent = accent,
            confirmLabel = "Save changes",
            onFieldChange = onFieldChange,
            onConfirm = onConfirm,
            onCancel = onCancel,
        )
    }
}

private fun suffixFor(label: String): String? = when (label) {
    "kcal" -> "/100g"
    "Protein", "Carbs", "Fat" -> "g/100g"
    else -> null
}

@Preview(name = "My Foods", showBackground = true, backgroundColor = 0xFF0E1013, widthDp = 380, heightDp = 720)
@Composable
private fun MyFoodsPreview() {
    DailyTrackerTheme {
        MyFoodsContent(
            state = MyFoodsUiState(
                loading = false,
                products = listOf(
                    ProductCard("1", "Amul", "Malai Paneer", false, "per 100g · 296 kcal · 18.5P · 5.4C · 22.7F", dev.rishabh.dailytracker.core.nutrition.NutrientTotals(emptyMap())),
                    ProductCard("2", null, "Boiled egg", true, "per 100g · 155 kcal · 12.6P · 1.1C · 10.6F", dev.rishabh.dailytracker.core.nutrition.NutrientTotals(emptyMap())),
                ),
            ),
            onBack = {}, onQueryChange = {}, onFilterChange = {}, onEdit = {}, onArchive = {},
            onEditFieldChange = { _, _ -> }, onSaveEdit = {}, onDismissEdit = {},
        )
    }
}
