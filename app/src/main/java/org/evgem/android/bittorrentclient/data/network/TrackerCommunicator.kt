package org.evgem.android.bittorrentclient.data.network

import android.util.Log
import org.evgem.android.bittorrentclient.data.bencode.*
import org.evgem.android.bittorrentclient.data.entity.Peer
import org.evgem.android.bittorrentclient.data.entity.TrackerRequest
import org.evgem.android.bittorrentclient.data.entity.TrackerResponse
import org.evgem.android.bittorrentclient.exception.BEncodeException
import org.evgem.android.bittorrentclient.util.NetworkUtil
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*

/**
 * Provides API for communication with tracker
 */
class TrackerCommunicator(private val announceUrl: String) {
    /**
     * It can block thread for SOCKET_TIMEOUT ms. See TorrentConstants.
     */
    fun sendRequest(request: TrackerRequest): TrackerResponse? {
        val params = ArrayList<Pair<String, ByteArray>>().apply {
            add("info_hash" to request.infoHash)
            add("peer_id" to request.peerId)
            add("port" to request.port)
            add("uploaded" to request.uploaded)
            add("downloaded" to request.downloaded)
            add("left" to request.left)
            add("compact" to request.compact)
            request.noPeerId?.let { add("no_peer_id" to it) }
            request.event?.let { add("event" to it) }
            request.ip?.let { add("ip" to it) }
            add("numwant" to request.numWant)
            request.key?.let { add("key" to it) }
            request.trackerId?.let { add("trackerid" to it) }
        }

        try {
            val responseData = httpRequest(announceUrl, params)
            val decoded = BDecoder.decode(ByteArrayInputStream(responseData))
            if (decoded !is BMap) {
                Log.e(TAG, "Error. Tracker response must be bencoded dictionary.")
                return null
            }
            return createTrackerResponse(decoded)
        } catch (e: Exception) {
            if (e is IOException || e is BEncodeException) {
                Log.e(TAG, Log.getStackTraceString(e))
                return null
            } else {
                throw e
            }
        }
    }

    private fun createTrackerResponse(map: BMap): TrackerResponse {
        val failureReason = map.value["failure reason"] as? BString
        val warningMessage = map.value["warning message"] as? BString
        val interval = map.value["interval"] as? BInteger
        val minInterval = map.value["min interval"] as? BInteger
        val trackerId = map.value["tracker id"] as? BString
        val complete = map.value["complete"] as? BInteger
        val incomplete = map.value["incomplete"] as? BInteger

        val peersData = map.value["peers"]
        val peers: List<Peer>? = when (peersData) {
            is BList -> {
                val result = ArrayList<Peer>()
                for (peerMap in peersData.value) {
                    if (peerMap as? BMap != null) {
                        val peerId = peerMap.value["peer id"] as? BString

                        val ipString = (peerMap.value["ip"] as? BString) ?: continue
                        val ip: InetAddress
                        try {
                            ip = InetAddress.getByName(ipString.toString())
                        } catch (e: UnknownHostException) {
                            Log.e(TAG, Log.getStackTraceString(e))
                            continue
                        }

                        val port = (peerMap.value["port"] as? BInteger) ?: continue

                        val peer = Peer(
                            peerId?.value,
                            ip,
                            port.value.toInt()
                        )
                        result.add(peer)
                    } else {
                        Log.d(TAG, "Error. Peer has wrong type")
                    }
                }
                result
            }
            is BString -> if (peersData.value.size % PEER_RAW_SIZE == 0) {
                val result = ArrayList<Peer>()
                for (i in 0 until peersData.value.size step PEER_RAW_SIZE) {
                    val ipData = peersData.value.sliceArray(i until i + IP_RAW_SIZE)
                    val portData = peersData.value.sliceArray(i + PORT_OFFSET until i + PORT_OFFSET + PORT_RAW_SIZE)

                    val ip: InetAddress
                    try {
                        ip = InetAddress.getByAddress(ipData)
                    } catch (e: UnknownHostException) {
                        Log.e(TAG, Log.getStackTraceString(e))
                        continue
                    }

                    val port = NetworkUtil.getPort(portData)

                    result.add(Peer(ip = ip, port = port))
                }
                result
            } else {
                null
            }
            null -> null
            else -> {
                Log.e(TAG, "Error. Peers have wrong type.")
                null
            }
        }

        return TrackerResponse(
            failureReason = failureReason?.toString(),
            warningMessage = warningMessage?.toString(),
            interval = interval?.value?.toInt(),
            minInterval = minInterval?.value?.toInt(),
            trackerId = trackerId?.value,
            complete = complete?.value?.toInt(),
            incomplete = incomplete?.value?.toInt(),
            peers = peers
        )
    }

    companion object {
        private val TAG = TrackerCommunicator::class.java.simpleName
        private const val PEER_RAW_SIZE = 6
        private const val IP_RAW_SIZE = 4
        private val PORT_OFFSET get() = IP_RAW_SIZE
        private const val PORT_RAW_SIZE = 2
    }
}