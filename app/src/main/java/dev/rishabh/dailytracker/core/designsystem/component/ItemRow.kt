package dev.rishabh.dailytracker.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.rishabh.dailytracker.core.designsystem.AccentColors
import dev.rishabh.dailytracker.core.designsystem.DailyTrackerTheme
import dev.rishabh.dailytracker.core.designsystem.DisabledOpacity
import dev.rishabh.dailytracker.core.designsystem.Dimens
import dev.rishabh.dailytracker.core.designsystem.OnAccent
import dev.rishabh.dailytracker.core.designsystem.OnSurface
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceVariant
import dev.rishabh.dailytracker.core.designsystem.Outline
import dev.rishabh.dailytracker.core.designsystem.Radius
import dev.rishabh.dailytracker.core.designsystem.Spacing
import dev.rishabh.dailytracker.core.designsystem.Surface2
import dev.rishabh.dailytracker.core.designsystem.TypeNumeric

/**
 * Checkable list row: checkbox, name, right-aligned mono value. The whole row is the tap
 * target. Checked = accent checkbox, strikethrough name, a subtle surface fill.
 *
 * @param accent defaults to the screen's activity accent (see ProvideActivityAccent).
 */
@Composable
fun ItemRow(
    name: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    checked: Boolean = false,
    disabled: Boolean = false,
    accent: AccentColors = DailyTrackerTheme.accent,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    Row(
        modifier = modifier
            .heightIn(min = Dimens.hitMin)
            .clip(RoundedCornerShape(Radius.md))
            .background(if (checked) Surface2 else Color.Transparent)
            .toggleable(value = checked, enabled = !disabled, role = Role.Checkbox, onValueChange = onCheckedChange)
            .alpha(if (disabled) DisabledOpacity else 1f)
            .padding(horizontal = Spacing.sp4),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sp3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckboxBox(checked = checked, accent = accent)
        Text(
            name,
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(value, style = TypeNumeric, color = OnSurfaceVariant)
        }
    }
}

/** The 22dp square check box shared by ItemRow and the generic checkbox field. */
@Composable
internal fun CheckboxBox(checked: Boolean, accent: AccentColors) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(Radius.xs))
            .background(if (checked) accent.base else Color.Transparent)
            .border(2.dp, if (checked) accent.base else Outline, RoundedCornerShape(Radius.xs)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = OnAccent, modifier = Modifier.size(16.dp))
        }
    }
}

@Preview(name = "ItemRow", showBackground = true, backgroundColor = 0xFF16181D, widthDp = 360)
@Composable
private fun ItemRowPreview() {
    DailyTrackerTheme {
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(Spacing.sp2)) {
            ItemRow(name = "Chicken breast", value = "264 kcal", accent = DailyTrackerTheme.accents.diet, checked = true)
            ItemRow(name = "Bench press 4×8", value = "60 kg", accent = DailyTrackerTheme.accents.workout)
            ItemRow(name = "Evening walk", value = "30 min", accent = DailyTrackerTheme.accents.sleep, disabled = true)
        }
    }
}
