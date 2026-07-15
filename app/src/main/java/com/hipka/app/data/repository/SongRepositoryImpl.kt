package com.hipka.app.data.repository

import com.hipka.app.data.local.dao.SongDao
import com.hipka.app.data.remote.api.SongApi
import com.hipka.app.domain.model.Song
import com.hipka.app.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SongRepositoryImpl @Inject constructor(
    private val songApi: SongApi,
    private val songDao: SongDao
) : SongRepository {

    /* ====================================================================
       UNCOMMENT THIS BLOCK FOR OFFLINE TESTING // tnx to IRAN!!!!
       A robust list of offline mock songs if net problems happen.
       ====================================================================
    private val mockSongs = listOf(
        Song(id = "mock-1", title = "Midnight City", artistName = "M83", coverImageUrl = "https://picsum.photos/200/200?random=1", audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", isLiked = false, isDownloaded = false, localFilePath = null),
        Song(id = "mock-2", title = "Starboy", artistName = "The Weeknd", coverImageUrl = "https://picsum.photos/200/200?random=2", audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3", isLiked = false, isDownloaded = false, localFilePath = null),
        Song(id = "mock-3", title = "Blinding Lights", artistName = "The Weeknd", coverImageUrl = "https://picsum.photos/200/200?random=3", audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3", isLiked = false, isDownloaded = false, localFilePath = null),
        Song(id = "mock-4", title = "Sweater Weather", artistName = "The Neighbourhood", coverImageUrl = "https://picsum.photos/200/200?random=4", audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3", isLiked = false, isDownloaded = false, localFilePath = null),
        Song(id = "mock-5", title = "Do I Wanna Know?", artistName = "Arctic Monkeys", coverImageUrl = "https://picsum.photos/200/200?random=5", audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3", isLiked = false, isDownloaded = false, localFilePath = null)
    )
    ==================================================================== */

    override fun getSongs(): Flow<List<Song>> = flow {
        try {
            val remoteSongs = songApi.testGetSongs()
            val songs = remoteSongs.map { dto ->
                Song(
                    id = dto.id,
                    title = dto.title,
                    artistName = dto.artistName,
                    coverImageUrl = dto.coverImageUrl,
                    audioUrl = dto.audioUrl
                )
            }
            emit(songs)
        } catch (e: Exception) {
            android.util.Log.e("REPO_ERROR", "Network failed. Emitting empty list. Cause: ${e.message}")

            // Real implementation: show empty state on network error
            emit(emptyList())

            // UNCOMMENT FOR OFFLINE TESTING:
            // emit(mockSongs)
        }
    }

    override suspend fun getSongById(id: String): Song? {
        val localSong = songDao.getSongById(id)
        if (localSong != null) {
            return Song(
                id = localSong.id,
                title = localSong.title,
                artistName = localSong.artistName,
                coverImageUrl = localSong.coverImageUrl,
                audioUrl = localSong.audioUrl,
                isLiked = localSong.isLiked,
                isDownloaded = localSong.isDownloaded,
                localFilePath = localSong.localFilePath
            )
        }

        // Real implementation: return null if not found
        return null

        // UNCOMMENT FOR OFFLINE TESTING:
        // return mockSongs.find { it.id == id }
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
                    audioUrl = dto.audioUrl
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("REPO_ERROR", "Search query failed. Returning empty list.")

            // Real implementation: return empty list on network error
            return emptyList()

            // UNCOMMENT FOR OFFLINE TESTING:
            /*
            return mockSongs.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artistName.contains(query, ignoreCase = true)
            }
            */
        }
    }
}