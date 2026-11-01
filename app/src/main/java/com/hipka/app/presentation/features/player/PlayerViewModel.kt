package com.hipka.app.presentation.features.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.domain.model.RepeatMode
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

    private val commandErrors = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 1)
    val playbackErrors = kotlinx.coroutines.flow.merge(playerRepository.playbackErrors, commandErrors)

    private fun launchCommand(block: suspend () -> Unit) = viewModelScope.launch {
        try { block() }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) { commandErrors.emit("Unable to control playback. Please try again.") }
    }

    init {
        viewModelScope.launch {
            playerRepository.isPlaying.collect { isPlaying ->
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }
        }
        viewModelScope.launch {
            playerRepository.currentSong.collect { song ->
                _uiState.update { it.copy(currentSong = song) }
            }
        }
        viewModelScope.launch {
            playerRepository.progress.collect { progress ->
                _uiState.update { it.copy(progress = progress) }
            }
        }
        viewModelScope.launch {
            playerRepository.sleepTimerRemainingMs.collect { remaining ->
                _uiState.update { it.copy(sleepTimerRemainingMs = remaining) }
            }
        }
        viewModelScope.launch {
            playerRepository.playbackSpeed.collect { speed ->
                _uiState.update { it.copy(playbackSpeed = speed) }
            }
        }
        viewModelScope.launch {
            playerRepository.isBuffering.collect { isBuffering ->
                _uiState.update { it.copy(isBuffering = isBuffering) }
            }
        }
        viewModelScope.launch {
            playerRepository.isShuffleEnabled.collect { enabled ->
                _uiState.update { it.copy(isShuffleEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            playerRepository.repeatMode.collect { mode ->
                _uiState.update { it.copy(repeatMode = mode) }
            }
        }
    }

    fun onIntent(intent: PlayerIntent) {
        when (intent) {
            is PlayerIntent.PlaySong -> playSong(intent.song)
            is PlayerIntent.PlayQueue -> playQueue(intent.songs, intent.startIndex)
            PlayerIntent.TogglePlayPause -> togglePlayPause()
            PlayerIntent.Stop -> stop()
            PlayerIntent.SkipNext -> skipToNext()
            PlayerIntent.SkipPrevious -> skipToPrevious()
            is PlayerIntent.SeekTo -> seekTo(intent.positionMs)
            is PlayerIntent.ShufflePlayList -> {
                if (intent.songs.isNotEmpty()) {
                    val shuffledList = intent.songs.shuffled()
                    // پخش اولین آهنگ از لیست مخلوط شده
                    playQueue(shuffledList, 0)
                }
            }
            PlayerIntent.ToggleShuffle -> toggleShuffle()
            PlayerIntent.CycleRepeatMode -> cycleRepeatMode()
            is PlayerIntent.SetSleepTimer -> playerRepository.startSleepTimer(intent.durationMs)
            PlayerIntent.CancelSleepTimer -> playerRepository.cancelSleepTimer()
            is PlayerIntent.SetPlaybackSpeed -> setPlaybackSpeed(intent.speed)
        }
    }

    private fun playSong(song: Song) {
        launchCommand {
            playerRepository.playSong(song)
        }
    }

    private fun playQueue(songs: List<Song>, startIndex: Int) {
        launchCommand {
            playerRepository.playQueue(songs, startIndex)
        }
    }

    private fun togglePlayPause() {
        launchCommand {
            if (_uiState.value.isPlaying || _uiState.value.isBuffering) playerRepository.pause() else playerRepository.resume()
        }
    }

    private fun stop() {
        launchCommand { playerRepository.stop() }
    }

    private fun toggleShuffle() {
        launchCommand { playerRepository.setShuffleEnabled(!_uiState.value.isShuffleEnabled) }
    }

    private fun cycleRepeatMode() {
        val nextMode = when (_uiState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        launchCommand { playerRepository.setRepeatMode(nextMode) }
    }

    private fun skipToNext() {
        launchCommand { playerRepository.skipToNext() }
    }

    private fun skipToPrevious() {
        launchCommand { playerRepository.skipToPrevious() }
    }

    private fun seekTo(positionMs: Long) {
        launchCommand { playerRepository.seekTo(positionMs) }
    }

    private fun setPlaybackSpeed(speed: Float) {
        launchCommand { playerRepository.setPlaybackSpeed(speed) }
    }
}
