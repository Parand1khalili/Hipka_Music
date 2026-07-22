package com.hipka.app.presentation.features.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.data.local.datastore.SessionManager
import com.hipka.app.domain.repository.DownloadRepository
import com.hipka.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(DownloadsState())
    val state: StateFlow<DownloadsState> = _state.asStateFlow()

    init {
        // لیست دانلودها از Room می‌آید، پس در حالت آفلاین هم کار می‌کند
        downloadRepository.getDownloadedSongs()
            .onEach { songs ->
                _state.update {
                    it.copy(isLoading = false, songs = songs.sortedBy(it.sortOrder), error = null)
                }
            }
            .catch { exception ->
                _state.update { it.copy(isLoading = false, error = exception.message) }
            }
            .launchIn(viewModelScope)

        observePremiumStatus()
    }

    private fun observePremiumStatus() {
        viewModelScope.launch {
            sessionManager.currentUserId.collect { userId ->
                val premium = userId
                    ?.let { runCatching { userRepository.getUserById(it)?.isPremium }.getOrNull() }
                    ?: false
                _state.update { it.copy(isPremium = premium) }
            }
        }
    }

    fun onIntent(intent: DownloadsIntent) {
        when (intent) {
            is DownloadsIntent.DeleteDownload -> deleteDownload(intent.songId)
            is DownloadsIntent.ChangeSortOrder -> changeSortOrder(intent.sortOrder)
        }
    }

    private fun deleteDownload(songId: String) {
        // لیست از Room می‌آید، پس بعد از حذف خودش به‌روزرسانی می‌شود
        viewModelScope.launch { downloadRepository.deleteDownload(songId) }
    }

    private fun changeSortOrder(sortOrder: DownloadsSortOrder) {
        _state.update { it.copy(sortOrder = sortOrder, songs = it.songs.sortedBy(sortOrder)) }
    }
}

private fun List<com.hipka.app.domain.model.Song>.sortedBy(order: DownloadsSortOrder) =
    when (order) {
        DownloadsSortOrder.TITLE -> sortedBy { it.title.lowercase() }
        DownloadsSortOrder.ARTIST -> sortedBy { it.artistName.lowercase() }
    }
