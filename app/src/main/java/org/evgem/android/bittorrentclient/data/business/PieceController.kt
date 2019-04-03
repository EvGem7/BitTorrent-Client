package org.evgem.android.bittorrentclient.data.business

import org.evgem.android.bittorrentclient.data.entity.TorrentInfo
import java.lang.IllegalArgumentException

class PieceController(private val torrentInfo: TorrentInfo) {
    val piecesStatus = BooleanArray(torrentInfo.pieces.size)

    val uploaded: Long get() = 0 // TODO
    val downloaded: Long get() {
        var count = 0
        for (gotPiece in piecesStatus) {
            if (gotPiece) {
                ++count
            }
        }
        return count.toLong() * torrentInfo.pieceLength
    }
    val left: Long get() = torrentInfo.pieces.size.toLong() * torrentInfo.pieceLength - downloaded

    fun addPiece(data: ByteArray, index: Int) {
        if (data.size != torrentInfo.pieceLength) {
            throw IllegalArgumentException("piece size is invalid")
        }
        piecesStatus[index] = true
        TODO("not implemented")
    }
}