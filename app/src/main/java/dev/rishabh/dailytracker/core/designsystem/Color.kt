package dev.rishabh.dailytracker.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * Translated from the CSS custom properties in design/claude-design/tokens/colors.css.
 * Token name -> Kotlin name is 1:1 so the two stay diffable.
 *
 * Dark is the only authored theme, so there is no light counterpart here.
 */

// --- Neutral dark base (Material 3 tonal surfaces) ---

/** --surface-0: app background */
val Surface0 = Color(0xFF0E1013)

/** --surface-1: cards */
val Surface1 = Color(0xFF16181D)

/** --surface-2: raised cards, sheets */
val Surface2 = Color(0xFF1C1F25)

/** --surface-3: highest elevation */
val Surface3 = Color(0xFF23262E)

/** --on-surface: primary text */
val OnSurface = Color(0xFFE4E6EA)

/** --on-surface-variant: secondary text */
val OnSurfaceVariant = Color(0xFFA2A8B3)

/** --on-surface-faint: tertiary text, placeholders */
val OnSurfaceFaint = Color(0xFF6A707C)

/** --outline: borders, dividers */
val Outline = Color(0xFF3A3E47)

/** --outline-variant: subtle dividers */
val OutlineVariant = Color(0xFF262A31)

/** --scrim: rgba(0, 0, 0, 0.55), behind sheets */
val Scrim = Color(0x8C000000)

/** Destructive actions (delete/archive). The one non-accent signal colour in the palette. */
val Danger = Color(0xFFE5646B)
val DangerContainer = Color(0xFF3A1A1E)

/** --on-accent: text/icons placed ON a solid accent fill */
val OnAccent = Color(0xFF101216)

// --- State layers ---

/** --state-hover: rgba(228, 230, 234, 0.06) */
val StateHover = Color(0x0FE4E6EA)

/** --state-pressed: rgba(228, 230, 234, 0.10) */
val StatePressed = Color(0x1AE4E6EA)

/** --disabled-opacity */
const val DisabledOpacity = 0.38f

// --- Activity accents ---
//
// Source tokens are oklch(0.80 0.14 H) with oklch(0.28 0.05 H) containers, i.e. every
// accent shares one lightness and chroma and differs only in hue.
//
// Compose's Color has no oklch constructor, so these are pre-converted to sRGB. Three of
// the four base accents fall outside the sRGB gamut at C=0.14, so they were gamut-mapped
// the way CSS Color 4 (and therefore the browser preview) does it: reduce chroma, holding
// lightness and hue fixed, until the colour is representable. Naive per-channel clamping
// would shift lightness per hue and break the equal-lightness rule the design relies on.
// Only --accent-study is visibly affected (C 0.14 -> 0.128).
//
// If a hue is ever re-tuned upstream, re-derive the hex rather than hand-editing it.

/** --accent-diet: oklch(0.80 0.14 150) — green, in gamut */
val AccentDiet = Color(0xFF75D78D)

/** --accent-workout: oklch(0.80 0.14 55) — orange */
val AccentWorkout = Color(0xFFFFA460)

/** --accent-study: oklch(0.80 0.14 250) — blue, gamut-mapped to C 0.128 */
val AccentStudy = Color(0xFF7BC3FF)

/** --accent-sleep: oklch(0.80 0.14 305) — violet */
val AccentSleep = Color(0xFFD3A6FF)

/** --accent-diet-container: oklch(0.28 0.05 150) */
val AccentDietContainer = Color(0xFF15301B)

/** --accent-workout-container: oklch(0.28 0.05 55) */
val AccentWorkoutContainer = Color(0xFF3C220F)

/** --accent-study-container: oklch(0.28 0.05 250) */
val AccentStudyContainer = Color(0xFF142A41)

/** --accent-sleep-container: oklch(0.28 0.05 305) */
val AccentSleepContainer = Color(0xFF2F223D)

/**
 * Which activity an accent belongs to.
 *
 * These four are the built-in activities. User-created activities pick one of these
 * accents rather than introducing a fifth hue — the design system authors exactly four.
 */
enum class ActivityKey { DIET, WORKOUT, STUDY, SLEEP }

/**
 * One activity's accent pair.
 *
 * @param base solid fill / icon / border colour
 * @param container tint used for selected fills and icon chips
 */
@Immutable
data class AccentColors(val base: Color, val container: Color)

/**
 * The four activity accents.
 *
 * Material 3 has no colour slot for four peer accents (primary/secondary/tertiary carry
 * different meaning), so they travel beside the ColorScheme instead of inside it.
 * Never mix two accents in one component.
 */
@Immutable
data class ActivityAccents(
    val diet: AccentColors = AccentColors(AccentDiet, AccentDietContainer),
    val workout: AccentColors = AccentColors(AccentWorkout, AccentWorkoutContainer),
    val study: AccentColors = AccentColors(AccentStudy, AccentStudyContainer),
    val sleep: AccentColors = AccentColors(AccentSleep, AccentSleepContainer),
) {
    fun of(key: ActivityKey): AccentColors = when (key) {
        ActivityKey.DIET -> diet
        ActivityKey.WORKOUT -> workout
        ActivityKey.STUDY -> study
        ActivityKey.SLEEP -> sleep
    }
}

/** Accents for the current theme. Read via [DailyTrackerTheme.accents]. */
val LocalActivityAccents = staticCompositionLocalOf { ActivityAccents() }

/**
 * The accent of the activity whose screen is currently being rendered.
 *
 * Screens belong to exactly one activity, so [dev.rishabh.dailytracker.core.designsystem.ProvideActivityAccent]
 * sets this once at the top of a screen and components below read it instead of taking an
 * accent parameter everywhere.
 */
val LocalCurrentAccent = staticCompositionLocalOf { AccentColors(AccentDiet, AccentDietContainer) }
