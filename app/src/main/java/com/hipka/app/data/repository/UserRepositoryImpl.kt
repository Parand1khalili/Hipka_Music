package com.hipka.app.data.repository

import com.hipka.app.data.remote.api.UserApi
import com.hipka.app.data.remote.dto.PremiumUpdateDto
import com.hipka.app.data.remote.dto.UserFollowDto
import com.hipka.app.data.remote.dto.toDomain
import com.hipka.app.domain.model.User
import com.hipka.app.domain.repository.UserRepository
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi
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
}