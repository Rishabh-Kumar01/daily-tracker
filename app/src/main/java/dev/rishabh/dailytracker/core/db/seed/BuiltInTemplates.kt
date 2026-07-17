package dev.rishabh.dailytracker.core.db.seed

import dev.rishabh.dailytracker.core.db.FieldType
import dev.rishabh.dailytracker.core.db.SummaryMetricTypes
import dev.rishabh.dailytracker.core.db.VariantSource

/*
 * The four built-in activities, expressed as data.
 *
 * This file is the proof of the core principle: Diet, Workout, Study and Sleep are rows,
 * not code. They run through the same seeder, the same tables and the same renderer that a
 * user-created or AI-drafted activity will. Nothing here is special-cased downstream.
 *
 * IDs are absent on purpose — the seeder generates them, so a template definition can never
 * smuggle in an ID from outside.
 */

data class FieldSpec(
    val fieldKey: String,
    val type: FieldType,
    val label: String,
    val unit: String? = null,
    val required: Boolean = false,
    val optionsJson: String? = null,
)

data class ItemSpec(
    val name: String,
    val hasVariants: Boolean = false,
    val variantSource: VariantSource? = null,
    val fields: List<FieldSpec>,
)

data class SubMenuSpec(
    val name: String,
    val scheduleJson: String? = null,
    val items: List<ItemSpec>,
)

data class TemplateSpec(
    val name: String,
    val icon: String,
    val color: String,
    val summaryMetricType: String?,
    val summaryMetricLabel: String?,
    val subMenus: List<SubMenuSpec>,
)

private const val SCHEDULE_DAILY = """{"type":"daily"}"""

/** Grams field shared by every food: 0 and up, 10g steps, defaulting to 100g. */
private val gramsField = FieldSpec(
    fieldKey = "amount",
    type = FieldType.QUANTITY,
    label = "Amount",
    unit = "g",
    required = true,
    optionsJson = """{"min":0,"step":10,"default":100}""",
)

/** Which product was eaten. Resolving this is what gives the entry its nutrients. */
private val variantField = FieldSpec(
    fieldKey = "variant",
    type = FieldType.ITEM_VARIANT,
    label = "Brand",
    required = true,
)

/**
 * Every food follows the food -> brand -> product pattern, so macros always resolve
 * through a product and are never typed in by hand.
 */
private fun food(name: String) = ItemSpec(
    name = name,
    hasVariants = true,
    variantSource = VariantSource.USER_LIBRARY,
    fields = listOf(variantField, gramsField),
)

private fun meal(name: String, foods: List<String>) = SubMenuSpec(
    name = name,
    scheduleJson = SCHEDULE_DAILY,
    items = foods.map(::food),
)

private val diet = TemplateSpec(
    name = "Diet",
    icon = "restaurant",
    // Matches --accent-diet after gamut mapping (see core/designsystem/Color.kt).
    color = "#75D78D",
    summaryMetricType = SummaryMetricTypes.SUM_FIELD,
    summaryMetricLabel = "kcal",
    subMenus = listOf(
        meal("Breakfast", listOf("Eggs", "Oats", "Milk", "Banana")),
        // Mirrors the Lunch Screen design, down to the item order.
        meal("Lunch", listOf("Paneer", "Dal", "Rice", "Roti", "Curd")),
        meal("Snacks", listOf("Almonds", "Tea", "Fruit")),
        meal("Dinner", listOf("Chicken", "Vegetables", "Roti", "Rice")),
    ),
)

/** reps x weight rows; the renderer shows last session inline. */
private val setGroupField = FieldSpec(
    fieldKey = "sets",
    type = FieldType.SET_GROUP,
    label = "Sets",
    required = true,
    optionsJson = """{"fields":["reps","weight"],"weight_unit":"kg"}""",
)

private val exerciseNote = FieldSpec(
    fieldKey = "note",
    type = FieldType.NOTE,
    label = "Note",
)

private fun exercise(name: String) = ItemSpec(name = name, fields = listOf(setGroupField, exerciseNote))

private val workout = TemplateSpec(
    name = "Workout",
    icon = "fitness_center",
    color = "#FFA460",
    summaryMetricType = SummaryMetricTypes.COMPLETION_PERCENT,
    summaryMetricLabel = "done",
    subMenus = listOf(
        SubMenuSpec("Push", items = listOf("Bench Press", "Overhead Press", "Triceps Pushdown").map(::exercise)),
        SubMenuSpec("Pull", items = listOf("Deadlift", "Pull-ups", "Barbell Row").map(::exercise)),
        SubMenuSpec("Legs", items = listOf("Squat", "Leg Press", "Calf Raise").map(::exercise)),
    ),
)

private val durationField = FieldSpec(
    fieldKey = "duration",
    type = FieldType.DURATION,
    label = "Duration",
    unit = "min",
    required = true,
    optionsJson = """{"timer_ui":true}""",
)

private val focusField = FieldSpec(
    fieldKey = "focus",
    type = FieldType.SCALE,
    label = "Focus",
    optionsJson = """{"min":1,"max":5}""",
)

private val study = TemplateSpec(
    name = "Study",
    icon = "school",
    color = "#7BC3FF",
    summaryMetricType = SummaryMetricTypes.SUM_FIELD,
    summaryMetricLabel = "min",
    subMenus = listOf(
        SubMenuSpec(
            "Sessions",
            scheduleJson = SCHEDULE_DAILY,
            items = listOf(
                ItemSpec("Reading", fields = listOf(durationField, focusField)),
                ItemSpec("Practice", fields = listOf(durationField, focusField)),
            ),
        ),
        SubMenuSpec(
            "Revision",
            scheduleJson = SCHEDULE_DAILY,
            items = listOf(
                ItemSpec("Flashcards", fields = listOf(durationField)),
                ItemSpec("MCQ Practice", fields = listOf(durationField)),
            ),
        ),
    ),
)

private val sleep = TemplateSpec(
    name = "Sleep",
    icon = "bedtime",
    color = "#D3A6FF",
    summaryMetricType = SummaryMetricTypes.SUM_FIELD,
    summaryMetricLabel = "h",
    subMenus = listOf(
        SubMenuSpec(
            "Bedtime",
            scheduleJson = SCHEDULE_DAILY,
            items = listOf(
                ItemSpec(
                    "Bedtime",
                    fields = listOf(
                        FieldSpec("bed_time", FieldType.TIME, "Bed at", required = true),
                        FieldSpec("restedness", FieldType.SCALE, "Restedness", optionsJson = """{"min":1,"max":5}"""),
                    ),
                ),
            ),
        ),
        // Naps are ordinary log entries with a duration field — no separate table.
        SubMenuSpec(
            "Nap",
            items = listOf(
                ItemSpec(
                    "Nap",
                    fields = listOf(FieldSpec("duration", FieldType.DURATION, "Duration", unit = "min", required = true)),
                ),
            ),
        ),
    ),
)

/** Installed in this order; index becomes sort_order. */
val BUILT_IN_TEMPLATES: List<TemplateSpec> = listOf(diet, workout, study, sleep)
