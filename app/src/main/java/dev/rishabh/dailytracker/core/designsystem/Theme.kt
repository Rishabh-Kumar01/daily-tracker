package dev.rishabh.dailytracker.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Placeholder theme so M1 builds. M2 replaces this with the real theme translated from
 * the CSS token files under design/claude-design/tokens, including the per-activity accents.
 *
 * Dark is the only authored theme (see the design system readme).
 */
@Composable
fun DailyTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(),
        content = content,
    )
}
