package dev.rishabh.dailytracker.feature.activities

import dev.rishabh.dailytracker.core.designsystem.ActivityKey

/** One row on the Home screen: a template plus its today summary. */
data class HomeActivity(
    val templateId: String,
    val name: String,
    val iconKey: String,
    /** Which of the four accents this activity uses. */
    val accent: ActivityKey,
    /** One-line today summary; "Nothing logged yet" when empty. */
    val summary: String,
)

/** A sub-menu row inside an activity (e.g. Lunch, Push, Bedtime). */
data class SubMenuRow(
    val subMenuId: String,
    val name: String,
    val itemCount: Int,
)

/** The header + rows shown when browsing one activity's sub-menus. */
data class ActivityDetail(
    val templateId: String,
    val name: String,
    val accent: ActivityKey,
    val subMenus: List<SubMenuRow>,
    /**
     * Whether this activity totals calories, and so shows the day-macros header.
     *
     * Read from the template's own summary metric (not by recognising "Diet" by name), so a
     * calorie-tracking activity the user creates gets the same header.
     */
    val tracksCalories: Boolean = false,
)

/** One item inside a sub-menu, with the labels of the fields it would log. */
data class ItemRowDetail(
    val itemId: String,
    val name: String,
    val hasVariants: Boolean,
    val fieldLabels: List<String>,
)

/** The header + item rows shown when browsing one sub-menu. */
data class SubMenuDetail(
    val subMenuId: String,
    val name: String,
    val accent: ActivityKey,
    val items: List<ItemRowDetail>,
    /**
     * Whether the items carry a set_group field, and so log through the generic set-logging
     * screen (Workout). Decided from the template data, never by activity name.
     */
    val hasSetLogging: Boolean = false,
) {
    /**
     * Whether this sub-menu logs the food -> brand -> product pattern, and so gets the meal
     * screen instead of the browse leaf.
     *
     * Decided from the template data rather than by recognising "Diet" by name: any
     * activity whose items carry variants earns the same screen, which is the whole point
     * of activities being data.
     */
    val isVariantLogging: Boolean get() = items.isNotEmpty() && items.all { it.hasVariants }
}
