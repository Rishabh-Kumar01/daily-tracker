package dev.rishabh.dailytracker.feature.foods

import dev.rishabh.dailytracker.core.db.NutrientKeys
import dev.rishabh.dailytracker.core.db.dao.MediaDao
import dev.rishabh.dailytracker.core.db.dao.ProductDao
import dev.rishabh.dailytracker.core.db.entity.ProductEntity
import dev.rishabh.dailytracker.core.db.entity.ProductNutrientEntity
import dev.rishabh.dailytracker.core.nutrition.NutrientTotals
import dev.rishabh.dailytracker.core.nutrition.per100gLine
import dev.rishabh.dailytracker.feature.diet.ValidatedProduct
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The My Foods library: read the product catalogue, edit a product in place, archive one.
 *
 * Reads are a live join of the (query-filtered) product list and the whole nutrient table,
 * mapped to display cards — the per-100g line is formatted from current nutrients, never a
 * stored figure, so an edit here is reflected everywhere the product is used.
 */
@Singleton
class ProductLibraryRepository @Inject constructor(
    private val productDao: ProductDao,
    private val mediaDao: MediaDao,
) {

    /** Active products matching [query] (blank = all), each with its formatted macro line. */
    fun observeLibrary(query: String): Flow<List<ProductCard>> {
        val products = if (query.isBlank()) {
            productDao.observeAllProducts()
        } else {
            productDao.searchProducts(query.trim())
        }
        return combine(products, productDao.observeAllNutrients()) { rows, nutrients ->
            val byProduct = nutrients.groupBy { it.productId }
            // Front photos resolve in one bulk read; a photo attach re-emits the product
            // (front_photo_ref lives on it), so this stays fresh without observing media.
            val photoPaths = rows.mapNotNull { it.frontPhotoRef }
                .takeIf { it.isNotEmpty() }
                ?.let { mediaDao.getByIds(it) }
                .orEmpty()
                .associate { it.mediaId to it.filePath }
            rows.map { it.toCard(byProduct[it.productId].orEmpty(), it.frontPhotoRef?.let(photoPaths::get)) }
        }
    }

    /**
     * Saves an edit back onto the same product row and its nutrients.
     *
     * Upserts rather than inserts (stable ids), so a correction never duplicates a row; the
     * grouping key, source, barcode and timestamps are preserved. A macro blanked in the
     * form is deleted so the edit is honest, while micronutrients the form does not show
     * (iron, calcium…) are left untouched.
     */
    suspend fun updateProduct(productId: String, product: ValidatedProduct) {
        val existing = productDao.getProduct(productId) ?: return
        productDao.upsertProduct(
            existing.copy(brand = product.brand, productName = product.productName),
        )
        val nutrients = product.nutrients.map { (key, amount) ->
            ProductNutrientEntity(
                id = "$productId:$key",
                productId = productId,
                nutrientKey = key,
                amountPer100g = amount,
            )
        }
        productDao.upsertNutrients(nutrients)
        for (key in EDITABLE_MACROS) {
            if (!product.nutrients.containsKey(key)) productDao.deleteNutrient(productId, key)
        }
    }

    /** Soft-delete: hides the product from pickers and search, history stays resolvable. */
    suspend fun archive(productId: String) = productDao.setArchived(productId, archived = true)

    private fun ProductEntity.toCard(nutrients: List<ProductNutrientEntity>, photoPath: String?): ProductCard {
        val per100 = NutrientTotals(nutrients.associate { it.nutrientKey to it.amountPer100g })
        return ProductCard(
            productId = productId,
            brand = brand,
            name = productName,
            isGeneric = brand == null,
            per100gLine = per100gLine(per100),
            per100g = per100,
            photoPath = photoPath,
        )
    }

    private companion object {
        /** The four macros the manual form edits; energy is always required by the validator. */
        val EDITABLE_MACROS = listOf(
            NutrientKeys.ENERGY_KCAL, NutrientKeys.PROTEIN_G,
            NutrientKeys.CARBS_G, NutrientKeys.FAT_G,
        )
    }
}
