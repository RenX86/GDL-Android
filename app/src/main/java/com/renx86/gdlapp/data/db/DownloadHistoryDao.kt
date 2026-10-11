package com.renx86.gdlapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
@Dao
interface DownloadHistoryDao {

    @Query("SELECT * FROM download_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<DownloadHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: DownloadHistoryEntity)

    @Update
    suspend fun update(history: DownloadHistoryEntity)

    @Query("UPDATE download_history SET status = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, errorMessage: String?): Int

    @Query("DELETE FROM download_history WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM download_history")
    suspend fun clearAll(): Int

    @Query("DELETE FROM download_history WHERE status NOT IN ('DOWNLOADING', 'QUEUED')")
    suspend fun clearCompleted(): Int
}
