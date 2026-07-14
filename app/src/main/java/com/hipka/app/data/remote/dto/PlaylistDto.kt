package com.hipka.app.data.remote.dto

import com.hipka.app.domain.model.Playlist
import com.hipka.app.domain.model.PlaylistCategory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDto(
    val id: String,
    val title: String,
    @SerialName("cover_image_url")
    val coverImageUrl: String? = null,
    val category: String = "user",
    @SerialName("owner_id")
    val ownerId: String? = null
)

fun PlaylistDto.toDomain(): Playlist = Playlist(
    id = id,
    title = title,
    coverImageUrl = coverImageUrl,
    category = PlaylistCategory.fromRaw(category),
    ownerId = ownerId
)