package dev.rishabh.dailytracker.feature.builder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rishabh.dailytracker.core.designsystem.AccentColors
import dev.rishabh.dailytracker.core.designsystem.DailyTrackerTheme
import dev.rishabh.dailytracker.core.designsystem.Danger
import dev.rishabh.dailytracker.core.designsystem.OnSurface
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceFaint
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceVariant
import dev.rishabh.dailytracker.core.designsystem.ProvideActivityAccent
import dev.rishabh.dailytracker.core.designsystem.Radius
import dev.rishabh.dailytracker.core.designsystem.ShapeFull
import dev.rishabh.dailytracker.core.designsystem.Spacing
import dev.rishabh.dailytracker.core.designsystem.Surface1
import dev.rishabh.dailytracker.core.designsystem.Surface2
import dev.rishabh.dailytracker.core.designsystem.Surface3
import dev.rishabh.dailytracker.core.designsystem.accentKeyForColor
import dev.rishabh.dailytracker.core.designsystem.builderIconKeys
import dev.rishabh.dailytracker.core.designsystem.component.AccentButton
import dev.rishabh.dailytracker.core.designsystem.component.BackTopBar
import dev.rishabh.dailytracker.core.designsystem.iconForKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class BuilderUiState(
    val draft: ActivityDraft = ActivityDraft(sections = listOf(emptySection())),
    val error: String? = null,
    /** Set once the activity is saved, so the screen can navigate into it. */
    val savedTemplateId: String? = null,
)

@HiltViewModel
class ActivityBuilderViewModel @Inject constructor(
    private val repository: ActivityBuilderRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BuilderUiState())
    val state: StateFlow<BuilderUiState> = _state.asStateFlow()

    fun onNameChange(value: String) = setDraft { it.copy(name = value) }
    fun onIconChange(key: String) = setDraft { it.copy(iconKey = key) }
    fun onColorChange(hex: String) = setDraft { it.copy(colorHex = hex) }

    fun onAddSection() = setDraft { it.copy(sections = it.sections + emptySection()) }
    fun onRemoveSection(id: String) = setDraft { it.copy(sections = it.sections.filterNot { s -> s.id == id }) }
    fun onSectionNameChange(id: String, value: String) = mapSection(id) { it.copy(name = value) }

    fun onAddItem(sectionId: String) = mapSection(sectionId) { it.copy(items = it.items + emptyItem()) }
    fun onRemoveItem(sectionId: String, itemId: String) =
        mapSection(sectionId) { it.copy(items = it.items.filterNot { i -> i.id == itemId }) }
    fun onItemNameChange(sectionId: String, itemId: String, value: String) =
        mapItem(sectionId, itemId) { it.copy(name = value) }

    fun onAddField(sectionId: String, itemId: String) =
        mapItem(sectionId, itemId) { it.copy(fields = it.fields + emptyField()) }
    fun onRemoveField(sectionId: String, itemId: String, fieldId: String) =
        mapItem(sectionId, itemId) { it.copy(fields = it.fields.filterNot { f -> f.id == fieldId }) }
    fun onFieldLabelChange(sectionId: String, itemId: String, fieldId: String, value: String) =
        mapField(sectionId, itemId, fieldId) { it.copy(label = value) }
    fun onFieldTypeChange(sectionId: String, itemId: String, fieldId: String, type: BuilderFieldType) =
        mapField(sectionId, itemId, fieldId) { it.copy(type = type) }
    fun onFieldUnitChange(sectionId: String, itemId: String, fieldId: String, value: String) =
        mapField(sectionId, itemId, fieldId) { it.copy(unit = value) }

    fun onSave() {
        val draft = _state.value.draft
        viewModelScope.launch {
            when (val result = repository.createActivity(draft)) {
                is CreateResult.Created -> _state.update { it.copy(savedTemplateId = result.templateId) }
                is CreateResult.Invalid -> _state.update { it.copy(error = result.message) }
            }
        }
    }

    private fun setDraft(transform: (ActivityDraft) -> ActivityDraft) =
        _state.update { it.copy(draft = transform(it.draft), error = null) }

    private fun mapSection(id: String, transform: (SectionDraft) -> SectionDraft) =
        setDraft { d -> d.copy(sections = d.sections.map { if (it.id == id) transform(it) else it }) }

    private fun mapItem(sectionId: String, itemId: String, transform: (ItemDraft) -> ItemDraft) =
        mapSection(sectionId) { s -> s.copy(items = s.items.map { if (it.id == itemId) transform(it) else it }) }

    private fun mapField(
        sectionId: String,
        itemId: String,
        fieldId: String,
        transform: (FieldDraft) -> FieldDraft,
    ) = mapItem(sectionId, itemId) { i -> i.copy(fields = i.fields.map { if (it.id == fieldId) transform(it) else it }) }
}

