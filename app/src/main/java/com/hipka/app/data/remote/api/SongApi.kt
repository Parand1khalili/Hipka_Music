package com.hipka.app.data.remote.api

import com.hipka.app.data.remote.dto.ArtistDto
import com.hipka.app.data.remote.dto.SongDto
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Body
import retrofit2.http.POST



interface SongApi {
    @GET("rest/v1/songs?select=*")
    suspend fun testGetSongs(): List<SongDto>

// search in title and artist
    @GET("rest/v1/songs")
    suspend fun searchSongs(
        @Query("or") orQuery: String, // OR filter supabase
        @Query("select") select: String = "*"
    ): List<SongDto>

    @POST("rest/v1/rpc/increment_play_count")
    suspend fun incrementPlayCount(
        @Body body: Map<String, String> // ارسال {"song_id": "uuid"} به سرور
    )

    // اضافه کردن ریکوئست برای لایک و آنلایک زنده در سابابیس
    @POST("rest/v1/rpc/toggle_song_like")
    suspend fun toggleSongLikeRemote(
        @Body body: Map<String, Any> // ارسال شامل p_user_id, p_song_id, p_is_liked
    )
    @POST("rest/v1/rpc/toggle_song_like")
    suspend fun toggleSongLikeRemote(
        @Body request: ToggleLikeRequest
    ): retrofit2.Response<Unit> // استفاده از Response برای گرفتن ارورهای احتمالی

    @GET("rest/v1/top_artists?select=*")
    suspend fun getTopArtists(): List<ArtistDto>

}
