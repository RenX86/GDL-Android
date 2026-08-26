package com.renx86.gdlapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DownloadHistoryEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadHistoryDao(): DownloadHistoryDao
}
