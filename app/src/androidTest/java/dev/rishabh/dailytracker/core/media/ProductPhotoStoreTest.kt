package dev.rishabh.dailytracker.core.media

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.rishabh.dailytracker.core.db.DailyTrackerDatabase
import dev.rishabh.dailytracker.core.db.FakeIdGenerator
import dev.rishabh.dailytracker.core.db.FakeTimeSource
import dev.rishabh.dailytracker.core.db.MediaType
import dev.rishabh.dailytracker.core.db.ProductSource
import dev.rishabh.dailytracker.core.db.entity.ProductEntity
import java.io.File
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The photo store against a real database and real file storage: captures move into
 * app-private storage with a media row, retakes replace cleanly, downloads are best-effort,
 * and a product that already has a photo is never clobbered by a scan re-save.
 */
@RunWith(AndroidJUnit4::class)
class ProductPhotoStoreTest {

    private lateinit var context: Context
    private lateinit var db: DailyTrackerDatabase
    private lateinit var server: MockWebServer
    private lateinit var store: ProductPhotoStore

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, DailyTrackerDatabase::class.java).build()
        server = MockWebServer()
        server.start()
        store = ProductPhotoStore(
            context = context,
            callFactory = OkHttpClient(),
            mediaDao = db.mediaDao(),
            productDao = db.productDao(),
            ids = FakeIdGenerator(prefix = "media"),
            time = FakeTimeSource(),
        )
        db.productDao().insertProduct(
            ProductEntity(
                productId = "p1", genericName = "butter", brand = "Amul",
                productName = "Butter", source = ProductSource.MANUAL, createdAt = 1L,
            ),
        )
    }

    @After
    fun tearDown() {
        if (::server.isInitialized) server.close()
        if (::db.isInitialized) db.close()
        if (::context.isInitialized) File(context.filesDir, "product_photos").deleteRecursively()
    }

    private fun tempCapture(contents: String): File {
        val file = File.createTempFile("capture_", ".jpg", context.cacheDir)
        file.writeText(contents)
        return file
    }

    private suspend fun frontPhotoRef(): String = checkNotNull(db.productDao().getProduct("p1")).let {
        checkNotNull(it.frontPhotoRef)
    }

    @Test
    fun a_capture_becomes_the_products_front_photo() = runTest {
        val capture = tempCapture("photo-bytes")

        val path = store.attachCapture("p1", capture)

        val media = checkNotNull(db.mediaDao().getById(frontPhotoRef()))
        assertThat(media.type).isEqualTo(MediaType.PRODUCT_FRONT)
        assertThat(media.filePath).isEqualTo(path)
        assertThat(media.sensitive).isFalse()
        assertThat(File(checkNotNull(path)).readText()).isEqualTo("photo-bytes")
        // The temp file is adopted, not copied.
        assertThat(capture.exists()).isFalse()
    }

    @Test
    fun a_retake_replaces_the_photo_and_deletes_the_old_one() = runTest {
        val firstPath = checkNotNull(store.attachCapture("p1", tempCapture("old")))
        val firstRef = frontPhotoRef()

        val secondPath = checkNotNull(store.attachCapture("p1", tempCapture("new")))

        assertThat(File(secondPath).readText()).isEqualTo("new")
        assertThat(File(firstPath).exists()).isFalse()
        assertThat(db.mediaDao().getById(firstRef)).isNull()
        assertThat(frontPhotoRef()).isNotEqualTo(firstRef)
    }

    @Test
    fun a_downloaded_image_attaches_and_is_not_fetched_twice() = runTest {
        server.enqueue(MockResponse(code = 200, body = "image-bytes"))
        val url = server.url("/front.jpg").toString()

        assertThat(store.attachFromUrl("p1", url)).isTrue()

        val ref = frontPhotoRef()
        val media = checkNotNull(db.mediaDao().getById(ref))
        assertThat(File(media.filePath).readText()).isEqualTo("image-bytes")

        // A re-save (e.g. a re-scan) must not clobber the photo already attached.
        server.enqueue(MockResponse(code = 200, body = "other"))
        assertThat(store.attachFromUrl("p1", url)).isFalse()
        assertThat(server.requestCount).isEqualTo(1)
        assertThat(frontPhotoRef()).isEqualTo(ref)
    }

    @Test
    fun a_failed_download_leaves_the_product_without_a_photo() = runTest {
        server.enqueue(MockResponse(code = 404))

        assertThat(store.attachFromUrl("p1", server.url("/missing.jpg").toString())).isFalse()

        assertThat(db.productDao().getProduct("p1")?.frontPhotoRef).isNull()
        assertThat(db.mediaDao().getById("media-1")).isNull()
        assertThat(File(context.filesDir, "product_photos/media-1.jpg").exists()).isFalse()
    }

    @Test
    fun discarding_a_pending_capture_removes_the_temp_file() = runTest {
        val capture = tempCapture("stale")

        store.discard(capture.absolutePath)

        assertThat(capture.exists()).isFalse()
    }
}
