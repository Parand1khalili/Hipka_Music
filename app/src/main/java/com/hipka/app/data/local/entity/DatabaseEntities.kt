package com.hipka.app.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// history search
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long
)

// song state ( downloaded - liked - offline musics)
@Entity(tableName = "songs")
data class LocalSongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artistName: String,
    val coverImageUrl: String,
    val audioUrl: String,
    val isLiked: Boolean,
    val isDownloaded: Boolean,
    val localFilePath: String? // downloaded file address on phone
)

// chat history
@Entity(tableName = "offline_messages")
data class OfflineMessageEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val timestamp: Long,
    val status: String,
    val sharedSongId: String?
)