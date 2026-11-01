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
        if (com.hipka.app.BuildConfig.LEGACY_DEMO_AUTH) playlistApi.getPlaylistsByCategory(categoryFilter = "ilike.$category").map { it.toDomain() } else emptyList()

    override suspend fun getUserPlaylists(ownerId: String): List<Playlist> =
        if (com.hipka.app.BuildConfig.LEGACY_DEMO_AUTH) playlistApi.getUserPlaylists(ownerIdFilter = "eq.$ownerId").map { it.toDomain() } else emptyList()
}
