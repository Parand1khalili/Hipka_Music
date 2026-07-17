package com.hipka.app.presentation.features.followedusers

import com.hipka.app.domain.model.User

data class FollowedUsersUiState(
    val users: List<User> = emptyList(),
    val followingIds: Set<String> = emptySet(),
    val followerIds: Set<String> = emptySet(), // ✨ اضافه شدن فیلد فالوورها برای حذف ارور کامپایل و واقعی‌سازی کامل
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface FollowedUsersIntent {
    data class ToggleFollow(val targetUserId: String) : FollowedUsersIntent
    data object Refresh : FollowedUsersIntent
}