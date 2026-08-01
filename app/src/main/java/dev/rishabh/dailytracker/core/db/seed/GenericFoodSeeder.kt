package dev.rishabh.dailytracker.core.db.seed

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.rishabh.dailytracker.core.common.IdGenerator
import dev.rishabh.dailytracker.core.common.TimeSource
import dev.rishabh.dailytracker.core.db.ProductSource
import dev.rishabh.dailytracker.core.db.dao.GenericFoodMetaDao
import dev.rishabh.dailytracker.core.db.dao.ProductDao
import dev.rishabh.dailytracker.core.db.entity.GenericFoodMetaEntity
import dev.rishabh.dailytracker.core.db.entity.ProductEntity
import dev.rishabh.dailytracker.core.db.entity.ProductNutrientEntity
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GenericFoodSeeder"
private const val ASSET_PATH = "generic_foods/generic_foods.v1.json"

/**
 * Seeds the bundled generic food dataset into Room.
 *
 * Idempotent per-slug: on every launch it checks whether each food's slug already exists in
 * [GenericFoodMetaDao]. Missing slugs are inserted; existing ones are left exactly as they are,
 * so a user edit (M9) or a logged day referencing a seeded product is never clobbered or
 * orphaned. A bumped [GenericFoodAsset.datasetVersion] therefore only ever *adds* new foods.
 * Seeding is resumable: each food is written independently, so a launch interrupted mid-seed
 * simply completes on the next one.
 *
 * Follows the same pattern as [TemplateSeeder]: deterministic IDs from [IdGenerator], current
 * time from [TimeSource], transactional writes through the DAOs.
 */
@Singleton
class GenericFoodSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val productDao: ProductDao,
    private val metaDao: GenericFoodMetaDao,
    private val ids: IdGenerator,
    private val time: TimeSource,
) {

    /**
     * Seed bundled generic foods if needed.
     *
     * Insert-missing-by-slug, always — never wipe-and-reinsert. A seeded product may have
     * been edited by the user (M9) and is referenced by historical log entries, so an
     * existing slug is left exactly as it is: correcting it would clobber the user's edit
     * and hard-deleting it would orphan a logged day. A future dataset version therefore
     * *adds* new foods; it never rewrites or removes an existing one. The per-row
     * dataset_version is still recorded so a later, edit-aware upgrade can target only
     * untouched rows.
     *
     * @return the number of foods actually inserted by this call.
     */
    suspend fun seedIfNeeded(): Int {
        val asset = loadAsset() ?: return 0

        var inserted = 0
        for (food in asset.foods) {
            if (!metaDao.hasSlug(food.slug)) {
                try {
                    insertFood(food, asset.datasetVersion)
                    inserted++
                } catch (e: Exception) {
                    // One malformed row must not abort the whole bundle — log it and carry on.
                    // The failed slug stays unseeded, so a later launch retries it.
                    Log.e(TAG, "Failed to seed '${food.slug}'", e)
                }
            }
        }
        if (inserted > 0) {
            Log.i(TAG, "Seeded $inserted new generic foods (dataset v${asset.datasetVersion})")
        }

        // One-time metadata backfill for rows seeded before serving units existed (the
        // Migration 2→3 columns start null). Metadata only — never touches nutrients — and
        // guarded so it is a no-op once every row has a unit.
        if (metaDao.countMissingServing() > 0) {
            for (food in asset.foods) {
                metaDao.backfillServing(food.slug, food.servingUnit, food.unitLabel, food.gramsPerUnit)
            }
            Log.i(TAG, "Backfilled serving units for existing generic foods")
        }
        return inserted
    }

    private suspend fun insertFood(food: FoodEntry, datasetVersion: Int) {
        val now = time.nowMillis()
        val productId = ids.newId()

        val product = ProductEntity(
            productId = productId,
            genericName = food.genericName,
            brand = null,
            productName = food.displayName,
            variant = food.prep,
            source = ProductSource.BUNDLED_GENERIC,
            servingSizeG = food.defaultServingG.toDouble(),
            createdAt = now,
        )

        val nutrients = food.per100g.map { (key, amount) ->
            ProductNutrientEntity(
                id = "$productId:$key",
                productId = productId,
                nutrientKey = key,
                amountPer100g = amount,
            )
        }

        productDao.insertProductWithNutrients(product, nutrients)

        val meta = GenericFoodMetaEntity(
            productId = productId,
            slug = food.slug,
            category = food.category,
            prep = food.prep,
            sourceForm = food.sourceForm,
            sourceDb = food.sourceDb,
            sourceRef = food.sourceRef,
            isApprox = food.isApprox,
            servingUnit = food.servingUnit,
            unitLabel = food.unitLabel,
            gramsPerUnit = food.gramsPerUnit,
            datasetVersion = datasetVersion,
        )
        metaDao.insert(meta)
    }

    private fun loadAsset(): GenericFoodAsset? {
        return try {
            context.assets.open(ASSET_PATH).use { stream ->
                val text = stream.bufferedReader().readText()
                genericFoodJson.decodeFromString<GenericFoodAsset>(text)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load generic foods asset", e)
            null
        }
    }
}
