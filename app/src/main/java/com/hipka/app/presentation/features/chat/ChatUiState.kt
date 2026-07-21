package com.hipka.app.presentation.features.chat

import com.hipka.app.domain.model.Message
import com.hipka.app.domain.model.Song
import com.hipka.app.domain.model.User

data class ChatUiState(
    val currentUserId: String = "",
    val peerUserId: String = "",
    val peerUser: User? = null,
    val messages: List<Message> = emptyList(),
    val sharedSongs: Map<String, Song> = emptyMap(),
    val availableSongs: List<Song> = emptyList(),
    val isPeerTyping: Boolean = false,
    val draftText: String = "",
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val isSongPickerOpen: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ChatIntent {
    data class DraftChanged(val text: String) : ChatIntent
    data object SendMessage : ChatIntent
    data object ToggleSongPicker : ChatIntent
    data class ShareSong(val song: Song) : ChatIntent
}