package com.renx86.gdlapp.di

import android.content.Context
import androidx.room.Room
import com.renx86.gdlapp.data.db.AppDatabase
import com.renx86.gdlapp.data.db.DownloadHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "gdl_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideDownloadHistoryDao(appDatabase: AppDatabase): DownloadHistoryDao {
        return appDatabase.downloadHistoryDao()
    }
}
