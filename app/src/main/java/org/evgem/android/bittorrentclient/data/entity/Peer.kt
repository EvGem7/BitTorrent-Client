package org.evgem.android.bittorrentclient.data.entity

import java.net.InetAddress

data class Peer(
    val peerId: ByteArray? = null,
    val ip: InetAddress,
    val port: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Peer

        if (peerId != null) {
            if (other.peerId == null) return false
            if (!peerId.contentEquals(other.peerId)) return false
        } else if (other.peerId != null) return false
        if (ip != other.ip) return false
        if (port != other.port) return false

        return true
    }

    override fun hashCode(): Int {
        var result = peerId?.contentHashCode() ?: 0
        result = 31 * result + ip.hashCode()
        result = 31 * result + port
        return result
    }
}