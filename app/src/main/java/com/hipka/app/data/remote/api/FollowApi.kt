package com.hipka.app.data.remote.api

import com.hipka.app.data.remote.dto.FollowDto
import com.hipka.app.data.remote.dto.FollowInsertDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface FollowApi {

    @GET("rest/v1/follows")
    suspend fun getFollowing(
        @Query("follower_id") followerIdFilter: String, // pass as "eq.<id>"
        @Query("select") select: String = "*"
    ): List<FollowDto>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/follows")
    suspend fun follow(@Body body: FollowInsertDto): List<FollowDto>

    @DELETE("rest/v1/follows")
    suspend fun unfollow(
        @Query("follower_id") followerIdFilter: String,
        @Query("following_id") followingIdFilter: String
    ): Response<Unit>
}