package com.hipka.app.presentation.features.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.data.local.datastore.SessionManager
import com.hipka.app.domain.model.Message
import com.hipka.app.domain.model.MessageStatus
import com.hipka.app.domain.model.Song
import com.hipka.app.domain.repository.ChatRepository
import com.hipka.app.domain.repository.SongRepository
import com.hipka.app.domain.repository.UserRepository
import com.hipka.app.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
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
        viewModelScope.launch(Dispatchers.IO) {
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

            startHybridSyncEngine(myId)
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
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { chatRepository.sendTypingStatus(myId, peerUserId, isTyping = false) }
            }
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
                // ✨ اصلاح وضعیت پیام‌های لودشده از دیتابیس لوکال تا هرگز حالت ساعت نگیرند
                val sanitizedHistory = history.map { msg ->
                    if (msg.status == MessageStatus.SENDING) msg.copy(status = MessageStatus.SENT)
                    else msg
                }
                _uiState.update { it.copy(messages = sanitizedHistory, isLoading = false) }
                resolveSharedSongs(sanitizedHistory.mapNotNull { it.sharedSongId })
            }
            .onFailure { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
    }

    private fun startHybridSyncEngine(myId: String) {
        viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {
            while (true) {
                delay(2500)
                runCatching { chatRepository.getConversationHistory(myId, peerUserId) }
                    .onSuccess { freshHistory ->
                        if (freshHistory.isNotEmpty()) {
                            _uiState.update { state ->
                                var hasChanges = false
                                val currentMap = state.messages.associateBy { it.id }.toMutableMap()

                                freshHistory.forEach { freshMsg ->
                                    val existing = currentMap[freshMsg.id]
                                    if (existing == null) {
                                        val sanitized = if (freshMsg.status == MessageStatus.SENDING) freshMsg.copy(status = MessageStatus.SENT) else freshMsg
                                        currentMap[freshMsg.id] = sanitized
                                        hasChanges = true
                                    } else {
                                        val isStatusProgression = when {
                                            existing.status == MessageStatus.SENDING && freshMsg.status != MessageStatus.SENDING -> true
                                            existing.status == MessageStatus.SENT && freshMsg.status == MessageStatus.READ -> true
                                            else -> false
                                        }

                                        if (isStatusProgression) {
                                            currentMap[freshMsg.id] = existing.copy(status = freshMsg.status)
                                            hasChanges = true
                                        }
                                    }
                                }

                                if (hasChanges) {
                                    val sortedList = currentMap.values.sortedBy { it.timestamp }
                                    state.copy(messages = sortedList)
                                } else {
                                    state
                                }
                            }

                            val hasUnreadFromPeer = freshHistory.any { it.senderId == peerUserId && it.status != MessageStatus.READ }
                            if (hasUnreadFromPeer) {
                                markPeerMessagesRead(myId)
                            }
                        }
                    }
            }
        }
    }

    private fun loadAvailableSongsToShare() {
        viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {
            songRepository.getSongs()
                .catch { e -> Log.e("ChatViewModel", "Error in getSongs stream: ${e.message}") }
                .collect { songs ->
                    _uiState.update { state -> state.copy(availableSongs = songs) }
                    resolveSharedSongs(_uiState.value.messages.mapNotNull { it.sharedSongId })
                }
        }
    }

    private fun markPeerMessagesRead(myId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { chatRepository.markConversationAsRead(myId, peerUserId) }
        }
    }

    private fun listenForIncoming(myId: String) {
        viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {
            chatRepository.observeIncomingMessages(myId)
                .catch { e -> Log.e("ChatViewModel", "Error in incoming messages flow: ${e.message}") }
                .collect { incoming ->
                    if (incoming.senderId.trim().equals(peerUserId.trim(), ignoreCase = true)) {
                        _uiState.update { state ->
                            if (state.messages.any { it.id == incoming.id }) state
                            else state.copy(messages = state.messages + incoming, isPeerTyping = false)
                        }

                        viewModelScope.launch(Dispatchers.IO) {
                            markPeerMessagesRead(myId)
                        }

                        if (incoming.sharedSongId != null) {
                            viewModelScope.launch(Dispatchers.IO) {
                                resolveSharedSongs(listOf(incoming.sharedSongId))
                            }
                        }
                    }
                }
        }
    }

    private fun listenForStatusUpdates(myId: String) {
        viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {
            chatRepository.observeSentMessageStatusUpdates(myId)
                .catch { e -> Log.e("ChatViewModel", "Error in status updates flow: ${e.message}") }
                .collect { updated ->
                    _uiState.update { state ->
                        state.copy(messages = state.messages.map { if (it.id == updated.id) updated else it })
                    }
                }
        }
    }

    private fun listenForTyping(myId: String) {
        viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {
            chatRepository.observeTypingStatus(myId, peerUserId)
                .catch { e -> Log.e("ChatViewModel", "Error in typing status flow: ${e.message}") }
                .collect { isTyping -> _uiState.update { it.copy(isPeerTyping = isTyping) } }
        }
    }

    private fun onDraftChanged(text: String) {
        _uiState.update { it.copy(draftText = text) }

        val myId = _uiState.value.currentUserId
        if (myId.isEmpty()) return

        if (text.isNotEmpty() && !isTypingSignalSent) {
            isTypingSignalSent = true
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { chatRepository.sendTypingStatus(myId, peerUserId, isTyping = true) }
            }
        }

        stoppedTypingJob?.cancel()
        stoppedTypingJob = viewModelScope.launch(Dispatchers.IO) {
            delay(2000)
            if (isTypingSignalSent) {
                isTypingSignalSent = false
                runCatching { chatRepository.sendTypingStatus(myId, peerUserId, isTyping = false) }
            }
        }
    }

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
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { chatRepository.sendTypingStatus(myId, peerUserId, isTyping = false) }
            }
        }

        val generatedId = UUID.randomUUID().toString()
        val pendingMessage = Message(
            id = generatedId,
            senderId = myId,
            receiverId = peerUserId,
            text = trimmed,
            status = MessageStatus.SENDING,
            sharedSongId = sharedSongId,
            timestamp = System.currentTimeMillis()
        )

        _uiState.update { state ->
            state.copy(
                draftText = "",
                messages = state.messages + pendingMessage
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                chatRepository.sendMessage(
                    id = generatedId,
                    myUserId = myId,
                    receiverId = peerUserId,
                    text = trimmed,
                    sharedSongId = sharedSongId
                )
            }.onSuccess { sent ->
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.map {
                            if (it.id == generatedId) sent else it
                        }
                    )
                }
                sharedSongId?.let { resolveSharedSongs(listOf(it)) }
            }.onFailure { e ->
                Log.e("ChatViewModel", "Failed to send message online: ${e.message}")
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.map {
                            if (it.id == generatedId) it.copy(status = MessageStatus.SENT) else it
                        }
                    )
                }
            }
        }
    }

    private fun resolveSharedSongs(songIds: List<String>) {
        val idsToResolve = songIds.filterNot { it in _uiState.value.sharedSongs.keys }
        if (idsToResolve.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val currentAvailable = _uiState.value.availableSongs
            val remainingIds = mutableListOf<String>()

            idsToResolve.forEach { songId ->
                val foundInMemory = currentAvailable.find { it.id == songId }
                if (foundInMemory != null) {
                    _uiState.update { it.copy(sharedSongs = it.sharedSongs + (songId to foundInMemory)) }
                } else {
                    remainingIds.add(songId)
                }
            }

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