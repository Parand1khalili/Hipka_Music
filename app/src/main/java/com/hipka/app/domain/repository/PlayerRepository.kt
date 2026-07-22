package com.hipka.app.domain.repository

import com.hipka.app.domain.model.PlaybackProgress
import com.hipka.app.domain.model.RepeatMode
import com.hipka.app.domain.model.Song
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface PlayerRepository {
    val isPlaying: StateFlow<Boolean>
    val isBuffering: StateFlow<Boolean>
    val currentSong: StateFlow<Song?>
    val progress: StateFlow<PlaybackProgress>
    val playbackErrors: SharedFlow<String>
    val sleepTimerRemainingMs: StateFlow<Long?>
    val playbackSpeed: StateFlow<Float>
    val isShuffleEnabled: StateFlow<Boolean>
    val repeatMode: StateFlow<RepeatMode>

    suspend fun playSong(song: Song)
    suspend fun playQueue(songs: List<Song>, startIndex: Int)
    suspend fun pause()
    suspend fun resume()
    suspend fun stop()
    suspend fun skipToNext()
    suspend fun skipToPrevious()
    suspend fun seekTo(positionMs: Long)
    fun startSleepTimer(durationMs: Long)
    fun cancelSleepTimer()
    suspend fun setPlaybackSpeed(speed: Float)
    suspend fun setShuffleEnabled(enabled: Boolean)
    suspend fun setRepeatMode(mode: RepeatMode)
}
