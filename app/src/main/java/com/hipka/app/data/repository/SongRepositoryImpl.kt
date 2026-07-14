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
            emit(emptyList())
        }
    }

    override suspend fun getSongById(id: String): Song? {
        val localSong = songDao.getSongById(id)
        return localSong?.let {
            Song(
                id = it.id,
                title = it.title,
                artistName = it.artistName,
                coverImageUrl = it.coverImageUrl,
                audioUrl = it.audioUrl,
                isLiked = it.isLiked,
                isDownloaded = it.isDownloaded,
                localFilePath = it.localFilePath
            )
        }
    }
}