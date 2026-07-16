package com.hipka.app.data.repository

import com.hipka.app.data.remote.api.FollowApi
import com.hipka.app.data.remote.api.UserApi
import com.hipka.app.data.remote.dto.FollowInsertDto
import com.hipka.app.data.remote.dto.toDomain
import com.hipka.app.domain.model.User
import com.hipka.app.domain.repository.FollowRepository
import javax.inject.Inject

class FollowRepositoryImpl @Inject constructor(
    private val followApi: FollowApi,
    private val userApi: UserApi
) : FollowRepository {

    override suspend fun follow(followerId: String, followingId: String) {
        followApi.follow(FollowInsertDto(followerId = followerId, followingId = followingId))
    }

    override suspend fun unfollow(followerId: String, followingId: String) {
        followApi.unfollow(
            followerIdFilter = "eq.$followerId",
            followingIdFilter = "eq.$followingId"
        )
    }

    override suspend fun getFollowingIds(followerId: String): Set<String> =
        followApi.getFollowing(followerIdFilter = "eq.$followerId")
            .map { it.followingId }
            .toSet()

    override suspend fun getFollowing(followerId: String): List<User> {
        val ids = getFollowingIds(followerId)
        if (ids.isEmpty()) return emptyList()
        val inFilter = "in.(${ids.joinToString(",")})"
        return userApi.getUsersByIds(idInFilter = inFilter).map { it.toDomain() }
    }
}