package com.hipka.app.data.repository

import com.hipka.app.data.remote.dto.FollowDto
import com.hipka.app.data.remote.dto.FollowInsertDto
import com.hipka.app.data.remote.dto.UserDto
import com.hipka.app.data.remote.dto.toDomain
import com.hipka.app.domain.model.User
import com.hipka.app.domain.repository.FollowRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject

class FollowRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : FollowRepository {

    override suspend fun follow(followerId: String, followingId: String) {
        supabaseClient.from("follows").insert(
            FollowInsertDto(followerId = followerId, followingId = followingId)
        )
    }

    override suspend fun unfollow(followerId: String, followingId: String) {
        supabaseClient.from("follows").delete {
            filter {
                eq("follower_id", followerId)
                eq("following_id", followingId)
            }
        }
    }

    override suspend fun getFollowingIds(followerId: String): Set<String> =
        supabaseClient.from("follows")
            .select { filter { eq("follower_id", followerId) } }
            .decodeList<FollowDto>()
            .map { it.followingId }
            .toSet()

    override suspend fun getFollowing(followerId: String): List<User> {
        val ids = getFollowingIds(followerId)
        if (ids.isEmpty()) return emptyList()
        return supabaseClient.from("users")
            .select { filter { isIn("id", ids.toList()) } }
            .decodeList<UserDto>()
            .map { it.toDomain() }
    }
}
