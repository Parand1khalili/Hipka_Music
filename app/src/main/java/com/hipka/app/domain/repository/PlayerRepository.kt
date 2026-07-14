package com.hipka.app.domain.repository

import com.hipka.app.domain.model.Song

interface PlayerRepository {
    suspend fun playSong(song: Song)
    suspend fun pause()
    suspend fun resume()
}
