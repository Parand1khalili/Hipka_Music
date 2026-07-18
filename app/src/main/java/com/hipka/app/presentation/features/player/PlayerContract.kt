package com.hipka.app.presentation.features.player

import com.hipka.app.domain.model.PlaybackProgress
import com.hipka.app.domain.model.Song

data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val progress: PlaybackProgress = PlaybackProgress(),
    val sleepTimerRemainingMs: Long? = null
)

sealed interface PlayerIntent {
    data class PlaySong(val song: Song) : PlayerIntent
    data class PlayQueue(val songs: List<Song>, val startIndex: Int) : PlayerIntent
    data object TogglePlayPause : PlayerIntent
    data object SkipNext : PlayerIntent
    data object SkipPrevious : PlayerIntent
    data class SeekTo(val positionMs: Long) : PlayerIntent
    data class ShufflePlayList(val songs: List<Song>) : PlayerIntent
    data class SetSleepTimer(val durationMs: Long) : PlayerIntent
    data object CancelSleepTimer : PlayerIntent
}
