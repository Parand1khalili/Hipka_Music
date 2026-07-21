package com.hipka.app.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.data.local.datastore.SessionManager
import com.hipka.app.domain.model.Song
import com.hipka.app.domain.repository.DownloadRepository
import com.hipka.app.domain.repository.SongRepository
import com.hipka.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongInteractionViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val downloadRepository: DownloadRepository,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val likedSongIds: StateFlow<Set<String>> = songRepository.observeLikedSongIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    val downloadedSongIds: StateFlow<Set<String>> = downloadRepository.getDownloadedSongIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    /** وضعیت اشتراک ویژه کاربر فعلی — دکمه دانلود فقط برای کاربران Premium فعال است */
    val isPremium: StateFlow<Boolean> = sessionManager.currentUserId
        .map { userId -> userId.loadPremiumStatus() }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    /** رویداد یک‌بارمصرف: کاربر عادی روی دکمه دانلود زده و باید پیام ارتقاء حساب ببیند */
    private val _premiumRequired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val premiumRequired: SharedFlow<Unit> = _premiumRequired

    /** رویداد یک‌بارمصرف: دانلود در صف قرار گرفت، برای بازخورد فوری به کاربر */
    private val _downloadStarted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val downloadStarted: SharedFlow<Unit> = _downloadStarted

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

    fun downloadSong(song: Song) {
        viewModelScope.launch {
            // وضعیت پریمیوم در لحظه کلیک خوانده می‌شود تا اگر StateFlow هنوز مقدار نگرفته باشد اشتباه نکنیم
            if (!sessionManager.currentUserId.first().loadPremiumStatus()) {
                _premiumRequired.tryEmit(Unit)
                return@launch
            }

            downloadRepository.enqueueDownload(song)
            _downloadStarted.tryEmit(Unit)
        }
    }

    private suspend fun String?.loadPremiumStatus(): Boolean =
        this?.let { runCatching { userRepository.getUserById(it)?.isPremium }.getOrNull() } ?: false
}
