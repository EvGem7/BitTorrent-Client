package org.evgem.android.bittorrentclient.data.entity.database

import android.arch.persistence.room.*
import android.os.Parcel
import org.evgem.android.bittorrentclient.data.entity.TorrentInfo

@Entity(tableName = "loading")
@TypeConverters(Loading.TorrentInfoTypeConverter::class)
data class Loading(
    @ColumnInfo(name = "torrent_info_binary")
    var torrentInfo: TorrentInfo,

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0
) {
    class TorrentInfoTypeConverter {
        @TypeConverter
        fun toByteArray(torrentInfo: TorrentInfo): ByteArray {
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
    }
}