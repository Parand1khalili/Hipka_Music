package com.hipka.app.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.domain.model.Song
import com.hipka.app.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongInteractionViewModel @Inject constructor(
    private val songRepository: SongRepository
) : ViewModel() {

    val likedSongIds: StateFlow<Set<String>> = songRepository.observeLikedSongIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    fun addToRecentlyPlayed(song: Song) {
        viewModelScope.launch {
            songRepository.addToRecentlyPlayed(song)
        }
    }

    // گرفتن کُل آبجکتِ آهنگ به جای آیدی
    fun toggleLike(song: Song) {
        viewModelScope.launch {
            songRepository.toggleAndInsertLike(song)
        }
    }
}