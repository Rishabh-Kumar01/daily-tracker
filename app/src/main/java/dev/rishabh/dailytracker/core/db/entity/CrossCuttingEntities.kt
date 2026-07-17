package dev.rishabh.dailytracker.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.rishabh.dailytracker.core.db.AiJobStatus
import dev.rishabh.dailytracker.core.db.AiTaskType
import dev.rishabh.dailytracker.core.db.MediaType

@Entity(tableName = "media")
data class MediaEntity(
    @PrimaryKey @ColumnInfo(name = "media_id") val mediaId: String,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "type") val type: MediaType,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    /**
     * Router rule: sensitive = true never leaves the device — local model only, app-private
     * storage, excluded from backup.
     */
    @ColumnInfo(name = "sensitive") val sensitive: Boolean = false,
)

/**
 * Key-value profile injected into LOCAL AI prompts only.
 *
 * Keys: weak_topics, preferred_difficulty, dietary_patterns, training_split, avg_sleep, ...
 * Written by background jobs reading the log tables. Never sent to a cloud backend
 * un-anonymised.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "value_json") val valueJson: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** Background work queue; survives app restarts. WorkManager executes, this table decides. */
@Entity(tableName = "ai_jobs")
data class AiJobEntity(
    @PrimaryKey @ColumnInfo(name = "job_id") val jobId: String,
    @ColumnInfo(name = "task_type") val taskType: AiTaskType,
    @ColumnInfo(name = "status") val status: AiJobStatus,
    @ColumnInfo(name = "payload_json") val payloadJson: String?,
    @ColumnInfo(name = "chosen_backend") val chosenBackend: String? = null,
    @ColumnInfo(name = "attempts") val attempts: Int = 0,
    @ColumnInfo(name = "result_ref") val resultRef: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** Local rate-limit ledger so the router can avoid 429s without asking the network. */
@Entity(tableName = "provider_quota")
data class ProviderQuotaEntity(
    @PrimaryKey @ColumnInfo(name = "backend_id") val backendId: String,
    /** rpm / rpd / tpd */
    @ColumnInfo(name = "window") val window: String,
    @ColumnInfo(name = "used") val used: Int = 0,
    @ColumnInfo(name = "resets_at") val resetsAt: Long,
)
