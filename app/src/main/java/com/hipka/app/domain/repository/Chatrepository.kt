package com.hipka.app.domain.repository

import com.hipka.app.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun getConversationHistory(myUserId: String, peerUserId: String): List<Message>
    suspend fun sendMessage(myUserId: String, receiverId: String, text: String, sharedSongId: String?): Message

    /** Emits every new message addressed to [myUserId], from any sender, as it arrives over Realtime. */
    fun observeIncomingMessages(myUserId: String): Flow<Message>
}
