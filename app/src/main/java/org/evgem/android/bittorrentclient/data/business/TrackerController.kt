package org.evgem.android.bittorrentclient.data.business

import org.evgem.android.bittorrentclient.data.entity.Peer
import org.evgem.android.bittorrentclient.data.entity.TorrentInfo
import org.evgem.android.bittorrentclient.data.entity.TrackerRequest
import org.evgem.android.bittorrentclient.data.network.TrackerCommunicator
import java.lang.IllegalStateException

class TrackerController(private val observer: Observer, torrentInfo: TorrentInfo) {
    private val communicators = ArrayList<TrackerCommunicator>()
    private var interval: Int = 0

    private var loopThread: LoopThread? = null

    init {
        for (announce in torrentInfo.announces) {
            communicators.add(TrackerCommunicator(announce))
        }
    }

    fun start() {
        if (loopThread != null) {
            throw IllegalStateException("This TrackerController is already running")
        }
        loopThread = LoopThread().apply { start() }
    }

    fun stop() {
        if (loopThread == null) {
            throw IllegalStateException("This TrackerController is already stopped")
        }
        loopThread?.interrupt()
    }

    interface Observer {
        fun onPeersObtained(peers: List<Peer>)
    }

    private inner class LoopThread : Thread() {
        override fun run() {
            super.run()
            while (true) {
                TrackerRequest()
            }
        }
    }
}