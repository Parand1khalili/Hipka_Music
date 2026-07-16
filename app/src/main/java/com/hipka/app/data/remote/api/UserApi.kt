package com.hipka.app.data.remote.api

import com.hipka.app.data.remote.dto.UserDto
import retrofit2.http.GET
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
}