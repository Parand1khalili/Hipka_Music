package com.hipka.app.presentation.features.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.core.network.NetworkMonitor
import com.hipka.app.data.local.datastore.SessionManager
import com.hipka.app.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val sessionManager: SessionManager,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistsUiState())
    val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onIntent(intent: PlaylistsIntent) {
        when (intent) {
            PlaylistsIntent.Refresh -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isOffline = false) }

            // پلی‌لیست‌ها هیچ کش محلی ندارند (برخلاف آهنگ‌ها)؛ به جای زدن یک
            // درخواست شبکه‌ای که حتماً شکست می‌خورد و پیام خام نشان می‌دهد،
            // مستقیماً اعلام می‌کنیم که این بخش بدون اینترنت قابل استفاده نیست.
            val isOnline = runCatching { networkMonitor.isOnline.first() }.getOrDefault(true)
            if (!isOnline) {
                _uiState.update { it.copy(isLoading = false, isOffline = true) }
                return@launch
            }

            runCatching {
                val world = playlistRepository.getPlaylistsByCategory("WORLD")
                val local = playlistRepository.getPlaylistsByCategory("LOCAL")
                val myId = sessionManager.currentUserId.first()
                val mine = myId?.let { playlistRepository.getUserPlaylists(it) } ?: emptyList()
                Triple(world, local, mine)
            }.onSuccess { (world, local, mine) ->
                _uiState.update {
                    it.copy(
                        worldPlaylists = world,
                        localPlaylists = local,
                        userPlaylists = mine,
                        isLoading = false
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }
}