package dev.rishabh.dailytracker.core.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migrations run against real on-disk databases, because this is the user's only copy of
 * their history and there is no cloud backup — a wrong migration is unrecoverable loss.
 *
 * [MigrationTestHelper.runMigrationsAndValidate] opens a database created at the old version,
 * applies the migration, and asserts the resulting schema matches the entities exactly
 * (validating against the exported schema JSON). A mismatch — a wrong column type, a missing
 * index — fails here rather than on the device.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DailyTrackerDatabase::class.java,
    )

    @Test
    fun migrate_4_to_5_adds_the_meal_template_tables() {
        helper.createDatabase(TEST_DB, 4).close()

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            5,
            true,
            DailyTrackerDatabase.MIGRATION_4_5,
        )

        db.query(
            "SELECT name FROM sqlite_master WHERE type='table' " +
                "AND name IN ('meal_templates', 'meal_template_items')",
        ).use { cursor ->
            val tables = buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
            assertThat(tables).containsExactly("meal_templates", "meal_template_items")
        }
        db.close()
    }

    @Test
    fun migrates_the_whole_chain_from_1_to_5() {
        helper.createDatabase(TEST_DB, 1).close()

        helper.runMigrationsAndValidate(
            TEST_DB,
            5,
            true,
            DailyTrackerDatabase.MIGRATION_1_2,
            DailyTrackerDatabase.MIGRATION_2_3,
            DailyTrackerDatabase.MIGRATION_3_4,
            DailyTrackerDatabase.MIGRATION_4_5,
        ).close()
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
