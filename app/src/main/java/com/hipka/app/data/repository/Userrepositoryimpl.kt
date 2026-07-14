package com.hipka.app.data.repository

import com.hipka.app.data.remote.dto.UserDto
import com.hipka.app.data.remote.dto.toDomain
import com.hipka.app.domain.model.User
import com.hipka.app.domain.repository.UserRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : UserRepository {

    override suspend fun getUserById(id: String): User? =
        supabaseClient.from("users")
            .select { filter { eq("id", id) } }
            .decodeList<UserDto>()
            .firstOrNull()
            ?.toDomain()

    override suspend fun getAllUsers(): List<User> =
        supabaseClient.from("users")
            .select()
            .decodeList<UserDto>()
            .map { it.toDomain() }

    override suspend fun searchUsersByName(query: String): List<User> =
        supabaseClient.from("users")
            .select { filter { ilike("name", "%$query%") } }
            .decodeList<UserDto>()
            .map { it.toDomain() }
}