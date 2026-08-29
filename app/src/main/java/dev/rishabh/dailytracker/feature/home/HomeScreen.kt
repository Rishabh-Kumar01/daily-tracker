package dev.rishabh.dailytracker.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import dev.rishabh.dailytracker.core.designsystem.ActivityKey
import dev.rishabh.dailytracker.core.designsystem.DailyTrackerTheme
import dev.rishabh.dailytracker.core.designsystem.OnSurface
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceVariant
import dev.rishabh.dailytracker.core.designsystem.Radius
import dev.rishabh.dailytracker.core.designsystem.Spacing
import dev.rishabh.dailytracker.core.designsystem.component.ActivityCard
import dev.rishabh.dailytracker.core.designsystem.iconForKey
import dev.rishabh.dailytracker.feature.activities.HomeActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    onActivityClick: (templateId: String) -> Unit,
    onMyFoodsClick: () -> Unit,
    onNewActivityClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onActivityClick = onActivityClick,
        onMyFoodsClick = onMyFoodsClick,
        onNewActivityClick = onNewActivityClick,
        modifier = modifier,
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onActivityClick: (String) -> Unit,
    onMyFoodsClick: () -> Unit = {},
    onNewActivityClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.screenGutter,
                end = Spacing.screenGutter,
                top = Spacing.sp8,
                bottom = Spacing.sp8,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sp3),
        ) {
            item {
                Column {
                    Text("Today", style = MaterialTheme.typography.headlineSmall, color = OnSurface)
                    Text(todayLabel(), style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                }
            }
            items(state.activities, key = { it.templateId }) { activity ->
                ActivityCard(
                    activity = activity.accent,
                    name = activity.name,
                    icon = iconForKey(activity.iconKey),
                    summary = activity.summary,
                    onClick = { onActivityClick(activity.templateId) },
                )
            }
            if (!state.loading && state.activities.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nothing set up yet", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                    }
                }
            }
            item {
                Text(
                    "+ New activity",
                    style = MaterialTheme.typography.labelLarge,
                    color = OnSurfaceVariant,
                    modifier = Modifier
                        .padding(top = Spacing.sp2)
                        .clip(RoundedCornerShape(Radius.md))
                        .clickable(role = Role.Button, onClick = onNewActivityClick)
                        .padding(horizontal = Spacing.sp3, vertical = Spacing.sp3),
                )
            }
            item {
                Text(
                    "My Foods →",
                    style = MaterialTheme.typography.labelLarge,
                    color = OnSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.md))
                        .clickable(role = Role.Button, onClick = onMyFoodsClick)
                        .padding(horizontal = Spacing.sp3, vertical = Spacing.sp3),
                )
            }
        }
    }
}

private fun todayLabel(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))

@Preview(name = "Home", showBackground = true, backgroundColor = 0xFF0E1013, widthDp = 380, heightDp = 640)
@Composable
private fun HomePreview() {
    DailyTrackerTheme {
        HomeContent(
            state = HomeUiState(
                loading = false,
                activities = listOf(
                    HomeActivity("1", "Diet", "restaurant", ActivityKey.DIET, "1 entry today"),
                    HomeActivity("2", "Workout", "fitness_center", ActivityKey.WORKOUT, "Nothing logged yet"),
                    HomeActivity("3", "Study", "school", ActivityKey.STUDY, "Nothing logged yet"),
                    HomeActivity("4", "Sleep", "bedtime", ActivityKey.SLEEP, "Nothing logged yet"),
                ),
            ),
            onActivityClick = {},
        )
    }
}
