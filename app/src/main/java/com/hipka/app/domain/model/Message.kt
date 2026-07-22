package com.hipka.app.domain.model

enum class MessageStatus {
    SENDING,
    SENT,
    READ;

    companion object {
        fun fromRaw(value: String?): MessageStatus {
            if (value.isNullOrBlank()) return SENT
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: SENT
        }
    }
}

data class Message(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val status: MessageStatus = MessageStatus.SENDING,
    val sharedSongId: String?,
    val timestamp: Long
)