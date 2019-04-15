package org.evgem.android.bittorrentclient.data.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import org.evgem.android.bittorrentclient.data.entity.database.Loading

@Database(entities = [Loading::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loadingDao(): LoadingDao

    companion object {
        fun init(applicationContext: Context) {
            INSTANCE = Room.databaseBuilder(applicationContext, AppDatabase::class.java, NAME).build()
        }

        lateinit var INSTANCE: AppDatabase

        private const val NAME = "app_database"
    }
}