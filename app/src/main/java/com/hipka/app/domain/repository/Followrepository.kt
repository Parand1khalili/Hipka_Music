package com.hipka.app.domain.repository

import com.hipka.app.domain.model.User

interface FollowRepository {
    suspend fun follow(followerId: String, followingId: String)
    suspend fun unfollow(followerId: String, followingId: String)
    suspend fun getFollowingIds(followerId: String): Set<String>
    suspend fun getFollowing(followerId: String): List<User>
}