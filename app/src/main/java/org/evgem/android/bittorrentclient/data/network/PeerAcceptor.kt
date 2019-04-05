package org.evgem.android.bittorrentclient.data.network

import android.util.Log
import java.io.IOException
import java.lang.IllegalStateException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

/**
 * Accepts incoming connections from peers.
 */
class PeerAcceptor(private val observer: Observer) {
    private var serverSocket: ServerSocket? = null
    private var loopThread: LoopThread? = null

    val running: Boolean get() = serverSocket != null
    val port: Int? get() = serverSocket?.localPort

    interface Observer {
        fun onPeerConnected(socket: Socket)
    }

    fun start(): Boolean {
        if (running) {
            throw IllegalStateException("PeerAcceptor is already running")
        }
        try {
            serverSocket = ServerSocket(0)
            loopThread = LoopThread().apply { start() }
            return true
        } catch (e: IOException) {
            Log.e(TAG, Log.getStackTraceString(e))
        }
        return false
    }

    fun stop() {
        if (!running) {
            throw IllegalStateException("PeerAcceptor is already stopped")
        }
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, Log.getStackTraceString(e))
        }
        serverSocket = null
        if (loopThread?.isAlive == true) {
            loopThread?.interrupt()
        }
        loopThread = null
    }

    private inner class LoopThread : Thread("peer acceptor") {
        override fun run() {
            super.run()
            Log.i(TAG, "accepting started")
            try {
                while (running) {
                    serverSocket?.accept()?.let { observer.onPeerConnected(it) }
                }
            } catch (ignored: SocketException) {

            } catch (e: IOException) {
                Log.e(TAG, Log.getStackTraceString(e))
            } catch (ignored: InterruptedException) {

            }
            Log.i(TAG, "accepting stopped")
        }
    }

    companion object {
        private const val TAG = "PeerAcceptor"
    }
}