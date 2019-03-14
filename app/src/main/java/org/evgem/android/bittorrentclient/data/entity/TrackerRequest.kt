package org.evgem.android.bittorrentclient.data.entity

data class TrackerRequest(
    val infoHash: ByteArray,
    val peerId: ByteArray,
    val port: ByteArray,
    val uploaded: ByteArray,
    val downloaded: ByteArray,
    val left: ByteArray,
    val compact: ByteArray,
    val noPeerId: ByteArray?,
    val event: ByteArray?,
    val ip: ByteArray?,
    val numWant: ByteArray,
    val key: ByteArray?,
    val trackerId: ByteArray?
) {
    constructor(
        infoHash: ByteArray,
        peerId: ByteArray,
        port: Int,
        uploaded: Long,
        downloaded: Long,
        left: Long,
        compact: Boolean = true,
        noPeerId: Boolean? = null,
        event: String? = null,
        ip: String? = null,
        numWant: Int = 50,
        key: ByteArray? = null,
        trackerId: ByteArray? = null
    ) : this(
        infoHash,
        peerId,
        port.toString().toByteArray(),
        uploaded.toString().toByteArray(),
        downloaded.toString().toByteArray(),
        left.toString().toByteArray(),
        (if (compact) "1" else "0").toByteArray(),
        noPeerId?.let { if (noPeerId) "1" else "0" }?.toByteArray(),
        event?.toByteArray(),
        ip?.toByteArray(),
        numWant.toString().toByteArray(),
        key,
        trackerId
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TrackerRequest

        if (!infoHash.contentEquals(other.infoHash)) return false
        if (!peerId.contentEquals(other.peerId)) return false
        if (!port.contentEquals(other.port)) return false
        if (!uploaded.contentEquals(other.uploaded)) return false
        if (!downloaded.contentEquals(other.downloaded)) return false
        if (!left.contentEquals(other.left)) return false
        if (!compact.contentEquals(other.compact)) return false
        if (noPeerId != null) {
            if (other.noPeerId == null) return false
            if (!noPeerId.contentEquals(other.noPeerId)) return false
        } else if (other.noPeerId != null) return false
        if (event != null) {
            if (other.event == null) return false
            if (!event.contentEquals(other.event)) return false
        } else if (other.event != null) return false
        if (ip != null) {
            if (other.ip == null) return false
            if (!ip.contentEquals(other.ip)) return false
        } else if (other.ip != null) return false
        if (!numWant.contentEquals(other.numWant)) return false
        if (key != null) {
            if (other.key == null) return false
            if (!key.contentEquals(other.key)) return false
        } else if (other.key != null) return false
        if (trackerId != null) {
            if (other.trackerId == null) return false
            if (!trackerId.contentEquals(other.trackerId)) return false
        } else if (other.trackerId != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = infoHash.contentHashCode()
        result = 31 * result + peerId.contentHashCode()
        result = 31 * result + port.contentHashCode()
        result = 31 * result + uploaded.contentHashCode()
        result = 31 * result + downloaded.contentHashCode()
        result = 31 * result + left.contentHashCode()
        result = 31 * result + compact.contentHashCode()
        result = 31 * result + (noPeerId?.contentHashCode() ?: 0)
        result = 31 * result + (event?.contentHashCode() ?: 0)
        result = 31 * result + (ip?.contentHashCode() ?: 0)
        result = 31 * result + numWant.contentHashCode()
        result = 31 * result + (key?.contentHashCode() ?: 0)
        result = 31 * result + (trackerId?.contentHashCode() ?: 0)
        return result
    }
}