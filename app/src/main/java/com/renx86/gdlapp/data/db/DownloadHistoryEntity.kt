package com.renx86.gdlapp.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object DownloadStatus {
    const val QUEUED = "QUEUED"
    const val DOWNLOADING = "DOWNLOADING"
    const val COMPLETED = "COMPLETED"
    const val FAILED = "FAILED"
    const val CANCELLED = "CANCELLED"
}

@Entity(
    tableName = "download_history",
    indices = [Index(value = ["timestamp"])]
)
data class DownloadHistoryEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val timestamp: Long,
    val status: String,
    val errorMessage: String? = null
)
