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
import dev.rishabh.dailytracker.core.designsystem.component.BackTopBar
import dev.rishabh.dailytracker.navigation.Routes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@HiltViewModel
class ActivityViewModel @Inject constructor(
    repository: ActivityRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val templateId: String = checkNotNull(savedStateHandle[Routes.ARG_TEMPLATE_ID])

    val state: StateFlow<ActivityDetail?> = repository.observeActivityDetail(templateId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun ActivityScreen(
    onBack: () -> Unit,
    onSubMenuClick: (subMenuId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel = hiltViewModel(),
) {
    val detail by viewModel.state.collectAsStateWithLifecycle()
    ActivityContent(detail = detail, onBack = onBack, onSubMenuClick = onSubMenuClick, modifier = modifier)
}

@Composable
private fun ActivityContent(
    detail: ActivityDetail?,
    onBack: () -> Unit,
    onSubMenuClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        ProvideActivityAccent(detail?.accent ?: ActivityKey.DIET) {
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
                    items(detail?.subMenus.orEmpty(), key = { it.subMenuId }) { sub ->
                        SubMenuNavRow(
                            name = sub.name,
                            itemCount = sub.itemCount,
                            onClick = { onSubMenuClick(sub.subMenuId) },
                        )
                    }
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

@Preview(name = "Activity", showBackground = true, backgroundColor = 0xFF0E1013, widthDp = 380, heightDp = 640)
@Composable
private fun ActivityPreview() {
    DailyTrackerTheme {
        ActivityContent(
            detail = ActivityDetail(
                templateId = "1", name = "Diet", accent = ActivityKey.DIET,
                subMenus = listOf(
                    SubMenuRow("a", "Breakfast", 4),
                    SubMenuRow("b", "Lunch", 5),
                    SubMenuRow("c", "Snacks", 3),
                    SubMenuRow("d", "Dinner", 4),
                ),
            ),
            onBack = {}, onSubMenuClick = {},
        )
    }
}
