package com.hipka.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SongDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("artist_name") val artistName: String,
    @SerialName("cover_image_url") val coverImageUrl: String,
    @SerialName("audio_url") val audioUrl: String,
    @SerialName("play_count") val playCount: Int? = 0,
    @SerialName("likes_count") val likesCount: Long? = 0,
    @SerialName("release_date") val releaseDate: String? = ""
)