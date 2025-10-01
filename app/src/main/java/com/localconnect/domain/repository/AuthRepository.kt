package com.localconnect.domain.repository

import com.localconnect.data.model.User

interface AuthRepository {
    suspend fun register(name: String, email: String, password: String, bio: String, profileImage: String): Result<User>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun logout()
    fun getCurrentUser(): User?
}

