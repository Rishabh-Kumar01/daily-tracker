package dev.rishabh.dailytracker.core.db.seed

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
 * Installs the built-in activities as template rows.
 *
 * Idempotent per template rather than "seed once ever": it checks each built-in by name,
 * so a built-in added in a later release installs on upgrade without re-installing (or
 * duplicating) the ones already present. A "have I seeded?" flag could not do that.
 *
 * A template the user archived stays archived — [seedIfNeeded] only ever adds what is
 * absent, and never resurrects or overwrites.
 */
@Singleton
class TemplateSeeder @Inject constructor(
    private val templateDao: TemplateDao,
    private val ids: IdGenerator,
    private val time: TimeSource,
) {

    /** @return the names of templates actually installed by this call. */
    suspend fun seedIfNeeded(): List<String> {
        val installed = mutableListOf<String>()
        BUILT_IN_TEMPLATES.forEachIndexed { index, spec ->
            if (templateDao.findTemplateByName(spec.name, CreatedBy.SYSTEM) != null) return@forEachIndexed
            install(spec, sortOrder = index)
            installed += spec.name
        }
        return installed
    }

    private suspend fun install(spec: TemplateSpec, sortOrder: Int) {
        val now = time.nowMillis()
        val templateId = ids.newId()

        val template = ActivityTemplateEntity(
            templateId = templateId,
            name = spec.name,
            icon = spec.icon,
            color = spec.color,
            createdBy = CreatedBy.SYSTEM,
            summaryMetricType = spec.summaryMetricType,
            summaryMetricLabel = spec.summaryMetricLabel,
            schemaVersion = SCHEMA_VERSION,
            isArchived = false,
            sortOrder = sortOrder,
            createdAt = now,
        )

        val subMenus = mutableListOf<SubMenuEntity>()
        val items = mutableListOf<ItemEntity>()
        val fields = mutableListOf<ItemFieldEntity>()

        spec.subMenus.forEachIndexed { subIndex, subSpec ->
            val subMenuId = ids.newId()
            subMenus += SubMenuEntity(
                subMenuId = subMenuId,
                templateId = templateId,
                name = subSpec.name,
                sortOrder = subIndex,
                scheduleJson = subSpec.scheduleJson,
            )

            subSpec.items.forEachIndexed { itemIndex, itemSpec ->
                val itemId = ids.newId()
                items += ItemEntity(
                    itemId = itemId,
                    subMenuId = subMenuId,
                    name = itemSpec.name,
                    hasVariants = itemSpec.hasVariants,
                    variantSource = itemSpec.variantSource,
                    sortOrder = itemIndex,
                )

                itemSpec.fields.forEachIndexed { fieldIndex, fieldSpec ->
                    fields += ItemFieldEntity(
                        fieldId = ids.newId(),
                        itemId = itemId,
                        fieldKey = fieldSpec.fieldKey,
                        // Persist the wire value, not the Kotlin name.
                        type = fieldSpec.type.wire,
                        label = fieldSpec.label,
                        unit = fieldSpec.unit,
                        required = fieldSpec.required,
                        sortOrder = fieldIndex,
                        optionsJson = fieldSpec.optionsJson,
                    )
                }
            }
        }

        templateDao.insertFullTemplate(template, subMenus, items, fields)
    }

    private companion object {
        /** Template format version; bump only when the template JSON shape changes. */
        const val SCHEMA_VERSION = 1
    }
}
