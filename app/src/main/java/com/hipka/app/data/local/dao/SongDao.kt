package com.hipka.app.data.local.dao

import androidx.room.*
import com.hipka.app.data.local.entity.LocalSongEntity
import com.hipka.app.data.local.entity.LocalUserLikeEntity
import com.hipka.app.data.local.entity.RecentSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    // --- متدهای عمومی و دانلود ---
    @Query("SELECT * FROM songs WHERE isDownloaded = 1")
    fun getDownloadedSongs(): Flow<List<LocalSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: LocalSongEntity)

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: String): LocalSongEntity?

    @Query("UPDATE songs SET isLiked = :isLiked WHERE id = :songId")
    suspend fun updateLikeStatus(songId: String, isLiked: Boolean)

    @Query("UPDATE songs SET isDownloaded = :isDownloaded, localFilePath = :localFilePath WHERE id = :songId")
    suspend fun updateDownloadStatus(songId: String, isDownloaded: Boolean, localFilePath: String?)

    @Query("SELECT id FROM songs WHERE isDownloaded = 1")
    fun getDownloadedSongIds(): Flow<List<String>>

    // --- کش کاتالوگ آهنگ‌ها برای حالت آفلاین ---

    /** همه آهنگ‌های کش‌شده — منبع نمایش لیست‌ها وقتی اینترنت نیست */
    @Query("SELECT * FROM songs ORDER BY playCount DESC")
    fun getAllCachedSongs(): Flow<List<LocalSongEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongsIfAbsent(songs: List<LocalSongEntity>)

    /**
     * فقط فیلدهایی که مالکشان سرور است به‌روزرسانی می‌شوند. عمداً isDownloaded،
     * localFilePath و isLiked دست‌نخورده می‌مانند تا رفرش کاتالوگ، دانلودها و
     * لایک‌های محلی کاربر را پاک نکند.
     */
    @Query("""
        UPDATE songs SET
            title = :title,
            artistName = :artistName,
            coverImageUrl = :coverImageUrl,
            audioUrl = :audioUrl,
            playCount = :playCount,
            likesCount = :likesCount,
            releaseDate = :releaseDate
        WHERE id = :id
    """)
    suspend fun updateSongMetadata(
        id: String,
        title: String,
        artistName: String,
        coverImageUrl: String,
        audioUrl: String,
        playCount: Int,
        likesCount: Long,
        releaseDate: String
    )

    /** ذخیره کاتالوگ تازه بدون از دست رفتن وضعیت محلی (دانلود/لایک) */
    @Transaction
    suspend fun cacheSongs(songs: List<LocalSongEntity>) {
        insertSongsIfAbsent(songs)
        songs.forEach { song ->
            updateSongMetadata(
                id = song.id,
                title = song.title,
                artistName = song.artistName,
                coverImageUrl = song.coverImageUrl,
                audioUrl = song.audioUrl,
                playCount = song.playCount,
                likesCount = song.likesCount,
                releaseDate = song.releaseDate
            )
        }
    }

    // متدهای قدیمی لایک (جهت سازگاری با بخش‌هایی که هنوز آپدیت نشده‌اند)
    @Query("SELECT * FROM songs WHERE isLiked = 1")
    fun getLikedSongs(): Flow<List<LocalSongEntity>>

    @Query("SELECT id FROM songs WHERE isLiked = 1")
    fun getLikedSongIds(): Flow<List<String>>

    // --- آهنگ‌های اخیر (ایزوله شده برای هر کاربر) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSong(recentSong: RecentSongEntity)

    @Query("""
        SELECT s.* FROM songs s 
        INNER JOIN recent_songs r ON s.id = r.songId 
        WHERE r.userId = :userId
        ORDER BY r.timestamp DESC
    """)
    fun getRecentlyPlayedSongsForUser(userId: String): Flow<List<LocalSongEntity>>

    // --- لایک‌ها (ایزوله شده برای هر کاربر) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserLike(like: LocalUserLikeEntity)

    @Query("DELETE FROM local_user_likes WHERE userId = :userId AND songId = :songId")
    suspend fun deleteUserLike(userId: String, songId: String)

    @Query("SELECT songs.* FROM songs INNER JOIN local_user_likes ON songs.id = local_user_likes.songId WHERE local_user_likes.userId = :userId")
    fun getLikedSongsForUser(userId: String): Flow<List<LocalSongEntity>>

    @Query("SELECT songId FROM local_user_likes WHERE userId = :userId")
    fun getLikedSongIdsForUser(userId: String): Flow<List<String>>

    @Query("SELECT COUNT(*) > 0 FROM local_user_likes WHERE userId = :userId AND songId = :songId")
    suspend fun isSongLikedByUser(userId: String, songId: String): Boolean
}