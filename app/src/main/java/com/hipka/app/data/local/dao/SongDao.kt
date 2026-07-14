package com.hipka.app.data.local.dao

import androidx.room.*
import com.hipka.app.data.local.entity.LocalSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs WHERE isLiked = 1")
    fun getLikedSongs(): Flow<List<LocalSongEntity>>

    @Query("SELECT * FROM songs WHERE isDownloaded = 1")
    fun getDownloadedSongs(): Flow<List<LocalSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: LocalSongEntity)

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: String): LocalSongEntity?
}