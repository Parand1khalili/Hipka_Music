package com.hipka.app.presentation.features.ArtistDetail

import com.hipka.app.domain.model.Song

data class ArtistDetailUiState(
    val artistName: String = "",
    val imageUrl: String = "",
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface ArtistDetailIntent {
    data class LoadArtistData(val artistName: String, val imageUrl: String) : ArtistDetailIntent
}