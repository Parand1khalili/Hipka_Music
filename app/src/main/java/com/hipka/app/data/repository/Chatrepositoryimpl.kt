package com.hipka.app.data.repository

import com.hipka.app.data.remote.api.MessageApi
import com.hipka.app.data.remote.dto.MessageDto
import com.hipka.app.data.remote.dto.toDomain
import com.hipka.app.domain.model.Message
import com.hipka.app.domain.repository.ChatRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import io.github.jan.supabase.realtime.decodeRecord

/**
 * Regular reads/writes go through Retrofit (MessageApi), matching every
 * other table in the app. Retrofit fundamentally can't do a WebSocket
 * subscription, so [observeIncomingMessages] alone uses supabase-kt's
 * Realtime client (see NetworkModule) — the one place in the app that needs
 * it, to satisfy the "DM must be WebSocket, no polling" requirement.
 */
class ChatRepositoryImpl @Inject constructor(
    private val messageApi: MessageApi,
    private val supabaseClient: SupabaseClient
) : ChatRepository {

    override suspend fun getConversationHistory(myUserId: String, peerUserId: String): List<Message> {
        val orQuery = "(and(sender_id.eq.$myUserId,receiver_id.eq.$peerUserId)," +
                "and(sender_id.eq.$peerUserId,receiver_id.eq.$myUserId))"
        return messageApi.getConversation(orQuery = orQuery).map { it.toDomain() }
    }

    override suspend fun sendMessage(
        myUserId: String,
        receiverId: String,
        text: String,
        sharedSongId: String?
    ): Message {
        // Client-generated id lets the UI de-duplicate against whatever
        // Realtime echoes back for this same row.
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
                trySend(action.decodeRecord<MessageDto>().toDomain())
            }
        }

        channel.subscribe()

        awaitClose {
            collectJob.cancel()
            // unsubscribe() is suspend in most supabase-kt versions; this is a
            // short, best-effort cleanup call from a non-suspend callback.
            runBlocking { channel.unsubscribe() }
        }
    }
}
