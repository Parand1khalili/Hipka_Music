package com.hipka.app.data.remote.api
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ToggleLikeRequest(
    @SerialName("p_user_id") val pUserId: String,
    @SerialName("p_song_id") val pSongId: String,
    @SerialName("p_is_liked") val pIsLiked: Boolean
)