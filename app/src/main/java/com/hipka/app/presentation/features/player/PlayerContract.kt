package com.hipka.app.presentation.features.player

import com.hipka.app.domain.model.Song

data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false
)

sealed interface PlayerIntent {
    data class PlaySong(val song: Song) : PlayerIntent
    data class PlayQueue(val songs: List<Song>, val startIndex: Int) : PlayerIntent
    data object TogglePlayPause : PlayerIntent
    data object SkipNext : PlayerIntent
    data object SkipPrevious : PlayerIntent
}
