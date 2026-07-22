package com.hipka.app.data.remote.dto

import com.hipka.app.domain.model.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("username") val username: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("password") val password: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_premium") val isPremium: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

fun UserDto.toDomain(): User = User(
    id = id,
    name = name,
    avatarUrl = avatarUrl,
    isPremium = isPremium,
    createdAt = createdAt ?: ""
)