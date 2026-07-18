package com.hipka.app.data.repository

import com.hipka.app.data.remote.api.UserApi
import com.hipka.app.data.remote.dto.toDomain
import com.hipka.app.domain.model.User
import com.hipka.app.domain.repository.UserRepository
import javax.inject.Inject
import  com.hipka.app.data.remote.dto.UserFollowDto

class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi
) : UserRepository {

    override suspend fun getUserById(id: String): User? =
        userApi.getUserById(idFilter = "eq.$id").firstOrNull()?.toDomain()

    override suspend fun getAllUsers(): List<User> =
        userApi.getAllUsers().map { it.toDomain() }

    override suspend fun searchUsersByName(query: String): List<User> =
        userApi.searchUsersByName(nameFilter = "ilike.*$query*").map { it.toDomain() }

    // ✨ واکشی آیدی‌های کسانی که فالو کرده‌ایم و مپ کردن فیلد target
    override suspend fun getFollowingIds(userId: String): List<String> =
        userApi.getFollowings(followerFilter = "eq.$userId").map { it.followingId }

    // ✨ واکشی آیدی‌های کسانی که ما را فالو کرده‌اند و مپ کردن فیلد source
    override suspend fun getFollowerIds(userId: String): List<String> =
        userApi.getFollowers(followingFilter = "eq.$userId").map { it.followerId }

    // 💡 اضافه کردن به انتهای کلاس UserRepositoryImpl
    override suspend fun followUser(followerId: String, followingId: String) {
        userApi.followUser(UserFollowDto(followerId = followerId, followingId = followingId))
    }

    override suspend fun unfollowUser(followerId: String, followingId: String) {
        userApi.unfollowUser(
            followerFilter = "eq.$followerId",
            followingFilter = "eq.$followingId"
        )
    }
}