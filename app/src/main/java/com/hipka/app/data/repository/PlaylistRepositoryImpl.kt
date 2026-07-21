package com.hipka.app.data.repository

import com.hipka.app.data.remote.api.PlaylistApi
import com.hipka.app.data.remote.dto.toDomain
import com.hipka.app.domain.model.Playlist
import com.hipka.app.domain.repository.PlaylistRepository
import javax.inject.Inject

class PlaylistRepositoryImpl @Inject constructor(
    private val playlistApi: PlaylistApi
) : PlaylistRepository {

    override suspend fun getPlaylistsByCategory(category: String): List<Playlist> =
        playlistApi.getPlaylistsByCategory(categoryFilter = "ilike.$category").map { it.toDomain() }

    override suspend fun getUserPlaylists(ownerId: String): List<Playlist> =
        playlistApi.getUserPlaylists(ownerIdFilter = "eq.$ownerId").map { it.toDomain() }
}