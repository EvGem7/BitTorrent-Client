package org.evgem.android.bittorrentclient.data.network

import android.util.Log
import org.evgem.android.bittorrentclient.data.entity.Peer
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.net.*
import java.nio.ByteBuffer
import java.util.*

class PeerCommunicator {
    var peer: Peer? = null
        private set

    var amChoking = true
        private set
    var amInterested = false
        private set
    var peerChoking = true
        private set
    var peerInterested = false
        private set

    //listeners for messages from peer
    private var onKeepAlive: (() -> Unit)? = null
    private var onChoke: (() -> Unit)? = null
    private var onUnchoke: (() -> Unit)? = null
    private var onInterested: (() -> Unit)? = null
    private var onUninterested: (() -> Unit)? = null
    private var onHave: ((Int) -> Unit)? = null
    private var onBitfield: ((BitSet) -> Unit)? = null
    private var onRequest: ((Int, Int, Int) -> Unit)? = null
    private var onPiece: ((Int, Int, ByteArray) -> Unit)? = null
    private var onCancel: ((Int, Int, Int) -> Unit)? = null
    private var onHandshake: ((ByteArray, ByteArray, ByteArray) -> Unit)? = null

    private var socket: Socket? = null
    private val input: InputStream? get() = socket?.getInputStream()
    private val output: OutputStream? get() = socket?.getOutputStream()

    var running = false
        private set

    fun start(peer: Peer) {
        if (!running) {
            this.peer = peer
            running = true
            InitSocketThread().start()
        } else {
            throw IllegalStateException("Error. This PeerCommunicator is already running.")
        }
    }

    fun start(socket: Socket) {
        if (!running) {
            this.socket = socket
            running = true
            LoopThread().start()
        } else {
            throw IllegalStateException("Error. This PeerCommunicator is already running.")
        }
    }

    fun stop() {
        running = false
        socket?.close()
        socket = null
    }

    fun handshake(infoHash: ByteArray, peerId: ByteArray, reserved: ByteArray) {
        if (reserved.size != RESERVED_SIZE ||
            infoHash.size != HASH_SIZE ||
            peerId.size != HASH_SIZE
        ) {
            throw IllegalArgumentException("Reserved size must be $RESERVED_SIZE. Hash and peer id sizes must be $HASH_SIZE")
        }
        output?.write(HANDSHAKE_HEADER.length.toByteArray())
        output?.write(HANDSHAKE_HEADER.toByteArray())
        output?.write(reserved)
        output?.write(infoHash)
        output?.write(peerId)
    }

    fun keepAlive() {
        output?.write(0.toByteArray())
    }

    fun choke() {
        output?.write(1.toByteArray())
        output?.write(CHOKE_ID)
        amChoking = true
    }

    fun unchoke() {
        output?.write(1.toByteArray())
        output?.write(UNCHOKE_ID)
        amChoking = false
    }

    fun interested() {
        output?.write(1.toByteArray())
        output?.write(INTERESTED_ID)
        amInterested = true
    }

    fun uninterested() {
        output?.write(1.toByteArray())
        output?.write(NOT_INTERESTED_ID)
        amInterested = false
    }

    fun have(pieceIndex: Int) {
        val length = 1 + Int.SIZE_BYTES
        output?.write(length.toByteArray())
        output?.write(HAVE_ID)
        output?.write(pieceIndex.toByteArray())
    }

    //TODO BitSet's methods valueOf(byte[]) and toByteArray() requires API 19. make own methods for API 16 support
    fun bitfield(bitSet: BitSet) {
        val payload = bitSet.toByteArray()
        val length = 1 + payload.size
        output?.write(length.toByteArray())
        output?.write(BITFIELD_ID)
        output?.write(payload)
    }

    fun request(pieceIndex: Int, offset: Int, length: Int) {
        requestOrCancel(pieceIndex, offset, length, REQUEST_ID)
    }

    fun piece(pieceIndex: Int, offset: Int, data: ByteArray) {
        val metadata = ByteBuffer.allocate(2 * Int.SIZE_BYTES)
            .putInt(pieceIndex)
            .putInt(offset)
            .array()
        val length = 1 + metadata.size + data.size
        output?.write(length.toByteArray())
        output?.write(PIECE_ID)
        output?.write(metadata)
        output?.write(data)
    }

    fun cancel(pieceIndex: Int, offset: Int, length: Int) {
        requestOrCancel(pieceIndex, offset, length, CANCEL_ID)
    }

    private fun requestOrCancel(pieceIndex: Int, offset: Int, length: Int, id: Int) {
        if (id != REQUEST_ID && id != CANCEL_ID) {
            throw IllegalArgumentException("ID must be REQUEST_ID or CANCEL_ID")
        }
        val payload = ByteBuffer.allocate(3 * Int.SIZE_BYTES)
            .putInt(pieceIndex)
            .putInt(offset)
            .putInt(length)
            .array()
        val prefixLength = 1 + payload.size
        output?.write(prefixLength.toByteArray())
        output?.write(id)
        output?.write(payload)
    }

