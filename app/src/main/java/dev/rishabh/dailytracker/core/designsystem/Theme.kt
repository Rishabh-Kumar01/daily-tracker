package dev.rishabh.dailytracker.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * The token-derived colour scheme.
 *
 * Dark only, by design: the design system authors no light theme, and there is no dynamic
 * colour — the activity accents are the app's identity and Material You would override them.
 *
 * The four activity accents are deliberately absent from this scheme. Material 3's
 * primary/secondary/tertiary slots carry fixed semantics and there are only three of them,
 * so accents ride alongside in [LocalActivityAccents] and are bound per screen by
 * [ProvideActivityAccent].
 */
private val DailyTrackerColorScheme = darkColorScheme(
    background = Surface0,
    onBackground = OnSurface,

    surface = Surface0,
    onSurface = OnSurface,
    surfaceVariant = Surface2,
    onSurfaceVariant = OnSurfaceVariant,

    // Tonal elevation: lighter surface = higher. The design uses no drop shadows except
    // on sheets, so these carry all of the depth.
    surfaceContainerLowest = Surface0,
    surfaceContainerLow = Surface1,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
    surfaceContainerHighest = Surface3,

    outline = Outline,
    outlineVariant = OutlineVariant,
    scrim = Scrim,

    // No global accent exists. Neutral defaults keep any stock M3 component from
    // inventing a colour; anything activity-coloured reads the accent explicitly.
    primary = OnSurface,
    onPrimary = OnAccent,
    secondary = OnSurfaceVariant,
    onSecondary = OnAccent,
)

@Composable
fun DailyTrackerTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalActivityAccents provides ActivityAccents()) {
        MaterialTheme(
            colorScheme = DailyTrackerColorScheme,
            typography = DailyTrackerTypography,
            shapes = DailyTrackerShapes,
            content = content,
        )
    }
}

/**
 * Binds one activity's accent for a screen's subtree.
 *
 * Screens belong to exactly one activity, so this is set once at the top of a screen and
 * components below read [DailyTrackerTheme.accent] rather than threading an accent
 * parameter through every call site. This is what keeps "never mix two accents in one
 * component" cheap to honour.
 */
@Composable
fun ProvideActivityAccent(key: ActivityKey, content: @Composable () -> Unit) {
    val accents = LocalActivityAccents.current
    CompositionLocalProvider(LocalCurrentAccent provides accents.of(key), content = content)
}

/** Theme accessors, mirroring how MaterialTheme exposes its own values. */
object DailyTrackerTheme {
    /** All four activity accents. */
    val accents: ActivityAccents
        @Composable @ReadOnlyComposable get() = LocalActivityAccents.current

    /** The accent of the activity currently being rendered (see [ProvideActivityAccent]). */
    val accent: AccentColors
        @Composable @ReadOnlyComposable get() = LocalCurrentAccent.current
}
