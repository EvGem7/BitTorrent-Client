package org.evgem.android.bittorrentclient.data.business

import android.util.Log
import org.evgem.android.bittorrentclient.data.entity.LoadingInfo
import org.evgem.android.bittorrentclient.data.entity.Peer
import org.evgem.android.bittorrentclient.data.entity.TorrentInfo
import org.evgem.android.bittorrentclient.data.network.PeerAcceptor
import java.net.Socket

/**
 * Controls other controllers: PeerController, TrackerController and PeerAcceptor.
 */
class LoadingController private constructor(
    val torrentInfo: TorrentInfo,
    private val observer: Observer,
    piecesStatus: BooleanArray
) : TrackerController.MasterController,
    PeerController.MasterController,
    PeerAcceptor.Observer,
    PieceController.Observer {

    private val trackerController = TrackerController(this, torrentInfo)
    private val peerAcceptor = PeerAcceptor(this)
    private val peerController = PeerController(this, torrentInfo)
    private val pieceController = PieceController(this, torrentInfo, piecesStatus)

    constructor(torrentInfo: TorrentInfo, observer: Observer) : this(
        torrentInfo,
        observer,
        BooleanArray(torrentInfo.pieces.size)
    )

    constructor(loadingInfo: LoadingInfo, observer: Observer) : this(
        loadingInfo.torrentInfo,
        observer,
        loadingInfo.piecesStatus
    )

    var status: Status = Status.DOWNLOADING
        private set

    enum class Status(val value: String) {
        DOWNLOADING("Downloading"),
        SEEDING("Seeding"),
        ERROR("Error"),
        STOPPED("Stopped")
    }

    interface Observer {
        fun onDownloaded(controller: LoadingController)
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
                status = Status.ERROR
                return false
            }
            trackerController.start()
            peerController.start()
            status = if (pieceController.isDownloaded) {
                Status.SEEDING
            } else {
                Status.DOWNLOADING
            }
            return true
        } catch (e: IllegalStateException) {
            Log.e(TAG, Log.getStackTraceString(e))
            status = Status.ERROR
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
            status = Status.STOPPED
            true
        } catch (e: IllegalStateException) {
            Log.e(TAG, Log.getStackTraceString(e))
            status = Status.ERROR
            false
        }
    }

    override fun onPieceReceived(piece: ByteArray, index: Int) {
        pieceController.addPiece(piece, index)
    }

    override fun requestPeers() {
        trackerController.requestPeers()
    }

    override fun onPeersObtained(peers: List<Peer>) {
        if (!isDownloaded) {
            peerController.addPeers(peers)
        }
    }

    override fun onPeerConnected(socket: Socket) {
        peerController.addPeer(socket)
    }

    override fun onFullyDownloaded() {
        trackerController.complete()
        status = Status.SEEDING
        observer.onDownloaded(this)
    }

    override fun notifyBadPiece(index: Int) {
        peerController.reloadPiece(index)
    }

    override fun getPiece(index: Int): ByteArray? = pieceController.getPiece(index)

    override val acceptingPort: Int get() = peerAcceptor.port ?: 0

    override val uploaded: Long get() = pieceController.uploaded
    override val downloaded: Long get() = pieceController.downloaded
    override val left: Long get() = pieceController.left
    override val piecesStatus: BooleanArray get() = pieceController.piecesStatus

    override val isDownloaded: Boolean get() = pieceController.isDownloaded

    companion object {
        private const val TAG = "LoadingController"
    }
}