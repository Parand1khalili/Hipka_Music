package com.hipka.app.presentation.features.search

import com.hipka.app.data.local.entity.SearchHistoryEntity
import com.hipka.app.domain.model.Song

enum class SearchFilter {
    ALL, SONG, ARTIST
}

data class SearchUiState(
    val searchQuery: String = "",
    val rawSearchResults: List<Song> = emptyList(), // Keeps the unfiltered network response
    val searchResults: List<Song> = emptyList(),    // The filtered list sent to UI
    val searchHistory: List<String> = emptyList(),
    val selectedFilter: SearchFilter = SearchFilter.ALL,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** جستجو به سرور نیاز دارد؛ در حالت آفلاین باید پیام مناسب نشان داده شود */
    val isOffline: Boolean = false,
    val hasSearchedBefore: Boolean = false
)

sealed interface SearchIntent {
    data class QueryChanged(val query: String) : SearchIntent
    data class SearchSong(val query: String) : SearchIntent
    data object ClearSearch : SearchIntent
    data class ChangeFilter(val filter: SearchFilter) : SearchIntent
    data class DeleteHistoryItem(val query: String) : SearchIntent
}