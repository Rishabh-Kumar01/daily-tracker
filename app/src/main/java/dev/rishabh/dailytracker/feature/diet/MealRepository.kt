package dev.rishabh.dailytracker.feature.diet

import dev.rishabh.dailytracker.core.common.IdGenerator
import dev.rishabh.dailytracker.core.common.TimeSource
import dev.rishabh.dailytracker.core.db.FieldType
import dev.rishabh.dailytracker.core.db.NutrientKeys
import dev.rishabh.dailytracker.core.db.ProductSource
import dev.rishabh.dailytracker.core.db.dao.GenericFoodMetaDao
import dev.rishabh.dailytracker.core.db.dao.LogDao
import dev.rishabh.dailytracker.core.db.dao.MediaDao
import dev.rishabh.dailytracker.core.db.dao.ProductDao
import dev.rishabh.dailytracker.core.db.dao.TemplateDao
import dev.rishabh.dailytracker.core.db.entity.LogEntryEntity
import dev.rishabh.dailytracker.core.db.entity.LogValueEntity
import dev.rishabh.dailytracker.core.db.entity.ProductEntity
import dev.rishabh.dailytracker.core.db.entity.ProductNutrientEntity
import dev.rishabh.dailytracker.core.designsystem.accentKeyForColor
import dev.rishabh.dailytracker.core.designsystem.component.Per100g
import dev.rishabh.dailytracker.core.nutrition.MacroCalculator
import dev.rishabh.dailytracker.core.nutrition.NutrientTotals
import dev.rishabh.dailytracker.core.nutrition.per100gLine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read + write model for one meal.
 *
 * Reads are a live join of five Flows: the meal's items, the product library, the nutrient
 * table, the generic food metadata, and what has been logged today. Nothing derived is stored —
 * every emission recomputes macros from current product_nutrients, so editing a product's
 * label fixes every meal that ever referenced it, past included.
 *
 * Writes are per-portion and immediate: each add or edit is its own transaction, so a meal
 * half-entered when the OS kills the app is still a meal half-saved.
 */
