package com.renx86.gdlapp.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.renx86.gdlapp.GDLApplication
import com.renx86.gdlapp.R

class DownloadService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1
        private const val EXTRA_STATUS = "status_text"

        // Helper to build the start intent with a status message
        fun startIntent(context: Context, statusText: String): Intent {
            return Intent(context, DownloadService::class.java).apply {
                putExtra(EXTRA_STATUS, statusText)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Read status text from intent, default to generic message
        val statusText = intent?.getStringExtra(EXTRA_STATUS) ?: "Downloading media..."

        val notification = NotificationCompat.Builder(this, GDLApplication.CHANNEL_ID)
            .setContentTitle("GDL Download")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}