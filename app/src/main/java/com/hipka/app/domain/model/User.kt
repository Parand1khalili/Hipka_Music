package com.hipka.app.domain.model

import com.hipka.app.data.remote.dto.UserDto

data class User(
    val id: String,
    val name: String,
    val username: String? = null,
    val email: String? = null,
    val avatarUrl: String?,
    val isPremium: Boolean = false,
    val createdAt: String? = null
)

fun UserDto.toDomain() = User(
    id = id,
    name = name,
    username = username,
    email = email,
    avatarUrl = avatarUrl,
    isPremium = isPremium,
    createdAt = createdAt
)