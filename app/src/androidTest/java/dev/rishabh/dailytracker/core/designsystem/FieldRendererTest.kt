package dev.rishabh.dailytracker.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import dev.rishabh.dailytracker.core.db.FieldType
import dev.rishabh.dailytracker.core.db.entity.ItemFieldEntity
import dev.rishabh.dailytracker.core.designsystem.component.FieldRenderer
import dev.rishabh.dailytracker.core.designsystem.component.model.LogValueDraft
import org.junit.Rule
import org.junit.Test

/**
 * Renders every field type through the one FieldRenderer and asserts each appears — the
 * instrumented proof that "activities are data" holds across the whole closed vocabulary,
 * and that an unknown type degrades to a card instead of crashing.
 */
class FieldRendererTest {

    @get:Rule
    val rule = createComposeRule()

    private fun field(
        type: String,
        label: String,
        key: String = "k",
        unit: String? = null,
        options: String? = null,
    ) = ItemFieldEntity(
        fieldId = "f",
        itemId = "i",
        fieldKey = key,
        type = type,
        label = label,
        unit = unit,
        required = false,
        sortOrder = 0,
        optionsJson = options,
    )

    private fun renderField(f: ItemFieldEntity, initial: LogValueDraft = LogValueDraft.empty(f.fieldKey)) {
        rule.setContent {
            DailyTrackerTheme {
                var draft by remember { mutableStateOf(initial) }
                FieldRenderer(field = f, draft = draft, onChange = { draft = it }, modifier = Modifier.padding(8.dp))
            }
        }
    }

    @Test
    fun everyKnownFieldTypeRenders() {
        // One field per entry in the closed vocabulary, all in one scrollable composition.
        // If any type were missing from the renderer's when(), it would fall through to the
        // unsupported card and its label would be absent from the tree.
        val samples = listOf(
            field(FieldType.CHECKBOX.wire, "Taken"),
            field(FieldType.QUANTITY.wire, "Amount", unit = "g", options = """{"min":0,"step":10,"default":100}"""),
            field(FieldType.ITEM_VARIANT.wire, "Brand"),
            field(FieldType.PHOTO.wire, "Scalp photo", options = """{"comparison_series":true}"""),
            field(FieldType.SCALE.wire, "Restedness", options = """{"min":1,"max":5}"""),
            field(FieldType.DURATION.wire, "Duration", unit = "min", options = """{"timer_ui":true}"""),
            field(FieldType.NOTE.wire, "Note"),
            field(FieldType.SET_GROUP.wire, "Sets"),
            field(FieldType.TIME.wire, "Bed at"),
            field(FieldType.SINGLE_SELECT.wire, "Mood", options = """{"options":[{"id":"good","label":"Good"}]}"""),
            field(FieldType.MULTI_SELECT.wire, "Symptoms", options = """{"options":[{"id":"itch","label":"Itch"}]}"""),
        )

        rule.setContent {
            DailyTrackerTheme {
                // Plain scrollable Column composes every child (unlike LazyColumn), so every
                // field is in the semantics tree even below the fold.
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    samples.forEach { sample ->
                        FieldRenderer(
                            field = sample,
                            draft = LogValueDraft.empty(sample.fieldKey),
                            onChange = {},
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }
        }

        for (sample in samples) {
            // existence in the tree proves each type produced its own field rather than the
            // unsupported fallback.
            rule.onNodeWithText(sample.label).assertExists()
        }
        // and the fallback card never appeared for any of them
        rule.onAllNodesWithText("Unsupported field", substring = true).assertCountEquals(0)
    }

    @Test
    fun unknownFieldTypeShowsUnsupportedCardNotACrash() {
        renderField(field("holographic_slider", "Mystery field"))

        rule.onNodeWithText("Mystery field").assertIsDisplayed()
        rule.onNodeWithText("Unsupported field (holographic_slider)").assertIsDisplayed()
    }

    @Test
    fun checkboxReportsTheToggleBack() {
        val f = field(FieldType.CHECKBOX.wire, "Taken", key = "taken")
        var latest = LogValueDraft.empty("taken")
        rule.setContent {
            DailyTrackerTheme {
                var draft by remember { mutableStateOf(LogValueDraft.empty("taken")) }
                FieldRenderer(f, draft, onChange = { draft = it; latest = it })
            }
        }

        rule.onNodeWithText("Taken").performClick()

        rule.runOnIdle { assert(latest.bool == true) { "checkbox should report checked=true" } }
    }

    @Test
    fun quantityStepperRespectsStepAndFloor() {
        val f = field(FieldType.QUANTITY.wire, "Amount", key = "amount", unit = "g", options = """{"min":0,"step":10,"default":100}""")
        renderField(f, LogValueDraft(fieldKey = "amount", number = 100.0))

        // default shown
        rule.onNodeWithText("100 g").assertIsDisplayed()
        rule.onNodeWithContentDescription("more").performClick()
        rule.onNodeWithText("110 g").assertIsDisplayed()
    }

    @Test
    fun quantityStepperClampsAtMin() {
        val f = field(FieldType.QUANTITY.wire, "Amount", key = "amount", unit = "g", options = """{"min":0,"step":10,"default":0}""")
        renderField(f, LogValueDraft(fieldKey = "amount", number = 0.0))

        rule.onNodeWithContentDescription("less").performClick()
        // floor at min=0, never negative
        rule.onNodeWithText("0 g").assertIsDisplayed()
    }

    @Test
    fun scaleSelectionHighlightsChosenValue() {
        val f = field(FieldType.SCALE.wire, "Restedness", key = "rest", options = """{"min":1,"max":5}""")
        var latest = LogValueDraft.empty("rest")
        rule.setContent {
            DailyTrackerTheme {
                var draft by remember { mutableStateOf(LogValueDraft.empty("rest")) }
                FieldRenderer(f, draft, onChange = { draft = it; latest = it })
            }
        }

        rule.onNodeWithText("4").performClick()

        rule.runOnIdle { assert(latest.number == 4.0) { "scale should report 4" } }
    }

    @Test
    fun malformedOptionsDoNotCrashTheField() {
        // options_json is a free column; a broken blob must degrade to defaults, not throw.
        renderField(field(FieldType.QUANTITY.wire, "Amount", unit = "g", options = "{not valid json"))
        rule.onNodeWithText("Amount").assertIsDisplayed()
    }
}
