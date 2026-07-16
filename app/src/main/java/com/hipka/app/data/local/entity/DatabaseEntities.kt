package com.hipka.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// 1. History search
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long
)

// 2. Song state (Downloaded, Liked, Offline music)
@Entity(tableName = "songs")
data class LocalSongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artistName: String,
    val coverImageUrl: String,
    val audioUrl: String,
    val playCount: Int,
    val releaseDate: String,
    val isLiked: Boolean,
    val isDownloaded: Boolean,
    val localFilePath: String? // Downloaded file address on the phone
)

// 3. Chat history
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