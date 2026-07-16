package com.hipka.app.presentation.features.recent

import com.hipka.app.domain.model.Song

data class RecentSongsState(
    val isLoading: Boolean = true,
    val songs: List<Song> = emptyList(),
    val error: String? = null
)
sealed class RecentSongsIntent {
    data class ToggleLike(val songId: String) : RecentSongsIntent()
}