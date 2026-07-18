package com.hipka.app.domain.repository

import com.hipka.app.domain.model.PlaybackProgress
import com.hipka.app.domain.model.Song
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface PlayerRepository {
    val isPlaying: StateFlow<Boolean>
    val currentSong: StateFlow<Song?>
    val progress: StateFlow<PlaybackProgress>
    val playbackErrors: SharedFlow<String>
    val sleepTimerRemainingMs: StateFlow<Long?>
    val playbackSpeed: StateFlow<Float>

    suspend fun playSong(song: Song)
    suspend fun playQueue(songs: List<Song>, startIndex: Int)
    suspend fun pause()
    suspend fun resume()
    suspend fun skipToNext()
    suspend fun skipToPrevious()
    suspend fun seekTo(positionMs: Long)
    fun startSleepTimer(durationMs: Long)
    fun cancelSleepTimer()
    suspend fun setPlaybackSpeed(speed: Float)
}
