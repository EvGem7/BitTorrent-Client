package org.evgem.android.bittorrentclient.data.business

import org.evgem.android.bittorrentclient.data.entity.Peer
import org.evgem.android.bittorrentclient.data.entity.TorrentInfo
import org.evgem.android.bittorrentclient.data.network.PeerAcceptor
import org.evgem.android.bittorrentclient.data.network.PeerCommunicator

/**
 * Controls other controllers: PeerController, TrackerController and PeerAcceptor.
 */
class LoadingController(private val torrentInfo: TorrentInfo) : TrackerController.MasterController, PeerAcceptor.Observer {
    override fun onPeersObtained(peers: List<Peer>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPeerConnected(peer: PeerCommunicator) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}