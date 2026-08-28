package dev.rishabh.dailytracker.feature.activities

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rishabh.dailytracker.core.designsystem.ActivityKey
import dev.rishabh.dailytracker.core.designsystem.DailyTrackerTheme
import dev.rishabh.dailytracker.core.designsystem.Dimens
import dev.rishabh.dailytracker.core.designsystem.OnSurface
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceFaint
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceVariant
import dev.rishabh.dailytracker.core.designsystem.ProvideActivityAccent
import dev.rishabh.dailytracker.core.designsystem.Radius
import dev.rishabh.dailytracker.core.designsystem.Spacing
import dev.rishabh.dailytracker.core.designsystem.Surface1
import dev.rishabh.dailytracker.core.designsystem.Surface2
import dev.rishabh.dailytracker.core.designsystem.Surface3
import dev.rishabh.dailytracker.core.designsystem.Scrim
import dev.rishabh.dailytracker.core.designsystem.AccentColors
import dev.rishabh.dailytracker.core.designsystem.component.AccentButton
import dev.rishabh.dailytracker.core.designsystem.component.BackTopBar
import dev.rishabh.dailytracker.core.nutrition.NutrientTotals
import dev.rishabh.dailytracker.core.settings.DietTargets
import dev.rishabh.dailytracker.core.settings.DietTargetsStore
import dev.rishabh.dailytracker.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

/** Which macro a targets field edits. */
enum class TargetMacro { KCAL, PROTEIN, CARBS, FAT }

/** The four editable target fields while the goals sheet is open. */
data class TargetsDraft(
    val kcal: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
)

