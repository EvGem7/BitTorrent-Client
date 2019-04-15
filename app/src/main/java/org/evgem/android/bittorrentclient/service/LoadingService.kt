package org.evgem.android.bittorrentclient.service

import android.app.Service
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import org.evgem.android.bittorrentclient.data.business.LoadingController
import org.evgem.android.bittorrentclient.data.entity.Loading

class LoadingService : Service() {
    private val loadings: LiveData<List<Loading>> = MutableLiveData()
    private val controllers = ArrayList<LoadingController>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        TODO()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return LoadingBinder()
    }

    inner class LoadingBinder : Binder() {
        val loadings = this@LoadingService.loadings
    }

    companion object {
        const val TORRENT_INFO_EXTRA = "org.evgem.android.bittorrentclient.service.TORRENT_INFO_EXTRA"
    }
}