@Singleton
class MealRepository @Inject constructor(
    private val templateDao: TemplateDao,
    private val logDao: LogDao,
    private val productDao: ProductDao,
    private val genericFoodMetaDao: GenericFoodMetaDao,
    private val mediaDao: MediaDao,
    private val ids: IdGenerator,
    private val time: TimeSource,
) {

    fun observeMeal(subMenuId: String): Flow<MealDetail?> {
        val today = time.today()
        return combine(
            templateDao.observeItems(subMenuId),
            productDao.observeAllProducts(),
            productDao.observeAllNutrients(),
            genericFoodMetaDao.observeAll(),
            logDao.observeProductQuantitiesForSubMenuDay(subMenuId, today),
        ) { items, products, nutrients, metaList, logged ->
            val subMenu = templateDao.getSubMenu(subMenuId) ?: return@combine null
            val template = templateDao.getTemplate(subMenu.templateId)
            val fields = templateDao.getFieldsForSubMenu(subMenuId).groupBy { it.itemId }
            val nutrientsByProduct = nutrients.groupBy { it.productId }
            val productsByGeneric = products.groupBy { it.genericName }
            // Front photos resolve in one bulk read; a photo attach re-emits the product
            // (front_photo_ref lives on it), so this stays fresh without observing media.
            val photoPaths = products.mapNotNull { it.frontPhotoRef }
                .takeIf { it.isNotEmpty() }
                ?.let { mediaDao.getByIds(it) }
                .orEmpty()
                .associate { it.mediaId to it.filePath }

            // Index generic_food_meta by product_id for O(1) lookup.
            val metaByProduct = metaList.associateBy { it.productId }
            // Index category -> list of product_ids for category-based matching.
            val productIdsByCategory = metaList
                .filter { it.category != null }
                .groupBy { it.category!! }
                .mapValues { (_, metas) -> metas.map { it.productId }.toSet() }

            val loggedByItem = logged.associateBy { it.itemId }

            val mealItems = items.map { item ->
                val itemFields = fields[item.itemId].orEmpty()
                val generic = genericNameOf(item.name)
                val portion = loggedByItem[item.itemId]?.let { q ->
                    val productId = q.productId ?: return@let null
                    val grams = q.grams ?: return@let null
                    LoggedPortion(
                        entryId = q.entryId,
                        productId = productId,
                        grams = grams,
                        totals = MacroCalculator.forQuantity(
                            nutrientsByProduct[productId].orEmpty(),
                            grams,
                        ),
                    )
                }

                // Gather products matching this item:
                // 1. Exact generic_name match (includes both branded and generic)
                // 2. Category match (only generic foods with a category)
                // Generic-first ordering: exact name matches first, then category matches.
                val exactProducts = productsByGeneric[generic].orEmpty()
                val categoryProductIds = productIdsByCategory[generic].orEmpty()
                val allProducts = if (categoryProductIds.isNotEmpty()) {
                    val categoryProducts = products.filter { it.productId in categoryProductIds }
                    exactProducts + (categoryProducts - exactProducts.toSet())
                } else {
                    exactProducts
                }

                MealItem(
                    itemId = item.itemId,
                    name = item.name,
                    genericName = generic,
                    quantityFieldKey = itemFields
                        .firstOrNull { it.type == FieldType.QUANTITY.wire }?.fieldKey,
                    variantFieldKey = itemFields
                        .firstOrNull { it.type == FieldType.ITEM_VARIANT.wire }?.fieldKey,
                    brands = allProducts.map { product ->
                        brandOption(
                            product,
                            nutrientsByProduct[product.productId].orEmpty(),
                            metaByProduct[product.productId],
                            product.frontPhotoRef?.let(photoPaths::get),
                        )
                    },
                    logged = portion,
                )
            }

            MealDetail(
                subMenuId = subMenu.subMenuId,
                templateId = subMenu.templateId,
                name = subMenu.name,
                accent = accentKeyForColor(template?.color),
                items = mealItems,
                totals = MacroCalculator.total(
                    logged.mapNotNull { q ->
                        q.productId?.let { MacroCalculator.Quantity(it, q.grams) }
                    },
                    nutrientsByProduct,
                ),
            )
        }
    }

    /**
     * Writes a portion, replacing whatever this item already has logged for the meal today.
     *
     * Entry and values commit together — an entry without its grams would render as a
     * portion with no macros, which is worse than no entry at all. [LogDao.replaceLog] is
     * the same transaction for the edit case, so re-logging never leaves two portions
     * behind.
     */
    suspend fun logPortion(
        templateId: String,
        subMenuId: String,
        item: MealItem,
        productId: String,
        grams: Double,
    ) {
        val now = time.nowMillis()
        val entry = LogEntryEntity(
            entryId = item.logged?.entryId ?: ids.newId(),
            templateId = templateId,
            subMenuId = subMenuId,
            itemId = item.itemId,
            loggedAt = now,
            localDate = time.localDateOf(now),
            variantRef = productId,
        )
        val values = buildList {
            item.quantityFieldKey?.let { key ->
                add(
                    LogValueEntity(
                        valueId = ids.newId(),
                        entryId = entry.entryId,
                        fieldKey = key,
                        valueNumber = grams,
                    ),
                )
            }
            // The chosen product is recorded on the entry as variant_ref; mirroring it into
            // the item_variant field keeps the entry self-describing for the generic
            // renderer and for export.
            item.variantFieldKey?.let { key ->
                add(
                    LogValueEntity(
                        valueId = ids.newId(),
                        entryId = entry.entryId,
                        fieldKey = key,
                        valueText = productId,
                    ),
                )
            }
        }

        if (item.logged == null) {
            logDao.insertLog(entry, values)
        } else {
            logDao.replaceLog(entry, values)
        }
        // Frequency ranking: the brand actually eaten floats to the top of the expansion.
        productDao.touch(productId, now)
    }

    /** Cascades to log_values via the foreign key. */
    suspend fun removePortion(entryId: String) = logDao.deleteEntry(entryId)

    /**
     * The grouping key a product scanned or typed under this item should get.
     *
     * The scanner only knows an item id; the food's name is what ties the resulting product
     * to the rest of its brand variants.
     */
    suspend fun genericNameForItem(itemId: String): String? =
        templateDao.getItem(itemId)?.let { genericNameOf(it.name) }

    /**
     * Tier-3 manual product creation.
     *
     * Deduped on normalised brand + name so typing "amul / malai paneer" twice updates the
     * one product rather than splitting a food's history across near-identical rows. IDs are
     * app-generated, never supplied by the caller.
     */
    suspend fun createManualProduct(
        genericName: String,
        product: ValidatedProduct,
        barcode: String? = null,
        source: ProductSource = ProductSource.MANUAL,
    ): String {
        // Barcode identifies a packet exactly, so it wins over the fuzzy name match: the
        // same product scanned twice must never become two rows, whatever it got called.
        val existing = barcode?.let { productDao.findByBarcode(it) }
            ?: productDao.findDuplicate(product.brand, product.productName)
        val productId = existing?.productId ?: ids.newId()
        val now = time.nowMillis()
        val row = ProductEntity(
            productId = productId,
            genericName = genericName,
            brand = product.brand,
            productName = product.productName,
            // Never drop a barcode already on the row: a product first entered by hand and
            // later scanned gains one, and re-saving from the sheet must not erase it.
            barcode = barcode ?: existing?.barcode,
            // Photos carry over the same way — a confirm-sheet re-save must not strip them.
            frontPhotoRef = existing?.frontPhotoRef,
            backPhotoRef = existing?.backPhotoRef,
            source = source,
            createdAt = existing?.createdAt ?: now,
            lastUsedAt = existing?.lastUsedAt,
        )
        val nutrients = product.nutrients.map { (key, amount) ->
            ProductNutrientEntity(
                // Stable per (product, nutrient) so re-saving corrects the row in place
                // rather than accumulating duplicates the calculator would sum.
                id = "$productId:$key",
                productId = productId,
                nutrientKey = key,
                amountPer100g = amount,
            )
        }
        if (existing == null) {
            productDao.insertProductWithNutrients(row, nutrients)
        } else {
            productDao.upsertProduct(row)
            productDao.upsertNutrients(nutrients)
        }
        return productId
    }

    /**
     * Scan pre-check: the product a barcode already belongs to.
     *
     * The scan flow asks this before touching the network so a re-scan gets an "already in
     * your foods" sheet instead of silently upserting the same row.
     */
    suspend fun productForBarcode(barcode: String): ExistingProduct? {
        val product = productDao.findByBarcode(barcode) ?: return null
        val per100 = NutrientTotals(
            productDao.getNutrients(product.productId)
                .associate { it.nutrientKey to it.amountPer100g },
        )
        return ExistingProduct(
            productId = product.productId,
            brand = product.brand,
            productName = product.productName,
            source = product.source,
            per100g = Per100g(
                kcal = per100[NutrientKeys.ENERGY_KCAL],
                protein = per100[NutrientKeys.PROTEIN_G],
                carbs = per100[NutrientKeys.CARBS_G],
                fat = per100[NutrientKeys.FAT_G],
            ),
            per100gLine = per100gLine(per100),
        )
    }
}

