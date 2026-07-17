package com.hipka.app.presentation.features.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.domain.model.Song
import com.hipka.app.domain.repository.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerRepository: PlayerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val playbackErrors: SharedFlow<String> = playerRepository.playbackErrors

    init {
        viewModelScope.launch {
            playerRepository.isPlaying.collect { isPlaying ->
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }
        }
    }

    fun onIntent(intent: PlayerIntent) {
        when (intent) {
            is PlayerIntent.PlaySong -> playSong(intent.song)
            PlayerIntent.TogglePlayPause -> togglePlayPause()
            is PlayerIntent.ShufflePlayList -> {
                if (intent.songs.isNotEmpty()) {
                    val shuffledList = intent.songs.shuffled()
                    // پخش اولین آهنگ از لیست مخلوط شده
                    playSong(shuffledList.first())
                    // TODO: در صورت نیاز در اسپرینت‌های بعدی کل لیست shuffledList به صف پخش (Queue) ریپازیتوری پاس داده شود.
                }
            }
        }
    }

    private fun playSong(song: Song) {
        _uiState.update { it.copy(currentSong = song) }
        viewModelScope.launch {
            playerRepository.playSong(song)
        }
    }

    private fun togglePlayPause() {
        viewModelScope.launch {
            if (_uiState.value.isPlaying) playerRepository.pause() else playerRepository.resume()
        }
    }
}
