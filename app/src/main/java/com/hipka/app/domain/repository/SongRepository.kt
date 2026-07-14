package com.hipka.app.domain.repository

import com.hipka.app.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun getSongs(): Flow<List<Song>>
    suspend fun getSongById(id: String): Song?
}