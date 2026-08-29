package dev.rishabh.dailytracker.feature.activities

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.rishabh.dailytracker.core.db.CreatedBy
import dev.rishabh.dailytracker.core.db.DailyTrackerDatabase
import dev.rishabh.dailytracker.core.db.FakeIdGenerator
import dev.rishabh.dailytracker.core.db.FakeTimeSource
import dev.rishabh.dailytracker.core.db.FieldType
import dev.rishabh.dailytracker.core.db.entity.ActivityTemplateEntity
import dev.rishabh.dailytracker.core.db.entity.ItemEntity
import dev.rishabh.dailytracker.core.db.entity.ItemFieldEntity
import dev.rishabh.dailytracker.core.db.entity.SubMenuEntity
import dev.rishabh.dailytracker.core.designsystem.component.model.LogValueDraft
import dev.rishabh.dailytracker.core.designsystem.component.model.SetRow
import dev.rishabh.dailytracker.core.designsystem.component.model.encodeSetRows
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The generic set-logging slice against a real database, exercised through a Workout template:
 * what the screen reads, what logging writes, previous-session recall, and re-log/clear.
 */
@RunWith(AndroidJUnit4::class)
class ItemLogRepositoryTest {

    private lateinit var db: DailyTrackerDatabase
    private lateinit var repository: ItemLogRepository
    private lateinit var time: FakeTimeSource

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DailyTrackerDatabase::class.java,
        ).build()
        time = FakeTimeSource()
        repository = ItemLogRepository(db.templateDao(), db.logDao(), FakeIdGenerator(), time)

        db.templateDao().insertFullTemplate(
            ActivityTemplateEntity("t1", "Workout", "fitness_center", "#FFA460", CreatedBy.SYSTEM, "completion_percent", "done", 1, false, 0, 1L),
            listOf(SubMenuEntity("s1", "t1", "Push", 0, null)),
            listOf(ItemEntity("i1", "s1", "Bench Press", false, null, 0)),
            listOf(
                ItemFieldEntity("f1", "i1", "sets", FieldType.SET_GROUP.wire, "Sets", null, true, 0, """{"fields":["reps","weight"],"weight_unit":"kg"}"""),
                ItemFieldEntity("f2", "i1", "note", FieldType.NOTE.wire, "Note", null, false, 1, null),
            ),
        )
    }

    @After
    fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    private suspend fun log() = checkNotNull(repository.observeSubMenuLog("s1").first())
    private suspend fun item() = log().items.single()

    private fun setsDraft(vararg rows: SetRow) = LogValueDraft("sets", json = encodeSetRows(rows.toList()))

    @Test
    fun exposes_the_items_and_fields_empty_before_anything_is_logged() = runTest {
        val item = item()
        assertThat(item.name).isEqualTo("Bench Press")
        assertThat(item.fields.map { it.fieldKey }).containsExactly("sets", "note").inOrder()
        assertThat(item.loggedEntryId).isNull()
        assertThat(item.recall).isNull()
        assertThat(item.committed.all { it.json == null && it.text == null }).isTrue()
    }

    @Test
    fun logging_sets_writes_an_entry_and_reads_back() = runTest {
        repository.logItem("t1", "s1", "i1", listOf(setsDraft(SetRow(8, 60.0), SetRow(8, 60.0))))

        val item = item()
        assertThat(item.loggedEntryId).isNotNull()
        val sets = item.committed.first { it.fieldKey == "sets" }.json
        assertThat(sets).contains("\"reps\":8")
        assertThat(sets).contains("\"weight\":60")
    }

    @Test
    fun an_empty_optional_field_writes_no_row() = runTest {
        repository.logItem("t1", "s1", "i1", listOf(setsDraft(SetRow(5, 40.0)), LogValueDraft("note", text = "")))

        val entryId = checkNotNull(item().loggedEntryId)
        assertThat(db.logDao().getValues(entryId).map { it.fieldKey }).containsExactly("sets")
    }

    @Test
    fun re_logging_replaces_the_entry_instead_of_adding_a_second() = runTest {
        repository.logItem("t1", "s1", "i1", listOf(setsDraft(SetRow(8, 60.0))))
        val first = checkNotNull(item().loggedEntryId)

        repository.logItem("t1", "s1", "i1", listOf(setsDraft(SetRow(8, 65.0))))

        assertThat(item().loggedEntryId).isEqualTo(first)
        assertThat(db.logDao().getEntriesForDay("t1", time.today())).hasSize(1)
        assertThat(item().committed.first { it.fieldKey == "sets" }.json).contains("65")
    }

    @Test
    fun recall_shows_the_last_session_from_a_previous_day() = runTest {
        // Yesterday's session.
        time.now = FakeTimeSource.FIXED_NOW - 24L * 60 * 60 * 1000
        repository.logItem("t1", "s1", "i1", listOf(setsDraft(SetRow(8, 60.0), SetRow(8, 60.0), SetRow(8, 60.0))))

        // Today, before logging again, the card recalls it.
        time.now = FakeTimeSource.FIXED_NOW
        val item = item()
        assertThat(item.recall).isEqualTo("3 × 8 @ 60 kg")
        // Recall is history, not today's value — nothing is logged today yet.
        assertThat(item.loggedEntryId).isNull()
    }

    @Test
    fun clearing_removes_todays_log_but_keeps_recall() = runTest {
        time.now = FakeTimeSource.FIXED_NOW - 24L * 60 * 60 * 1000
        repository.logItem("t1", "s1", "i1", listOf(setsDraft(SetRow(10, 50.0))))
        time.now = FakeTimeSource.FIXED_NOW
        repository.logItem("t1", "s1", "i1", listOf(setsDraft(SetRow(8, 55.0))))
        val entryId = checkNotNull(item().loggedEntryId)

        repository.clearItem(entryId)

        val item = item()
        assertThat(item.loggedEntryId).isNull()
        assertThat(item.recall).isEqualTo("1 × 10 @ 50 kg")
    }
}
