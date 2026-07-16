package com.hipka.app.domain.repository

import com.hipka.app.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun getSongs(): Flow<List<Song>>
    suspend fun getSongById(id: String): Song?
    suspend fun searchSongs(query: String): List<Song>

    fun getLikedSongs(): Flow<List<Song>>
    fun getRecentlyPlayedSongs(): Flow<List<Song>>
    suspend fun toggleLike(songId: String)
    suspend fun addToRecentlyPlayed(song: Song)

    fun observeLikedSongIds(): Flow<List<String>>
    suspend fun toggleAndInsertLike(song: Song)
}