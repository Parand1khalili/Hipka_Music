package com.hipka.app.presentation.features.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.data.local.datastore.SessionManager
import com.hipka.app.domain.model.Song
import com.hipka.app.domain.repository.ChatRepository
import com.hipka.app.domain.repository.SongRepository
import com.hipka.app.domain.repository.UserRepository
import com.hipka.app.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val songRepository: SongRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val peerUserId: String = checkNotNull(savedStateHandle[Screen.ChatConversation.ARG_PEER_USER_ID])

    private val _uiState = MutableStateFlow(ChatUiState(peerUserId = peerUserId))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var stoppedTypingJob: Job? = null
    private var isTypingSignalSent = false

    init {
        viewModelScope.launch {
            val myId = sessionManager.currentUserId.first()
            if (myId == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "No demo user selected — pick one on the Profile tab.") }
                return@launch
            }
            _uiState.update { it.copy(currentUserId = myId) }

            loadPeer()
            loadHistory(myId)
            loadAvailableSongsToShare()
            markPeerMessagesRead(myId)
            listenForIncoming(myId)
            listenForStatusUpdates(myId)
            listenForTyping(myId)
        }
    }

    fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.DraftChanged -> onDraftChanged(intent.text)
            ChatIntent.SendMessage -> sendMessage(text = _uiState.value.draftText, sharedSongId = null)
            ChatIntent.ToggleSongPicker -> _uiState.update { it.copy(isSongPickerOpen = !it.isSongPickerOpen) }
            is ChatIntent.ShareSong -> shareSong(intent.song)
        }
    }

    override fun onCleared() {
        super.onCleared()
        val myId = _uiState.value.currentUserId
        if (myId.isNotEmpty() && isTypingSignalSent) {
            viewModelScope.launch { chatRepository.sendTypingStatus(myId, peerUserId, isTyping = false) }
        }
    }

    private suspend fun loadPeer() {
        runCatching { userRepository.getUserById(peerUserId) }
            .onSuccess { user -> _uiState.update { it.copy(peerUser = user) } }
    }

    private suspend fun loadHistory(myId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching { chatRepository.getConversationHistory(myId, peerUserId) }
            .onSuccess { history ->
                _uiState.update { it.copy(messages = history, isLoading = false) }
                resolveSharedSongs(history.mapNotNull { it.sharedSongId })
            }
            .onFailure { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
    }

    private fun loadAvailableSongsToShare() {
        viewModelScope.launch {
            runCatching {
                songRepository.getSongs().collect { songs ->
                    _uiState.update { state -> state.copy(availableSongs = songs) }
                    // پس از لود شدن لیست آهنگ‌ها، دوباره تمام آیدی‌های داخل پیام‌ها بررسی می‌شوند
                    resolveSharedSongs(_uiState.value.messages.mapNotNull { it.sharedSongId })
                }
            }
        }
    }

    private fun markPeerMessagesRead(myId: String) {
        viewModelScope.launch {
            runCatching { chatRepository.markConversationAsRead(myId, peerUserId) }
        }
    }

    private fun listenForIncoming(myId: String) {
        viewModelScope.launch {
            chatRepository.observeIncomingMessages(myId)
                .filter { it.senderId == peerUserId }
                .collect { incoming ->
                    _uiState.update { state ->
                        if (state.messages.any { it.id == incoming.id }) state
                        else state.copy(messages = state.messages + incoming, isPeerTyping = false)
                    }
                    incoming.sharedSongId?.let { resolveSharedSongs(listOf(it)) }
                    markPeerMessagesRead(myId)
                }
        }
    }

    private fun listenForStatusUpdates(myId: String) {
        viewModelScope.launch {
            chatRepository.observeSentMessageStatusUpdates(myId)
                .collect { updated ->
                    _uiState.update { state ->
                        state.copy(messages = state.messages.map { if (it.id == updated.id) updated else it })
                    }
                }
        }
    }

    private fun listenForTyping(myId: String) {
        viewModelScope.launch {
            chatRepository.observeTypingStatus(myId, peerUserId)
                .collect { isTyping -> _uiState.update { it.copy(isPeerTyping = isTyping) } }
        }
    }

    private fun onDraftChanged(text: String) {
        _uiState.update { it.copy(draftText = text) }

        val myId = _uiState.value.currentUserId
        if (myId.isEmpty()) return

        if (text.isNotEmpty() && !isTypingSignalSent) {
            isTypingSignalSent = true
            viewModelScope.launch { chatRepository.sendTypingStatus(myId, peerUserId, isTyping = true) }
        }

        stoppedTypingJob?.cancel()
        stoppedTypingJob = viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            if (isTypingSignalSent) {
                isTypingSignalSent = false
                chatRepository.sendTypingStatus(myId, peerUserId, isTyping = false)
            }
        }
    }

    // ✨ ۱. به محض کلیک کاربر، آهنگ بلافاصله در حافظهٔ UI قرار می‌گیرد
    private fun shareSong(song: Song) {
        _uiState.update { state ->
            state.copy(sharedSongs = state.sharedSongs + (song.id to song))
        }
        sendMessage(text = song.title, sharedSongId = song.id)
        _uiState.update { it.copy(isSongPickerOpen = false) }
    }

    private fun sendMessage(text: String, sharedSongId: String?) {
        val trimmed = text.trim()
        val myId = _uiState.value.currentUserId
        if (trimmed.isEmpty() || myId.isEmpty()) return

        stoppedTypingJob?.cancel()
        if (isTypingSignalSent) {
            isTypingSignalSent = false
            viewModelScope.launch { chatRepository.sendTypingStatus(myId, peerUserId, isTyping = false) }
        }

        _uiState.update { it.copy(draftText = "", isSending = true) }
        viewModelScope.launch {
            runCatching { chatRepository.sendMessage(myId, peerUserId, trimmed, sharedSongId) }
                .onSuccess { sent ->
                    _uiState.update { it.copy(isSending = false, messages = it.messages + sent) }
                    sharedSongId?.let { resolveSharedSongs(listOf(it)) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSending = false, errorMessage = e.message) }
                }
        }
    }

    // ✨ ۲. اولویت با جستجوی سریع در RAM است؛ اگر پیدا نشد سپس دیتابیس استعلام می‌شود
    private fun resolveSharedSongs(songIds: List<String>) {
        val idsToResolve = songIds.filterNot { it in _uiState.value.sharedSongs.keys }
        if (idsToResolve.isEmpty()) return

        viewModelScope.launch {
            val currentAvailable = _uiState.value.availableSongs
            val remainingIds = mutableListOf<String>()

            // جستجوی آنی در رم
            idsToResolve.forEach { songId ->
                val foundInMemory = currentAvailable.find { it.id == songId }
                if (foundInMemory != null) {
                    _uiState.update { it.copy(sharedSongs = it.sharedSongs + (songId to foundInMemory)) }
                } else {
                    remainingIds.add(songId)
                }
            }

            // برای آیدی‌های باقی‌مانده (اگر از دیتابیس صدا زده شوند)
            remainingIds.forEach { songId ->
                runCatching { songRepository.getSongById(songId) }
                    .onSuccess { song ->
                        if (song != null) {
                            _uiState.update { it.copy(sharedSongs = it.sharedSongs + (songId to song)) }
                        }
                    }
            }
        }
    }
}