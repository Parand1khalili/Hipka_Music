package com.hipka.app.data.repository

import com.hipka.app.data.local.dao.MessageDao
import com.hipka.app.data.local.entity.OfflineMessageEntity
import com.hipka.app.data.remote.api.MessageApi
import com.hipka.app.data.remote.dto.MessageDto
import com.hipka.app.data.remote.dto.toDomain
import com.hipka.app.domain.model.Message
import com.hipka.app.domain.model.MessageStatus
import com.hipka.app.domain.repository.ChatRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import java.util.UUID
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val messageApi: MessageApi,
    private val messageDao: MessageDao,
    private val supabaseClient: SupabaseClient
) : ChatRepository {

    @Serializable
    private data class TypingPayload(val senderId: String, val isTyping: Boolean)

    // ✨ ساختار سبک اختصاصی برای پکت‌های آپدیت وضعیت سوبابیس جهت جلوگیری از خطای سریالایز
    @Serializable
    private data class MessageStatusUpdateDto(val id: String, val status: String)

    // جریان انتشار تغییرات وضعیت برای تست لوکال/دیتابیس
    private val localStatusUpdatesFlow = MutableSharedFlow<Message>(extraBufferCapacity = 64)

    private companion object {
        const val TYPING_EVENT = "typing"
    }

    override suspend fun getConversationHistory(myUserId: String, peerUserId: String): List<Message> {
        return try {
            val sentByMe = messageApi.getConversation(
                orQuery = "(and(sender_id.eq.$myUserId,receiver_id.eq.$peerUserId))"
            )
            val sentByPeer = messageApi.getConversation(
                orQuery = "(and(sender_id.eq.$peerUserId,receiver_id.eq.$myUserId))"
            )
            val merged = (sentByMe + sentByPeer).sortedBy { it.timestamp }

            messageDao.insertMessages(merged.map { it.toEntity() })
            merged.map { it.toDomain() }
        } catch (e: Exception) {
            messageDao.getConversationOnce(myUserId, peerUserId).map { it.toDomain() }
        }
    }

    override suspend fun sendMessage(
        myUserId: String,
        receiverId: String,
        text: String,
        sharedSongId: String?
    ): Message {
        val dto = MessageDto(
            id = UUID.randomUUID().toString(),
            senderId = myUserId,
            receiverId = receiverId,
            text = text,
            status = "SENT",
            sharedSongId = sharedSongId,
            timestamp = System.currentTimeMillis()
        )
        messageApi.sendMessage(dto)
        messageDao.insertMessage(dto.toEntity())
        return dto.toDomain()
    }

    override fun observeIncomingMessages(myUserId: String): Flow<Message> = callbackFlow {
        val channel = supabaseClient.channel("messages-inbox-$myUserId")

        val changes = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
            filter = "receiver_id=eq.$myUserId"
        }

        val collectJob = launch {
            changes.collect { action ->
                val dto = action.decodeRecord<MessageDto>()
                messageDao.insertMessage(dto.toEntity())
                trySend(dto.toDomain())
            }
        }

        channel.subscribe()

        awaitClose {
            collectJob.cancel()
            runBlocking { channel.unsubscribe() }
        }
    }

    override fun observeSentMessageStatusUpdates(myUserId: String): Flow<Message> = callbackFlow {
        val channel = supabaseClient.channel("messages-sent-status-$myUserId")

        val changes = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "messages"
            filter = "sender_id=eq.$myUserId"
        }

        // ۱. شنیدن تغییرات لوکال (برای تست تک‌گوشی و حالت آفلاین)
        val localJob = launch {
            localStatusUpdatesFlow
                .filter { it.senderId == myUserId }
                .collect { trySend(it) }
        }

        // ۲. شنیدن تغییرات Realtime سوبابیس (بدون خطای سریالایز)
        val collectJob = launch {
            changes.collect { action ->
                try {
                    val updateDto = action.decodeRecord<MessageStatusUpdateDto>()
                    messageDao.updateStatus(updateDto.id, updateDto.status)
                    val updatedEntity = messageDao.getMessageById(updateDto.id)
                    if (updatedEntity != null) {
                        trySend(updatedEntity.toDomain())
                    }
                } catch (_: Exception) {
                    // Fallback
                    runCatching {
                        val fullDto = action.decodeRecord<MessageDto>()
                        messageDao.updateStatus(fullDto.id, fullDto.status)
                        trySend(fullDto.toDomain())
                    }
                }
            }
        }

        channel.subscribe()

        awaitClose {
            localJob.cancel()
            collectJob.cancel()
            runBlocking { channel.unsubscribe() }
        }
    }

    override suspend fun markConversationAsRead(myUserId: String, peerUserId: String) {
        try {
            messageApi.markConversationAsRead(
                senderIdFilter = "eq.$peerUserId",
                receiverIdFilter = "eq.$myUserId"
            )
        } catch (_: Exception) {
        }

        messageDao.updateStatusForConversation(
            senderId = peerUserId,
            receiverId = myUserId,
            status = MessageStatus.READ.name
        )

        // ✨ انتشار تغییر وضعیت سین شدن به جریان زنده لوکال
        val conversation = messageDao.getConversationOnce(myUserId, peerUserId)
        conversation.filter { it.senderId == peerUserId && it.status == MessageStatus.READ.name }
            .forEach { entity ->
                localStatusUpdatesFlow.emit(entity.toDomain())
            }
    }

    override fun observeTypingStatus(myUserId: String, peerUserId: String): Flow<Boolean> = callbackFlow {
        val channel = supabaseClient.channel(typingChannelName(myUserId, peerUserId))

        val collectJob = launch {
            channel.broadcastFlow<TypingPayload>(event = TYPING_EVENT)
                .filter { it.senderId == peerUserId }
                .map { it.isTyping }
                .collect { isTyping -> trySend(isTyping) }
        }

        channel.subscribe()

        awaitClose {
            collectJob.cancel()
            runBlocking { channel.unsubscribe() }
        }
    }

    override suspend fun sendTypingStatus(myUserId: String, peerUserId: String, isTyping: Boolean) {
        val channel = supabaseClient.channel(typingChannelName(myUserId, peerUserId))

        val payload = TypingPayload(senderId = myUserId, isTyping = isTyping)
        val jsonPayload = Json.encodeToJsonElement(payload).jsonObject

        channel.broadcast(event = TYPING_EVENT, message = jsonPayload)
    }

    private fun typingChannelName(userIdA: String, userIdB: String): String =
        "typing-" + listOf(userIdA, userIdB).sorted().joinToString("-")

    private fun MessageDto.toEntity() = OfflineMessageEntity(
        id = id,
        senderId = senderId,
        receiverId = receiverId,
        text = text,
        timestamp = timestamp,
        status = status,
        sharedSongId = sharedSongId
    )

    private fun OfflineMessageEntity.toDomain() = Message(
        id = id,
        senderId = senderId,
        receiverId = receiverId,
        text = text,
        status = MessageStatus.fromRaw(status),
        sharedSongId = sharedSongId,
        timestamp = timestamp
    )
}