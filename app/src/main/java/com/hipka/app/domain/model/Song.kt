package com.hipka.app.domain.model

data class Song(
    val id: String,
    val title: String,
    val artistName: String,
    val coverImageUrl: String,
    val audioUrl: String,
    val isLiked: Boolean = false,
    val playCount: Int = 0,         // Added for "Popular" sorting
    val releaseDate: String = "",    // Added for "New Releases" sorting (e.g., "2026-07-15")
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null // to do
)