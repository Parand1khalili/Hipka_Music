package com.hipka.app.presentation.features.likedsongs

import com.hipka.app.domain.model.Song

data class LikedSongsState(
    val isLoading: Boolean = true,
    val songs: List<Song> = emptyList(),
    val error: String? = null
)

sealed class LikedSongsIntent {
    data class ToggleLike(val songId: String) : LikedSongsIntent()
}