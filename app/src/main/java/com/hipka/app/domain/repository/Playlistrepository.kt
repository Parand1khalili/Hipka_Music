package com.hipka.app.domain.repository

import com.hipka.app.domain.model.Playlist

interface PlaylistRepository {
    suspend fun getPlaylistsByCategory(category: String): List<Playlist>
    suspend fun getUserPlaylists(ownerId: String): List<Playlist>
}
