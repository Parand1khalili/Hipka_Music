package com.hipka.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArtistDto(
    @SerialName("artist_name") val artistName: String,
    @SerialName("total_play_count") val totalPlayCount: Int,
    @SerialName("artist_image_url") val artistImageUrl: String? = null
)