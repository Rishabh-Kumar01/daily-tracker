package dev.rishabh.dailytracker.core.db.seed

import dev.rishabh.dailytracker.core.common.IdGenerator
import dev.rishabh.dailytracker.core.common.TimeSource
import dev.rishabh.dailytracker.core.db.CreatedBy
import dev.rishabh.dailytracker.core.db.dao.LogDao
import dev.rishabh.dailytracker.core.db.dao.TemplateDao
import dev.rishabh.dailytracker.core.db.entity.ActivityTemplateEntity
import dev.rishabh.dailytracker.core.db.entity.ItemEntity
import dev.rishabh.dailytracker.core.db.entity.ItemFieldEntity
import dev.rishabh.dailytracker.core.db.entity.SubMenuEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Installs and updates the built-in activities as template rows.
 *
 * Idempotent per template rather than "seed once ever": it checks each built-in by name, so a
 * built-in added in a later release installs on upgrade without duplicating the ones already
 * present. When a built-in's structure changes, its [TemplateSpec.schemaVersion] is bumped and
 * the stored template is rebuilt in place — same template_id, refreshed sub-menus/items/fields.
 *
 * A template the user archived stays archived: an install only adds what is absent, and a
 * rebuild preserves the archive flag.
 */
@Singleton
class TemplateSeeder @Inject constructor(
    private val templateDao: TemplateDao,
    private val logDao: LogDao,
    private val ids: IdGenerator,
    private val time: TimeSource,
) {

    /** @return the names of templates installed or rebuilt by this call. */
    suspend fun seedIfNeeded(): List<String> {
        val touched = mutableListOf<String>()
        BUILT_IN_TEMPLATES.forEachIndexed { index, spec ->
            val existing = templateDao.findTemplateByName(spec.name, CreatedBy.SYSTEM)
            when {
                existing == null -> {
                    install(spec, sortOrder = index)
                    touched += spec.name
                }
                existing.schemaVersion < spec.schemaVersion -> {
                    rebuild(existing, spec)
                    touched += spec.name
                }
            }
        }
        return touched
    }

    private suspend fun install(spec: TemplateSpec, sortOrder: Int) {
        val built = build(spec, ids.newId(), sortOrder, isArchived = false, createdAt = time.nowMillis())
        templateDao.insertFullTemplate(built.template, built.subMenus, built.items, built.fields)
    }

    /**
     * Replaces a built-in's structure to match the current spec.
     *
     * The activity's logs are cleared first: they reference the items about to be swapped out,
     * and no schema keeps orphaned items around. This is why a built-in restructure is a
     * deliberate, version-gated event, not something an ordinary release does casually.
     */
    private suspend fun rebuild(existing: ActivityTemplateEntity, spec: TemplateSpec) {
        logDao.deleteEntriesForTemplate(existing.templateId)
        val built = build(
            spec = spec,
            templateId = existing.templateId,
            sortOrder = existing.sortOrder,
            isArchived = existing.isArchived,
            createdAt = existing.createdAt,
        )
        templateDao.rebuildStructure(built.template, built.subMenus, built.items, built.fields)
    }

    private fun build(
        spec: TemplateSpec,
        templateId: String,
        sortOrder: Int,
        isArchived: Boolean,
        createdAt: Long,
    ): Built {
        val template = ActivityTemplateEntity(
            templateId = templateId,
            name = spec.name,
            icon = spec.icon,
            color = spec.color,
            createdBy = CreatedBy.SYSTEM,
            summaryMetricType = spec.summaryMetricType,
            summaryMetricLabel = spec.summaryMetricLabel,
            schemaVersion = spec.schemaVersion,
            isArchived = isArchived,
            sortOrder = sortOrder,
            createdAt = createdAt,
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

        return Built(template, subMenus, items, fields)
    }

    private class Built(
        val template: ActivityTemplateEntity,
        val subMenus: List<SubMenuEntity>,
        val items: List<ItemEntity>,
        val fields: List<ItemFieldEntity>,
    )
}
