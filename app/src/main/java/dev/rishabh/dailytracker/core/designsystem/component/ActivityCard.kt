package dev.rishabh.dailytracker.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.rishabh.dailytracker.core.designsystem.ActivityKey
import dev.rishabh.dailytracker.core.designsystem.DailyTrackerTheme
import dev.rishabh.dailytracker.core.designsystem.DisabledOpacity
import dev.rishabh.dailytracker.core.designsystem.Dimens
import dev.rishabh.dailytracker.core.designsystem.OnAccent
import dev.rishabh.dailytracker.core.designsystem.OnSurface
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceFaint
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceVariant
import dev.rishabh.dailytracker.core.designsystem.Radius
import dev.rishabh.dailytracker.core.designsystem.Spacing
import dev.rishabh.dailytracker.core.designsystem.Surface1
import dev.rishabh.dailytracker.core.designsystem.displayName
import dev.rishabh.dailytracker.core.designsystem.iconForKey
import dev.rishabh.dailytracker.core.designsystem.iconKey

/**
 * Home-screen entry card for one activity: icon chip, name, one-line today summary.
 *
 * The activity drives the accent, so this component (unlike the others) takes an
 * [ActivityKey] rather than a bare accent — that's how the design keeps one hue per
 * activity without the caller wiring it up.
 *
 * @param summary values joined by "·", never a sentence.
 */
@Composable
fun ActivityCard(
    activity: ActivityKey,
    summary: String,
    modifier: Modifier = Modifier,
    name: String = activity.displayName,
    icon: ImageVector = iconForKey(activity.iconKey),
    selected: Boolean = false,
    disabled: Boolean = false,
    onClick: () -> Unit = {},
) {
    val accent = DailyTrackerTheme.accents.of(activity)
    val background = if (selected) accent.container else Surface1
    val borderColor = if (selected) accent.base else null

    Row(
        modifier = modifier
            .heightIn(min = Dimens.hitMin)
            .clip(RoundedCornerShape(Radius.lg))
            .background(background)
            .then(if (borderColor != null) Modifier.border(1.5.dp, borderColor, RoundedCornerShape(Radius.lg)) else Modifier)
            .clickable(enabled = !disabled, role = Role.Button, onClick = onClick)
            .alpha(if (disabled) DisabledOpacity else 1f)
            .padding(Spacing.sp4),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sp4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(Radius.xl))
                .background(if (selected) accent.base else accent.container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) OnAccent else accent.base,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = OnSurface)
            Text(
                summary,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = OnSurfaceFaint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(name = "ActivityCard", showBackground = true, backgroundColor = 0xFF0E1013, widthDp = 360)
@Composable
private fun ActivityCardPreview() {
    DailyTrackerTheme {
        Column(
            modifier = Modifier.padding(Spacing.sp4),
            verticalArrangement = Arrangement.spacedBy(Spacing.sp3),
        ) {
            ActivityCard(ActivityKey.DIET, "1,840 kcal · 132g protein")
            ActivityCard(ActivityKey.STUDY, "2h 10m · Linear algebra", selected = true)
            ActivityCard(ActivityKey.SLEEP, "7h 20m · bed 23:40", selected = true)
            ActivityCard(ActivityKey.WORKOUT, "Nothing logged yet", disabled = true)
        }
    }
}
