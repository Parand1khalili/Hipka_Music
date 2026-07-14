package com.hipka.app.data.repository

import com.hipka.app.data.remote.dto.PlaylistDto
import com.hipka.app.data.remote.dto.toDomain
import com.hipka.app.domain.model.Playlist
import com.hipka.app.domain.repository.PlaylistRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject

class PlaylistRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : PlaylistRepository {

    override suspend fun getPlaylistsByCategory(category: String): List<Playlist> =
        supabaseClient.from("playlists")
            .select { filter { eq("category", category) } }
            .decodeList<PlaylistDto>()
            .map { it.toDomain() }

    override suspend fun getUserPlaylists(ownerId: String): List<Playlist> =
        supabaseClient.from("playlists")
            .select { filter { eq("owner_id", ownerId) } }
            .decodeList<PlaylistDto>()
            .map { it.toDomain() }
}