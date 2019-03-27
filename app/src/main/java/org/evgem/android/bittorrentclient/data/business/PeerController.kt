package org.evgem.android.bittorrentclient.data.business

import org.evgem.android.bittorrentclient.data.entity.Peer
import org.evgem.android.bittorrentclient.data.entity.TorrentInfo

/**
 * Controls communication with peers.
 */
class PeerController(private val torrentInfo: TorrentInfo) {
    private val peers = ArrayList<Peer>()

    fun addPeer(peer: Peer) {
        TODO()
    }

    fun addPeers(peers: List<Peer>) {
        for (peer in peers) {
            addPeer(peer)
        }
    }
}