package org.evgem.android.bittorrentclient.data.database

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import org.evgem.android.bittorrentclient.data.entity.LoadingInfo

@Dao
interface LoadingInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(loadingInfo: LoadingInfo): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(loadingInfoList: List<LoadingInfo>)

    @Query("SELECT * FROM loading")
    fun getAll(): List<LoadingInfo>
}