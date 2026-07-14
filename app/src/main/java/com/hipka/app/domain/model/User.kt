package com.hipka.app.domain.model

data class User(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val isPremium: Boolean = false, // premium users
    val createdAt: String
)