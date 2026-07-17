package dev.rishabh.dailytracker.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.ui.graphics.vector.ImageVector

/*
 * Templates store an icon as a string key (the design system uses Material Symbols names).
 * Compose can't resolve those dynamically, so known keys map to bundled vectors and
 * anything unknown falls back — a user- or AI-created activity with an unfamiliar icon
 * still renders, it just gets the generic mark.
 */

/** Display name for a built-in activity when a template doesn't override it. */
val ActivityKey.displayName: String
    get() = when (this) {
        ActivityKey.DIET -> "Diet"
        ActivityKey.WORKOUT -> "Workout"
        ActivityKey.STUDY -> "Study"
        ActivityKey.SLEEP -> "Sleep"
    }

/** Canonical icon key for a built-in activity (matches the seeded templates). */
val ActivityKey.iconKey: String
    get() = when (this) {
        ActivityKey.DIET -> "restaurant"
        ActivityKey.WORKOUT -> "fitness_center"
        ActivityKey.STUDY -> "school"
        ActivityKey.SLEEP -> "bedtime"
    }

/** Resolves a stored icon key to a vector, or the generic fallback for unknown keys. */
fun iconForKey(key: String?): ImageVector = when (key) {
    "restaurant" -> Icons.Rounded.Restaurant
    "fitness_center" -> Icons.Rounded.FitnessCenter
    "school" -> Icons.Rounded.School
    "bedtime" -> Icons.Rounded.Bedtime
    else -> Icons.Rounded.Category
}
