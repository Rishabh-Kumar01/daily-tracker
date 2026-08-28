package dev.rishabh.dailytracker.feature.activities

import dev.rishabh.dailytracker.core.common.TimeSource
import dev.rishabh.dailytracker.core.db.SummaryMetricTypes
import dev.rishabh.dailytracker.core.db.dao.LogDao
import dev.rishabh.dailytracker.core.db.dao.ProductDao
import dev.rishabh.dailytracker.core.db.dao.TemplateDao
import dev.rishabh.dailytracker.core.db.entity.ActivityTemplateEntity
import dev.rishabh.dailytracker.core.designsystem.accentKeyForColor
import dev.rishabh.dailytracker.core.nutrition.MacroCalculator
import dev.rishabh.dailytracker.core.nutrition.NutrientTotals
import dev.rishabh.dailytracker.core.nutrition.homeMacroSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read model for the activity browser: Home, then template → sub-menus → items.
 *
 * Everything is a Flow off Room, so the screens update live — the startup seed appears on
 * Home without a refresh, and a future logged entry re-emits the summary.
 */
@Singleton
class ActivityRepository @Inject constructor(
    private val templateDao: TemplateDao,
    private val logDao: LogDao,
    private val productDao: ProductDao,
    private val time: TimeSource,
) {

    /** Home: every active activity with its today summary. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeHome(): Flow<List<HomeActivity>> =
        templateDao.observeActiveTemplates().flatMapLatest { templates ->
            if (templates.isEmpty()) {
                flowOf(emptyList())
            } else {
                // One summary flow per activity, recombined into the list. combine over an
                // empty list never emits, which is why the empty case is handled above.
                combine(templates.map { template -> homeActivityFlow(template) }) { it.toList() }
            }
        }

    private fun homeActivityFlow(template: ActivityTemplateEntity): Flow<HomeActivity> {
        val today = time.today()
        val summary = if (template.tracksCalories) {
            nutritionSummaryFlow(template.templateId, today)
        } else {
            logDao.observeEntryCountForDay(template.templateId, today).map(::summaryFor)
        }
        return summary.map { text ->
            HomeActivity(
                templateId = template.templateId,
                name = template.name,
                iconKey = template.icon,
                accent = accentKeyForColor(template.color),
                summary = text,
            )
        }
    }

    /**
     * Whether this activity's summary is a nutrition total rather than an entry count.
     *
     * Read from the template's own summary metric, so an activity the user creates to track
     * calories gets the same treatment — no name matching on "Diet".
     */
    private val ActivityTemplateEntity.tracksCalories: Boolean
        get() = summaryMetricType == SummaryMetricTypes.SUM_FIELD &&
            summaryMetricLabel.equals("kcal", ignoreCase = true)

    /**
     * Day macros for a calorie-tracking activity.
     *
     * Recomputed from current product_nutrients on every emission — Home is a read of the
     * log, never a cache of one.
     */
    private fun nutritionSummaryFlow(templateId: String, today: String): Flow<String> =
        combine(
            logDao.observeProductQuantitiesForDay(templateId, today),
            productDao.observeAllNutrients(),
        ) { quantities, nutrients ->
            if (quantities.isEmpty()) return@combine summaryFor(0)
            val totals = MacroCalculator.total(
                quantities.mapNotNull { q -> q.productId?.let { MacroCalculator.Quantity(it, q.grams) } },
                nutrients.groupBy { it.productId },
            )
            homeMacroSummary(totals)
        }

    /**
     * Today summary from the log.
     *
     * Generic and count-based so it works for every activity, built-in or user-created.
     * Calorie-tracking activities override it with real macros above.
     */
    private fun summaryFor(entryCount: Int): String = when (entryCount) {
        0 -> "Nothing logged yet"
        1 -> "1 entry today"
        else -> "$entryCount entries today"
    }

    /** One activity's sub-menus, each with how many items it holds. */
    fun observeActivityDetail(templateId: String): Flow<ActivityDetail?> =
        templateDao.observeSubMenus(templateId).map { subMenus ->
            val template = templateDao.getTemplate(templateId) ?: return@map null
            ActivityDetail(
                templateId = template.templateId,
                name = template.name,
                accent = accentKeyForColor(template.color),
                subMenus = subMenus.map { sub ->
                    SubMenuRow(
                        subMenuId = sub.subMenuId,
                        name = sub.name,
                        itemCount = templateDao.getItems(sub.subMenuId).size,
                    )
                },
                tracksCalories = template.tracksCalories,
            )
        }

    /**
     * The day's macro totals for a calorie-tracking activity, recomputed at read time.
     *
     * Powers the day-detail header. Like the Home summary it derives from current
     * product_nutrients on every emission, so a corrected label moves the day total too —
     * nothing here is cached or stored.
     */
    fun observeDayTotals(templateId: String): Flow<NutrientTotals> {
        val today = time.today()
        return combine(
            logDao.observeProductQuantitiesForDay(templateId, today),
            productDao.observeAllNutrients(),
        ) { quantities, nutrients ->
            MacroCalculator.total(
                quantities.mapNotNull { q -> q.productId?.let { MacroCalculator.Quantity(it, q.grams) } },
                nutrients.groupBy { it.productId },
            )
        }
    }

    /** One sub-menu's items, each with the labels of the fields it would log. */
    fun observeSubMenuDetail(subMenuId: String): Flow<SubMenuDetail?> =
        templateDao.observeItems(subMenuId).map { items ->
            val subMenu = templateDao.getSubMenu(subMenuId) ?: return@map null
            val template = templateDao.getTemplate(subMenu.templateId)
            SubMenuDetail(
                subMenuId = subMenu.subMenuId,
                name = subMenu.name,
                accent = accentKeyForColor(template?.color),
                items = items.map { item ->
                    ItemRowDetail(
                        itemId = item.itemId,
                        name = item.name,
                        hasVariants = item.hasVariants,
                        fieldLabels = templateDao.getFields(item.itemId).map { it.label },
                    )
                },
            )
        }
}
