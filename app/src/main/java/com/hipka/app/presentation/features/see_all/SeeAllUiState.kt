package com.hipka.app.presentation.features.see_all

import com.hipka.app.domain.model.Song

sealed interface SeeAllUiState {
    data object Loading : SeeAllUiState
    data object Empty : SeeAllUiState
    data class Error(val message: String) : SeeAllUiState

    data class Success(
        val songs: List<Song>,
        val titleResId: Int // R.string آیدی رشته متنی عنوان متناسب با سکشن برای هماهنگی با
    ) : SeeAllUiState
}