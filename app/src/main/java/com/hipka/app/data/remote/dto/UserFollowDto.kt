package com.hipka.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserFollowDto(
    @SerialName("follower_id") val followerId: String,
    @SerialName("following_id") val followingId: String
)