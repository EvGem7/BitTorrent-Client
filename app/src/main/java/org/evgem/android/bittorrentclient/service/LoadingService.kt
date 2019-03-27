package org.evgem.android.bittorrentclient.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class LoadingService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        TODO()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return LoadingBinder()
    }

    inner class LoadingBinder : Binder()
}