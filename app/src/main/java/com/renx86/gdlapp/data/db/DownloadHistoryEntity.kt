package com.renx86.gdlapp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_history")
data class DownloadHistoryEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val timestamp: Long,
    val status: String, // e.g. "DOWNLOADING", "COMPLETED", "FAILED"
    val errorMessage: String? = null
)