/**
 * An item's name is the grouping key for its brand variants.
 *
 * Case- and space-normalised so the item "Paneer" and a product saved under "paneer" meet.
 */
internal fun genericNameOf(itemName: String): String = itemName.trim().lowercase()

private fun brandOption(
    product: ProductEntity,
    nutrients: List<ProductNutrientEntity>,
    meta: dev.rishabh.dailytracker.core.db.entity.GenericFoodMetaEntity?,
    photoPath: String?,
): BrandOption {
    val isGeneric = meta != null
    val per100 = NutrientTotals(nutrients.associate { it.nutrientKey to it.amountPer100g })
    return BrandOption(
        productId = product.productId,
        brand = product.brand,
        productName = product.productName,
        per100g = Per100g(
            kcal = per100[NutrientKeys.ENERGY_KCAL],
            protein = per100[NutrientKeys.PROTEIN_G],
            carbs = per100[NutrientKeys.CARBS_G],
            fat = per100[NutrientKeys.FAT_G],
        ),
        per100gLine = per100gLine(per100),
        variant = product.variant,
        isGeneric = isGeneric,
        isApprox = meta?.isApprox == true,
        defaultServingG = product.servingSizeG,
        // Serving unit comes from the generic-food metadata; branded products (no meta)
        // stay on grams, matching "default to grams unless a serving is known".
        servingUnit = meta?.servingUnit,
        unitLabel = meta?.unitLabel,
        gramsPerUnit = meta?.gramsPerUnit,
        photoPath = photoPath,
    )
}
