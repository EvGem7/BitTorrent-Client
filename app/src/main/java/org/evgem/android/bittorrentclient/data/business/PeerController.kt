package org.evgem.android.bittorrentclient.data.business

import android.util.Log
import org.evgem.android.bittorrentclient.constants.HASH_ALGORITHM
import org.evgem.android.bittorrentclient.constants.PEER_ID
import org.evgem.android.bittorrentclient.data.entity.Peer
import org.evgem.android.bittorrentclient.data.entity.TorrentInfo
import org.evgem.android.bittorrentclient.data.network.PeerCommunicator
import org.evgem.android.bittorrentclient.util.FixedBitSet
import java.net.Socket
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.Comparator
import kotlin.collections.HashMap

/**
 * Controls communication with peers.
 */
class PeerController(private val master: MasterController, private val torrentInfo: TorrentInfo) {
    private val communicators = ArrayList<PeerCommunicator>() //TODO add synchronization
    private val pendingPeers = LinkedBlockingQueue<Peer>()
    private val pendingPeerSockets = LinkedBlockingQueue<Socket>()

    private val piecesFreqs = IntArray(torrentInfo.pieces.size)

    private val providedPieces = HashMap<Peer, FreqSortedTreeSet>() // map peer to piece indexes that peer provides

    private val downloadingPieces = HashMap<Peer, Piece>() // one peer downloads one piece


    var running: Boolean = false
        private set

    var status: Status = Status.DOWNLOADING
        private set

    enum class Status { DOWNLOADING, SEEDING }

    interface MasterController {
        fun onPieceReceived(piece: ByteArray, index: Int)
        val piecesStatus: BooleanArray
    }

    fun start() {
        if (running) {
            throw IllegalStateException("this peer controller is already running")
        }
        for (communicator in communicators) {
            if (communicator.running) {
                Log.w(TAG, "communicator with ${communicator.peer} is already running")
                continue
            }
            try {
                communicator.peer?.let { communicator.start(it) } ?: communicators.remove(communicator)
            } catch (e: IllegalStateException) {
                Log.e(TAG, Log.getStackTraceString(e))
            }
        }
        while (pendingPeers.isNotEmpty()) {
            val peer = pendingPeers.poll()
            communicators += PeerCommunicator().apply { start(peer) }
        }
        while (pendingPeerSockets.isNotEmpty()) {
            val socket = pendingPeerSockets.poll()
            communicators += PeerCommunicator().apply { start(socket) }
        }
        running = true
    }

    fun stop() {
        if (!running) {
            throw IllegalStateException("this peer controller is already stopped")
        }
        for (communicator in communicators) {
            if (!communicator.running) {
                Log.w(TAG, "communicator with ${communicator.peer} is already stopped")
                continue
            }
            try {
                communicator.stop()
            } catch (e: IllegalStateException) {
                Log.e(TAG, Log.getStackTraceString(e))
            }
        }
        running = false
    }

    fun addPeer(peer: Peer) {
        if (running) {
            val communicator = initCommunicator()
            communicator.start(peer)
            communicators += communicator
        } else {
            pendingPeers += peer
        }
    }

    fun addPeer(peerSocket: Socket) {
        if (running) {
            val communicator = initCommunicator()
            communicator.start(peerSocket)
            communicators += communicator
        } else {
            pendingPeerSockets += peerSocket
        }
    }

    fun addPeers(peers: List<Peer>) {
        for (peer in peers) {
            addPeer(peer)
        }
    }

    private fun getPeerRequest(peer: Peer): PeerRequest? {
        return getPeerRequest(peer, providedPieces[peer] ?: return null)
    }

    private fun getPeerRequest(peer: Peer, provided: FreqSortedTreeSet): PeerRequest? {
        for (pieceIndex in provided) {
            val piece = downloadingPieces[peer]
            if (piece != null) {
                return PeerRequest(pieceIndex, piece.got, getBlockLength(piece))
            } else {
                if (!master.piecesStatus[pieceIndex]) { // if not downloaded
                    var alreadyDownloading = false
                    for ((_, value) in downloadingPieces) {
                        if (value.index == pieceIndex) {
                            alreadyDownloading = true
                            break
                        }
                    }
                    if (alreadyDownloading) {
                        continue
                    }
                    val newPiece = Piece(pieceIndex)
                    downloadingPieces[peer] = newPiece
                    return PeerRequest(pieceIndex, 0, getBlockLength(newPiece))
                }
            }
        }
        return null
    }

    private fun notifyBlockDownloaded(peer: Peer, index: Int, offset: Int, data: ByteArray) {
        val piece = downloadingPieces[peer] ?: throw IllegalArgumentException("this peer isn't downloading anything")
        data.copyInto(piece.data, offset)
        piece.got += data.size
        if (piece.got == torrentInfo.pieceLength) {
            val pieceHash: ByteArray = MessageDigest.getInstance(HASH_ALGORITHM).digest(piece.data)
            if (!pieceHash.contentEquals(torrentInfo.pieces[index])) {
                Log.e(TAG, "Piece hash is not valid. index=$index; offset=$offset")
                return
            }
            downloadingPieces.remove(peer)
            master.onPieceReceived(piece.data, index)
        }
    }

    private fun getBlockLength(piece: Piece) = (torrentInfo.pieceLength - piece.got) % MAX_BLOCK_LENGTH + 1

    private fun initCommunicator() = PeerCommunicator().setOnSocketInitializedListener {
        handshake(torrentInfo.infoHash, PEER_ID)
    }.setOnHandshakeListener { _, infoHash, _ ->
        if (!infoHash.contentEquals(torrentInfo.infoHash)) {
            stop()
            communicators.remove(this)
        }
        val bitSet = FixedBitSet((torrentInfo.pieces.size + 7) / 8)
        for ((index, gotPiece) in master.piecesStatus.withIndex()) {
            bitSet.bits.set(index, gotPiece)
        }
        bitfield(bitSet)
    }.setOnBitfieldListener { bitSet ->
        val provided = FreqSortedTreeSet()
        for (i in 0 until piecesFreqs.size) {
            if (bitSet.bits[i]) {
                ++piecesFreqs[i]
                provided += i
            }
        }
        if (provided.isEmpty()) {
            return@setOnBitfieldListener
        }
        peer?.let { peer ->
            providedPieces[peer] = provided
            getPeerRequest(peer, provided)?.let { request ->
                unchoke()
                interested()
                if (peerChoking) {
                    setOnUnchokeListener {
                        request(request.index, request.offset, request.length)
                        setOnUnchokeListener(null)
                    }
                } else {
                    request(request.index, request.offset, request.length)
                }
            }
        }
    }.setOnHaveListener { pieceIndex ->
        ++piecesFreqs[pieceIndex]
        peer?.let { providedPieces[it]?.add(pieceIndex) }
    }.setOnPieceListener { index, offset, data ->
        peer?.let { peer ->
            notifyBlockDownloaded(peer, index, offset, data)
            getPeerRequest(peer)?.let { request -> request(request.index, request.offset, request.length) }
        }
    }

    private data class PeerRequest(val index: Int, val offset: Int, val length: Int)

    private inner class FreqSortedTreeSet : TreeSet<Int>(Comparator { i1, i2 -> piecesFreqs[i1] - piecesFreqs[i2] })

    private inner class Piece(val index: Int, var got: Int = 0, val data: ByteArray = ByteArray(torrentInfo.pieceLength))

    companion object {
        private const val TAG = "PeerController"

        private const val MAX_BLOCK_LENGTH = 1 shl 14 // 2^14
    }
}