    /////////////////////////
    //listener setters here//
    /////////////////////////
    fun setOnHandshakeListener(listener: ((reserved: ByteArray, infoHash: ByteArray, peerId: ByteArray) -> Unit)?): PeerCommunicator {
        onHandshake = listener
        return this
    }

    fun setOnKeepAliveListener(listener: (() -> Unit)?): PeerCommunicator {
        onKeepAlive = listener
        return this
    }

    fun setOnChokeListener(listener: (() -> Unit)?): PeerCommunicator {
        onChoke = listener
        return this
    }

    fun setOnUnchokeListener(listener: (() -> Unit)?): PeerCommunicator {
        onUnchoke = listener
        return this
    }

    fun setOnInterestedListener(listener: (() -> Unit)?): PeerCommunicator {
        onInterested = listener
        return this
    }

    fun setOnUninterestedListener(listener: (() -> Unit)?): PeerCommunicator {
        onUninterested = listener
        return this
    }

    fun setOnHaveListener(listener: ((pieceIndex: Int) -> Unit)?): PeerCommunicator {
        onHave = listener
        return this
    }

    fun setOnBitfieldListener(listener: ((BitSet) -> Unit)?): PeerCommunicator {
        onBitfield = listener
        return this
    }

    fun setOnRequestListener(listener: ((index: Int, offset: Int, length: Int) -> Unit)?): PeerCommunicator {
        onRequest = listener
        return this
    }

    fun setOnPieceListener(listener: ((index: Int, offset: Int, data: ByteArray) -> Unit)?): PeerCommunicator {
        onPiece = listener
        return this
    }

    fun setOnCancelListener(listener: ((index: Int, offset: Int, length: Int) -> Unit)?): PeerCommunicator {
        onCancel = listener
        return this
    }

    private inner class InitSocketThread : Thread() {
        override fun run() {
            super.run()
            peer?.let { peer ->
                try {
                    socket = Socket().apply { connect(InetSocketAddress(peer.ip, peer.port), SOCKET_TIMEOUT) }
                    LoopThread().start()
                } catch (e: SocketTimeoutException) {
                    Log.e(TAG, Log.getStackTraceString(e))
                    this@PeerCommunicator.stop()
                }
                return
            }
            Log.e(TAG, "Error. Trying to init socket while target peer is null.")
        }
    }

