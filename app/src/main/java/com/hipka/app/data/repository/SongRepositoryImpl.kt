package com.hipka.app.data.repository

import com.hipka.app.core.network.NetworkMonitor
import com.hipka.app.data.local.dao.SongDao
import com.hipka.app.data.local.entity.LocalSongEntity
import com.hipka.app.data.local.entity.RecentSongEntity
import com.hipka.app.data.remote.api.SongApi
import com.hipka.app.domain.model.DataSourceState
import com.hipka.app.domain.model.Song
import com.hipka.app.domain.model.SongsResult
import com.hipka.app.domain.repository.SongRepository
import com.hipka.app.data.local.dao.SearchHistoryDao
import com.hipka.app.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.hipka.app.data.local.datastore.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.hipka.app.data.local.entity.LocalUserLikeEntity
import com.hipka.app.data.remote.api.ToggleLikeRequest
import com.hipka.app.domain.model.Artist


class SongRepositoryImpl @Inject constructor(
    private val songApi: SongApi,
    private val songDao: SongDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager
) : SongRepository {

    override fun getSongs(): Flow<List<Song>> =
        getSongsWithSource().map { it.songs }

    /**
     * استراتژی cache-first: اول کش محلی نمایش داده می‌شود تا در حالت آفلاین هم
     * چیزی برای دیدن باشد، بعد اگر اینترنت بود کاتالوگ تازه گرفته و در Room ذخیره
     * می‌شود. اگر شبکه شکست بخورد، کش قبلی حفظ می‌شود نه اینکه لیست خالی شود.
     */
    override fun getSongsWithSource(): Flow<SongsResult> = flow {
        val cached = songDao.getAllCachedSongs().first().map { it.toDomainModel() }.deduplicated()
        val isOnline = runCatching { networkMonitor.isOnline.first() }.getOrDefault(true)

        if (cached.isNotEmpty()) {
            // وقتی آنلاین هستیم، کش فقط یک نمایش سریع اولیه است و نباید بنر آفلاین
            // را روشن کند؛ فقط در حالت آفلاین حالت CACHED گزارش می‌شود.
            emit(
                SongsResult(
                    songs = cached,
                    sourceState = if (isOnline) DataSourceState.CACHED_WHILE_LOADING else DataSourceState.CACHED
                )
            )
        }

        if (!isOnline) {
            if (cached.isEmpty()) {
                emit(SongsResult(emptyList(), DataSourceState.UNAVAILABLE))
            }
            return@flow
        }

        try {
            val remoteSongs = songApi.testGetSongs()
            val songs = remoteSongs.map { dto ->
                Song(
                    id = dto.id,
                    title = dto.title,
                    artistName = dto.artistName,
                    coverImageUrl = dto.coverImageUrl,
                    audioUrl = dto.audioUrl,
                    playCount = dto.playCount ?: 0,
                    likesCount = dto.likesCount ?: 0,
                    releaseDate = dto.releaseDate ?: ""
                )
            }.deduplicated()

            // ذخیره برای دفعه بعدی که کاربر آفلاین است
            songDao.cacheSongs(songs.map { it.toEntity() })
            emit(SongsResult(songs, DataSourceState.FRESH))
        } catch (e: Exception) {
            android.util.Log.e("REPO_ERROR", "Network failed, serving cache. Cause: ${e.message}")
            emit(
                SongsResult(
                    songs = cached,
                    sourceState = if (cached.isEmpty()) DataSourceState.UNAVAILABLE else DataSourceState.CACHED
                )
            )
        }
    }

    override suspend fun getSongById(id: String): Song? {
        val localSong = songDao.getSongById(id)
        if (localSong != null) {
            return localSong.toDomainModel()
        }
        return null
    }

    override suspend fun searchSongs(query: String): List<Song> {
        if (query.isBlank()) return emptyList()
        return try {
            val orQuery = "(title.ilike.*$query*,artist_name.ilike.*$query*)"
            val remoteSongs = songApi.searchSongs(orQuery)
            remoteSongs.map { dto ->
                Song(
                    id = dto.id,
                    title = dto.title,
                    artistName = dto.artistName,
                    coverImageUrl = dto.coverImageUrl,
                    audioUrl = dto.audioUrl,
                    playCount = dto.playCount ?: 0,
                    likesCount = dto.likesCount ?: 0,
                    releaseDate = dto.releaseDate ?: ""
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("REPO_ERROR", "Search query failed. Returning empty list.")
            emptyList()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getLikedSongs(): Flow<List<Song>> {
        return sessionManager.currentUserId.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                songDao.getLikedSongsForUser(userId).map { localSongs ->
                    localSongs.map { it.toDomainModel() }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getRecentlyPlayedSongs(): Flow<List<Song>> {
        return sessionManager.currentUserId.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                songDao.getRecentlyPlayedSongsForUser(userId).map { localSongs ->
                    localSongs.map { it.toDomainModel() }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeLikedSongIds(): Flow<List<String>> {
        return sessionManager.currentUserId.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) flowOf(emptyList())
            else songDao.getLikedSongIdsForUser(userId)
        }
    }

    override suspend fun toggleAndInsertLike(song: Song) {
        val currentUserId = sessionManager.currentUserId.first()
        if (currentUserId.isNullOrBlank()) {
            android.util.Log.e("LIKE_SYSTEM", "No user logged in!")
            return
        }

        val isCurrentlyLiked = songDao.isSongLikedByUser(currentUserId, song.id)
        val newLikeStatus = !isCurrentlyLiked

        if (songDao.getSongById(song.id) == null) {
            songDao.insertSong(
                LocalSongEntity(
                    id = song.id, title = song.title, artistName = song.artistName,
                    coverImageUrl = song.coverImageUrl, audioUrl = song.audioUrl,
                    playCount = song.playCount, likesCount = song.likesCount,
                    releaseDate = song.releaseDate, isLiked = false, isDownloaded = false, localFilePath = null
                )
            )
        }

        if (newLikeStatus) {
            songDao.insertUserLike(LocalUserLikeEntity(currentUserId, song.id))
        } else {
            songDao.deleteUserLike(currentUserId, song.id)
        }

        try {
            val isOnline = networkMonitor.isOnline.first()
            if (isOnline) {
                val response = songApi.toggleSongLikeRemote(
                    ToggleLikeRequest(
                        pUserId = currentUserId,
                        pSongId = song.id,
                        pIsLiked = newLikeStatus
                    )
                )

                if (response.isSuccessful) {
                    android.util.Log.d("SUPABASE_SYNC", "Like synced with Supabase for user: $currentUserId")
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("SUPABASE_SYNC", "Supabase rejected the like! Error: $errorBody")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SUPABASE_SYNC", "Failed to sync like status: ${e.message}")
        }
    }

    override suspend fun addToRecentlyPlayed(song: Song) {
        val currentUserId = sessionManager.currentUserId.first()
        if (currentUserId.isNullOrBlank()) return

        val existingSong = songDao.getSongById(song.id)
        if (existingSong == null) {
            songDao.insertSong(
                LocalSongEntity(
                    id = song.id, title = song.title, artistName = song.artistName,
                    coverImageUrl = song.coverImageUrl, audioUrl = song.audioUrl,
                    playCount = song.playCount, likesCount = song.likesCount,
                    releaseDate = song.releaseDate, isLiked = false, isDownloaded = false, localFilePath = null
                )
            )
        }

        songDao.insertRecentSong(
            RecentSongEntity(
                userId = currentUserId,
                songId = song.id,
                timestamp = System.currentTimeMillis()
            )
        )

        try {
            val isOnline = networkMonitor.isOnline.first()
            if (isOnline) {
                songApi.incrementPlayCount(mapOf("p_song_id" to song.id))
                android.util.Log.d("SUPABASE_SYNC", "Play count incremented on server.")
            }
        } catch (e: Exception) {
            android.util.Log.e("SUPABASE_SYNC", "Failed to increment play count: ${e.message}")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getSearchHistory(): Flow<List<String>> {
        return sessionManager.currentUserId.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                searchHistoryDao.getSearchHistoryForUser(userId).map { entities ->
                    entities.map { it.query }
                }
            }
        }
    }

    override suspend fun saveSearchQuery(query: String) {
        if (query.isBlank()) return

        val currentUserId = sessionManager.currentUserId.first()
        if (!currentUserId.isNullOrBlank()) {
            val entity = SearchHistoryEntity(
                userId = currentUserId,
                query = query,
                timestamp = System.currentTimeMillis()
            )
            searchHistoryDao.insertSearchQuery(entity)
        }
    }

    override suspend fun deleteSearchQuery(query: String) {
        val currentUserId = sessionManager.currentUserId.first()
        if (!currentUserId.isNullOrBlank()) {
            searchHistoryDao.deleteSearchQuery(currentUserId, query)
        }
    }

    override suspend fun clearAllSearchHistory() {
        val currentUserId = sessionManager.currentUserId.first()
        if (!currentUserId.isNullOrBlank()) {
            searchHistoryDao.clearAllHistoryForUser(currentUserId)
        }
    }

    /**
     * دیتای دمو سرور شامل چند ردیف تکراری برای همان آهنگ است (عنوان/خواننده
     * یکسان، id متفاوت) — این باعث می‌شد مثلاً «Bohemian Rhapsody» دو بار در
     * هر بخش خانه نمایش داده شود. این متد فقط اولین رخداد هر ترک را نگه می‌دارد.
     */
    private fun List<Song>.deduplicated(): List<Song> =
        distinctBy { "${it.title.trim().lowercase()}|${it.artistName.trim().lowercase()}" }

    /** فقط برای کش کاتالوگ؛ وضعیت محلی (لایک/دانلود) در DAO حفظ می‌شود */
    private fun Song.toEntity(): LocalSongEntity = LocalSongEntity(
        id = id,
        title = title,
        artistName = artistName,
        coverImageUrl = coverImageUrl,
        audioUrl = audioUrl,
        playCount = playCount,
        likesCount = likesCount,
        releaseDate = releaseDate,
        isLiked = false,
        isDownloaded = false,
        localFilePath = null
    )

    private fun LocalSongEntity.toDomainModel(): Song {
        return Song(
            id = id,
            title = title,
            artistName = artistName,
            coverImageUrl = coverImageUrl,
            audioUrl = audioUrl,
            playCount = playCount,
            likesCount = likesCount,
            releaseDate = releaseDate,
            isLiked = isLiked,
            isDownloaded = isDownloaded,
            localFilePath = localFilePath
        )
    }

    override fun getTopArtists(): Flow<List<Artist>> = kotlinx.coroutines.flow.flow {
        try {
            val remoteArtists = songApi.getTopArtists()
            val artists = remoteArtists.map { dto ->
                Artist(
                    name = dto.artistName,
                    totalPlayCount = dto.totalPlayCount,
                    imageUrl = dto.artistImageUrl ?: ""
                )
            }
            emit(artists)
        } catch (e: Exception) {
            android.util.Log.e("REPO_ERROR", "Failed to fetch top artists: ${e.message}")
            emit(emptyList())
        }
    }

    override suspend fun getSongsByArtist(artistName: String): List<Song> {
        return try {
            val remoteSongs = songApi.getSongsByArtist(artistFilter = "eq.$artistName")
            remoteSongs.map { dto ->
                Song(
                    id = dto.id,
                    title = dto.title,
                    artistName = dto.artistName,
                    coverImageUrl = dto.coverImageUrl,
                    audioUrl = dto.audioUrl,
                    playCount = dto.playCount ?: 0,
                    likesCount = dto.likesCount ?: 0,
                    releaseDate = dto.releaseDate ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}