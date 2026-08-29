package dev.rishabh.dailytracker.feature.sleep

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.rishabh.dailytracker.core.db.DailyTrackerDatabase
import dev.rishabh.dailytracker.core.db.FakeIdGenerator
import dev.rishabh.dailytracker.core.db.FakeTimeSource
import dev.rishabh.dailytracker.core.db.MissionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** A no-op scheduler so tests never arm a real OS alarm. */
private class FakeScheduler(context: Context) : SleepAlarmScheduler(context) {
    val scheduled = mutableListOf<Pair<String, Long>>()
    val cancelled = mutableListOf<String>()
    override fun schedule(sessionId: String, triggerAtMillis: Long) { scheduled += sessionId to triggerAtMillis }
    override fun cancel(sessionId: String) { cancelled += sessionId }
}

@RunWith(AndroidJUnit4::class)
class SleepRepositoryTest {

    private lateinit var db: DailyTrackerDatabase
    private lateinit var scheduler: FakeScheduler
    private lateinit var time: FakeTimeSource
    private lateinit var repository: SleepRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DailyTrackerDatabase::class.java).build()
        scheduler = FakeScheduler(context)
        time = FakeTimeSource()
        repository = SleepRepository(db.sleepDao(), scheduler, FakeIdGenerator(), time)
    }

    @After
    fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    @Test
    fun confirming_bedtime_derives_the_wake_time_and_schedules() = runTest {
        val session = repository.confirmBedtime(7.5)

        // computed_wake_at = bed_confirmed_at + target_hours, materialised.
        assertThat(session.bedConfirmedAt).isEqualTo(time.now)
        assertThat(session.computedWakeAt).isEqualTo(time.now + (7.5 * 3_600_000).toLong())
        assertThat(session.missionType).isEqualTo(MissionType.MATH)
        assertThat(scheduler.scheduled).containsExactly(session.sessionId to session.computedWakeAt)
        assertThat(repository.observePending().first()?.sessionId).isEqualTo(session.sessionId)
    }

    @Test
    fun a_new_bedtime_supersedes_the_previous_alarm() = runTest {
        val first = repository.confirmBedtime(8.0)
        val second = repository.confirmBedtime(6.0)

        assertThat(scheduler.cancelled).contains(first.sessionId)
        // Only the latest is pending; the old session is gone.
        assertThat(db.sleepDao().getById(first.sessionId)).isNull()
        assertThat(repository.observePending().first()?.sessionId).isEqualTo(second.sessionId)
    }

    @Test
    fun dismissing_records_the_actual_wake_and_cancels() = runTest {
        val session = repository.confirmBedtime(7.0)
        time.now += 7L * 3_600_000

        repository.dismissAlarm(session.sessionId)

        assertThat(db.sleepDao().getById(session.sessionId)?.actualWakeAt).isEqualTo(time.now)
        assertThat(scheduler.cancelled).contains(session.sessionId)
        // Once woken, it is no longer the pending alarm.
        assertThat(repository.observePending().first()).isNull()
    }

    @Test
    fun cancelling_clears_the_pending_alarm() = runTest {
        val session = repository.confirmBedtime(7.5)

        repository.cancelPending()

        assertThat(db.sleepDao().getById(session.sessionId)).isNull()
        assertThat(scheduler.cancelled).contains(session.sessionId)
        assertThat(repository.observePending().first()).isNull()
    }

    @Test
    fun reschedule_all_rearms_future_alarms() = runTest {
        val session = repository.confirmBedtime(8.0)
        scheduler.scheduled.clear()

        repository.rescheduleAll()

        assertThat(scheduler.scheduled).containsExactly(session.sessionId to session.computedWakeAt)
    }
}
