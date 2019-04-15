package org.evgem.android.bittorrentclient

import android.app.Application
import org.evgem.android.bittorrentclient.data.database.AppDatabase

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDatabase.init(this)
    }
}