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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.rishabh.dailytracker.core.designsystem.AccentColors
import dev.rishabh.dailytracker.core.designsystem.DailyTrackerTheme
import dev.rishabh.dailytracker.core.designsystem.DisabledOpacity
import dev.rishabh.dailytracker.core.designsystem.Dimens
import dev.rishabh.dailytracker.core.designsystem.FontMono
import dev.rishabh.dailytracker.core.designsystem.OnSurface
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceFaint
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceVariant
import dev.rishabh.dailytracker.core.designsystem.Radius
import dev.rishabh.dailytracker.core.designsystem.Spacing
import dev.rishabh.dailytracker.core.designsystem.Surface2
import dev.rishabh.dailytracker.core.designsystem.Surface3

/**
 * Product search-result row for picking a branded food: thumbnail, uppercase brand
 * caption, product name, mono per-100g macro line. Selected = accent border + container
 * fill + trailing check.
 *
 * Product art is user content, so absent a [thumbnailUrl] this draws a striped placeholder
 * rather than inventing an image.
 *
 * @param per100g preformatted, e.g. "per 100g · 296 kcal · 18.5P · 5.4C · 22.7F".
 */
@Composable
fun BrandPickerRow(
    brand: String,
    product: String,
    per100g: String,
    modifier: Modifier = Modifier,
    thumbnailUrl: String? = null,
    accent: AccentColors = DailyTrackerTheme.accent,
    selected: Boolean = false,
    disabled: Boolean = false,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .heightIn(min = Dimens.rowHeight)
            .clip(RoundedCornerShape(Radius.md))
            .background(if (selected) accent.container else Color.Transparent)
            .then(if (selected) Modifier.border(1.5.dp, accent.base, RoundedCornerShape(Radius.md)) else Modifier)
            .clickable(enabled = !disabled, role = Role.Button, onClick = onClick)
            .alpha(if (disabled) DisabledOpacity else 1f)
            .padding(horizontal = Spacing.sp3, vertical = Spacing.sp2),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sp3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(thumbnailUrl)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                brand.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) accent.base else OnSurfaceVariant,
            )
            Text(
                product,
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                per100g,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontMono),
                color = OnSurfaceFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = accent.base, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun Thumbnail(url: String?) {
    val shape = RoundedCornerShape(Radius.sm)
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.size(Dimens.thumbnail).clip(shape).background(Surface3),
        )
    } else {
        // 45° two-tone stripes, tiled — the design's placeholder for missing product art.
        val stripes = Brush.linearGradient(
            0.0f to Surface3, 0.5f to Surface3, 0.5f to Surface2, 1.0f to Surface2,
            start = Offset(0f, 0f),
            end = Offset(18f, 18f),
            tileMode = TileMode.Repeated,
        )
        Box(
            modifier = Modifier.size(Dimens.thumbnail).clip(shape).background(stripes),
            contentAlignment = Alignment.Center,
        ) {
            Text("IMG", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = OnSurfaceFaint)
        }
    }
}

@Preview(name = "BrandPickerRow", showBackground = true, backgroundColor = 0xFF16181D, widthDp = 360)
@Composable
private fun BrandPickerRowPreview() {
    DailyTrackerTheme {
        Column(modifier = Modifier.padding(Spacing.sp2)) {
            BrandPickerRow(
                brand = "Amul", product = "Malai Paneer",
                per100g = "per 100g · 296 kcal · 18.5P · 5.4C · 22.7F",
                accent = DailyTrackerTheme.accents.diet, selected = true,
            )
            BrandPickerRow(
                brand = "Mother Dairy", product = "Paneer",
                per100g = "per 100g · 265 kcal · 18.9P · 3.3C · 20.0F",
                accent = DailyTrackerTheme.accents.diet,
            )
            BrandPickerRow(
                brand = "iD Fresh", product = "Paneer",
                per100g = "per 100g · 257 kcal · 17.7P · 4.5C · 19.2F",
                accent = DailyTrackerTheme.accents.diet, disabled = true,
            )
        }
    }
}
