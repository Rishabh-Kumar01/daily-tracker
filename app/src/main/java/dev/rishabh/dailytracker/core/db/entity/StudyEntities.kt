package dev.rishabh.dailytracker.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.rishabh.dailytracker.core.db.IngestStatus

/*
 * Study side. Schema now, features later: the PDF -> chapter -> chunk -> MCQ pipeline is
 * Phase 3 and FSRS scheduling is Phase 2. These tables exist so those phases are additive
 * rather than a migration.
 */

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey @ColumnInfo(name = "subject_id") val subjectId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "color") val color: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
)

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["subject_id"],
            childColumns = ["subject_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MediaEntity::class,
            parentColumns = ["media_id"],
            childColumns = ["source_pdf_ref"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("subject_id"), Index("source_pdf_ref")],
)
data class ChapterEntity(
    @PrimaryKey @ColumnInfo(name = "chapter_id") val chapterId: String,
    @ColumnInfo(name = "subject_id") val subjectId: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "page_start") val pageStart: Int?,
    @ColumnInfo(name = "page_end") val pageEnd: Int?,
    @ColumnInfo(name = "source_pdf_ref") val sourcePdfRef: String? = null,
    /** Drives the per-chapter progress the UI shows during ingest. */
    @ColumnInfo(name = "ingest_status") val ingestStatus: IngestStatus = IngestStatus.PENDING,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
)

/** Retrieval store for MCQ generation. Chunk size target: 500-1000 tokens. */
@Entity(
    tableName = "chapter_chunks",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["chapter_id"],
            childColumns = ["chapter_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("chapter_id")],
)
data class ChapterChunkEntity(
    @PrimaryKey @ColumnInfo(name = "chunk_id") val chunkId: String,
    @ColumnInfo(name = "chapter_id") val chapterId: String,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "page_ref") val pageRef: Int?,
    @ColumnInfo(name = "token_count") val tokenCount: Int?,
    /** JSON array. */
    @ColumnInfo(name = "topic_tags") val topicTags: String? = null,
)

/** Pre-generated MCQ bank. */
@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["chapter_id"],
            childColumns = ["chapter_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ChapterChunkEntity::class,
            parentColumns = ["chunk_id"],
            childColumns = ["source_chunk_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("chapter_id"), Index("source_chunk_id"), Index("validated")],
)
data class QuestionEntity(
    @PrimaryKey @ColumnInfo(name = "question_id") val questionId: String,
    @ColumnInfo(name = "chapter_id") val chapterId: String,
    /** Grounding: the UI can show the passage a question came from. */
    @ColumnInfo(name = "source_chunk_id") val sourceChunkId: String? = null,
    @ColumnInfo(name = "stem") val stem: String,
    /** JSON array of exactly 4. */
    @ColumnInfo(name = "options_json") val optionsJson: String,
    @ColumnInfo(name = "correct_index") val correctIndex: Int,
    @ColumnInfo(name = "explanation") val explanation: String?,
    /** 1-3 */
    @ColumnInfo(name = "difficulty") val difficulty: Int?,
    @ColumnInfo(name = "topic_tag") val topicTag: String?,
    /** Model id, kept for quality auditing across backends. */
    @ColumnInfo(name = "generator_backend") val generatorBackend: String?,
    /** Only validated rows are servable. */
    @ColumnInfo(name = "validated") val validated: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "question_attempts",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["question_id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("question_id")],
)
data class QuestionAttemptEntity(
    @PrimaryKey @ColumnInfo(name = "attempt_id") val attemptId: String,
    @ColumnInfo(name = "question_id") val questionId: String,
    @ColumnInfo(name = "answered_at") val answeredAt: Long,
    @ColumnInfo(name = "chosen_index") val chosenIndex: Int,
    @ColumnInfo(name = "is_correct") val isCorrect: Boolean,
    @ColumnInfo(name = "time_taken_ms") val timeTakenMs: Long?,
)

/** FSRS state. The scheduler is deterministic — no LLM involved. */
@Entity(
    tableName = "review_schedule",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["question_id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("due_date")],
)
data class ReviewScheduleEntity(
    @PrimaryKey @ColumnInfo(name = "question_id") val questionId: String,
    @ColumnInfo(name = "due_date") val dueDate: String,
    @ColumnInfo(name = "stability") val stability: Double,
    @ColumnInfo(name = "difficulty") val difficulty: Double,
    @ColumnInfo(name = "last_reviewed") val lastReviewed: Long?,
    @ColumnInfo(name = "lapses") val lapses: Int = 0,
)
