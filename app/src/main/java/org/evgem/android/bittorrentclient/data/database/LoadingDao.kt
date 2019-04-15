package org.evgem.android.bittorrentclient.data.database

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import org.evgem.android.bittorrentclient.data.entity.database.Loading

@Dao
interface LoadingDao {
    @Insert
    fun insert(loading: Loading)

    @Query("SELECT * FROM loading")
    fun getAll(): List<Loading>
}