package dev.rishabh.dailytracker.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WaterDrop
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
    "self_improvement" -> Icons.Rounded.SelfImprovement
    "medication" -> Icons.Rounded.Medication
    "water_drop" -> Icons.Rounded.WaterDrop
    "favorite" -> Icons.Rounded.Favorite
    "spa" -> Icons.Rounded.Spa
    "directions_run" -> Icons.Rounded.DirectionsRun
    "menu_book" -> Icons.Rounded.MenuBook
    "local_drink" -> Icons.Rounded.LocalDrink
    "brush" -> Icons.Rounded.Brush
    "checklist" -> Icons.Rounded.Checklist
    "category" -> Icons.Rounded.Category
    else -> Icons.Rounded.Category
}

/** The icons a user can pick for a custom activity, as stable string keys. */
val builderIconKeys: List<String> = listOf(
    "category",
    "self_improvement",
    "medication",
    "water_drop",
    "favorite",
    "spa",
    "directions_run",
    "menu_book",
    "local_drink",
    "brush",
    "checklist",
    "restaurant",
    "fitness_center",
    "school",
    "bedtime",
)

/**
 * Maps a template's stored colour hex to one of the four accents.
 *
 * There are only four authored accents, so a user-created activity picks one of them too.
 * The built-ins store exactly the gamut-mapped accent hexes; anything unrecognised falls
 * back to Diet rather than inventing a fifth hue. Case-insensitive to tolerate #RRGGBB
 * written either way.
 */
fun accentKeyForColor(colorHex: String?): ActivityKey = when (colorHex?.uppercase()) {
    "#75D78D" -> ActivityKey.DIET
    "#FFA460" -> ActivityKey.WORKOUT
    "#7BC3FF" -> ActivityKey.STUDY
    "#D3A6FF" -> ActivityKey.SLEEP
    else -> ActivityKey.DIET
}
