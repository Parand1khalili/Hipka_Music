package com.hipka.app.presentation.features.chat

import com.hipka.app.domain.model.Message

data class ChatUiState(
    val currentUserId: String = "",
    val peerUserId: String = "",
    val messages: List<Message> = emptyList(),
    val draftText: String = "",
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ChatIntent {
    data class DraftChanged(val text: String) : ChatIntent
    data object SendMessage : ChatIntent
}