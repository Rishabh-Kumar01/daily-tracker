package dev.rishabh.dailytracker.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.rishabh.dailytracker.core.designsystem.AccentColors
import dev.rishabh.dailytracker.core.designsystem.ActivityKey
import dev.rishabh.dailytracker.core.designsystem.DailyTrackerTheme
import dev.rishabh.dailytracker.core.designsystem.Dimens
import dev.rishabh.dailytracker.core.designsystem.OnAccent
import dev.rishabh.dailytracker.core.designsystem.OnSurface
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceFaint
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceVariant
import dev.rishabh.dailytracker.core.designsystem.Outline
import dev.rishabh.dailytracker.core.designsystem.OutlineVariant
import dev.rishabh.dailytracker.core.designsystem.Radius
import dev.rishabh.dailytracker.core.designsystem.Spacing
import dev.rishabh.dailytracker.core.designsystem.Surface0
import dev.rishabh.dailytracker.core.designsystem.Surface1
import dev.rishabh.dailytracker.core.designsystem.Surface2
import dev.rishabh.dailytracker.core.designsystem.Surface3
import dev.rishabh.dailytracker.core.designsystem.TypeNumeric
import dev.rishabh.dailytracker.core.designsystem.TypeNumericLarge

/**
 * Debug-only specimen screen: renders the palette and type scale straight from the theme
 * so the Compose translation can be eyeballed against the guidelines cards under
 * design/claude-design/guidelines.
 *
 * Debug source set only — never present in a release build.
 */
class TokenGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DailyTrackerTheme { TokenGalleryScreen() } }
    }
}

@Composable
fun TokenGalleryScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = Spacing.screenGutter,
                end = Spacing.screenGutter,
                top = Spacing.sp8,
                bottom = Spacing.sp8,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sp3),
        ) {
            item {
                Text("Daily Tracker", style = MaterialTheme.typography.headlineSmall, color = OnSurface)
                Text(
                    "Design tokens — dark, Material 3, one accent per activity",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sp4))
            }

            item { SectionLabel("Surfaces & neutrals") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sp2)) {
                    NeutralSwatch("surface-0", Surface0)
                    NeutralSwatch("surface-1", Surface1)
                    NeutralSwatch("surface-2", Surface2)
                    NeutralSwatch("surface-3", Surface3)
                    NeutralSwatch("on-surface", OnSurface)
                    NeutralSwatch("on-surface-variant", OnSurfaceVariant)
                    NeutralSwatch("on-surface-faint", OnSurfaceFaint)
                    NeutralSwatch("outline", Outline)
                    NeutralSwatch("outline-variant", OutlineVariant)
                }
            }

            item { SectionLabel("Activity accents") }
            item {
                val accents = DailyTrackerTheme.accents
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sp2)) {
                    ActivityKey.entries.forEach { key ->
                        AccentSwatch(key.name.lowercase(), accents.of(key))
                    }
                }
            }

            item { SectionLabel("Type scale") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sp2)) {
                    TypeRow("Today", "headline 24/32", MaterialTheme.typography.headlineSmall)
                    TypeRow("Total 0% Greek Yogurt", "title-lg 20/28", MaterialTheme.typography.titleLarge)
                    TypeRow("Workout", "title 16/24", MaterialTheme.typography.titleMedium)
                    TypeRow("Chicken breast", "body-lg 16/24", MaterialTheme.typography.bodyLarge)
                    TypeRow("1,840 kcal · 132g protein", "body 14/20", MaterialTheme.typography.bodyMedium)
                    TypeRow("Add to log", "label 12/16", MaterialTheme.typography.labelMedium)
                    TypeRow("per 100g · 54 kcal · 10.3P", "caption 11/16", MaterialTheme.typography.labelSmall)
                }
            }

            item { SectionLabel("Numeric type (mono)") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sp2)) {
                    TypeRow("150 g", "numeric-lg 22/28", TypeNumericLarge)
                    TypeRow("264 kcal", "numeric 14/20", TypeNumeric)
                    TypeRow("7h 20m", "numeric 14/20", TypeNumeric)
                }
            }

            item { SectionLabel("Shape & spacing") }
            item { ShapeRow() }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Column {
        Spacer(Modifier.height(Spacing.sp4))
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = OnSurfaceFaint,
        )
        Spacer(Modifier.height(Spacing.sp2))
        HorizontalDivider(color = OutlineVariant)
        Spacer(Modifier.height(Spacing.sp2))
    }
}

