package com.hipka.app.domain.repository

import com.hipka.app.domain.model.Song
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface PlayerRepository {
    val isPlaying: StateFlow<Boolean>
    val playbackErrors: SharedFlow<String>

    suspend fun playSong(song: Song)
    suspend fun pause()
    suspend fun resume()
}
