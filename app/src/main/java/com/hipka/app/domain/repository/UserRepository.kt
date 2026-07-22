package com.hipka.app.domain.repository

import com.hipka.app.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUserById(id: String): User?
    suspend fun getAllUsers(): List<User>
    suspend fun searchUsersByName(query: String): List<User>

    suspend fun getFollowingIds(userId: String): List<String>
    suspend fun getFollowerIds(userId: String): List<String>
    suspend fun followUser(followerId: String, followingId: String)
    suspend fun unfollowUser(followerId: String, followingId: String)

    // متد تغییر وضعیت پریمیوم
    suspend fun setPremiumStatus(userId: String, isPremium: Boolean)

    // متدهای احراز هویت
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(
        name: String,
        username: String = "",
        email: String,
        password: String
    ): Result<User>
    suspend fun logout()
    fun isLoggedIn(): Flow<Boolean>
    fun getCurrentUserId(): Flow<String?>
}