package com.hipka.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Used for decoding rows read back from `follows`. */
@Serializable
data class FollowDto(
    val id: Long? = null,
    @SerialName("follower_id")
    val followerId: String,
    @SerialName("following_id")
    val followingId: String
)

/**
 * Used for inserts only. `follows.id` is `generated always as identity`,
 * so it must never be included in the insert payload — a separate DTO
 * (rather than reusing [FollowDto] with a null id) guarantees that.
 */
@Serializable
data class FollowInsertDto(
    @SerialName("follower_id")
    val followerId: String,
    @SerialName("following_id")
    val followingId: String
)
