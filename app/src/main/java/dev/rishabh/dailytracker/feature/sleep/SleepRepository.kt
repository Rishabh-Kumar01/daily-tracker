package dev.rishabh.dailytracker.feature.sleep

import dev.rishabh.dailytracker.core.common.IdGenerator
import dev.rishabh.dailytracker.core.common.TimeSource
import dev.rishabh.dailytracker.core.db.MissionType
import dev.rishabh.dailytracker.core.db.dao.SleepDao
import dev.rishabh.dailytracker.core.db.entity.SleepSessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The wake-alarm write model: confirm a bedtime, cancel it, dismiss the ringing alarm, and
 * reschedule after a reboot.
 *
 * The one invariant worth stating: `computed_wake_at = bed_confirmed_at + target_hours`,
 * fixed at confirmation — never recomputed from a planned bedtime.
 */
@Singleton
class SleepRepository @Inject constructor(
    private val sleepDao: SleepDao,
    private val scheduler: SleepAlarmScheduler,
    private val ids: IdGenerator,
    private val time: TimeSource,
) {

    fun observePending(): Flow<SleepSessionEntity?> = sleepDao.observePending()

    /**
     * Confirms bedtime now and schedules the wake alarm.
     *
     * Any unfinished alarm is superseded — there is only ever one live wake alarm — so
     * confirming again tonight moves it rather than stacking a second.
     */
    suspend fun confirmBedtime(targetHours: Double): SleepSessionEntity {
        val now = time.nowMillis()
        val wakeAt = now + (targetHours * MILLIS_PER_HOUR).toLong()
        supersedePending()
        val session = SleepSessionEntity(
            sessionId = ids.newId(),
            bedConfirmedAt = now,
            targetHours = targetHours,
            computedWakeAt = wakeAt,
            missionType = MissionType.MATH,
        )
        sleepDao.insert(session)
        scheduler.schedule(session.sessionId, wakeAt)
        return session
    }

    suspend fun cancelPending() = supersedePending()

    /** Silences the ringing alarm and records when the user actually woke. */
    suspend fun dismissAlarm(sessionId: String) {
        scheduler.cancel(sessionId)
        sleepDao.setActualWakeAt(sessionId, time.nowMillis())
    }

    /** BOOT_COMPLETED: re-arm every future alarm, since the OS drops them on restart. */
    suspend fun rescheduleAll() {
        sleepDao.getSchedulable(time.nowMillis()).forEach {
            scheduler.schedule(it.sessionId, it.computedWakeAt)
        }
    }

    private suspend fun supersedePending() {
        sleepDao.getPending()?.let {
            scheduler.cancel(it.sessionId)
            sleepDao.delete(it.sessionId)
        }
    }

    private companion object {
        const val MILLIS_PER_HOUR = 3_600_000.0
    }
}
