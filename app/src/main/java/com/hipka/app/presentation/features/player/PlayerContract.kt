package com.hipka.app.presentation.features.player

import com.hipka.app.domain.model.Song

data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false
)

sealed interface PlayerIntent {
    data class PlaySong(val song: Song) : PlayerIntent
    data object TogglePlayPause : PlayerIntent
    data class ShufflePlayList(val songs: List<Song>) : PlayerIntent
}
