package dev.rishabh.dailytracker.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.rishabh.dailytracker.core.db.FieldType
import dev.rishabh.dailytracker.core.db.entity.ItemFieldEntity
import dev.rishabh.dailytracker.core.designsystem.ActivityKey
import dev.rishabh.dailytracker.core.designsystem.DailyTrackerTheme
import dev.rishabh.dailytracker.core.designsystem.OnSurface
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceFaint
import dev.rishabh.dailytracker.core.designsystem.OutlineVariant
import dev.rishabh.dailytracker.core.designsystem.Spacing
import dev.rishabh.dailytracker.core.designsystem.component.ActivityCard
import dev.rishabh.dailytracker.core.designsystem.component.BrandPickerRow
import dev.rishabh.dailytracker.core.designsystem.component.ConfirmField
import dev.rishabh.dailytracker.core.designsystem.component.ConfirmSheet
import dev.rishabh.dailytracker.core.designsystem.component.FieldRenderer
import dev.rishabh.dailytracker.core.designsystem.component.ItemRow
import dev.rishabh.dailytracker.core.designsystem.component.Per100g
import dev.rishabh.dailytracker.core.designsystem.component.QuantitySheet
import dev.rishabh.dailytracker.core.designsystem.component.model.LogValueDraft
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Debug-only gallery of the component library and the FieldRenderer across the whole field
 * vocabulary. Debug source set only, so it never reaches a release build.
 */
class ComponentGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DailyTrackerTheme { ComponentGalleryScreen() } }
    }
}

@Composable
private fun ComponentGalleryScreen() {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            contentPadding = PaddingValues(Spacing.sp4),
            verticalArrangement = Arrangement.spacedBy(Spacing.sp3),
        ) {
            item { Header("Components") }
            item {
                ActivityCard(ActivityKey.DIET, "1,840 kcal · 132g protein")
            }
            item { ActivityCard(ActivityKey.STUDY, "2h 10m · Linear algebra", selected = true) }
            item {
                ItemRow("Chicken breast", value = "264 kcal", accent = DailyTrackerTheme.accents.diet, checked = true)
            }
            item {
                BrandPickerRow(
                    "Amul", "Malai Paneer", "per 100g · 296 kcal · 18.5P · 5.4C · 22.7F",
                    accent = DailyTrackerTheme.accents.diet, selected = true,
                )
            }
            item {
                QuantitySheet(
                    brand = "Amul", product = "Malai Paneer",
                    per100g = Per100g(296.0, 18.5, 5.4, 22.7),
                    initialGrams = 150.0, edited = true, accent = DailyTrackerTheme.accents.diet,
                )
            }
            item {
                ConfirmSheet(
                    title = "Log workout", accent = DailyTrackerTheme.accents.workout, focusedField = 2,
                    fields = listOf(
                        ConfirmField("Exercise", "Bench press"),
                        ConfirmField("Sets × reps", "4 × 8"),
                        ConfirmField("Weight", "60", suffix = "kg"),
                    ),
                )
            }

            item { Header("FieldRenderer — every field type") }
            items(galleryFields()) { f -> FieldSample(f) }
            item { FieldSample(ItemFieldEntity("x", "i", "k", "holographic_slider", "Unknown type", null, false, 0, null)) }
        }
    }
}

@Composable
private fun FieldSample(field: ItemFieldEntity) {
    var draft by remember { mutableStateOf(LogValueDraft.empty(field.fieldKey)) }
    FieldRenderer(field, draft, onChange = { draft = it }, accent = DailyTrackerTheme.accents.study)
}

@Composable
private fun Header(text: String) {
    Column {
        Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = OnSurfaceFaint)
        HorizontalDivider(color = OutlineVariant)
    }
}

private fun galleryFields(): List<ItemFieldEntity> {
    fun f(type: String, label: String, key: String, unit: String? = null, options: String? = null) =
        ItemFieldEntity("id-$key", "i", key, type, label, unit, false, 0, options)
    return listOf(
        f(FieldType.CHECKBOX.wire, "Taken", "chk"),
        f(FieldType.QUANTITY.wire, "Amount", "qty", "g", """{"min":0,"step":10,"default":100}"""),
        f(FieldType.ITEM_VARIANT.wire, "Brand", "var"),
        f(FieldType.PHOTO.wire, "Scalp photo", "pho", options = """{"comparison_series":true}"""),
        f(FieldType.SCALE.wire, "Restedness", "scl", options = """{"min":1,"max":5}"""),
        f(FieldType.DURATION.wire, "Duration", "dur", "min", """{"timer_ui":true}"""),
        f(FieldType.NOTE.wire, "Note", "not"),
        f(FieldType.SET_GROUP.wire, "Sets", "set"),
        f(FieldType.TIME.wire, "Bed at", "tim"),
        f(FieldType.SINGLE_SELECT.wire, "Mood", "ssel", options = """{"options":[{"id":"good","label":"Good"},{"id":"ok","label":"OK"},{"id":"bad","label":"Bad"}]}"""),
        f(FieldType.MULTI_SELECT.wire, "Symptoms", "msel", options = """{"options":[{"id":"itch","label":"Itch"},{"id":"flake","label":"Flaking"}]}"""),
    )
}