/** Everything the activity screen renders: the sub-menus, plus the Diet day rollup. */
data class ActivityUiState(
    val detail: ActivityDetail? = null,
    val dayTotals: NutrientTotals = NutrientTotals.EMPTY,
    val targets: DietTargets = DietTargets(),
    /** Non-null while the "set goals" sheet is open. */
    val editingTargets: TargetsDraft? = null,
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val repository: ActivityRepository,
    private val targetsStore: DietTargetsStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val templateId: String = checkNotNull(savedStateHandle[Routes.ARG_TEMPLATE_ID])
    private val editing = MutableStateFlow<TargetsDraft?>(null)

    val state: StateFlow<ActivityUiState> = combine(
        repository.observeActivityDetail(templateId),
        repository.observeDayTotals(templateId),
        targetsStore.targets,
        editing,
    ) { detail, totals, targets, edit ->
        ActivityUiState(detail = detail, dayTotals = totals, targets = targets, editingTargets = edit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityUiState())

    /** Opens the goals sheet, prefilled with the current goals. */
    fun onEditTargets() {
        val t = state.value.targets
        editing.value = TargetsDraft(
            kcal = t.kcal.asTargetField(),
            protein = t.proteinG.asTargetField(),
            carbs = t.carbsG.asTargetField(),
            fat = t.fatG.asTargetField(),
        )
    }

    fun onTargetsFieldChange(macro: TargetMacro, value: String) {
        val clean = value.filter { it.isDigit() || it == '.' }
        editing.update { draft ->
            draft ?: return@update null
            when (macro) {
                TargetMacro.KCAL -> draft.copy(kcal = clean)
                TargetMacro.PROTEIN -> draft.copy(protein = clean)
                TargetMacro.CARBS -> draft.copy(carbs = clean)
                TargetMacro.FAT -> draft.copy(fat = clean)
            }
        }
    }

    fun onDismissTargets() {
        editing.value = null
    }

    /** Persists the edited goals; a blank field clears that macro's goal. */
    fun onSaveTargets() {
        val draft = editing.value ?: return
        viewModelScope.launch {
            targetsStore.setTargets(
                DietTargets(
                    kcal = draft.kcal.toDoubleOrNull(),
                    proteinG = draft.protein.toDoubleOrNull(),
                    carbsG = draft.carbs.toDoubleOrNull(),
                    fatG = draft.fat.toDoubleOrNull(),
                ),
            )
            editing.value = null
        }
    }
}

private fun Double?.asTargetField(): String = when {
    this == null -> ""
    this % 1.0 == 0.0 -> toLong().toString()
    else -> toString()
}

@Composable
fun ActivityScreen(
    onBack: () -> Unit,
    onSubMenuClick: (subMenuId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ActivityContent(
        state = state,
        onBack = onBack,
        onSubMenuClick = onSubMenuClick,
        onEditTargets = viewModel::onEditTargets,
        onTargetsFieldChange = viewModel::onTargetsFieldChange,
        onSaveTargets = viewModel::onSaveTargets,
        onDismissTargets = viewModel::onDismissTargets,
        modifier = modifier,
    )
}

@Composable
private fun ActivityContent(
    state: ActivityUiState,
    onBack: () -> Unit,
    onSubMenuClick: (String) -> Unit,
    onEditTargets: () -> Unit = {},
    onTargetsFieldChange: (TargetMacro, String) -> Unit = { _, _ -> },
    onSaveTargets: () -> Unit = {},
    onDismissTargets: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val detail = state.detail
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        ProvideActivityAccent(detail?.accent ?: ActivityKey.DIET) {
            val accent = DailyTrackerTheme.accent
            Box(modifier = Modifier.fillMaxSize()) {
                Column {
                    BackTopBar(title = detail?.name ?: "", onBack = onBack)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = Spacing.screenGutter, end = Spacing.screenGutter,
                            top = Spacing.sp2, bottom = Spacing.sp8,
                        ),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sp2),
                    ) {
                        if (detail?.tracksCalories == true) {
                            item(key = "day-totals") {
                                DietDayHeader(
                                    totals = state.dayTotals,
                                    targets = state.targets,
                                    accent = accent,
                                    onEditTargets = onEditTargets,
                                )
                            }
                        }
                        items(detail?.subMenus.orEmpty(), key = { it.subMenuId }) { sub ->
                            SubMenuNavRow(
                                name = sub.name,
                                itemCount = sub.itemCount,
                                onClick = { onSubMenuClick(sub.subMenuId) },
                            )
                        }
                    }
                }

                val draft = state.editingTargets
                if (draft != null) {
                    TargetsSheet(
                        draft = draft,
                        accent = accent,
                        onFieldChange = onTargetsFieldChange,
                        onSave = onSaveTargets,
                        onCancel = onDismissTargets,
                    )
                }
            }
        }
    }
}

@Composable
private fun SubMenuNavRow(name: String, itemCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.rowHeight)
            .clip(RoundedCornerShape(Radius.lg))
            .background(Surface1)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.sp4, vertical = Spacing.sp3),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sp3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = OnSurface)
            Text(
                if (itemCount == 1) "1 item" else "$itemCount items",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
            )
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceFaint, modifier = Modifier.size(20.dp))
    }
}

/**
 * The day-detail header for a calorie-tracking activity: the four macro totals, each with
 * progress towards its goal when one is set.
 *
 * Totals arrive already computed at read time; this only renders them. Goals are optional —
 * with none set it reads as a running tally, and the action becomes "Set goals".
 */
@Composable
private fun DietDayHeader(
    totals: NutrientTotals,
    targets: DietTargets,
    accent: AccentColors,
    onEditTargets: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(Surface1)
            .padding(Spacing.sp4),
        verticalArrangement = Arrangement.spacedBy(Spacing.sp3),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Today".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceFaint,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (targets.any) "Edit goals" else "Set goals",
                style = MaterialTheme.typography.labelLarge,
                color = accent.base,
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.md))
                    .clickable(role = Role.Button, onClick = onEditTargets)
                    .padding(horizontal = Spacing.sp2, vertical = Spacing.sp1),
            )
        }
        MacroProgressRow("Calories", totals.energyKcal, targets.kcal, "kcal", accent)
        MacroProgressRow("Protein", totals.proteinG, targets.proteinG, "g", accent)
        MacroProgressRow("Carbs", totals.carbsG, targets.carbsG, "g", accent)
        MacroProgressRow("Fat", totals.fatG, targets.fatG, "g", accent)
    }
}

