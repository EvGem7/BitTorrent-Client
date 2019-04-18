package org.evgem.android.bittorrentclient.data.entity

import android.arch.persistence.room.*
import android.os.Parcel
import org.evgem.android.bittorrentclient.util.FixedBitSet
import java.nio.ByteBuffer

/**
 * Entity class for Room Database.
 */
@Entity(tableName = "loading")
@TypeConverters(LoadingInfo.TorrentInfoTypeConverter::class)
data class LoadingInfo(
    @ColumnInfo(name = "torrent_info_binary")
    var torrentInfo: TorrentInfo,

    @ColumnInfo(name = "pieces_status")
    var piecesStatus: BooleanArray = BooleanArray(torrentInfo.pieces.size),

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0
) {
    class TorrentInfoTypeConverter {
        @TypeConverter
        fun fromTorrentInfo(torrentInfo: TorrentInfo): ByteArray {
            val parcel = Parcel.obtain()

            torrentInfo.writeToParcel(parcel, 0)
            val result = parcel.marshall()

            parcel.recycle()
            return result
        }

        @TypeConverter
        fun toTorrentInfo(byteArray: ByteArray): TorrentInfo {
            val parcel = Parcel.obtain()

            parcel.unmarshall(byteArray, 0, byteArray.size)
            parcel.setDataPosition(0)
            val result = TorrentInfo.CREATOR.createFromParcel(parcel)

            parcel.recycle()
            return result
        }

        @TypeConverter
        fun fromPiecesStatus(piecesStatus: BooleanArray): ByteArray {
            val size: Int = (piecesStatus.size + 7) / 8
            val bitSet = FixedBitSet(size)
            for ((index, status) in piecesStatus.withIndex()) {
                bitSet.bits[index] = status
            }

            return ByteBuffer.allocate(size + Int.SIZE_BYTES)
                .putInt(piecesStatus.size)
                .put(bitSet.toByteArray())
                .array()
        }

        @TypeConverter
        fun toPiecesStatus(byteArray: ByteArray): BooleanArray {
            val buffer = ByteBuffer.wrap(byteArray)
            val size = buffer.int
            val rowData = ByteArray(buffer.remaining())
            buffer.get(rowData)
            val bitSet = FixedBitSet(rowData)
            return BooleanArray(size) { index -> bitSet.bits[index] }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LoadingInfo

        if (torrentInfo != other.torrentInfo) return false
        if (!piecesStatus.contentEquals(other.piecesStatus)) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = torrentInfo.hashCode()
        result = 31 * result + piecesStatus.contentHashCode()
        result = 31 * result + id.hashCode()
        return result
    }


}