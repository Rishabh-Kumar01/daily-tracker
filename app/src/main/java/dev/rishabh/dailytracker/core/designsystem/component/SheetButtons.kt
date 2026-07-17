package dev.rishabh.dailytracker.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.rishabh.dailytracker.core.designsystem.DisabledOpacity
import dev.rishabh.dailytracker.core.designsystem.Dimens
import dev.rishabh.dailytracker.core.designsystem.OnAccent
import dev.rishabh.dailytracker.core.designsystem.OnSurface
import dev.rishabh.dailytracker.core.designsystem.Outline
import dev.rishabh.dailytracker.core.designsystem.ShapeFull
import dev.rishabh.dailytracker.core.designsystem.Spacing

/*
 * Pill buttons shared by the sheets. Built by hand rather than with Material3 Button so the
 * accent fill is exact and doesn't fight the ColorScheme (which has no global accent slot).
 * Both are full-round and meet the 48dp minimum hit target.
 */

/** Filled accent action, e.g. "Add to log". */
@Composable
internal fun AccentButton(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .heightIn(min = Dimens.hitMin)
            .clip(ShapeFull)
            .background(accent)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else DisabledOpacity)
            .padding(horizontal = Spacing.sp5),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = OnAccent)
    }
}

/** Outlined neutral action, e.g. "Cancel". */
@Composable
internal fun OutlineButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .heightIn(min = Dimens.hitMin)
            .clip(ShapeFull)
            .border(1.dp, Outline, ShapeFull)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else DisabledOpacity)
            .padding(horizontal = Spacing.sp5),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = OnSurface)
    }
}

/** The 32×4 grab handle drawn at the top of every bottom sheet. */
@Composable
internal fun SheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 32.dp, height = 4.dp)
            .clip(ShapeFull)
            .background(Outline),
    )
}
