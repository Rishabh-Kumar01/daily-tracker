package dev.rishabh.dailytracker.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/*
 * Translated from design/claude-design/tokens/typography.css.
 *
 * Fonts: the CSS asks for Roboto (UI) and Roboto Mono (numbers). Roboto is already the
 * Android system default, and FontFamily.Monospace resolves to the platform mono face, so
 * neither needs to ship as an asset. CSS px map to sp 1:1.
 */

/** --font-ui: 'Roboto', system-ui, sans-serif */
val FontUi = FontFamily.Default

/** --font-mono: 'Roboto Mono', ui-monospace, monospace */
val FontMono = FontFamily.Monospace

/** --tracking-label: 0.03em */
val TrackingLabel = 0.03.em

/**
 * The design's type scale mapped onto Material 3's slots.
 *
 * Only the slots the design actually specifies are overridden; the rest keep M3 defaults
 * so any stock component still renders sensibly.
 */
val DailyTrackerTypography = Typography().run {
    copy(
        // --type-headline: 500 24px/32px — screen titles
        headlineSmall = headlineSmall.copy(
            fontFamily = FontUi, fontWeight = FontWeight.Medium,
            fontSize = 24.sp, lineHeight = 32.sp,
        ),
        // --type-title-lg: 500 20px/28px — sheet titles
        titleLarge = titleLarge.copy(
            fontFamily = FontUi, fontWeight = FontWeight.Medium,
            fontSize = 20.sp, lineHeight = 28.sp,
        ),
        // --type-title: 500 16px/24px — card/component names
        titleMedium = titleMedium.copy(
            fontFamily = FontUi, fontWeight = FontWeight.Medium,
            fontSize = 16.sp, lineHeight = 24.sp,
        ),
        // --type-body-lg: 400 16px/24px — list item names
        bodyLarge = bodyLarge.copy(
            fontFamily = FontUi, fontWeight = FontWeight.Normal,
            fontSize = 16.sp, lineHeight = 24.sp,
        ),
        // --type-body: 400 14px/20px — summaries, descriptions
        bodyMedium = bodyMedium.copy(
            fontFamily = FontUi, fontWeight = FontWeight.Normal,
            fontSize = 14.sp, lineHeight = 20.sp,
        ),
        // --type-label: 500 12px/16px + --tracking-label — chips, buttons
        labelMedium = labelMedium.copy(
            fontFamily = FontUi, fontWeight = FontWeight.Medium,
            fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = TrackingLabel,
        ),
        // --type-caption: 400 11px/16px — per-100g lines, hints
        labelSmall = labelSmall.copy(
            fontFamily = FontUi, fontWeight = FontWeight.Normal,
            fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.sp,
        ),
    )
}

/*
 * Numeric styles have no Material 3 slot, so they live outside Typography.
 *
 * The design's rule: any value that is right-aligned or live-updating renders in mono, so
 * digits do not jitter as they change.
 */

/** --type-numeric: 500 14px/20px mono — right-aligned values */
val TypeNumeric = TextStyle(
    fontFamily = FontMono, fontWeight = FontWeight.Medium,
    fontSize = 14.sp, lineHeight = 20.sp,
)

/** --type-numeric-lg: 500 22px/28px mono — stepper readout */
val TypeNumericLarge = TextStyle(
    fontFamily = FontMono, fontWeight = FontWeight.Medium,
    fontSize = 22.sp, lineHeight = 28.sp,
)
