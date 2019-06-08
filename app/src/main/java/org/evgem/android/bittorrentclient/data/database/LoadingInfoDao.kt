package org.evgem.android.bittorrentclient.data.database

import android.arch.persistence.room.*
import org.evgem.android.bittorrentclient.data.entity.LoadingInfo

@Dao
interface LoadingInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(loadingInfo: LoadingInfo): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(loadingInfoList: List<LoadingInfo>)

    @Query("SELECT * FROM loading")
    fun getAll(): List<LoadingInfo>

    @Delete
    fun delete(loadingInfo: LoadingInfo)
}