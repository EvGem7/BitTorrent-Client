package org.evgem.android.bittorrentclient.data.entity

import android.os.Parcel
import android.os.Parcelable
import org.evgem.android.bittorrentclient.constants.HASH_SIZE
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

data class TorrentInfo(
    val announces: Set<String>, //for tracker requests

    //for downloading
    val pieceLength: Int,
    val pieces: List<ByteArray>,
    val files: List<File>,

    //additional information
    val creationDate: Date?,
    val comment: String?,
    val createdBy: String?,
    val encoding: String?,

    //hash of info dictionary
    val infoHash: ByteArray
) : Parcelable {
    data class File(
        val path: String,
        val length: Long
    )

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.let {
            it.writeInt(announces.size)
            for (announce in announces) {
                it.writeString(announce)
            }

            it.writeInt(pieceLength)

            it.writeInt(pieces.size)
            for (piece in pieces) {
                it.writeByteArray(piece)
            }

            it.writeInt(files.size)
            for (file in files) {
                it.writeString(file.path)
                it.writeLong(file.length)
            }

            it.writeLong(creationDate?.time ?: -1)
            it.writeString(comment)
            it.writeString(createdBy)
            it.writeString(encoding)

            it.writeByteArray(infoHash)
        }
    }

    override fun describeContents(): Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TorrentInfo

        if (announces != other.announces) return false
        if (pieceLength != other.pieceLength) return false
        if (pieces != other.pieces) return false
        if (files != other.files) return false
        if (creationDate != other.creationDate) return false
        if (comment != other.comment) return false
        if (createdBy != other.createdBy) return false
        if (encoding != other.encoding) return false
        if (!infoHash.contentEquals(other.infoHash)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = announces.hashCode()
        result = 31 * result + pieceLength
        result = 31 * result + pieces.hashCode()
        result = 31 * result + files.hashCode()
        result = 31 * result + (creationDate?.hashCode() ?: 0)
        result = 31 * result + (comment?.hashCode() ?: 0)
        result = 31 * result + (createdBy?.hashCode() ?: 0)
        result = 31 * result + (encoding?.hashCode() ?: 0)
        result = 31 * result + infoHash.contentHashCode()
        return result
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<TorrentInfo> {
            override fun createFromParcel(source: Parcel?): TorrentInfo? {
                if (source == null) {
                    return null
                }
                var size = source.readInt()
                val announces = HashSet<String>()
                while (size-- > 0) {
                    announces.add(source.readString() ?: return null)
                }

                val pieceLength = source.readInt()

                size = source.readInt()
                val pieces = ArrayList<ByteArray>()
                while (size-- > 0) {
                    val piece = ByteArray(HASH_SIZE)
                    source.readByteArray(piece)
                    pieces.add(piece)
                }

                size = source.readInt()
                val files = ArrayList<File>()
                while (size-- > 0) {
                    val path = source.readString() ?: return null
                    val length = source.readLong()
                    files.add(File(path, length))
                }

                val creationDate = source.readLong().let {
                    if (it == -1L) {
                        null
                    } else {
                        Date(it)
                    }
                }
                val comment = source.readString()
                val createdBy = source.readString()
                val encoding = source.readString()

                val infoHash = ByteArray(HASH_SIZE)
                source.readByteArray(infoHash)

                return TorrentInfo(
                    announces,
                    pieceLength,
                    pieces,
                    files,
                    creationDate,
                    comment,
                    createdBy,
                    encoding,
                    infoHash
                )
            }

            override fun newArray(size: Int): Array<TorrentInfo?> {
                return Array(size) { null }
            }
        }
    }
}