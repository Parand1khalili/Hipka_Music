package com.hipka.app.data.remote.dto

import com.hipka.app.domain.model.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class UserDto(
    val id: String,
    val name: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("is_premium")
    val isPremium: Boolean = false,
    @SerialName("created_at")
    val createdAt: String
)


fun UserDto.toDomain(): User = User(
    id = id,
    name = name,
    avatarUrl = avatarUrl,
    isPremium = isPremium,
    createdAt = createdAt
)