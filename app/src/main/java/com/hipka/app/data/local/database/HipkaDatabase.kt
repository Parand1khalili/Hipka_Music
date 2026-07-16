package com.hipka.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hipka.app.data.local.dao.SearchHistoryDao
import com.hipka.app.data.local.dao.SongDao
import com.hipka.app.data.local.entity.OfflineMessageEntity
import com.hipka.app.data.local.entity.SearchHistoryEntity
import com.hipka.app.data.local.entity.LocalSongEntity
import com.hipka.app.data.local.entity.RecentSongEntity
@Database(
    entities = [LocalSongEntity::class, SearchHistoryEntity::class, OfflineMessageEntity::class, RecentSongEntity::class],
    version = 2,
    exportSchema = false
)
abstract class HipkaDatabase : RoomDatabase() {
    abstract val songDao: SongDao
    abstract val searchHistoryDao: SearchHistoryDao
}