@Composable
private fun MacroProgressRow(
    label: String,
    value: Double,
    target: Double?,
    unit: String,
    accent: AccentColors,
) {
    val shown = Math.round(value).toString()
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sp1)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (target != null) "$shown / ${Math.round(target)} $unit" else "$shown $unit",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurface,
            )
        }
        if (target != null && target > 0) {
            val fraction = (value / target).coerceIn(0.0, 1.0).toFloat()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Surface3),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(accent.base),
                )
            }
        }
    }
}

/** Edits the four daily macro goals. A blank field means no goal for that macro. */
@Composable
private fun TargetsSheet(
    draft: TargetsDraft,
    accent: AccentColors,
    onFieldChange: (TargetMacro, String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Scrim)
                .clickable(role = Role.Button, onClick = onCancel),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = Radius.lg, topEnd = Radius.lg))
                .background(Surface2)
                // Swallow taps so a press inside the sheet doesn't dismiss via the scrim.
                .clickable(enabled = false) {}
                .padding(Spacing.sp5),
            verticalArrangement = Arrangement.spacedBy(Spacing.sp3),
        ) {
            Text("Daily goals", style = MaterialTheme.typography.titleMedium, color = OnSurface)
            Text(
                "Leave a field blank for no goal on that macro.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
            )
            TargetField("Calories", "kcal", draft.kcal, accent) { onFieldChange(TargetMacro.KCAL, it) }
            TargetField("Protein", "g", draft.protein, accent) { onFieldChange(TargetMacro.PROTEIN, it) }
            TargetField("Carbs", "g", draft.carbs, accent) { onFieldChange(TargetMacro.CARBS, it) }
            TargetField("Fat", "g", draft.fat, accent) { onFieldChange(TargetMacro.FAT, it) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sp1),
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
                AccentButton("Save", accent = accent.base, onClick = onSave)
            }
        }
    }
}

@Composable
private fun TargetField(
    label: String,
    unit: String,
    value: String,
    accent: AccentColors,
    onValueChange: (String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sp3),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = OnSurface, modifier = Modifier.weight(1f))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = OnSurface),
            cursorBrush = SolidColor(accent.base),
            modifier = Modifier
                .width(112.dp)
                .clip(RoundedCornerShape(Radius.md))
                .background(Surface3)
                .padding(horizontal = Spacing.sp3, vertical = Spacing.sp2),
            decorationBox = { field ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text("—", style = MaterialTheme.typography.bodyLarge, color = OnSurfaceFaint)
                        }
                        field()
                    }
                    Text(unit, style = MaterialTheme.typography.labelMedium, color = OnSurfaceFaint)
                }
            },
        )
    }
}

@Preview(name = "Activity", showBackground = true, backgroundColor = 0xFF0E1013, widthDp = 380, heightDp = 640)
@Composable
private fun ActivityPreview() {
    DailyTrackerTheme {
        ActivityContent(
            state = ActivityUiState(
                detail = ActivityDetail(
                    templateId = "1", name = "Diet", accent = ActivityKey.DIET,
                    subMenus = listOf(
                        SubMenuRow("a", "Breakfast", 4),
                        SubMenuRow("b", "Lunch", 5),
                        SubMenuRow("c", "Snacks", 3),
                        SubMenuRow("d", "Dinner", 4),
                    ),
                    tracksCalories = true,
                ),
                dayTotals = NutrientTotals(
                    mapOf("energy_kcal" to 1240.0, "protein_g" to 68.0, "carbs_g" to 130.0, "fat_g" to 42.0),
                ),
                targets = DietTargets(kcal = 2000.0, proteinG = 120.0),
            ),
            onBack = {}, onSubMenuClick = {},
        )
    }
}
