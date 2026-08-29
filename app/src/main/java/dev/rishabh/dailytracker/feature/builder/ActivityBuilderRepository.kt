package dev.rishabh.dailytracker.feature.builder

import dev.rishabh.dailytracker.core.common.IdGenerator
import dev.rishabh.dailytracker.core.common.TimeSource
import dev.rishabh.dailytracker.core.db.CreatedBy
import dev.rishabh.dailytracker.core.db.dao.TemplateDao
import dev.rishabh.dailytracker.core.db.entity.ActivityTemplateEntity
import dev.rishabh.dailytracker.core.db.entity.ItemEntity
import dev.rishabh.dailytracker.core.db.entity.ItemFieldEntity
import dev.rishabh.dailytracker.core.db.entity.SubMenuEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns a builder [ActivityDraft] into template rows — the manual half of "activities are
 * data": a user-created activity is new rows in the same four template tables the built-ins
 * use, rendered and logged by the same generic screens with nothing special-cased.
 *
 * A deterministic validator sits in front of the write: blank names are rejected with a
 * clear message, field keys are slugged and de-duplicated per item, and every id is
 * generated here, never taken from the draft.
 */
@Singleton
class ActivityBuilderRepository @Inject constructor(
    private val templateDao: TemplateDao,
    private val ids: IdGenerator,
    private val time: TimeSource,
) {

    suspend fun createActivity(draft: ActivityDraft): CreateResult {
        validate(draft)?.let { return CreateResult.Invalid(it) }

        val now = time.nowMillis()
        val templateId = ids.newId()
        val template = ActivityTemplateEntity(
            templateId = templateId,
            name = draft.name.trim(),
            icon = draft.iconKey,
            color = draft.colorHex,
            createdBy = CreatedBy.USER,
            // No calorie/completion metric — the generic count summary covers a custom
            // activity on Home until the user asks for more.
            summaryMetricType = null,
            summaryMetricLabel = null,
            sortOrder = templateDao.maxTemplateSortOrder() + 1,
            createdAt = now,
        )

        val subMenus = mutableListOf<SubMenuEntity>()
        val items = mutableListOf<ItemEntity>()
        val fields = mutableListOf<ItemFieldEntity>()

        draft.sections.forEachIndexed { sectionIndex, section ->
            val subMenuId = ids.newId()
            subMenus += SubMenuEntity(
                subMenuId = subMenuId,
                templateId = templateId,
                name = section.name.trim(),
                sortOrder = sectionIndex,
                scheduleJson = null,
            )
            section.items.forEachIndexed { itemIndex, item ->
                val itemId = ids.newId()
                items += ItemEntity(
                    itemId = itemId,
                    subMenuId = subMenuId,
                    name = item.name.trim(),
                    hasVariants = false,
                    variantSource = null,
                    sortOrder = itemIndex,
                )
                val usedKeys = mutableSetOf<String>()
                item.fields.forEachIndexed { fieldIndex, field ->
                    val key = fieldKey(field.label, usedKeys)
                    usedKeys += key
                    fields += ItemFieldEntity(
                        fieldId = ids.newId(),
                        itemId = itemId,
                        fieldKey = key,
                        type = field.type.fieldType.wire,
                        label = field.label.trim(),
                        unit = if (field.type.needsUnit) field.unit.trim().ifBlank { null } else null,
                        required = false,
                        sortOrder = fieldIndex,
                        optionsJson = optionsFor(field.type),
                    )
                }
            }
        }

        templateDao.insertFullTemplate(template, subMenus, items, fields)
        return CreateResult.Created(templateId)
    }

    /** First problem with the draft as a user-facing sentence, or null when it is valid. */
    private fun validate(draft: ActivityDraft): String? {
        if (draft.name.isBlank()) return "Give your activity a name."
        if (draft.sections.isEmpty()) return "Add at least one section."
        draft.sections.forEach { section ->
            if (section.name.isBlank()) return "Every section needs a name."
            if (section.items.isEmpty()) return "“${section.name.trim()}” needs at least one item."
            section.items.forEach { item ->
                if (item.name.isBlank()) return "Every item in “${section.name.trim()}” needs a name."
                if (item.fields.isEmpty()) return "“${item.name.trim()}” needs at least one field."
                if (item.fields.any { it.label.isBlank() }) {
                    return "Every field in “${item.name.trim()}” needs a label."
                }
            }
        }
        return null
    }

    private fun optionsFor(type: BuilderFieldType): String? = when (type) {
        BuilderFieldType.SCALE -> """{"min":1,"max":5}"""
        BuilderFieldType.QUANTITY -> """{"min":0,"step":1}"""
        else -> null
    }
}

/**
 * A stable field_key slugged from a label, unique within its item.
 *
 * log_values reference fields by this key, so it must be deterministic and collision-free;
 * two fields both labelled "Amount" become "amount" and "amount_2".
 */
internal fun fieldKey(label: String, used: Set<String>): String {
    val base = label.trim().lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .ifBlank { "field" }
    if (base !in used) return base
    var n = 2
    while ("${base}_$n" in used) n++
    return "${base}_$n"
}
