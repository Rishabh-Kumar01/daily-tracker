package dev.rishabh.dailytracker.feature.activities

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rishabh.dailytracker.core.designsystem.ActivityKey
import dev.rishabh.dailytracker.core.designsystem.DailyTrackerTheme
import dev.rishabh.dailytracker.core.designsystem.OnSurface
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceVariant
import dev.rishabh.dailytracker.core.designsystem.ProvideActivityAccent
import dev.rishabh.dailytracker.core.designsystem.Radius
import dev.rishabh.dailytracker.core.designsystem.Spacing
import dev.rishabh.dailytracker.core.designsystem.Surface1
import dev.rishabh.dailytracker.core.designsystem.component.BackTopBar
import dev.rishabh.dailytracker.feature.diet.MealScreen
import dev.rishabh.dailytracker.navigation.Routes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SubMenuViewModel @Inject constructor(
    repository: ActivityRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val subMenuId: String = checkNotNull(savedStateHandle[Routes.ARG_SUB_MENU_ID])

    val state: StateFlow<SubMenuDetail?> = repository.observeSubMenuDetail(subMenuId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun SubMenuScreen(
    onBack: () -> Unit,
    onScanClick: (itemId: String) -> Unit,
    modifier: Modifier = Modifier,
    pendingScanLogItemId: String? = null,
    pendingScanLogProductId: String? = null,
    onScanLogConsumed: () -> Unit = {},
    viewModel: SubMenuViewModel = hiltViewModel(),
) {
    val detail by viewModel.state.collectAsStateWithLifecycle()
    // Which screen a sub-menu gets is a property of its data, not of which activity it
    // belongs to: variant-backed items log through the meal screen, set_group items through
    // the generic set-logging screen (Workout). Anything else still browses read-only until
    // its own slice lands.
    when {
        detail?.isVariantLogging == true -> MealScreen(
            onBack = onBack,
            onScanClick = onScanClick,
            pendingScanLogItemId = pendingScanLogItemId,
            pendingScanLogProductId = pendingScanLogProductId,
            onScanLogConsumed = onScanLogConsumed,
            modifier = modifier,
        )

        detail?.hasSetLogging == true -> ItemLogScreen(onBack = onBack, modifier = modifier)

        else -> SubMenuContent(detail = detail, onBack = onBack, modifier = modifier)
    }
}

@Composable
private fun SubMenuContent(
    detail: SubMenuDetail?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        ProvideActivityAccent(detail?.accent ?: ActivityKey.DIET) {
            Column {
                BackTopBar(title = detail?.name ?: "", onBack = onBack)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Spacing.screenGutter, end = Spacing.screenGutter,
                        top = Spacing.sp2, bottom = Spacing.sp8,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sp2),
                ) {
                    items(detail?.items.orEmpty(), key = { it.itemId }) { item ->
                        ItemCard(item)
                    }
                }
            }
        }
    }
}

/**
 * Read-only preview of an item and the fields it would log.
 *
 * M5 is browse-only. The Diet meal flow (M6) replaces this leaf for Diet sub-menus with the
 * real logging screen; other activities get generic logging in a later slice.
 */
@Composable
private fun ItemCard(item: ItemRowDetail) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(Surface1)
            .padding(Spacing.sp4),
        verticalArrangement = Arrangement.spacedBy(Spacing.sp1),
    ) {
        Text(item.name, style = MaterialTheme.typography.titleMedium, color = OnSurface)
        val subtitle = buildList {
            if (item.hasVariants) add("brands")
            addAll(item.fieldLabels)
        }.joinToString(" · ")
        if (subtitle.isNotEmpty()) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
        }
    }
}

@Preview(name = "SubMenu", showBackground = true, backgroundColor = 0xFF0E1013, widthDp = 380, heightDp = 640)
@Composable
private fun SubMenuPreview() {
    DailyTrackerTheme {
        SubMenuContent(
            detail = SubMenuDetail(
                subMenuId = "b", name = "Lunch", accent = ActivityKey.DIET,
                items = listOf(
                    ItemRowDetail("1", "Paneer", true, listOf("Brand", "Amount")),
                    ItemRowDetail("2", "Dal", true, listOf("Brand", "Amount")),
                    ItemRowDetail("3", "Rice", true, listOf("Brand", "Amount")),
                ),
            ),
            onBack = {},
        )
    }
}
