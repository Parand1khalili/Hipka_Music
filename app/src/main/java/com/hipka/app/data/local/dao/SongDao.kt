package com.hipka.app.data.local.dao

import androidx.room.*
import com.hipka.app.data.local.entity.LocalSongEntity
import com.hipka.app.data.local.entity.RecentSongEntity
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

    @Query("UPDATE songs SET isLiked = :isLiked WHERE id = :songId")
    suspend fun updateLikeStatus(songId: String, isLiked: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSong(recentSong: RecentSongEntity)

    // گرفتن لیست آهنگ‌های اخیر با استفاده از JOIN روی جدول songs
    // مرتب‌شده بر اساس جدیدترین زمان پخش (timestamp)
    @Query("""
        SELECT s.* FROM songs s 
        INNER JOIN recent_songs r ON s.id = r.songId 
        ORDER BY r.timestamp DESC
    """)
    fun getRecentlyPlayedSongs(): Flow<List<LocalSongEntity>>

    @Query("SELECT id FROM songs WHERE isLiked = 1")
    fun getLikedSongIds(): Flow<List<String>>

}