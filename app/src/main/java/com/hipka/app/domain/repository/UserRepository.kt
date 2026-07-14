package com.hipka.app.domain.repository

import com.hipka.app.domain.model.User

interface UserRepository {
    suspend fun getUserById(id: String): User?
    suspend fun getAllUsers(): List<User>
    suspend fun searchUsersByName(query: String): List<User>
}
