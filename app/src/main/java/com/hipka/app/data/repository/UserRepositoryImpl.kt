package com.hipka.app.data.repository

import com.hipka.app.data.local.datastore.SessionManager
import com.hipka.app.data.remote.api.UserApi
import com.hipka.app.data.remote.dto.PremiumUpdateDto
import com.hipka.app.data.remote.dto.UserDto
import com.hipka.app.data.remote.dto.UserFollowDto
import com.hipka.app.data.remote.dto.toDomain
import com.hipka.app.domain.model.User
import com.hipka.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val sessionManager: SessionManager
) : UserRepository {

    override suspend fun getUserById(id: String): User? =
        userApi.getUserById(idFilter = "eq.$id").firstOrNull()?.toDomain()

    override suspend fun getAllUsers(): List<User> =
        userApi.getAllUsers().map { it.toDomain() }

    override suspend fun searchUsersByName(query: String): List<User> =
        userApi.searchUsersByName(nameFilter = "ilike.*$query*").map { it.toDomain() }

    override suspend fun getFollowingIds(userId: String): List<String> =
        userApi.getFollowings(followerFilter = "eq.$userId").map { it.followingId }

    override suspend fun getFollowerIds(userId: String): List<String> =
        userApi.getFollowers(followingFilter = "eq.$userId").map { it.followerId }

    override suspend fun followUser(followerId: String, followingId: String) {
        userApi.followUser(UserFollowDto(followerId = followerId, followingId = followingId))
    }

    override suspend fun unfollowUser(followerId: String, followingId: String) {
        userApi.unfollowUser(
            followerFilter = "eq.$followerId",
            followingFilter = "eq.$followingId"
        )
    }

    override suspend fun setPremiumStatus(userId: String, isPremium: Boolean) {
        userApi.updatePremiumStatus(
            idFilter = "eq.$userId",
            body = PremiumUpdateDto(isPremium = isPremium)
        )
    }

    // ورود با ایمیل یا نام کاربری
    override suspend fun login(emailOrUsername: String, password: String): Result<User> {
        return try {
            val cleanIdentifier = emailOrUsername.trim()
            val users = userApi.loginUserOr(
                orFilter = "(email.eq.$cleanIdentifier,username.eq.$cleanIdentifier)",
                passwordFilter = "eq.$password"
            )
            val userDto = users.firstOrNull()
            if (userDto != null) {
                val user = userDto.toDomain()
                sessionManager.setCurrentUser(user.id)
                Result.success(user)
            } else {
                Result.failure(Exception("AUTH_INVALID_CREDENTIALS"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun register(name: String, username: String, email: String, password: String, gender: String): Result<User> {
        return try {
            val newUserId = UUID.randomUUID().toString()
            val defaultAvatarName = if (gender == "female") "avatar_female" else "avatar_male"
            val newDto = UserDto(
                id = newUserId,
                name = name.trim(),
                username = username.trim(),
                email = email.trim(),
                password = password,
                avatarUrl = defaultAvatarName,
                isPremium = false,
                createdAt = null
            )
            val createdUsers = userApi.registerUser(newDto)
            val user = createdUsers.firstOrNull()?.toDomain() ?: newDto.toDomain()
            sessionManager.setCurrentUser(user.id)
            Result.success(user)
        } catch (e: Exception) {
            android.util.Log.e("REGISTER_ERROR", "Registration failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        sessionManager.clearCurrentUser()
    }

    override fun isLoggedIn(): Flow<Boolean> = sessionManager.isLoggedIn

    override fun getCurrentUserId(): Flow<String?> = sessionManager.currentUserId
}