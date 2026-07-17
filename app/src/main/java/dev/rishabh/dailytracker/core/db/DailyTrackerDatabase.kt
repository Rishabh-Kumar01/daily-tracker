package dev.rishabh.dailytracker.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.rishabh.dailytracker.core.db.dao.LogDao
import dev.rishabh.dailytracker.core.db.dao.ProductDao
import dev.rishabh.dailytracker.core.db.dao.TemplateDao
import dev.rishabh.dailytracker.core.db.entity.ActivityTemplateEntity
import dev.rishabh.dailytracker.core.db.entity.AiJobEntity
import dev.rishabh.dailytracker.core.db.entity.ChapterChunkEntity
import dev.rishabh.dailytracker.core.db.entity.ChapterEntity
import dev.rishabh.dailytracker.core.db.entity.ItemEntity
import dev.rishabh.dailytracker.core.db.entity.ItemFieldEntity
import dev.rishabh.dailytracker.core.db.entity.LogEntryEntity
import dev.rishabh.dailytracker.core.db.entity.LogValueEntity
import dev.rishabh.dailytracker.core.db.entity.MediaEntity
import dev.rishabh.dailytracker.core.db.entity.ProductEntity
import dev.rishabh.dailytracker.core.db.entity.ProductNutrientEntity
import dev.rishabh.dailytracker.core.db.entity.ProviderQuotaEntity
import dev.rishabh.dailytracker.core.db.entity.QuestionAttemptEntity
import dev.rishabh.dailytracker.core.db.entity.QuestionEntity
import dev.rishabh.dailytracker.core.db.entity.ReviewScheduleEntity
import dev.rishabh.dailytracker.core.db.entity.SleepSessionEntity
import dev.rishabh.dailytracker.core.db.entity.SubMenuEntity
import dev.rishabh.dailytracker.core.db.entity.SubjectEntity
import dev.rishabh.dailytracker.core.db.entity.UserProfileEntity

/**
 * The whole schema lands in v1, including the Study and Sleep tables whose features are
 * Phase 2/3. Defining them now keeps those phases additive instead of a migration, and
 * costs nothing while they're unused.
 *
 * sync_meta is intentionally absent — the schema defers it until cloud backup exists.
 */
@Database(
    entities = [
        // Template side
        ActivityTemplateEntity::class,
        SubMenuEntity::class,
        ItemEntity::class,
        ItemFieldEntity::class,
        // Log side
        LogEntryEntity::class,
        LogValueEntity::class,
        // Food
        ProductEntity::class,
        ProductNutrientEntity::class,
        // Study
        SubjectEntity::class,
        ChapterEntity::class,
        ChapterChunkEntity::class,
        QuestionEntity::class,
        QuestionAttemptEntity::class,
        ReviewScheduleEntity::class,
        // Sleep
        SleepSessionEntity::class,
        // Cross-cutting
        MediaEntity::class,
        UserProfileEntity::class,
        AiJobEntity::class,
        ProviderQuotaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class DailyTrackerDatabase : RoomDatabase() {
    abstract fun templateDao(): TemplateDao
    abstract fun logDao(): LogDao
    abstract fun productDao(): ProductDao

    companion object {
        const val NAME = "daily_tracker.db"
    }
}
