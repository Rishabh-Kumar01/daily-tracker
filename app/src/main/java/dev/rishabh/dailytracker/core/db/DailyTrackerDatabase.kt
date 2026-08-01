package dev.rishabh.dailytracker.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.rishabh.dailytracker.core.db.dao.GenericFoodMetaDao
import dev.rishabh.dailytracker.core.db.dao.LogDao
import dev.rishabh.dailytracker.core.db.dao.ProductDao
import dev.rishabh.dailytracker.core.db.dao.TemplateDao
import dev.rishabh.dailytracker.core.db.entity.ActivityTemplateEntity
import dev.rishabh.dailytracker.core.db.entity.AiJobEntity
import dev.rishabh.dailytracker.core.db.entity.ChapterChunkEntity
import dev.rishabh.dailytracker.core.db.entity.ChapterEntity
import dev.rishabh.dailytracker.core.db.entity.GenericFoodMetaEntity
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
 * The whole schema. Version 1 had template, log, product, study, sleep, and cross-cutting
 * tables. Version 2 adds the [GenericFoodMetaEntity] companion table for bundled generic foods.
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
        GenericFoodMetaEntity::class,
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
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class DailyTrackerDatabase : RoomDatabase() {
    abstract fun templateDao(): TemplateDao
    abstract fun logDao(): LogDao
    abstract fun productDao(): ProductDao
    abstract fun genericFoodMetaDao(): GenericFoodMetaDao

    companion object {
        const val NAME = "daily_tracker.db"

        /**
         * Migration 1 → 2: adds the [generic_food_meta] companion table.
         *
         * Purely additive — no column changes, no data loss. The seeder populates the new
         * table on the first launch after upgrade.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS generic_food_meta (
                        product_id TEXT NOT NULL PRIMARY KEY,
                        slug TEXT NOT NULL,
                        category TEXT,
                        prep TEXT,
                        source_form TEXT,
                        source_db TEXT,
                        source_ref TEXT,
                        is_approx INTEGER NOT NULL DEFAULT 0,
                        dataset_version INTEGER NOT NULL,
                        FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_generic_food_meta_product_id ON generic_food_meta(product_id)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_generic_food_meta_slug ON generic_food_meta(slug)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_generic_food_meta_category ON generic_food_meta(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_generic_food_meta_dataset_version ON generic_food_meta(dataset_version)")
            }
        }

        /**
         * Migration 2 → 3: adds the serving-unit columns to [generic_food_meta].
         *
         * Purely additive (three nullable columns). The columns describe how a food is
         * logged (count / household / grams); they are seed metadata, not user data, so the
         * seeder backfills them from the asset on the next launch. Nothing existing changes.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE generic_food_meta ADD COLUMN serving_unit TEXT")
                db.execSQL("ALTER TABLE generic_food_meta ADD COLUMN unit_label TEXT")
                db.execSQL("ALTER TABLE generic_food_meta ADD COLUMN grams_per_unit REAL")
            }
        }
    }
}
