package org.evgem.android.bittorrentclient.data.business

import android.util.Log
import org.evgem.android.bittorrentclient.data.entity.Peer
import org.evgem.android.bittorrentclient.data.entity.TorrentInfo
import org.evgem.android.bittorrentclient.data.network.PeerAcceptor
import java.lang.IllegalStateException
import java.net.Socket

/**
 * Controls other controllers: PeerController, TrackerController and PeerAcceptor.
 */
class LoadingController(torrentInfo: TorrentInfo, private val observer: Observer) :
    TrackerController.MasterController,
    PeerController.MasterController,
    PeerAcceptor.Observer,
    PieceController.Observer {
    private val trackerController = TrackerController(this, torrentInfo)
    private val peerAcceptor = PeerAcceptor(this)
    private val peerController = PeerController(this, torrentInfo)
    private val pieceController = PieceController(this, torrentInfo)

    interface Observer {
        fun onDownloaded()
    }

    fun start(): Boolean {
        if (
            peerAcceptor.running ||
            peerController.running ||
            trackerController.running
        ) {
            Log.e(TAG, "trying to run something already running")
            return false
        }
        try {
            if (!peerAcceptor.start()) {
                Log.e(TAG, "cannot start peer acceptor")
                return false
            }
            trackerController.start()
            peerController.start()
            return true
        } catch (e: IllegalStateException) {
            Log.e(TAG, Log.getStackTraceString(e))
            return false
        }
    }

    fun stop(): Boolean {
        if (
            !peerAcceptor.running ||
            !peerController.running ||
            !trackerController.running
        ) {
            Log.e(TAG, "trying to stop something already stopped")
            return false
        }
        return try {
            trackerController.stop()
            peerController.stop()
            peerAcceptor.stop()
            true
        } catch (e: IllegalStateException) {
            Log.e(TAG, Log.getStackTraceString(e))
            false
        }
    }

    override fun onPieceReceived(piece: ByteArray, index: Int) {
        pieceController.addPiece(piece, index)
    }

    override fun onPeersObtained(peers: List<Peer>) {
        peerController.addPeers(peers)
    }

    override fun onPeerConnected(socket: Socket) {
        peerController.addPeer(socket)
    }

    override fun onFullyDownloaded() {
        observer.onDownloaded()
    }

    override val acceptingPort: Int get() = peerAcceptor.port ?: 0

    override val uploaded: Long get() = pieceController.uploaded
    override val downloaded: Long get() = pieceController.downloaded
    override val left: Long get() = pieceController.left
    override val piecesStatus: BooleanArray get() = pieceController.piecesStatus

    companion object {
        private const val TAG = "LoadingController"
    }
}