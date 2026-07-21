package com.hipka.app.presentation.features.topartists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopArtistsViewModel @Inject constructor(
    private val songRepository: SongRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TopArtistsUiState())
    val uiState: StateFlow<TopArtistsUiState> = _uiState.asStateFlow()

    init {
        onIntent(TopArtistsIntent.LoadTopArtists)
    }

    fun onIntent(intent: TopArtistsIntent) {
        when (intent) {
            TopArtistsIntent.LoadTopArtists -> fetchTopArtists()
        }
    }

    private fun fetchTopArtists() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            songRepository.getTopArtists().collect { artistList ->
                _uiState.update {
                    it.copy(
                        artists = artistList,
                        isLoading = false,
                        errorMessage = if (artistList.isEmpty()) "هیچ داده‌ای یافت نشد" else null
                    )
                }
            }
        }
    }
}