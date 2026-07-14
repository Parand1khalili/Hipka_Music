package com.hipka.app.data.remote.dto

import com.hipka.app.domain.model.Message
import com.hipka.app.domain.model.MessageStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class MessageDto(
    val id: String,
    @SerialName("sender_id")
    val senderId: String,
    @SerialName("receiver_id")
    val receiverId: String,
    val text: String,
    val status: String = "SENDING",
    @SerialName("shared_song_id")
    val sharedSongId: String? = null,
    val timestamp: Long
)


fun MessageDto.toDomain(): Message = Message(
    id = id,
    senderId = senderId,
    receiverId = receiverId,
    text = text,
    status = MessageStatus.fromRaw(status),
    sharedSongId = sharedSongId,
    timestamp = timestamp
)