@Composable
private fun NeutralSwatch(token: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(width = 72.dp, height = 40.dp)
                .clip(RoundedCornerShape(Radius.sm))
                .background(color)
                .border(1.dp, OutlineVariant, RoundedCornerShape(Radius.sm)),
        )
        Spacer(Modifier.width(Spacing.sp3))
        Text(token, style = TypeNumeric, color = OnSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(color.hex(), style = TypeNumeric, color = OnSurfaceFaint)
    }
}

@Composable
private fun AccentSwatch(name: String, accent: AccentColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // base fill, with on-accent text proving the contrast pairing
        Box(
            Modifier
                .size(width = 72.dp, height = 40.dp)
                .clip(RoundedCornerShape(Radius.sm))
                .background(accent.base),
            contentAlignment = Alignment.Center,
        ) {
            Text("Aa", style = MaterialTheme.typography.labelMedium, color = OnAccent)
        }
        Spacer(Modifier.width(Spacing.sp2))
        // container tint, with the accent on top as used for chips
        Box(
            Modifier
                .size(width = 56.dp, height = 40.dp)
                .clip(RoundedCornerShape(Radius.sm))
                .background(accent.container),
            contentAlignment = Alignment.Center,
        ) {
            Text("Aa", style = MaterialTheme.typography.labelMedium, color = accent.base)
        }
        Spacer(Modifier.width(Spacing.sp3))
        Text(name, style = TypeNumeric, color = OnSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(accent.base.hex(), style = TypeNumeric, color = OnSurfaceFaint)
    }
}

@Composable
private fun TypeRow(sample: String, token: String, style: TextStyle) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(sample, style = style, color = OnSurface, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(Spacing.sp3))
        Text(token, style = TypeNumeric, color = OnSurfaceFaint)
    }
}

@Composable
private fun ShapeRow() {
    val shapes = listOf(
        "xs 6" to Radius.xs,
        "sm 8" to Radius.sm,
        "md 12" to Radius.md,
        "lg 16" to Radius.lg,
        "xl 28" to Radius.xl,
    )
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sp4),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            shapes.forEach { (label, radius) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(radius))
                            .background(Surface3)
                            .border(1.dp, Outline, RoundedCornerShape(radius)),
                    )
                    Spacer(Modifier.height(Spacing.sp1))
                    Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceFaint)
                }
            }
        }
        Spacer(Modifier.height(Spacing.sp4))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(Dimens.hitMin)
                    .clip(RoundedCornerShape(Radius.md))
                    .background(Surface3)
                    .border(1.dp, DailyTrackerTheme.accents.workout.base, RoundedCornerShape(Radius.md)),
            )
            Spacer(Modifier.width(Spacing.sp3))
            Text("hit-min 48dp · row-height 56dp", style = TypeNumeric, color = OnSurfaceFaint)
        }
    }
}

/** Renders a colour as #RRGGBB for side-by-side comparison with the CSS tokens. */
private fun Color.hex(): String {
    val argb = toArgb()
    return "#%02X%02X%02X".format(
        (argb shr 16) and 0xFF,
        (argb shr 8) and 0xFF,
        argb and 0xFF,
    )
}

@Preview(name = "Tokens", showBackground = true, heightDp = 1800)
@Composable
private fun TokenGalleryPreview() {
    DailyTrackerTheme { TokenGalleryScreen() }
}