private fun uid() = UUID.randomUUID().toString()
internal fun emptyField() = FieldDraft(uid())
internal fun emptyItem() = ItemDraft(uid(), fields = listOf(emptyField()))
internal fun emptySection() = SectionDraft(uid(), items = listOf(emptyItem()))

@Composable
fun ActivityBuilderScreen(
    onBack: () -> Unit,
    onSaved: (templateId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActivityBuilderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.savedTemplateId) {
        state.savedTemplateId?.let(onSaved)
    }
    ActivityBuilderContent(
        state = state,
        onBack = onBack,
        callbacks = BuilderCallbacks(viewModel),
        modifier = modifier,
    )
}

/** Bundles the many nested edit callbacks so the content signature stays readable. */
private class BuilderCallbacks(vm: ActivityBuilderViewModel) {
    val onName = vm::onNameChange
    val onIcon = vm::onIconChange
    val onColor = vm::onColorChange
    val onAddSection = vm::onAddSection
    val onRemoveSection = vm::onRemoveSection
    val onSectionName = vm::onSectionNameChange
    val onAddItem = vm::onAddItem
    val onRemoveItem = vm::onRemoveItem
    val onItemName = vm::onItemNameChange
    val onAddField = vm::onAddField
    val onRemoveField = vm::onRemoveField
    val onFieldLabel = vm::onFieldLabelChange
    val onFieldType = vm::onFieldTypeChange
    val onFieldUnit = vm::onFieldUnitChange
    val onSave = vm::onSave
}

@Composable
private fun ActivityBuilderContent(
    state: BuilderUiState,
    onBack: () -> Unit,
    callbacks: BuilderCallbacks,
    modifier: Modifier = Modifier,
) {
    val draft = state.draft
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        ProvideActivityAccent(accentKeyForColor(draft.colorHex)) {
            val accent = DailyTrackerTheme.accent
            Column {
                BackTopBar(title = "New activity", onBack = onBack)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = Spacing.screenGutter),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sp3),
                ) {
                    BuilderTextField(draft.name, "Activity name", accent, onValueChange = callbacks.onName)
                    IconPicker(draft.iconKey, accent, onPick = callbacks.onIcon)
                    ColorPicker(draft.colorHex, onPick = callbacks.onColor)

                    draft.sections.forEach { section ->
                        SectionCard(section = section, accent = accent, callbacks = callbacks)
                    }

                    AddRow("Add section", accent, onClick = callbacks.onAddSection)

                    if (state.error != null) {
                        Text(
                            state.error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Danger,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface2)
                        .padding(Spacing.sp4),
                    horizontalArrangement = Arrangement.End,
                ) {
                    AccentButton("Create activity", accent = accent.base, onClick = callbacks.onSave)
                }
            }
        }
    }
}

@Composable
private fun SectionCard(section: SectionDraft, accent: AccentColors, callbacks: BuilderCallbacks) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(Surface1)
            .padding(Spacing.sp3),
        verticalArrangement = Arrangement.spacedBy(Spacing.sp2),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sp2)) {
            Box(Modifier.weight(1f)) {
                BuilderTextField(section.name, "Section, e.g. Daily", accent) { callbacks.onSectionName(section.id, it) }
            }
            RemoveButton("remove section") { callbacks.onRemoveSection(section.id) }
        }
        section.items.forEach { item ->
            ItemCard(sectionId = section.id, item = item, accent = accent, callbacks = callbacks)
        }
        AddRow("Add item", accent) { callbacks.onAddItem(section.id) }
    }
}

