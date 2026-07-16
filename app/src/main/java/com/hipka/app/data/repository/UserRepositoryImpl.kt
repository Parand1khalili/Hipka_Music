package com.hipka.app.data.repository

import com.hipka.app.data.remote.api.UserApi
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
}