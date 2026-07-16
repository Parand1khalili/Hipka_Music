package com.hipka.app.presentation.features.playlists

import com.hipka.app.domain.model.Playlist

data class PlaylistsUiState(
    val worldPlaylists: List<Playlist> = emptyList(),
    val localPlaylists: List<Playlist> = emptyList(),
    val userPlaylists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface PlaylistsIntent {
    data object Refresh : PlaylistsIntent
}