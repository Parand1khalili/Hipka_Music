package com.hipka.app.presentation.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.core.network.NetworkMonitor
import com.hipka.app.domain.model.Song
import com.hipka.app.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            songRepository.getSearchHistory().collect { history ->
                _uiState.update { it.copy(searchHistory = history) }
            }
        }

        viewModelScope.launch {
            searchQueryFlow
                .debounce(500L)
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .flatMapLatest { query ->
                    _uiState.update { it.copy(isLoading = true, errorMessage = null, hasSearchedBefore = true) }

                    flow {
                        // جستجو فقط سمت سرور انجام می‌شود؛ در حالت آفلاین به جای
                        // «نتیجه‌ای یافت نشد» باید علت واقعی (نبود اینترنت) گفته شود
                        if (!networkMonitor.isOnline.first()) {
                            _uiState.update { it.copy(isOffline = true) }
                            emit(emptyList())
                            return@flow
                        }

                        _uiState.update { it.copy(isOffline = false) }
                        val results = songRepository.searchSongs(query)
                        emit(results)
                    }
                }
                .collect { rawResults ->
                    _uiState.update { state ->
                        state.copy(
                            rawSearchResults = rawResults,
                            searchResults = applyFilter(rawResults, state.selectedFilter, state.searchQuery),
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun onIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> handleQueryChanged(intent.query)
            is SearchIntent.SearchSong -> executeExplicitSearch(intent.query)
            SearchIntent.ClearSearch -> handleClearSearch()
            is SearchIntent.ChangeFilter -> handleChangeFilter(intent.filter)
            is SearchIntent.DeleteHistoryItem -> deleteHistoryItem(intent.query)
        }
    }

    private fun handleQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            handleClearSearch()
        } else {
            searchQueryFlow.value = query
        }
    }

    private fun executeExplicitSearch(query: String) {
        if (query.isBlank()) return

        _uiState.update { it.copy(searchQuery = query) }
        searchQueryFlow.value = query

        viewModelScope.launch {
            // 👈 استفاده از متد ریپازیتوری برای ذخیره امن تاریخچه
            songRepository.saveSearchQuery(query)
        }
    }

    private fun handleClearSearch() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                rawSearchResults = emptyList(),
                searchResults = emptyList(),
                isLoading = false,
                errorMessage = null,
                hasSearchedBefore = false
            )
        }
        searchQueryFlow.value = ""
    }

    private fun handleChangeFilter(filter: SearchFilter) {
        _uiState.update { state ->
            state.copy(
                selectedFilter = filter,
                searchResults = applyFilter(state.rawSearchResults, filter, state.searchQuery)
            )
        }
    }

    private fun applyFilter(songs: List<Song>, filter: SearchFilter, query: String): List<Song> {
        if (query.isBlank()) return songs

        return when (filter) {
            SearchFilter.ALL -> songs
            SearchFilter.SONG -> songs.filter { it.title.contains(query, ignoreCase = true) }
            SearchFilter.ARTIST -> songs.filter { it.artistName.contains(query, ignoreCase = true) }
        }
    }

    private fun deleteHistoryItem(query: String) {
        viewModelScope.launch {
            songRepository.deleteSearchQuery(query)
        }
    }
}