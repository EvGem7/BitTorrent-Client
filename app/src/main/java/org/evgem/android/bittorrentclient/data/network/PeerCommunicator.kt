package org.evgem.android.bittorrentclient.data.network

import android.util.Log
import org.evgem.android.bittorrentclient.data.entity.Peer
import java.io.InputStream
import java.io.OutputStream
import java.lang.IllegalStateException
import java.net.*
import java.util.*

class PeerCommunicator {
    private var _peer: Peer? = null
    val peer get() = _peer

    private var _amChoking = true
    private var _amInterested = false
    private var _peerChoking = true
    private var _peerInterested = false

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
    private var onHandshake: (() -> Unit)? = null

    private var socket: Socket? = null
    private val input: InputStream? get() = socket?.getInputStream()
    private val output: OutputStream? get() = socket?.getOutputStream()

    private var running = false

    val amChoking get() = _amChoking
    val amInterested get() = _amInterested
    val peerChoking get() = _peerChoking
    val peerInterested get() = _peerInterested

    fun start(peer: Peer) {
        if (!running) {
            _peer = peer
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

    fun handshake() {

    }

    fun keepAlive() {

    }

    fun choke() {

    }

    fun unchoke() {

    }

    fun interested() {

    }

    fun uninterested() {

    }

    fun have(pieceIndex: Int) {

    }

    //TODO BitSet's methods valueOf(byte[]) and toByteArray() requires API 19. make own methods for API 16 support
    fun bitfield(bitSet: BitSet) {

    }

    fun request(pieceIndex: Int, offset: Int, length: Int) {

    }

    fun piece(pieceIndex: Int, offset: Int, data: ByteArray) {

    }

    fun cancel(pieceIndex: Int, offset: Int, length: Int) {

    }

    /////////////////////////
    //listener setters here//
    /////////////////////////
    fun setOnHandshakeListener(listener: (() -> Unit)?): PeerCommunicator {
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
                }
                return
            }
            Log.e(TAG, "Error. Trying to start handshake while target peer is null.")
        }
    }

    private inner class LoopThread : Thread() {
        override fun run() {
            super.run()
            try {
                while (running) {
                    //TODO some logic here
                }
            } catch (e: SocketException) {
                //TODO in case of stop
            }
        }
    }

    companion object {
        private const val TAG = "PeerCommunicator"
        private const val SOCKET_TIMEOUT = 2000 //2 seconds
    }
}