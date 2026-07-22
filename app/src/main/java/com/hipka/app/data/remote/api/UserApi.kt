package com.hipka.app.data.remote.api

import com.hipka.app.data.remote.dto.PremiumUpdateDto
import com.hipka.app.data.remote.dto.UserDto
import com.hipka.app.data.remote.dto.UserFollowDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

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

    // متدهای قبلی لاگین برای حفظ سازگاری با کد بقیه اعضای تیم
    @GET("rest/v1/users")
    suspend fun loginUser(
        @Query("email") emailFilter: String,
        @Query("password") passwordFilter: String,
        @Query("select") select: String = "*"
    ): List<UserDto>

    // متد جدید اختصاصی جهت ورود با ایمیل یا نام کاربری
    @GET("rest/v1/users")
    suspend fun loginUserOr(
        @Query("or") orFilter: String, // pass as "(email.eq.xyz,name.eq.xyz)"
        @Query("password") passwordFilter: String,
        @Query("select") select: String = "*"
    ): List<UserDto>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/users")
    suspend fun registerUser(
        @Body user: UserDto
    ): List<UserDto>

    @GET("rest/v1/user_follows")
    suspend fun getFollowings(
        @Query("follower_id") followerFilter: String, // pass as "eq.<userId>"
        @Query("select") select: String = "*"
    ): List<UserFollowDto>

    @GET("rest/v1/user_follows")
    suspend fun getFollowers(
        @Query("following_id") followingFilter: String, // pass as "eq.<userId>"
        @Query("select") select: String = "*"
    ): List<UserFollowDto>

    @POST("rest/v1/user_follows")
    suspend fun followUser(@Body relationship: UserFollowDto)

    @DELETE("rest/v1/user_follows")
    suspend fun unfollowUser(
        @Query("follower_id") followerFilter: String, // "eq.<myId>"
        @Query("following_id") followingFilter: String // "eq.<targetId>"
    )

    @PATCH("rest/v1/users")
    suspend fun updatePremiumStatus(
        @Query("id") idFilter: String, // pass as "eq.<id>"
        @Body body: PremiumUpdateDto
    ): Response<Unit>
}