package com.hipka.app.domain.repository

import com.hipka.app.domain.model.User

interface UserRepository {
    suspend fun getUserById(id: String): User?
    suspend fun getAllUsers(): List<User>
    suspend fun searchUsersByName(query: String): List<User>
    suspend fun getFollowingIds(userId: String): List<String>
    suspend fun getFollowerIds(userId: String): List<String>

    // ✨ اضافه شدن متدهای مدیریت رابطه دیتابیس
    suspend fun followUser(followerId: String, followingId: String)
    suspend fun unfollowUser(followerId: String, followingId: String)
}