package com.hipka.app.domain.model

enum class MessageStatus { SENDING, SENT, READ }

data class Message(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val timestamp: Long,
    val status: MessageStatus,
    val sharedSong: Song? = null // share song in chat
)