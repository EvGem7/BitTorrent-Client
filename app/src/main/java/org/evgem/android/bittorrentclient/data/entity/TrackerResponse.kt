package org.evgem.android.bittorrentclient.data.entity

data class TrackerResponse(
    val failureReason: String? = null,
    val warningMessage: String? = null,
    val interval: Int? = null,
    val minInterval: Int? = null,
    val trackerId: ByteArray? = null,
    val complete: Int? = null,
    val incomplete: Int? = null,
    val peers: List<Peer>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TrackerResponse

        if (failureReason != other.failureReason) return false
        if (warningMessage != other.warningMessage) return false
        if (interval != other.interval) return false
        if (minInterval != other.minInterval) return false
        if (trackerId != null) {
            if (other.trackerId == null) return false
            if (!trackerId.contentEquals(other.trackerId)) return false
        } else if (other.trackerId != null) return false
        if (complete != other.complete) return false
        if (incomplete != other.incomplete) return false
        if (peers != other.peers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = failureReason?.hashCode() ?: 0
        result = 31 * result + (warningMessage?.hashCode() ?: 0)
        result = 31 * result + (interval ?: 0)
        result = 31 * result + (minInterval ?: 0)
        result = 31 * result + (trackerId?.contentHashCode() ?: 0)
        result = 31 * result + (complete ?: 0)
        result = 31 * result + (incomplete ?: 0)
        result = 31 * result + (peers?.hashCode() ?: 0)
        return result
    }
}