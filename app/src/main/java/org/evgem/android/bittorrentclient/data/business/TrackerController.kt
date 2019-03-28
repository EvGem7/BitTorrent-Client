package org.evgem.android.bittorrentclient.data.business

import android.util.Log
import org.evgem.android.bittorrentclient.constants.PEER_ID
import org.evgem.android.bittorrentclient.data.entity.Peer
import org.evgem.android.bittorrentclient.data.entity.TorrentInfo
import org.evgem.android.bittorrentclient.data.entity.TrackerRequest
import org.evgem.android.bittorrentclient.data.entity.TrackerResponse
import org.evgem.android.bittorrentclient.data.network.TrackerCommunicator
import java.lang.IllegalStateException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

/**
 * Controls communication with tracker while loading.
 */
class TrackerController(private val master: MasterController, torrentInfo: TorrentInfo) {
    private val communicators = ArrayList<TrackerCommunicator>()
    private var intervals = ArrayList<Pair<TrackerCommunicator, Int>>() //changing intervals must be atomic
    private val infoHash: ByteArray = torrentInfo.infoHash

    private var loopThread: LoopThread? = null

    val running: Boolean get() = loopThread != null

    init {
        for (announce in torrentInfo.announces) {
            communicators.add(TrackerCommunicator(announce))
        }
    }

    fun start() {
        if (running) {
            throw IllegalStateException("This TrackerController is already running")
        }
        loopThread = LoopThread() // safes from multiple start() call
        thread {
            synchronized(intervals) {
                for (communicator in communicators) {
                    val response = communicator.sendRequest(getTrackerRequest(STARTED_EVENT)) ?: continue
                    processTrackerResponse(communicator, response)
                }
            }
            loopThread?.start()
        }
    }

    fun stop() {
        if (loopThread == null) {
            throw IllegalStateException("This TrackerController is already stopped")
        }
        loopThread?.interrupt()
        loopThread = null
        thread {
            synchronized(intervals) {
                for (communicator in communicators) {
                    val response = communicator.sendRequest(getTrackerRequest(STOPPED_EVENT)) ?: continue
                    processTrackerResponse(communicator, response)
                }
            }
        }
    }

    fun complete() {
        thread {
            synchronized(intervals) {
                for (communicator in communicators) {
                    val response = communicator.sendRequest(getTrackerRequest(COMPLETED_EVENT)) ?: continue
                    processTrackerResponse(communicator, response)
                }
            }
        }
    }

    interface MasterController {
        fun onPeersObtained(peers: List<Peer>)

        val acceptingPort: Int

        val uploaded: Long
        val downloaded: Long
        val left: Long
    }

    private fun processTrackerResponse(communicator: TrackerCommunicator, response: TrackerResponse) {
        fun min(a: Int?, b: Int?): Int? {
            if (a == null) {
                return b
            }
            if (b == null) {
                return a
            }
            return kotlin.math.min(a, b)
        }
        val interval = min(response.interval, response.minInterval)
        if (interval != null) {
            intervals.add(communicator to interval * 1000)
        } else {
            intervals.add(communicator to 0)
        }

        if (response.failureReason != null) {
            Log.e(TAG, response.failureReason)
            return
        }
        response.warningMessage?.let { Log.w(TAG, it) }
        response.peers?.let { master.onPeersObtained(it) }
    }

    private fun getTrackerRequest(event: String? = null) = TrackerRequest(
        infoHash,
        PEER_ID,
        master.acceptingPort,
        master.uploaded,
        master.downloaded,
        master.left,
        event = event
    )

    private inner class LoopThread : Thread() {
        override fun run() {
            super.run()
            while (true) {
                val localIntervals: ArrayList<Pair<TrackerCommunicator, Int>>
                synchronized(intervals) {
                    localIntervals = ArrayList(intervals)
                }
                localIntervals.sortBy { it.second } //by interval

                val responses = LinkedList<Pair<TrackerCommunicator, TrackerResponse>>()
                var waited = 0L
                for ((communicator, interval) in localIntervals) {
                    Thread.sleep(interval - waited)
                    waited = interval.toLong()

                    //ignoring possible thread blocking
                    val response = communicator.sendRequest(getTrackerRequest()) ?: continue
                    responses += communicator to response
                }

                synchronized(intervals) {
                    intervals.clear()
                    responses.forEach { processTrackerResponse(it.first, it.second) }
                }
            }
        }
    }

    companion object {
        private const val TAG = "TrackerController"

        private const val STARTED_EVENT = "started"
        private const val STOPPED_EVENT = "stopped"
        private const val COMPLETED_EVENT = "completed"
    }
}