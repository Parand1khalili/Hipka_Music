package com.hipka.app.presentation.features.see_all

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.R
import com.hipka.app.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeeAllViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val songRepository: SongRepository
) : ViewModel() {

    // گرفتن نام سکشن از کامپوننت Navigation اندروید
    private val sectionName: String = savedStateHandle.get<String>("section") ?: "popular"

    private val _uiState = MutableStateFlow<SeeAllUiState>(SeeAllUiState.Loading)
    val uiState: StateFlow<SeeAllUiState> = _uiState.asStateFlow()

    init {
        loadSectionSongs()
    }

    fun loadSectionSongs() {
        _uiState.value = SeeAllUiState.Loading

        viewModelScope.launch {
            songRepository.getSongs()
                .catch { exception ->
                    _uiState.value = SeeAllUiState.Error(exception.message ?: "An unknown error occurred")
                }
                .collect { allSongs ->
                    if (allSongs.isEmpty()) {
                        _uiState.value = SeeAllUiState.Empty
                        return@collect
                    }

                    // پردازش و سورت کردن داتا دقیقاً بر اساس همان منطق هوم اسکرین شما
                    val (processedSongs, titleRes) = when (sectionName) {
                        "popular" -> {
                            allSongs.sortedByDescending { it.playCount } to R.string.home_section_popular
                        }
                        "new_releases" -> {
                            allSongs.sortedByDescending { it.releaseDate } to R.string.home_section_new_releases
                        }
                        else -> {
                            allSongs to R.string.app_name // فال‌بک پیش‌فرض
                        }
                    }

                    if (processedSongs.isEmpty()) {
                        _uiState.value = SeeAllUiState.Empty
                    } else {
                        _uiState.value = SeeAllUiState.Success(
                            songs = processedSongs,
                            titleResId = titleRes
                        )
                    }
                }
        }
    }
}