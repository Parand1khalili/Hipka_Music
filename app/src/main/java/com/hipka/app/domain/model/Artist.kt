package com.hipka.app.domain.model

data class Artist(
    val id: String = "",
    val name: String,
    val totalPlayCount: Int,
    val imageUrl: String
)