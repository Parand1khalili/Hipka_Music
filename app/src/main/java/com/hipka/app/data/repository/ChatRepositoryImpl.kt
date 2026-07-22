package com.hipka.app.data.repository

import android.util.Log
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
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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

    private val localStatusUpdatesFlow = MutableSharedFlow<Message>(extraBufferCapacity = 64)

    private val jsonFormatter = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // ✨ نگه‌داشتن مرجع چنل تایپینگ فعال جهت جلوگیری از disconnect/reconnect مداوم
    private var activeTypingChannel: RealtimeChannel? = null
    private var activeTypingChannelName: String? = null

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
        id: String,
        myUserId: String,
        receiverId: String,
        text: String,
        sharedSongId: String?
    ): Message {
        val dto = MessageDto(
            id = id,
            senderId = myUserId,
            receiverId = receiverId,
            text = text.ifBlank { " " },
            status = MessageStatus.SENT.name,
            sharedSongId = sharedSongId?.ifBlank { null },
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(dto.toEntity())

        runCatching {
            messageApi.sendMessage(dto)
        }.onFailure { e ->
            Log.e("ChatRepo", "Network error on sendMessage: ${e.message}")
        }

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
                runCatching {
                    val dto = action.decodeRecord<MessageDto>()
                    messageDao.insertMessage(dto.toEntity())
                    trySend(dto.toDomain())
                }.onFailure { e ->
                    Log.e("ChatRepo", "Error parsing incoming message: ${e.message}")
                }
            }
        }

        channel.subscribe()

        awaitClose {
            collectJob.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { channel.unsubscribe() }
            }
        }
    }

    override fun observeSentMessageStatusUpdates(myUserId: String): Flow<Message> = callbackFlow {
        val channel = supabaseClient.channel("messages-sent-status-$myUserId")

        val changes = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "messages"
            filter = "sender_id=eq.$myUserId"
        }

        val localJob = launch {
            localStatusUpdatesFlow
                .filter { it.senderId == myUserId }
                .collect { trySend(it) }
        }

        val collectJob = launch {
            changes.collect { action ->
                runCatching {
                    val recordJson = action.record
                    val msgId = recordJson["id"]?.toString()?.replace("\"", "")
                    val statusStr = recordJson["status"]?.toString()?.replace("\"", "") ?: "READ"

                    if (!msgId.isNullOrEmpty()) {
                        messageDao.updateStatus(msgId, statusStr)
                        val updatedEntity = messageDao.getMessageById(msgId)
                        if (updatedEntity != null) {
                            trySend(updatedEntity.toDomain())
                        }
                    }
                }.onFailure {
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
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { channel.unsubscribe() }
            }
        }
    }

    override suspend fun markConversationAsRead(myUserId: String, peerUserId: String) {
        messageDao.updateStatusForConversation(
            senderId = peerUserId,
            receiverId = myUserId,
            status = MessageStatus.READ.name
        )

        runCatching {
            messageApi.markConversationAsRead(
                senderIdFilter = "eq.$peerUserId",
                receiverIdFilter = "eq.$myUserId"
            )
        }.onFailure { e ->
            Log.e("ChatRepo", "Error marking as read on remote: ${e.message}")
        }

        val conversation = messageDao.getConversationOnce(myUserId, peerUserId)
        conversation.filter { it.senderId == peerUserId && it.status == MessageStatus.READ.name }
            .forEach { entity ->
                localStatusUpdatesFlow.emit(entity.toDomain())
            }
    }

    // ✨ ۱. اتصال و سابسکرایب پایدار چنل تایپینگ
    override fun observeTypingStatus(myUserId: String, peerUserId: String): Flow<Boolean> = callbackFlow {
        val chName = typingChannelName(myUserId, peerUserId)
        val channel = getOrCreateTypingChannel(chName)

        val collectJob = launch {
            channel.broadcastFlow<TypingPayload>(event = TYPING_EVENT)
                .filter { it.senderId == peerUserId }
                .map { it.isTyping }
                .collect { isTyping ->
                    Log.d("ChatRepo", "Received typing status: $isTyping")
                    trySend(isTyping)
                }
        }

        awaitClose {
            collectJob.cancel()
        }
    }

    // ✨ ۲. ارسال سیگنال تایپینگ روی همان کانال فعال و متصل
    override suspend fun sendTypingStatus(myUserId: String, peerUserId: String, isTyping: Boolean) {
        runCatching {
            val chName = typingChannelName(myUserId, peerUserId)
            val channel = getOrCreateTypingChannel(chName)

            val payload = TypingPayload(senderId = myUserId, isTyping = isTyping)
            val jsonPayload = jsonFormatter.encodeToJsonElement(payload).jsonObject

            Log.d("ChatRepo", "Broadcasting typing: $isTyping on $chName")
            channel.broadcast(event = TYPING_EVENT, message = jsonPayload)
        }.onFailure { e ->
            Log.e("ChatRepo", "Error in sendTypingStatus: ${e.message}")
        }
    }

    private suspend fun getOrCreateTypingChannel(chName: String): RealtimeChannel {
        if (activeTypingChannelName == chName && activeTypingChannel != null) {
            return activeTypingChannel!!
        }
        val channel = supabaseClient.channel(chName)
        channel.subscribe()
        activeTypingChannel = channel
        activeTypingChannelName = chName
        return channel
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