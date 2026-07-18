package com.hipka.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// 1. History search
@Entity(
    tableName = "search_history",
    primaryKeys = ["userId", "query"] // کلید ترکیبی برای جلوگیری از تداخل سرچ یوزرها
)
data class SearchHistoryEntity(
    val userId: String,
    val query: String,
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
    val likesCount: Long,
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

// 4. recent songs
@Entity(
    tableName = "recent_songs",
    primaryKeys = ["userId", "songId"] // کلید ترکیبی برای جلوگیری از تداخل تاریخچه پخش
)
data class RecentSongEntity(
    val userId: String,
    val songId: String,
    val timestamp: Long
)

// 5. User Likes
@Entity(
    tableName = "local_user_likes",
    primaryKeys = ["userId", "songId"]
)
data class LocalUserLikeEntity(
    val userId: String,
    val songId: String
)