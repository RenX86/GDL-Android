package com.renx86.gdlapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GDLApplication : Application(), SingletonImageLoader.Factory {
    companion object {
        const val CHANNEL_ID = "gdl_downloads"
        const val CHANNEL_UPDATES_ID = "gdl_updates_v2"
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }

    override fun onCreate() {
        super.onCreate()

        val downloadChannel = NotificationChannel(
            CHANNEL_ID,
            "Downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows active download progress"
        }

        val updateChannel = NotificationChannel(
            CHANNEL_UPDATES_ID,
            "App Updates",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifies when a new app version is available"
        }

        val manager = getSystemService(NotificationManager::class.java)
        // Old channel had default importance (no pop-up); importance can't be changed in place
        manager.deleteNotificationChannel("gdl_updates")
        manager.createNotificationChannel(downloadChannel)
        manager.createNotificationChannel(updateChannel)
    }
}

