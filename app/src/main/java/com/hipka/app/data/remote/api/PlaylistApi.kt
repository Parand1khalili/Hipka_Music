package com.hipka.app.data.remote.api

import com.hipka.app.data.remote.dto.PlaylistDto
import retrofit2.http.GET
import retrofit2.http.Query

interface PlaylistApi {

    @GET("rest/v1/playlists")
    suspend fun getPlaylistsByCategory(
        @Query("category") categoryFilter: String, // pass as "ilike.<category>" (case-insensitive, see PlaylistRepositoryImpl)
        @Query("select") select: String = "*"
    ): List<PlaylistDto>

    @GET("rest/v1/playlists")
    suspend fun getUserPlaylists(
        @Query("owner_id") ownerIdFilter: String, // pass as "eq.<ownerId>"
        @Query("select") select: String = "*"
    ): List<PlaylistDto>
}