    private inner class LoopThread : Thread() {
        override fun run() {
            super.run()
            try {
                while (running) {
                    if (input == null) {
                        running = false
                        break
                    }
                    input?.let { input ->
                        //read length-prefix
                        val lengthData = ByteArray(Int.SIZE_BYTES)
                        if (!input.readFromPeer(lengthData, Int.SIZE_BYTES)) {
                            running = false
                            return@let
                        }
                        val length = ByteBuffer.wrap(lengthData).int

                        //keep-alive check
                        if (length == 0) {
                            onKeepAlive?.invoke()
                            return@let
                        }

                        //handshake check
                        if (HANDSHAKE_HEADER.length == length) {
                            val headerData = ByteArray(length)
                            input.mark(length)
                            if (!input.readFromPeer(headerData, length)) {
                                running = false
                                return@let
                            }

                            var isHandshake = true
                            for ((i: Int, c: Char) in HANDSHAKE_HEADER.withIndex()) {
                                if (headerData[i] != c.toByte()) {
                                    isHandshake = false
                                    break
                                }
                            }

                            if (isHandshake) {
                                val handshakeData = ByteArray(HANDSHAKE_SIZE)
                                if (!input.readFromPeer(handshakeData, HANDSHAKE_SIZE)) {
                                    Log.e(TAG, "Cannot perform handshake")
                                    running = false
                                    return@let
                                }
                                peer = getPeer(handshakeData)
                                peer?.peerId?.let { peerId ->
                                    val reserved = handshakeData.sliceArray(0 until RESERVED_SIZE)
                                    val infoHash =
                                        handshakeData.sliceArray(INFO_HASH_HANDSHAKE_OFFSET until INFO_HASH_HANDSHAKE_OFFSET + HASH_SIZE)
                                    onHandshake?.invoke(reserved, infoHash, peerId)
                                }
                                return@let
                            } else {
                                input.reset()
                            }
                        }

                        //check messages with IDs
                        val id = input.read()
                        when (id) {
                            -1 -> running = false
                            CHOKE_ID -> {
                                peerChoking = true
                                onChoke?.invoke()
                            }
                            UNCHOKE_ID -> {
                                peerChoking = false
                                onUnchoke?.invoke()
                            }
                            INTERESTED_ID -> {
                                peerInterested = true
                                onInterested?.invoke()
                            }
                            NOT_INTERESTED_ID -> {
                                peerInterested = false
                                onUninterested?.invoke()
                            }
                            HAVE_ID -> {
                                val data = ByteArray(Int.SIZE_BYTES)
                                if (!input.readFromPeer(data, Int.SIZE_BYTES)) {
                                    running = false
                                    return@let
                                }
                                val pieceIndex = ByteBuffer.wrap(data).int
                                onHave?.invoke(pieceIndex)
                            }
                            BITFIELD_ID -> {
                                val payloadLength = length - 1 //message length minus one id byte
                                val payload = ByteArray(payloadLength)
                                if (!input.readFromPeer(payload, payloadLength)) {
                                    running = false
                                    return@let
                                }
                                onBitfield?.invoke(BitSet.valueOf(payload))
                            }
                            in arrayOf(REQUEST_ID, CANCEL_ID) -> {
                                val data = ByteArray(Int.SIZE_BYTES * 3) //pieceIndex, begin, size integers
                                if (!input.readFromPeer(data, data.size)) {
                                    running = false
                                    return@let
                                }
                                val pieceIndex = ByteBuffer.wrap(data, 0, Int.SIZE_BYTES).int
                                val begin = ByteBuffer.wrap(data, Int.SIZE_BYTES, Int.SIZE_BYTES).int
                                val size = ByteBuffer.wrap(data, 2 * Int.SIZE_BYTES, Int.SIZE_BYTES).int
                                when (id) {
                                    REQUEST_ID -> onRequest?.invoke(pieceIndex, begin, size)
                                    CANCEL_ID -> onCancel?.invoke(pieceIndex, begin, size)
                                }
                            }
                            PIECE_ID -> {
                                val metadata = ByteArray(Int.SIZE_BYTES * 2) //pieceIndex, begin
                                if (!input.readFromPeer(metadata, metadata.size)) {
                                    running = false
                                    return@let
                                }
                                val pieceIndex = ByteBuffer.wrap(metadata, 0, Int.SIZE_BYTES).int
                                val begin = ByteBuffer.wrap(metadata, Int.SIZE_BYTES, Int.SIZE_BYTES).int

                                val payloadSize = length - 2 * Int.SIZE_BYTES - 1 // two integers and id byte
                                val payload = ByteArray(payloadSize)
                                if (!input.readFromPeer(payload, payloadSize)) {
                                    running = false
                                    return@let
                                }
                                onPiece?.invoke(pieceIndex, begin, payload)
                            }
                        }
                    }
                }
            } catch (e: SocketException) {
                //TODO in case of stop
            } catch (e: Exception) {
                Log.e(TAG, Log.getStackTraceString(e))
            }
        }

        private fun getPeer(handshakeData: ByteArray): Peer? {
            val peerId = handshakeData.sliceArray(PEER_ID_HANDSHAKE_OFFSET until PEER_ID_HANDSHAKE_OFFSET + HASH_SIZE)
            socket?.let {
                return Peer(peerId, it.inetAddress, it.port)
            }
            return null
        }
    }

    private fun InputStream.readFromPeer(buffer: ByteArray, length: Int): Boolean {
        val read = read(buffer, 0, length)
        if (read != length) {
            Log.e(TAG, "Cannot read $length bytes from peer. There are only $read bytes.")
            return false
        }
        return true
    }

    private fun Int.toByteArray() = ByteBuffer.allocate(Int.SIZE_BYTES)
        .putInt(this)
        .array()

    companion object {
        private const val TAG = "PeerCommunicator"
        private const val SOCKET_TIMEOUT = 2000 //2 seconds

        //handshake stuff
        private const val HANDSHAKE_HEADER = "BitTorrent protocol"
        private const val RESERVED_SIZE = 8 //reserved bytes
        private const val HASH_SIZE = 20 //sha1 hash size
        private const val HANDSHAKE_SIZE = RESERVED_SIZE + 2 * HASH_SIZE
        private const val INFO_HASH_HANDSHAKE_OFFSET = RESERVED_SIZE
        private const val PEER_ID_HANDSHAKE_OFFSET = INFO_HASH_HANDSHAKE_OFFSET + HASH_SIZE

        //message IDs
        private const val CHOKE_ID = 0
        private const val UNCHOKE_ID = 1
        private const val INTERESTED_ID = 2
        private const val NOT_INTERESTED_ID = 3
        private const val HAVE_ID = 4
        private const val BITFIELD_ID = 5
        private const val REQUEST_ID = 6
        private const val PIECE_ID = 7
        private const val CANCEL_ID = 8
    }
}