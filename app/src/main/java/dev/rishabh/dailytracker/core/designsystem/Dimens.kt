package dev.rishabh.dailytracker.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/*
 * Translated from design/claude-design/tokens/spacing.css. CSS px map to dp 1:1.
 */

/** The 4px base grid: --sp-1 … --sp-8. */
object Spacing {
    val sp1 = 4.dp
    val sp2 = 8.dp
    val sp3 = 12.dp
    val sp4 = 16.dp
    val sp5 = 20.dp
    val sp6 = 24.dp
    val sp8 = 32.dp

    /** 16px screen gutters (design system layout rule). */
    val screenGutter = sp4
}

/** Corner radii — the M3 shape scale the design uses. */
object Radius {
    /** --radius-xs: checkboxes */
    val xs = 6.dp

    /** --radius-sm: thumbnails */
    val sm = 8.dp

    /** --radius-md: list rows */
    val md = 12.dp

    /** --radius-lg: cards */
    val lg = 16.dp

    /** --radius-xl: bottom sheets (top corners) */
    val xl = 28.dp
}

/** Touch-target and row sizing. */
object Dimens {
    /** --hit-min: minimum touch target */
    val hitMin = 48.dp

    /** --row-height: list rows */
    val rowHeight = 56.dp

    /** Product/brand thumbnail edge (BrandPickerRow). */
    val thumbnail = 44.dp
}

/** --radius-full: pills, steppers. */
val ShapeFull = RoundedCornerShape(percent = 50)

/** Top-rounded sheet shape: --radius-xl on the top corners only. */
val ShapeSheet = RoundedCornerShape(topStart = Radius.xl, topEnd = Radius.xl)

/**
 * Material 3 shape scale mapped to the design's radii, so stock components pick up the
 * right corners without every call site passing a shape.
 */
val DailyTrackerShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.xs),
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(Radius.xl),
)
