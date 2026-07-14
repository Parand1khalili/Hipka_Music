package com.hipka.app.data.remote.api

import com.hipka.app.data.remote.dto.SongDto
import retrofit2.http.GET

interface SongApi {
    @GET("rest/v1/songs?select=*")
    suspend fun testGetSongs(): List<SongDto>
}