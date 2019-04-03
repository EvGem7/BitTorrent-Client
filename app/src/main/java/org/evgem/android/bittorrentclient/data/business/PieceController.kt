package org.evgem.android.bittorrentclient.data.business

import org.evgem.android.bittorrentclient.data.entity.TorrentInfo
import java.io.RandomAccessFile
import java.lang.IllegalArgumentException

/**
 * Responsible for putting pieces into files. Paths to files should be absolute.
 */
class PieceController(private val torrentInfo: TorrentInfo) {
    private val files = ArrayList<RandomAccessFile>(torrentInfo.files.size)
    init {
        for (file in torrentInfo.files) {
            val raFile = RandomAccessFile(file.path, "rw")
            raFile.setLength(file.length)
            files += raFile
        }
    }

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
        if (index >= torrentInfo.pieces.size) {
            throw IllegalArgumentException("too large index: $index")
        }

        var offset = index.toLong() * torrentInfo.pieceLength
        var file: RandomAccessFile? = null
        for (f in files) {
            if (offset < f.length()) {
                file = f
                break
            } else {
                offset -= f.length()
            }
        }
        if (file == null) {
            throw UnknownError("cannot find file containing this piece. index=$index")
        }
        file.seek(offset)
        file.write(data)

        piecesStatus[index] = true
    }
}