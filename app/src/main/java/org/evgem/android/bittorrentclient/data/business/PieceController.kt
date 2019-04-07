package org.evgem.android.bittorrentclient.data.business

import org.evgem.android.bittorrentclient.data.entity.TorrentInfo
import java.io.File
import java.io.RandomAccessFile
import java.lang.IllegalArgumentException

/**
 * Responsible for putting pieces into files. Paths to files should be absolute.
 */
class PieceController(private val observer: Observer, private val torrentInfo: TorrentInfo) {
    private val files = ArrayList<RandomAccessFile>(torrentInfo.files.size)
    init {
        for (file in torrentInfo.files) {
            File(file.path).parentFile.mkdirs()
            val raFile = RandomAccessFile(file.path, "rw")
            raFile.setLength(file.length)
            files += raFile
        }
    }

    interface Observer {
        fun onFullyDownloaded()
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
        var written = 0
        for (file in files) {
            if (offset < file.length()) {
                file.seek(offset)
                val toFill = file.length() - offset
                if ((data.size - written) <= toFill) {
                    file.write(data, written, data.size - written)
                    break
                } else {
                    file.write(data, written, toFill.toInt())
                    written += toFill.toInt()
                    offset = 0
                }
            } else {
                offset -= file.length()
            }
        }

        piecesStatus[index] = true

        var fullyDownloaded = true
        for (downloaded in piecesStatus) {
            if (!downloaded) {
                fullyDownloaded = false
                break
            }
        }
        if (fullyDownloaded) {
            observer.onFullyDownloaded()
        }
    }
}