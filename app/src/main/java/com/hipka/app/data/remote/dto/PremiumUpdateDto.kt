package com.hipka.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable
data class PremiumUpdateDto(
    @SerialName("is_premium")
    val isPremium: Boolean
)