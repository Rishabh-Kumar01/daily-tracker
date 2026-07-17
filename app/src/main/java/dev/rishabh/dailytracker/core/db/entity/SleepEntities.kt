package dev.rishabh.dailytracker.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.rishabh.dailytracker.core.db.LockMode
import dev.rishabh.dailytracker.core.db.MissionType

/**
 * Sleep sessions. Schema now, features later (Phase 2).
 *
 * Naps are ordinary log_entries with a duration field under the Sleep template — there is
 * deliberately no extra table for them.
 *
 * `computed_wake_at = bed_confirmed_at + target_hours`, computed at confirmation and never
 * derived from the planned bedtime. The BOOT_COMPLETED receiver reschedules from this table.
 */
@Entity(
    tableName = "sleep_sessions",
    foreignKeys = [
        ForeignKey(
            entity = MediaEntity::class,
            parentColumns = ["media_id"],
            childColumns = ["mission_reference_photo_ref"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("bed_confirmed_at"), Index("mission_reference_photo_ref")],
)
data class SleepSessionEntity(
    @PrimaryKey @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "bed_confirmed_at") val bedConfirmedAt: Long,
    @ColumnInfo(name = "target_hours") val targetHours: Double,
    /** bed_confirmed_at + target_hours, materialised at confirmation time. */
    @ColumnInfo(name = "computed_wake_at") val computedWakeAt: Long,
    /** Null until the alarm is actually dismissed. */
    @ColumnInfo(name = "actual_wake_at") val actualWakeAt: Long? = null,
    @ColumnInfo(name = "lock_mode") val lockMode: LockMode = LockMode.OFF,
    @ColumnInfo(name = "mission_type") val missionType: MissionType = MissionType.NONE,
    @ColumnInfo(name = "mission_item_name") val missionItemName: String? = null,
    @ColumnInfo(name = "mission_reference_photo_ref") val missionReferencePhotoRef: String? = null,
    /** 1-5, null until the user answers. */
    @ColumnInfo(name = "restedness") val restedness: Int? = null,
    @ColumnInfo(name = "interruptions") val interruptions: Int = 0,
)
