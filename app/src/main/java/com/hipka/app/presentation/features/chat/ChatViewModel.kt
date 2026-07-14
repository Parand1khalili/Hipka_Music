package com.hipka.app.presentation.features.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hipka.app.data.local.datastore.SessionManager
import com.hipka.app.domain.repository.ChatRepository
import com.hipka.app.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val sessionManager: SessionManager
) : ViewModel() {

    private val peerUserId: String = checkNotNull(savedStateHandle[Screen.ChatConversation.ARG_PEER_USER_ID])

    private val _uiState = MutableStateFlow(ChatUiState(peerUserId = peerUserId))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val myId = sessionManager.currentUserId.first()
            if (myId == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "No demo user selected — pick one on the Profile tab.") }
                return@launch
            }
            _uiState.update { it.copy(currentUserId = myId) }
            loadHistory(myId)
            listenForIncoming(myId)
        }
    }

    fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.DraftChanged -> _uiState.update { it.copy(draftText = intent.text) }
            ChatIntent.SendMessage -> sendMessage()
        }
    }

    private suspend fun loadHistory(myId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching { chatRepository.getConversationHistory(myId, peerUserId) }
            .onSuccess { history -> _uiState.update { it.copy(messages = history, isLoading = false) } }
            .onFailure { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
    }

    private fun listenForIncoming(myId: String) {
        viewModelScope.launch {
            chatRepository.observeIncomingMessages(myId)
                .filter { it.senderId == peerUserId } // this ViewModel only cares about this one conversation
                .collect { incoming ->
                    _uiState.update { state ->
                        if (state.messages.any { it.id == incoming.id }) state
                        else state.copy(messages = state.messages + incoming)
                    }
                }
        }
    }

    private fun sendMessage() {
        val text = _uiState.value.draftText.trim()
        val myId = _uiState.value.currentUserId
        if (text.isEmpty() || myId.isEmpty()) return

        _uiState.update { it.copy(draftText = "", isSending = true) }
        viewModelScope.launch {
            runCatching { chatRepository.sendMessage(myId, peerUserId, text, sharedSongId = null) }
                .onSuccess { sent ->
                    _uiState.update { it.copy(isSending = false, messages = it.messages + sent) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSending = false, errorMessage = e.message) }
                }
        }
    }
}