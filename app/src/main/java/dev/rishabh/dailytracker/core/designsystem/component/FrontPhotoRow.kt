package dev.rishabh.dailytracker.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.rishabh.dailytracker.core.designsystem.AccentColors
import dev.rishabh.dailytracker.core.designsystem.DailyTrackerTheme
import dev.rishabh.dailytracker.core.designsystem.Dimens
import dev.rishabh.dailytracker.core.designsystem.Radius
import dev.rishabh.dailytracker.core.designsystem.Spacing
import java.io.File

/**
 * The front-photo affordance inside a product sheet: the current photo (if any) plus the
 * add/retake action. The photo is a local file — captures and downloaded pack shots alike.
 */
@Composable
fun FrontPhotoRow(
    photoPath: String?,
    accent: AccentColors = DailyTrackerTheme.accent,
    onClick: () -> Unit = {},
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sp3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (photoPath != null) {
            AsyncImage(
                model = File(photoPath),
                contentDescription = null,
                modifier = Modifier.size(Dimens.thumbnail).clip(RoundedCornerShape(Radius.sm)),
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(Radius.md))
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = Spacing.sp2, vertical = Spacing.sp2),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sp2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.CameraAlt,
                contentDescription = null,
                tint = accent.base,
                modifier = Modifier.size(20.dp),
            )
            Text(
                if (photoPath == null) "Add front photo" else "Retake photo",
                style = MaterialTheme.typography.labelLarge,
                color = accent.base,
            )
        }
    }
}
