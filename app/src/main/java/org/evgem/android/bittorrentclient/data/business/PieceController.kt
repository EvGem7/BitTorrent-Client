package org.evgem.android.bittorrentclient.data.business

import android.util.Log
import org.evgem.android.bittorrentclient.constants.HASH_ALGORITHM
import org.evgem.android.bittorrentclient.data.entity.TorrentInfo
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.Executors

/**
 * Responsible for putting pieces into files. Paths to files should be absolute.
 */
class PieceController(
    private val observer: Observer,
    private val torrentInfo: TorrentInfo,
    val piecesStatus: BooleanArray
) {
    private val files = ArrayList<RandomAccessFile>(torrentInfo.files.size)

    private val messageDigest = MessageDigest.getInstance(HASH_ALGORITHM)

    val isDownloaded: Boolean get() = downloaded == torrentInfo.totalSize

    val uploaded: Long get() = 0 // TODO

    var downloaded: Long

    val left: Long get() = torrentInfo.totalSize - downloaded

    val pool = Executors.newFixedThreadPool(1)

    init {
        for (file in torrentInfo.files) {
            File(file.path).parentFile.mkdirs()
            val raFile = RandomAccessFile(file.path, "rw")
            raFile.setLength(file.length)
            files += raFile
        }

        downloaded = 0L
        for ((index, gotPiece) in piecesStatus.withIndex()) {
            if (gotPiece) {
                downloaded += if (index != piecesStatus.lastIndex) {
                    torrentInfo.pieceLength
                } else {
                    torrentInfo.lastPieceLength
                }
            }
        }
    }

    interface Observer {
        fun onFullyDownloaded()
        fun notifyBadPiece(index: Int)
    }

    fun addPiece(data: ByteArray, index: Int) {
        if (index >= torrentInfo.pieces.size) {
            throw IllegalArgumentException("too large index: $index")
        }

        piecesStatus[index] = true

        pool.submit {
            val pieceHash: ByteArray = messageDigest.digest(data)
            if (!pieceHash.contentEquals(torrentInfo.pieces[index])) {
                Log.e(TAG, "Piece hash is not valid. index=$index")
                piecesStatus[index] = false
                observer.notifyBadPiece(index)
                return@submit
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

            downloaded += data.size
            if (downloaded == torrentInfo.totalSize) {
                observer.onFullyDownloaded()
                pool.shutdown()
            }
        }
    }

    companion object {
        private const val TAG = "PieceController"
    }
}