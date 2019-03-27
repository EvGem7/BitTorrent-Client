package org.evgem.android.bittorrentclient.data.network

import android.util.Log
import org.evgem.android.bittorrentclient.constants.HASH_SIZE
import org.evgem.android.bittorrentclient.data.entity.Peer
import org.evgem.android.bittorrentclient.util.FixedBitSet
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.nio.ByteBuffer

/**
 * Provides API for communication with peer.
 */
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
    private var onKeepAlive: (PeerCommunicator.() -> Unit)? = null
    private var onChoke: (PeerCommunicator.() -> Unit)? = null
    private var onUnchoke: (PeerCommunicator.() -> Unit)? = null
    private var onInterested: (PeerCommunicator.() -> Unit)? = null
    private var onUninterested: (PeerCommunicator.() -> Unit)? = null
    private var onHave: (PeerCommunicator.(Int) -> Unit)? = null
    private var onBitfield: (PeerCommunicator.(FixedBitSet) -> Unit)? = null
    private var onRequest: (PeerCommunicator.(Int, Int, Int) -> Unit)? = null
    private var onPiece: (PeerCommunicator.(Int, Int, ByteArray) -> Unit)? = null
    private var onCancel: (PeerCommunicator.(Int, Int, Int) -> Unit)? = null
    private var onHandshake: (PeerCommunicator.(ByteArray, ByteArray, ByteArray) -> Unit)? = null
    private var onSocketInitialized: (PeerCommunicator.() -> Unit)? = null

    private var socket: Socket? = null
    private val input: InputStream? get() = socket?.getInputStream()
    private val output: OutputStream? get() = socket?.getOutputStream()

    var running = false
        private set

    var handshakeConcluded = false
        private set

    private fun reset() {
        amChoking = true
        amInterested = false
        peerChoking = true
        peerInterested = false

        running = false
        handshakeConcluded = false

        socket = null
    }

    fun start(peer: Peer) {
        if (!running) {
            reset()
            this.peer = peer
            running = true
            InitSocketThread().start()
        } else {
            throw IllegalStateException("Error. This PeerCommunicator is already running.")
        }
    }

    fun start(socket: Socket) {
        if (!running) {
            reset()
            this.socket = socket.apply { soTimeout = 0 }

            running = true
            LoopThread().start()
        } else {
            throw IllegalStateException("Error. This PeerCommunicator is already running.")
        }
    }

    fun stop() {
        if (!running) {
            throw IllegalStateException("Error. This PeerCommunicator was already stopped.")
        }
        running = false
        socket?.close()
    }

    fun handshake(infoHash: ByteArray, peerId: ByteArray, reserved: ByteArray = ByteArray(Byte.SIZE_BITS) { 0 }) {
        if (reserved.size != RESERVED_SIZE ||
            infoHash.size != HASH_SIZE ||
            peerId.size != HASH_SIZE
        ) {
            throw IllegalArgumentException("Reserved size must be $RESERVED_SIZE. Hash and peer id sizes must be $HASH_SIZE")
        }
        output?.write(HANDSHAKE_HEADER.length)
        output?.write(HANDSHAKE_HEADER.toByteArray())
        output?.write(reserved)
        output?.write(infoHash)
        output?.write(peerId)
        output?.flush()
    }

    fun keepAlive() {
        output?.write(0.toByteArray())
        output?.flush()
    }

    fun choke() {
        output?.write(1.toByteArray())
        output?.write(CHOKE_ID)
        output?.flush()
        amChoking = true
    }

    fun unchoke() {
        output?.write(1.toByteArray())
        output?.write(UNCHOKE_ID)
        output?.flush()
        amChoking = false
    }

    fun interested() {
        output?.write(1.toByteArray())
        output?.write(INTERESTED_ID)
        output?.flush()
        amInterested = true
    }

    fun notInterested() {
        output?.write(1.toByteArray())
        output?.write(NOT_INTERESTED_ID)
        output?.flush()
        amInterested = false
    }

    fun have(pieceIndex: Int) {
        val length = 1 + Int.SIZE_BYTES
        output?.write(length.toByteArray())
        output?.write(HAVE_ID)
        output?.write(pieceIndex.toByteArray())
        output?.flush()
    }

    fun bitfield(bitSet: FixedBitSet) {
        val payload = bitSet.toByteArray()
        val length = 1 + payload.size
        output?.write(length.toByteArray())
        output?.write(BITFIELD_ID)
        output?.write(payload)
        output?.flush()
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
        output?.flush()
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
        output?.flush()
    }

    /////////////////////////
    //listener setters here//
    /////////////////////////
    fun setOnHandshakeListener(listener: (PeerCommunicator.(reserved: ByteArray, infoHash: ByteArray, peerId: ByteArray) -> Unit)?): PeerCommunicator {
        onHandshake = listener
        return this
    }

    fun setOnKeepAliveListener(listener: (PeerCommunicator.() -> Unit)?): PeerCommunicator {
        onKeepAlive = listener
        return this
    }

    fun setOnChokeListener(listener: (PeerCommunicator.() -> Unit)?): PeerCommunicator {
        onChoke = listener
        return this
    }

    fun setOnUnchokeListener(listener: (PeerCommunicator.() -> Unit)?): PeerCommunicator {
        onUnchoke = listener
        return this
    }

    fun setOnInterestedListener(listener: (PeerCommunicator.() -> Unit)?): PeerCommunicator {
        onInterested = listener
        return this
    }

    fun setOnUninterestedListener(listener: (PeerCommunicator.() -> Unit)?): PeerCommunicator {
        onUninterested = listener
        return this
    }

    fun setOnHaveListener(listener: (PeerCommunicator.(pieceIndex: Int) -> Unit)?): PeerCommunicator {
        onHave = listener
        return this
    }

    fun setOnBitfieldListener(listener: (PeerCommunicator.(FixedBitSet) -> Unit)?): PeerCommunicator {
        onBitfield = listener
        return this
    }

    fun setOnRequestListener(listener: (PeerCommunicator.(index: Int, offset: Int, length: Int) -> Unit)?): PeerCommunicator {
        onRequest = listener
        return this
    }

    fun setOnPieceListener(listener: (PeerCommunicator.(index: Int, offset: Int, data: ByteArray) -> Unit)?): PeerCommunicator {
        onPiece = listener
        return this
    }

    fun setOnCancelListener(listener: (PeerCommunicator.(index: Int, offset: Int, length: Int) -> Unit)?): PeerCommunicator {
        onCancel = listener
        return this
    }

    fun setOnSocketInitializedListener(listener: (PeerCommunicator.() -> Unit)?): PeerCommunicator {
        onSocketInitialized = listener
        return this
    }

    private inner class InitSocketThread : Thread() {
        override fun run() {
            super.run()
            peer?.let { peer ->
                try {
                    socket = Socket().apply {
                        connect(InetSocketAddress(peer.ip, peer.port), SOCKET_TIMEOUT)
                        soTimeout = 0
                    }

                    LoopThread().start()
                    onSocketInitialized?.invoke(this@PeerCommunicator)
                } catch (e: SocketTimeoutException) {
                    Log.e(tag, "Connection timed out!")
                    reset()
                } catch (e: Exception) {
                    Log.e(tag, Log.getStackTraceString(e))
                    reset()
                }
                return
            }
            Log.e(tag, "Error. Trying to init socket while target peer is null.")
            reset()
        }
    }

    private inner class LoopThread : Thread() {
        override fun run() {
            super.run()
            Log.i(tag, "Communication started")
            try {
                while (running) {
                    if (input == null) {
                        reset()
                        break
                    }
                    input?.let { input ->
                        //handshake check
                        if (!handshakeConcluded) {
                            val length = input.readFromPeer()
                            if (length != HANDSHAKE_HEADER.length) {
                                Log.e(tag, "Got incorrect length while handshake: $length")
                                reset()
                                return@let
                            }
                            val headerData = ByteArray(length)
                            input.readFromPeer(headerData)

                            for ((i: Int, c: Char) in HANDSHAKE_HEADER.withIndex()) {
                                if (headerData[i] != c.toByte()) {
                                    Log.e(tag, "Handshake header doesn't match the protocol")
                                    reset()
                                    return@let
                                }
                            }

                            val handshakeData = ByteArray(HANDSHAKE_SIZE)
                            input.readFromPeer(handshakeData)
                            peer = getPeer(handshakeData)
                            handshakeConcluded = true
                            peer?.peerId?.let { peerId ->
                                val reserved = handshakeData.sliceArray(0 until RESERVED_SIZE)
                                val infoHash =
                                    handshakeData.sliceArray(INFO_HASH_HANDSHAKE_OFFSET until INFO_HASH_HANDSHAKE_OFFSET + HASH_SIZE)
                                onHandshake?.invoke(this@PeerCommunicator, reserved, infoHash, peerId)
                            }
                            return@let
                        }

                        //read length-prefix
                        val lengthData = ByteArray(Int.SIZE_BYTES)
                        input.readFromPeer(lengthData, PEER_MESSAGE_TIMEOUT)
                        val length = ByteBuffer.wrap(lengthData).int

                        //keep-alive check
                        if (length == 0) {
                            onKeepAlive?.invoke(this@PeerCommunicator)
                            return@let
                        }

                        //check messages with IDs
                        val id = input.readFromPeer()
                        when (id) {
                            CHOKE_ID -> {
                                peerChoking = true
                                onChoke?.invoke(this@PeerCommunicator)
                            }
                            UNCHOKE_ID -> {
                                peerChoking = false
                                onUnchoke?.invoke(this@PeerCommunicator)
                            }
                            INTERESTED_ID -> {
                                peerInterested = true
                                onInterested?.invoke(this@PeerCommunicator)
                            }
                            NOT_INTERESTED_ID -> {
                                peerInterested = false
                                onUninterested?.invoke(this@PeerCommunicator)
                            }
                            HAVE_ID -> {
                                val data = ByteArray(Int.SIZE_BYTES)
                                input.readFromPeer(data)
                                val pieceIndex = ByteBuffer.wrap(data).int
                                onHave?.invoke(this@PeerCommunicator, pieceIndex)
                            }
                            BITFIELD_ID -> {
                                val payloadLength = length - 1 //message length minus one id byte
                                val payload = ByteArray(payloadLength)
                                input.readFromPeer(payload)
                                onBitfield?.invoke(this@PeerCommunicator, FixedBitSet(payload))
                            }
                            in arrayOf(REQUEST_ID, CANCEL_ID) -> {
                                val data = ByteArray(Int.SIZE_BYTES * 3) //pieceIndex, begin, size integers
                                input.readFromPeer(data)
                                val pieceIndex = ByteBuffer.wrap(data, 0, Int.SIZE_BYTES).int
                                val begin = ByteBuffer.wrap(data, Int.SIZE_BYTES, Int.SIZE_BYTES).int
                                val size = ByteBuffer.wrap(data, 2 * Int.SIZE_BYTES, Int.SIZE_BYTES).int
                                when (id) {
                                    REQUEST_ID -> onRequest?.invoke(this@PeerCommunicator, pieceIndex, begin, size)
                                    CANCEL_ID -> onCancel?.invoke(this@PeerCommunicator, pieceIndex, begin, size)
                                }
                            }
                            PIECE_ID -> {
                                val metadata = ByteArray(Int.SIZE_BYTES * 2) //pieceIndex, begin
                                input.readFromPeer(metadata)
                                val pieceIndex = ByteBuffer.wrap(metadata, 0, Int.SIZE_BYTES).int
                                val begin = ByteBuffer.wrap(metadata, Int.SIZE_BYTES, Int.SIZE_BYTES).int

                                val payloadSize = length - 2 * Int.SIZE_BYTES - 1 // two integers and id byte
                                val payload = ByteArray(payloadSize)
                                input.readFromPeer(payload)
                                onPiece?.invoke(this@PeerCommunicator, pieceIndex, begin, payload)
                            }
                        }
                    }
                }
            } catch (ignored: SocketException) {

            } catch (e: Exception) {
                Log.e(tag, Log.getStackTraceString(e))
            }
            reset()
            Log.i(tag, "Communication stopped")
        }

        private fun getPeer(handshakeData: ByteArray): Peer? {
            val peerId = handshakeData.sliceArray(PEER_ID_HANDSHAKE_OFFSET until PEER_ID_HANDSHAKE_OFFSET + HASH_SIZE)
            socket?.let {
                return Peer(peerId, it.inetAddress, it.port)
            }
            return null
        }
    }

    private fun InputStream.readFromPeer(timeout: Int = SOCKET_TIMEOUT): Int {
        socket?.soTimeout = timeout
        val read = read()
        if (read == -1) {
            throw EOFException()
        }
        socket?.soTimeout = 0
        return read
    }

    private fun InputStream.readFromPeer(buffer: ByteArray, timeout: Int = SOCKET_TIMEOUT) {
        socket?.soTimeout = timeout
        var read = 0
        while (read != buffer.size) {
            val r = read(buffer, read, buffer.size - read)
            if (r == -1) {
                throw EOFException()
            }
            read += r
        }
        socket?.soTimeout = 0
    }

    private fun Int.toByteArray() = ByteBuffer.allocate(Int.SIZE_BYTES)
        .putInt(this)
        .array()

    private val tag get() = "PeerCommunicator with $peer"

    companion object {
        private const val SOCKET_TIMEOUT = 2000 //2 seconds
        private const val PEER_MESSAGE_TIMEOUT = 120_000 //2 minutes

        //handshake stuff
        private const val HANDSHAKE_HEADER = "BitTorrent protocol"
        private const val RESERVED_SIZE = 8 //reserved bytes
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