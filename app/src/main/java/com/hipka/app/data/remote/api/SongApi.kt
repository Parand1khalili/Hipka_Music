package com.hipka.app.data.remote.api

import com.hipka.app.data.remote.dto.SongDto
import retrofit2.http.GET
import retrofit2.http.Query

interface SongApi {
    @GET("rest/v1/songs?select=*")
    suspend fun testGetSongs(): List<SongDto>

// search in title and artist
    @GET("rest/v1/songs")
    suspend fun searchSongs(
        @Query("or") orQuery: String, // OR filter supabase
        @Query("select") select: String = "*"
    ): List<SongDto>
}