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
    val encoding: String?
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
        }
    }

    override fun describeContents(): Int = 0

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

                return TorrentInfo(
                    announces,
                    pieceLength,
                    pieces,
                    files,
                    creationDate,
                    comment,
                    createdBy,
                    encoding
                )
            }

            override fun newArray(size: Int): Array<TorrentInfo?> {
                return Array(size) { null }
            }
        }
    }
}