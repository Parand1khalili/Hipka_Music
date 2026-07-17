package com.hipka.app.data.remote.api

import com.hipka.app.data.remote.dto.UserDto
import com.hipka.app.data.remote.dto.UserFollowDto
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Body

interface UserApi {

    @GET("rest/v1/users?select=*")
    suspend fun getAllUsers(): List<UserDto>

    @GET("rest/v1/users")
    suspend fun getUserById(
        @Query("id") idFilter: String, // pass as "eq.<id>"
        @Query("select") select: String = "*"
    ): List<UserDto>

    @GET("rest/v1/users")
    suspend fun searchUsersByName(
        @Query("name") nameFilter: String, // pass as "ilike.*<query>*"
        @Query("select") select: String = "*"
    ): List<UserDto>

    @GET("rest/v1/users")
    suspend fun getUsersByIds(
        @Query("id") idInFilter: String, // pass as "in.(id1,id2,...)"
        @Query("select") select: String = "*"
    ): List<UserDto>

    // ✨ واکشی روابط فالووینگ (کسانی که این کاربر فالو کرده است)
    @GET("rest/v1/user_follows")
    suspend fun getFollowings(
        @Query("follower_id") followerFilter: String, // pass as "eq.<userId>"
        @Query("select") select: String = "*"
    ): List<UserFollowDto>

    // ✨ واکشی روابط فالوور (کسانی که این کاربر را فالو کرده‌اند)
    @GET("rest/v1/user_follows")
    suspend fun getFollowers(
        @Query("following_id") followingFilter: String, // pass as "eq.<userId>"
        @Query("select") select: String = "*"
    ): List<UserFollowDto>

    // 💡 اضافه کردن به انتهای اینترفیس UserApi
    @POST("rest/v1/user_follows")
    suspend fun followUser(@Body relationship: UserFollowDto)

    @DELETE("rest/v1/user_follows")
    suspend fun unfollowUser(
        @Query("follower_id") followerFilter: String, // "eq.<myId>"
        @Query("following_id") followingFilter: String // "eq.<targetId>"
    )
}