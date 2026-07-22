package com.hipka.app.presentation.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.domain.model.DataSourceState
import com.hipka.app.domain.repository.PlaylistRepository
import com.hipka.app.domain.repository.SongRepository
import com.hipka.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
        observeCurrentUser()
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.RefreshHome -> loadHomeData()
        }
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            userRepository.getCurrentUserId().collect { userId ->
                if (!userId.isNullOrBlank()) {
                    fetchUser(userId)
                } else {
                    _uiState.update { it.copy(currentUser = null) }
                }
            }
        }
    }

    private suspend fun fetchUser(userId: String) {
        val user = runCatching { userRepository.getUserById(userId) }.getOrNull()
        if (user != null) {
            _uiState.update { it.copy(currentUser = user) }
        }
    }

    private fun loadHomeData() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        loadPlaylists()
        loadSongs()

        // به‌روزرسانی مجدد اطلاعات کاربر در زمان رفرش
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserId().firstOrNull()
            if (!userId.isNullOrBlank()) {
                fetchUser(userId)
            }
        }
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            val worldPlaylists = runCatching { playlistRepository.getPlaylistsByCategory("WORLD") }
                .getOrDefault(emptyList())
            val localPlaylists = runCatching { playlistRepository.getPlaylistsByCategory("LOCAL") }
                .getOrDefault(emptyList())

            _uiState.update {
                it.copy(globalPlaylists = worldPlaylists, localPlaylists = localPlaylists)
            }
        }
    }

    private fun loadSongs() {
        viewModelScope.launch {
            songRepository.getSongsWithSource()
                .catch { exception ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = exception.message)
                    }
                }
                .collect { result ->
                    val allSongs = result.songs
                    val carousel = allSongs.take(3)
                    val popular = allSongs.sortedByDescending { it.playCount }.take(5)
                    val newReleases = allSongs.sortedByDescending { it.releaseDate }.take(5)

                    _uiState.update {
                        it.copy(
                            carouselSongs = carousel,
                            popularSongs = popular,
                            newReleases = newReleases,
                            isLoading = false,
                            isShowingCachedData = result.sourceState == DataSourceState.CACHED,
                            isOfflineWithNoCache = result.sourceState == DataSourceState.UNAVAILABLE,
                            errorMessage = null
                        )
                    }
                }
        }
    }
}