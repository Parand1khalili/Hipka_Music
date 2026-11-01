package com.hipka.app.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AudiusApi {
    @GET("tracks/trending")
    suspend fun trending(@Query("limit") limit: Int = 100): AudiusTracks
    @GET("tracks/search")
    suspend fun search(@Query("query") query: String, @Query("limit") limit: Int = 100): AudiusTracks
    @GET("tracks/{id}")
    suspend fun track(@Path("id") id: String): AudiusTrackResponse
}

@Serializable
data class AudiusTrackResponse(val data: AudiusTrack? = null)

@Serializable
data class AudiusTracks(val data: List<AudiusTrack> = emptyList())

@Serializable
data class AudiusUser(val name: String = "Unknown artist")

@Serializable
data class AudiusArtwork(@SerialName("480x480") val medium: String? = null)

@Serializable
data class AudiusTrack(
    val id: String,
    val title: String,
    val user: AudiusUser,
    val artwork: AudiusArtwork? = null,
    @SerialName("play_count") val plays: Int = 0,
    @SerialName("favorite_count") val favorites: Long = 0,
    @SerialName("release_date") val released: String? = null,
    @SerialName("is_streamable") val streamable: Boolean = false,
    @SerialName("is_stream_gated") val gated: Boolean = true,
    @SerialName("is_downloadable") val downloadable: Boolean = false,
    @SerialName("is_download_gated") val downloadGated: Boolean = true
) {
    fun toSongDto(): com.hipka.app.data.remote.dto.SongDto? {
        if (!streamable || gated || !id.matches(Regex("[A-Za-z0-9]+"))) return null
        return com.hipka.app.data.remote.dto.SongDto(
            id = "audius:$id", title = title, artistName = user.name,
            coverImageUrl = artwork?.medium.orEmpty(),
            audioUrl = "https://api.audius.co/v1/tracks/$id/stream?app_name=HipkaMusic",
            playCount = plays, likesCount = favorites, releaseDate = released.orEmpty()
        )
    }
}
