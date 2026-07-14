package com.hipka.app.domain.model

enum class PlaylistCategory {
    WORLD, LOCAL, USER;

    companion object {
        fun fromRaw(value: String): PlaylistCategory =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: USER
    }
}

data class Playlist(
    val id: String,
    val title: String,
    val coverImageUrl: String?,
    val category: PlaylistCategory,
    val ownerId: String? // null for World/Local (editorial) playlists
)