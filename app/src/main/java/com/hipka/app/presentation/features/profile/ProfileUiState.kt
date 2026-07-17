package com.hipka.app.presentation.features.profile

import com.hipka.app.domain.model.User

data class ProfileUiState(
    val currentUser: User? = null,
    val allUsers: List<User> = emptyList(),
    val followingIds: Set<String> = emptySet(),
    val followerIds: Set<String> = emptySet(), // ✨ اضافه شدن لیست زنده فالوورها
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface ProfileIntent {
    data class SelectDemoUser(val userId: String) : ProfileIntent
    data object Logout : ProfileIntent
    data object Retry : ProfileIntent
}