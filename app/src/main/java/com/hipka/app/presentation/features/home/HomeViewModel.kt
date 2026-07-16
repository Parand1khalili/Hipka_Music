package com.hipka.app.presentation.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.domain.repository.PlaylistRepository
import com.hipka.app.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.RefreshHome -> loadHomeData()
        }
    }


    private fun loadHomeData() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val worldPlaylists = playlistRepository.getPlaylistsByCategory("WORLD")
                val localPlaylists = playlistRepository.getPlaylistsByCategory("LOCAL")

                songRepository.getSongs()
                    .catch { exception ->
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = exception.message)
                        }
                    }
                    .collect { allSongs ->
                        // 1. Featured Today: Take the top 3 items
                        val carousel = allSongs.take(3)

                        // 2. Popular: Sort by play count descending, and take the top 5
                        val popular = allSongs.sortedByDescending { it.playCount }.take(5)

                        // 3. New Releases: Sort by release date descending (newest first), and take the top 5
                        val newReleases = allSongs.sortedByDescending { it.releaseDate }.take(5)

                        _uiState.update {
                            it.copy(
                                carouselSongs = carousel,
                                popularSongs = popular,
                                newReleases = newReleases,
                                globalPlaylists = worldPlaylists,
                                localPlaylists = localPlaylists,
                                isLoading = false
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }
}