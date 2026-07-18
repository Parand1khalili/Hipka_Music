package com.hipka.app.presentation.features.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecentSongsViewModel @Inject constructor(
    private val songRepository: SongRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RecentSongsState())
    val state: StateFlow<RecentSongsState> = _state.asStateFlow()

    init {
        //یک وقفه کوتاه دادیم تا شیمر دیده بشه
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            observeRecentSongs()
        }
    }

    private fun observeRecentSongs() {
        songRepository.getRecentlyPlayedSongs()
            .onEach { songs ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    songs = songs,
                    error = null
                )
            }
            .catch { exception ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = exception.message ?: "Unknown Error occurred"
                )
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: RecentSongsIntent) {
        when (intent) {
            is RecentSongsIntent.ToggleLike -> {
                viewModelScope.launch {
                    val song = _state.value.songs.find { it.id == intent.songId }
                    if (song != null) {
                        songRepository.toggleAndInsertLike(song)
                    }
                }
            }
        }
    }
}