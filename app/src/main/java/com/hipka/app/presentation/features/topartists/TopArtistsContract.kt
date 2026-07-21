package com.hipka.app.presentation.features.topartists

import com.hipka.app.domain.model.Artist

data class TopArtistsUiState(
    val artists: List<Artist> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface TopArtistsIntent {
    data object LoadTopArtists : TopArtistsIntent
}