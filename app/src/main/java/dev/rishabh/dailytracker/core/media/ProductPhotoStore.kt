package dev.rishabh.dailytracker.core.media

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.rishabh.dailytracker.core.common.IdGenerator
import dev.rishabh.dailytracker.core.common.TimeSource
import dev.rishabh.dailytracker.core.db.MediaType
import dev.rishabh.dailytracker.core.db.dao.MediaDao
import dev.rishabh.dailytracker.core.db.dao.ProductDao
import dev.rishabh.dailytracker.core.db.entity.MediaEntity
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Request

/**
 * Owns product photos on disk and their [MediaEntity] rows.
 *
 * Photos live in `filesDir/product_photos/<mediaId>.jpg` — app-private storage, no
 * FileProvider, nothing in the gallery. The product's `front_photo_ref` points at the media
 * row and the media row points at the file. Both attach paths are best-effort: a failure
 * leaves the product saved, just without a photo.
 */
@Singleton
class ProductPhotoStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callFactory: Call.Factory,
    private val mediaDao: MediaDao,
    private val productDao: ProductDao,
    private val ids: IdGenerator,
    private val time: TimeSource,
) {

    /**
     * Downloads [url] and sets it as the product's front photo; true when one was attached.
     *
     * Skipped (false, no request) when the product already has a front photo — a scan
     * re-save must not clobber a photo the user captured themselves.
     */
    suspend fun attachFromUrl(productId: String, url: String): Boolean = withContext(Dispatchers.IO) {
        if (productDao.getProduct(productId)?.frontPhotoRef != null) return@withContext false
        val mediaId = ids.newId()
        val file = fileFor(mediaId)
        val ok = runCatching {
            callFactory.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) return@use false
                response.body.byteStream().use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                true
            }
        }.getOrDefault(false)
        if (!ok) {
            file.delete()
            return@withContext false
        }
        mediaDao.insert(mediaEntity(mediaId, file))
        productDao.setFrontPhoto(productId, mediaId)
        true
    }

    /**
     * Adopts a camera temp file as the product's front photo, returning the stored path.
     *
     * Unlike [attachFromUrl] this is an explicit user action, so it replaces any existing
     * photo. The old file and media row are deleted — a media row for a product front is
     * referenced only by that product, so replacing orphans it.
     */
    suspend fun attachCapture(productId: String, capture: File): String? = withContext(Dispatchers.IO) {
        val previousRef = productDao.getProduct(productId)?.frontPhotoRef
        val mediaId = ids.newId()
        val file = fileFor(mediaId)
        val ok = runCatching {
            capture.renameTo(file) || capture.copyTo(file, overwrite = true).run { capture.delete(); true }
        }.getOrDefault(false)
        if (!ok) return@withContext null
        mediaDao.insert(mediaEntity(mediaId, file))
        productDao.setFrontPhoto(productId, mediaId)
        if (previousRef != null) {
            mediaDao.getById(previousRef)?.let { File(it.filePath).delete() }
            mediaDao.delete(previousRef)
        }
        file.absolutePath
    }

    /** Drops a capture that never got attached (sheet dismissed, or replaced by a retake). */
    suspend fun discard(capturePath: String) {
        withContext(Dispatchers.IO) { File(capturePath).delete() }
    }

    private fun mediaEntity(mediaId: String, file: File) = MediaEntity(
        mediaId = mediaId,
        filePath = file.absolutePath,
        type = MediaType.PRODUCT_FRONT,
        createdAt = time.nowMillis(),
        // Product fronts are pack shots, not personal data — but the app already excludes
        // everything from backup, so the flag is descriptive here.
        sensitive = false,
    )

    private fun fileFor(mediaId: String): File =
        File(File(context.filesDir, "product_photos").apply { mkdirs() }, "$mediaId.jpg")
}
