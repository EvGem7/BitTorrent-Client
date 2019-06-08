package org.evgem.android.bittorrentclient

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import org.evgem.android.bittorrentclient.data.database.AppDatabase

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDatabase.init(this)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Loadings", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "loadings"
    }
}