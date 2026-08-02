package dev.rishabh.dailytracker.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.rishabh.dailytracker.core.db.entity.MediaEntity

@Dao
interface MediaDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(media: MediaEntity)

    @Query("SELECT * FROM media WHERE media_id = :mediaId")
    suspend fun getById(mediaId: String): MediaEntity?

    /** Thumbnail lookup for pickers — one bulk read instead of a per-row fan-out. */
    @Query("SELECT * FROM media WHERE media_id IN (:mediaIds)")
    suspend fun getByIds(mediaIds: List<String>): List<MediaEntity>

    @Query("DELETE FROM media WHERE media_id = :mediaId")
    suspend fun delete(mediaId: String)
}
