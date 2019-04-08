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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import kotlin.Comparator
import kotlin.collections.ArrayList

/**
 * Controls communication with peers.
 */
class PeerController(private val master: MasterController, private val torrentInfo: TorrentInfo) {
    private val communicators = Collections.synchronizedList(
        ArrayList<PeerCommunicator>()
    )
    private val pendingPeers = LinkedBlockingQueue<Peer>()
    private val pendingPeerSockets = LinkedBlockingQueue<Socket>()

    private val piecesFreqs = Collections.synchronizedList(
        ArrayList<Int>(torrentInfo.pieces.size).apply { repeat(torrentInfo.pieces.size) { add(0) } }
    )

    private val providedPieces =
        ConcurrentHashMap<Peer, FreqsArrayList>() // map peer to piece indexes that peer provides

    private val downloadingPieces = ConcurrentHashMap<Peer, Piece>()

    private var loopThread: LoopThread? = null

    private val peerPriorities = ConcurrentHashMap<Peer, Int>()

    private val lastPieceLength: Int

    init {
        var totalSize = 0L
        torrentInfo.files.forEach { totalSize += it.length }
        val rest = (totalSize % torrentInfo.pieceLength).toInt()
        lastPieceLength = if (rest != 0) rest else torrentInfo.pieceLength
    }

    var running: Boolean = false
        private set

    var status: Status = Status.DOWNLOADING
        private set

    enum class Status { DOWNLOADING, SEEDING }

    interface MasterController {
        fun requestPeers()
        fun onPieceReceived(piece: ByteArray, index: Int)
        val piecesStatus: BooleanArray
    }

    fun start() {
        if (running) {
            throw IllegalStateException("this peer controller is already running")
        }
        synchronized(communicators) {
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
        loopThread = LoopThread().apply { start() }
    }

    fun stop() {
        if (!running) {
            throw IllegalStateException("this peer controller is already stopped")
        }
        synchronized(communicators) {
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
        }
        running = false
        if (loopThread?.isAlive == true) {
            loopThread?.interrupt()
        }
    }

    fun addPeer(peer: Peer) {
        if (running) {
            synchronized(communicators) {
                for (communicator in communicators) {
                    if (communicator.peer?.ip == peer.ip &&
                        communicator.peer?.port == peer.port
                    ) {
                        return
                    }
                }
            }
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

    @Synchronized
    private fun getPeerRequest(peer: Peer, provided: FreqsArrayList): PeerRequest? {
        downloadingPieces[peer]?.let { piece ->
            return PeerRequest(piece.index, piece.got, getBlockLength(piece))
        }

        val sorted = TreeSet<Int>(Comparator { i1, i2 ->
            val diff = piecesFreqs[i1] - piecesFreqs[i2]
            when {
                diff != 0 -> diff
                Random().nextBoolean() -> 1
                else -> -1
            }
        })
        provided.forEach { sorted.add(it) }

        for (pieceIndex in sorted) {
            if (!master.piecesStatus[pieceIndex]) { // if not downloaded
                val newPiece = Piece(pieceIndex)
                val priority = peerPriorities[peer] ?: 0
                var alreadyDownloading = false

                //TODO now several different peer may download the same piece. fix it
                for ((currentPeer, piece) in downloadingPieces) {
                    if (piece.index == pieceIndex) {
                        val currentPriority = peerPriorities[currentPeer] ?: 0
                        if (currentPriority >= priority) {
                            alreadyDownloading = true
                            break
                        } else {
                            downloadingPieces.remove(currentPeer)
                            newPiece.got = piece.got
                            newPiece.data = piece.data
                        }
                    }
                }
                if (alreadyDownloading) {
                    continue
                }
                downloadingPieces[peer] = newPiece
                return PeerRequest(pieceIndex, newPiece.got, getBlockLength(newPiece))
            }
        }
        return null
    }

    @Synchronized
    private fun notifyBlockDownloaded(peer: Peer, index: Int, offset: Int, data: ByteArray) {
        peerPriorities[peer] = 1 + (peerPriorities[peer] ?: 0)
        val piece = downloadingPieces[peer]
        if (piece == null) {
            Log.d(TAG, "got unexpected piece: $index")
            return
        }
        if (piece.index != index) {
            throw IllegalArgumentException("piece's index doesn't match downloaded block's index")
        }
        data.copyInto(piece.data, offset)
        piece.got += data.size
        
        if (piece.got == getPieceLength(index)) {
            downloadingPieces.remove(peer)

            val pieceHash: ByteArray = MessageDigest.getInstance(HASH_ALGORITHM).digest(piece.data)
            if (!pieceHash.contentEquals(torrentInfo.pieces[index])) {
                Log.e(TAG, "Piece hash is not valid. index=$index; offset=$offset")
                return
            }
            master.onPieceReceived(piece.data, index)
        }
    }

    private fun getBlockLength(piece: Piece): Int {
        val pieceLength = getPieceLength(piece.index)
        val rest = (pieceLength - piece.got) % MAX_BLOCK_LENGTH
        return if (rest == 0) MAX_BLOCK_LENGTH else rest
    }

    private fun getPieceLength(index: Int) = if (index != torrentInfo.pieces.lastIndex) {
        torrentInfo.pieceLength
    } else {
        lastPieceLength
    }

    private fun initCommunicator() = PeerCommunicator().setOnConnectionFailed {
        communicators.remove(this)
    }.setOnCommunicationStartedListener {
        peer?.let { peerPriorities[it] = 0 }
        handshake(torrentInfo.infoHash, PEER_ID)
    }.setOnCommunicationStoppedListener {
        communicators.remove(this)
        peer?.let { peer ->
            downloadingPieces.remove(peer)
            val provided = providedPieces.remove(peer) ?: return@let
            for (pieceIndex in provided) {
                --piecesFreqs[pieceIndex]
            }
            peerPriorities.remove(peer)
        }
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
        val provided = FreqsArrayList()
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
        peer?.let { peer ->
            val provided = providedPieces[peer] ?: return@setOnHaveListener
            if (provided.find { pieceIndex == it } == null) {
                provided += pieceIndex
            }
        }
    }.setOnPieceListener { index, offset, data ->
        peer?.let { peer ->
            notifyBlockDownloaded(peer, index, offset, data)
            getPeerRequest(peer)?.let { request -> request(request.index, request.offset, request.length) }
        }
    }.setOnChokeListener {
        Log.d(TAG, "${peer?.ip} choke me")
    }.setOnInterestedListener {
        Log.d(TAG, "${peer?.ip} interested in me")
    }.setOnUninterestedListener {
        Log.d(TAG, "${peer?.ip} not interested in me")
    }

    private data class PeerRequest(val index: Int, val offset: Int, val length: Int)

    private inner class Piece(
        val index: Int,
        var got: Int = 0,
        var data: ByteArray = ByteArray(getPieceLength(index))
    )

    private inner class FreqsArrayList : ArrayList<Int>(torrentInfo.pieces.size)

    private inner class LoopThread : Thread("piece availability checker") {
        override fun run() {
            super.run()
            while (running) {
                Thread.sleep(PIECE_AVAILABILITY_PERIOD)
                synchronized(piecesFreqs) {
                    for (freq in piecesFreqs) {
                        if (freq == 0) {
                            master.requestPeers()
                            Log.i(TAG, "peers requested")
                            break
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "PeerController"

        private const val MAX_BLOCK_LENGTH = 1 shl 14 // 2^14

        private const val PIECE_AVAILABILITY_PERIOD = 10_000L // 10 seconds
    }
}