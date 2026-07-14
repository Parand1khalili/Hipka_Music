package com.hipka.app.data.repository

import com.hipka.app.data.remote.dto.MessageDto
import com.hipka.app.data.remote.dto.toDomain
import com.hipka.app.domain.model.Message
import com.hipka.app.domain.repository.ChatRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.decodeRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ChatRepository {

    override suspend fun getConversationHistory(myUserId: String, peerUserId: String): List<Message> {
        val sentByMe = supabaseClient.from("messages")
            .select {
                filter {
                    eq("sender_id", myUserId)
                    eq("receiver_id", peerUserId)
                }
            }
            .decodeList<MessageDto>()

        val sentByPeer = supabaseClient.from("messages")
            .select {
                filter {
                    eq("sender_id", peerUserId)
                    eq("receiver_id", myUserId)
                }
            }
            .decodeList<MessageDto>()

        return (sentByMe + sentByPeer)
            .sortedBy { it.timestamp }
            .map { it.toDomain() }
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
        supabaseClient.from("messages").insert(dto)
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
                try {
                    val messageDto = action.decodeRecord<MessageDto>()
                    trySend(messageDto.toDomain())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        channel.subscribe()

        awaitClose {
            collectJob.cancel()
            runBlocking { channel.unsubscribe() }
        }
    }
}