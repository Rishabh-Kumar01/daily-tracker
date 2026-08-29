package dev.rishabh.dailytracker.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.rishabh.dailytracker.core.db.entity.SleepSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: SleepSessionEntity)

    @Query("SELECT * FROM sleep_sessions WHERE session_id = :sessionId")
    suspend fun getById(sessionId: String): SleepSessionEntity?

    /**
     * The one pending alarm: the latest session not yet woken from.
     *
     * There is at most one live wake alarm; a new bedtime supersedes an unfinished one, so
     * "latest, not yet dismissed" is the alarm the UI shows and the service acts on.
     */
    @Query("SELECT * FROM sleep_sessions WHERE actual_wake_at IS NULL ORDER BY computed_wake_at DESC LIMIT 1")
    fun observePending(): Flow<SleepSessionEntity?>

    @Query("SELECT * FROM sleep_sessions WHERE actual_wake_at IS NULL ORDER BY computed_wake_at DESC LIMIT 1")
    suspend fun getPending(): SleepSessionEntity?

    /** Future, not-yet-dismissed sessions — what the boot receiver reschedules. */
    @Query("SELECT * FROM sleep_sessions WHERE actual_wake_at IS NULL AND computed_wake_at > :now")
    suspend fun getSchedulable(now: Long): List<SleepSessionEntity>

    /** Marks the session woken; the null-until-dismissal invariant on actual_wake_at ends here. */
    @Query("UPDATE sleep_sessions SET actual_wake_at = :wakeAt WHERE session_id = :sessionId")
    suspend fun setActualWakeAt(sessionId: String, wakeAt: Long)

    /** Cancelling tonight's alarm removes the pending session outright. */
    @Query("DELETE FROM sleep_sessions WHERE session_id = :sessionId")
    suspend fun delete(sessionId: String)
}
