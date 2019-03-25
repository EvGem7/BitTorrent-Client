package org.evgem.android.bittorrentclient.data.business

import org.evgem.android.bittorrentclient.data.entity.Peer
import org.evgem.android.bittorrentclient.data.entity.TorrentInfo

class PeerController(private val torrentInfo: TorrentInfo) : TrackerController.Observer {
    private val peers = ArrayList<Peer>()

    override fun onPeersObtained(peers: List<Peer>) {
        for (peer in peers) {
            addPeer(peer)
        }
    }

    private fun addPeer(peer: Peer) {
        TODO()
    }
}