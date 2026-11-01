package com.hipka.app.presentation.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.core.network.NetworkMonitor
import com.hipka.app.domain.model.Song
import com.hipka.app.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private data class Request(val query: String, val immediate: Boolean = false, val revision: Long = 0)
    private val requests = MutableStateFlow(Request(""))

    init {
        viewModelScope.launch {
            songRepository.getSearchHistory().collect { history ->
                _uiState.update { it.copy(searchHistory = history) }
            }
        }
        viewModelScope.launch {
            // Cancel the previous request immediately, including its debounce delay.
            requests.collectLatest { request ->
                val query = request.query.trim()
                if (query.isBlank()) return@collectLatest
                if (!request.immediate) delay(400)
                _uiState.update { it.copy(isLoading = true, errorMessage = null, hasSearchedBefore = true) }
                try {
                    val offline = !networkMonitor.isOnline.first()
                    val results = if (offline) songRepository.getSongs().first().filter {
                        it.title.contains(query, true) || it.artistName.contains(query, true)
                    } else songRepository.searchSongs(query)
                    _uiState.update { it.copy(rawSearchResults = results,
                        searchResults = applyFilter(results, it.selectedFilter, query),
                        isLoading = false, isOffline = offline) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "SEARCH_FAILED") }
                }
            }
        }
    }

    fun onIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> search(intent.query, false)
            is SearchIntent.SearchSong -> {
                search(intent.query, true)
                viewModelScope.launch { songRepository.saveSearchQuery(intent.query.trim()) }
            }
            SearchIntent.ClearSearch -> search("", true)
            is SearchIntent.ChangeFilter -> _uiState.update {
                it.copy(selectedFilter = intent.filter,
                    searchResults = applyFilter(it.rawSearchResults, intent.filter, it.searchQuery))
            }
            is SearchIntent.DeleteHistoryItem -> viewModelScope.launch {
                songRepository.deleteSearchQuery(intent.query)
            }
        }
    }

    private fun search(query: String, immediate: Boolean) {
        _uiState.update { it.copy(searchQuery = query, errorMessage = null, isOffline = false,
            isLoading = query.isNotBlank(), hasSearchedBefore = query.isNotBlank(),
            rawSearchResults = emptyList(), searchResults = emptyList()) }
        requests.value = Request(query, immediate, requests.value.revision + 1)
    }

    private fun applyFilter(songs: List<Song>, filter: SearchFilter, query: String): List<Song> =
        when (filter) {
            SearchFilter.ALL -> songs
            SearchFilter.SONG -> songs.filter { it.title.contains(query.trim(), true) }
            SearchFilter.ARTIST -> songs.filter { it.artistName.contains(query.trim(), true) }
        }
}
