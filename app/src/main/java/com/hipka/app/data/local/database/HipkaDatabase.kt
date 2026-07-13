package com.hipka.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hipka.app.data.local.database.dao.SongDao
import com.hipka.app.data.local.database.entity.LocalSongEntity
import com.hipka.app.data.local.database.entity.OfflineMessageEntity
import com.hipka.app.data.local.database.entity.SearchHistoryEntity

@Database(
    entities = [LocalSongEntity::class, SearchHistoryEntity::class, OfflineMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class HipkaDatabase : RoomDatabase() {
    abstract val songDao: SongDao
}