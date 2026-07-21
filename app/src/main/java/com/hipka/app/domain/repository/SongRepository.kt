package com.hipka.app.domain.repository

import com.hipka.app.domain.model.Artist
import com.hipka.app.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun getSongs(): Flow<List<Song>>
    suspend fun getSongById(id: String): Song?
    suspend fun searchSongs(query: String): List<Song>

    fun getLikedSongs(): Flow<List<Song>>
    fun getRecentlyPlayedSongs(): Flow<List<Song>>
    fun observeLikedSongIds(): Flow<List<String>>
    suspend fun toggleAndInsertLike(song: Song)
    suspend fun addToRecentlyPlayed(song: Song)
    fun getSearchHistory(): Flow<List<String>>
    suspend fun saveSearchQuery(query: String)
    suspend fun deleteSearchQuery(query: String)
    suspend fun clearAllSearchHistory()
    fun getTopArtists(): Flow<List<Artist>>
    suspend fun getSongsByArtist(artistName: String): List<Song>
}