@Composable
private fun ItemCard(sectionId: String, item: ItemDraft, accent: AccentColors, callbacks: BuilderCallbacks) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Surface2)
            .padding(Spacing.sp3),
        verticalArrangement = Arrangement.spacedBy(Spacing.sp2),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sp2)) {
            Box(Modifier.weight(1f)) {
                BuilderTextField(item.name, "Item, e.g. Minoxidil", accent) { callbacks.onItemName(sectionId, item.id, it) }
            }
            RemoveButton("remove item") { callbacks.onRemoveItem(sectionId, item.id) }
        }
        item.fields.forEach { field ->
            FieldRow(sectionId = sectionId, itemId = item.id, field = field, accent = accent, callbacks = callbacks)
        }
        AddRow("Add field", accent) { callbacks.onAddField(sectionId, item.id) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FieldRow(
    sectionId: String,
    itemId: String,
    field: FieldDraft,
    accent: AccentColors,
    callbacks: BuilderCallbacks,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .background(Surface3)
            .padding(Spacing.sp2),
        verticalArrangement = Arrangement.spacedBy(Spacing.sp2),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sp2)) {
            Box(Modifier.weight(1f)) {
                BuilderTextField(field.label, "Field, e.g. Applied", accent) { callbacks.onFieldLabel(sectionId, itemId, field.id, it) }
            }
            if (field.type.needsUnit) {
                Box(Modifier.width(72.dp)) {
                    BuilderTextField(field.unit, "unit", accent) { callbacks.onFieldUnit(sectionId, itemId, field.id, it) }
                }
            }
            RemoveButton("remove field") { callbacks.onRemoveField(sectionId, itemId, field.id) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sp1), verticalArrangement = Arrangement.spacedBy(Spacing.sp1)) {
            BuilderFieldType.entries.forEach { type ->
                val on = type == field.type
                Text(
                    type.display,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (on) accent.base else OnSurfaceVariant,
                    modifier = Modifier
                        .clip(ShapeFull)
                        .background(if (on) accent.container else Surface2)
                        .then(if (on) Modifier.border(1.dp, accent.base, ShapeFull) else Modifier)
                        .clickable(role = Role.RadioButton) { callbacks.onFieldType(sectionId, itemId, field.id, type) }
                        .padding(horizontal = Spacing.sp3, vertical = Spacing.sp1),
                )
            }
        }
    }
}

@Composable
private fun IconPicker(selected: String, accent: AccentColors, onPick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sp2),
    ) {
        builderIconKeys.forEach { key ->
            val on = key == selected
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(Radius.md))
                    .background(if (on) accent.container else Surface2)
                    .then(if (on) Modifier.border(1.5.dp, accent.base, RoundedCornerShape(Radius.md)) else Modifier)
                    .clickable(role = Role.RadioButton) { onPick(key) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(iconForKey(key), contentDescription = null, tint = if (on) accent.base else OnSurfaceVariant, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun ColorPicker(selected: String, onPick: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sp3)) {
        builderColorHexes.forEach { hex ->
            val color = Color(android.graphics.Color.parseColor(hex))
            val on = hex.equals(selected, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(if (on) Modifier.border(2.dp, OnSurface, CircleShape) else Modifier)
                    .clickable(role = Role.RadioButton) { onPick(hex) },
                contentAlignment = Alignment.Center,
            ) {
                if (on) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun AddRow(label: String, accent: AccentColors, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(ShapeFull)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.sp2, vertical = Spacing.sp1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sp2),
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = accent.base, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = accent.base)
    }
}

@Composable
private fun RemoveButton(description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(ShapeFull)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Close, contentDescription = description, tint = OnSurfaceFaint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun BuilderTextField(
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
            .clip(RoundedCornerShape(Radius.sm))
            .background(Surface3)
            .padding(horizontal = Spacing.sp3, vertical = Spacing.sp2),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = OnSurfaceFaint)
            }
            inner()
        },
    )
}
