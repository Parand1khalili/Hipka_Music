package com.hipka.app.presentation.features.home

import com.hipka.app.domain.model.Song

data class HomeUiState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true
)
