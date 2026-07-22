package com.hipka.app.domain.repository

import com.hipka.app.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun getConversationHistory(myUserId: String, peerUserId: String): List<Message>

    suspend fun sendMessage(
        id: String = java.util.UUID.randomUUID().toString(),
        myUserId: String,
        receiverId: String,
        text: String,
        sharedSongId: String? = null
    ): Message

    /** Emits every new message addressed to [myUserId], from any sender, as it arrives over Realtime. */
    fun observeIncomingMessages(myUserId: String): Flow<Message>

    /** Emits messages [myUserId] sent whenever their `status` changes (e.g. flips to READ). */
    fun observeSentMessageStatusUpdates(myUserId: String): Flow<Message>

    /** Marks everything [peerUserId] sent to [myUserId] as READ. */
    suspend fun markConversationAsRead(myUserId: String, peerUserId: String)

    /** True while the other side of this conversation is actively typing. */
    fun observeTypingStatus(myUserId: String, peerUserId: String): Flow<Boolean>

    suspend fun sendTypingStatus(myUserId: String, peerUserId: String, isTyping: Boolean)
}