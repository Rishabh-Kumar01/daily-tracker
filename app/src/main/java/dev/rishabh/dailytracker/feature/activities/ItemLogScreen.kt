package dev.rishabh.dailytracker.feature.activities

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rishabh.dailytracker.core.db.FieldType
import dev.rishabh.dailytracker.core.db.entity.ItemFieldEntity
import dev.rishabh.dailytracker.core.designsystem.AccentColors
import dev.rishabh.dailytracker.core.designsystem.ActivityKey
import dev.rishabh.dailytracker.core.designsystem.DailyTrackerTheme
import dev.rishabh.dailytracker.core.designsystem.OnSurface
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceFaint
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceVariant
import dev.rishabh.dailytracker.core.designsystem.ProvideActivityAccent
import dev.rishabh.dailytracker.core.designsystem.Radius
import dev.rishabh.dailytracker.core.designsystem.Spacing
import dev.rishabh.dailytracker.core.designsystem.Surface1
import dev.rishabh.dailytracker.core.designsystem.component.AccentButton
import dev.rishabh.dailytracker.core.designsystem.component.BackTopBar
import dev.rishabh.dailytracker.core.designsystem.component.FieldRenderer
import dev.rishabh.dailytracker.core.designsystem.component.model.LogValueDraft
import dev.rishabh.dailytracker.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Generic logging screen state.
 *
 * [working] holds in-progress edits per item, overlaid on the committed values from the log.
 * The overlay is why a re-emission of the log flow (e.g. after logging one exercise) never
 * clobbers another exercise's half-entered sets: an item shows [working] until it is logged,
 * then the entry is dropped so it re-seeds from what was just saved.
 */
data class ItemLogUiState(
    val log: SubMenuLog? = null,
    val working: Map<String, List<LogValueDraft>> = emptyMap(),
) {
    fun draftsFor(item: ItemLog): List<LogValueDraft> = working[item.itemId] ?: item.committed
}

@HiltViewModel
class ItemLogViewModel @Inject constructor(
    private val repository: ItemLogRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val subMenuId: String = checkNotNull(savedStateHandle[Routes.ARG_SUB_MENU_ID])
    private val working = MutableStateFlow<Map<String, List<LogValueDraft>>>(emptyMap())

    val state: StateFlow<ItemLogUiState> =
        combine(repository.observeSubMenuLog(subMenuId), working) { log, edits ->
            ItemLogUiState(log = log, working = edits)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemLogUiState())

    fun onFieldChange(itemId: String, newDraft: LogValueDraft) {
        val item = state.value.log?.items?.firstOrNull { it.itemId == itemId } ?: return
        val current = state.value.working[itemId] ?: item.committed
        val updated = current.map { if (it.fieldKey == newDraft.fieldKey) newDraft else it }
        working.update { it + (itemId to updated) }
    }

    /** Commits the item's current drafts, then drops the overlay so it re-seeds from the save. */
    fun onLog(itemId: String) {
        val log = state.value.log ?: return
        val item = log.items.firstOrNull { it.itemId == itemId } ?: return
        val drafts = state.value.working[itemId] ?: item.committed
        viewModelScope.launch {
            repository.logItem(log.templateId, log.subMenuId, itemId, drafts)
            working.update { it - itemId }
        }
    }

    fun onClear(itemId: String) {
        val entryId = state.value.log?.items?.firstOrNull { it.itemId == itemId }?.loggedEntryId ?: return
        viewModelScope.launch {
            repository.clearItem(entryId)
            working.update { it - itemId }
        }
    }
}

@Composable
fun ItemLogScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ItemLogViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ItemLogContent(
        state = state,
        onBack = onBack,
        onFieldChange = viewModel::onFieldChange,
        onLog = viewModel::onLog,
        onClear = viewModel::onClear,
        modifier = modifier,
    )
}

@Composable
private fun ItemLogContent(
    state: ItemLogUiState,
    onBack: () -> Unit,
    onFieldChange: (String, LogValueDraft) -> Unit,
    onLog: (String) -> Unit,
    onClear: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val log = state.log
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        ProvideActivityAccent(log?.accent ?: ActivityKey.WORKOUT) {
            val accent = DailyTrackerTheme.accent
            Column {
                BackTopBar(title = log?.name ?: "", onBack = onBack)
                LazyColumn(
                    modifier = Modifier.fillMaxSize().imePadding(),
                    contentPadding = PaddingValues(
                        start = Spacing.screenGutter, end = Spacing.screenGutter,
                        top = Spacing.sp2, bottom = Spacing.sp8,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sp2),
                ) {
                    items(log?.items.orEmpty(), key = { it.itemId }) { item ->
                        ExerciseCard(
                            item = item,
                            drafts = state.draftsFor(item),
                            accent = accent,
                            onFieldChange = { draft -> onFieldChange(item.itemId, draft) },
                            onLog = { onLog(item.itemId) },
                            onClear = { onClear(item.itemId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    item: ItemLog,
    drafts: List<LogValueDraft>,
    accent: AccentColors,
    onFieldChange: (LogValueDraft) -> Unit,
    onLog: () -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(Surface1)
            .padding(Spacing.sp4),
        verticalArrangement = Arrangement.spacedBy(Spacing.sp3),
    ) {
        Text(item.name, style = MaterialTheme.typography.titleMedium, color = OnSurface)
        if (item.recall != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sp2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.History, contentDescription = null, tint = OnSurfaceFaint, modifier = Modifier.size(16.dp))
                Text(
                    "Last time · ${item.recall}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceFaint,
                )
            }
        }
        // Every field of the exercise renders through the one generic renderer.
        item.fields.zip(drafts).forEach { (field, draft) ->
            FieldRenderer(
                field = field,
                draft = draft,
                onChange = onFieldChange,
                accent = accent,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sp3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccentButton(
                if (item.loggedEntryId != null) "Update" else "Log",
                accent = accent.base,
                onClick = onLog,
            )
            if (item.loggedEntryId != null) {
                Text(
                    "Clear",
                    style = MaterialTheme.typography.labelLarge,
                    color = OnSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.md))
                        .clickable(role = Role.Button, onClick = onClear)
                        .padding(horizontal = Spacing.sp3, vertical = Spacing.sp2),
                )
            }
        }
    }
}

@Preview(name = "ItemLog", showBackground = true, backgroundColor = 0xFF0E1013, widthDp = 380, heightDp = 720)
@Composable
private fun ItemLogPreview() {
    DailyTrackerTheme {
        ItemLogContent(
            state = ItemLogUiState(
                log = SubMenuLog(
                    subMenuId = "s", templateId = "t", name = "Push", accent = ActivityKey.WORKOUT,
                    items = listOf(
                        ItemLog(
                            itemId = "i1", name = "Bench Press",
                            fields = listOf(
                                ItemFieldEntity("f1", "i1", "sets", FieldType.SET_GROUP.wire, "Sets", null, true, 0, """{"fields":["reps","weight"]}"""),
                            ),
                            committed = listOf(LogValueDraft("sets", json = """[{"reps":8,"weight":60.0},{"reps":8,"weight":60.0}]""")),
                            loggedEntryId = null,
                            recall = "3 × 8 @ 60 kg",
                        ),
                    ),
                ),
            ),
            onBack = {}, onFieldChange = { _, _ -> }, onLog = {}, onClear = {},
        )
    }
}
