package com.example.localconnect.repository

import com.example.localconnect.data.model.User

interface AuthRepository {
    suspend fun register(name: String, email: String, password: String, bio: String, profileImage: String): Result<User>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun logout()
    fun getCurrentUser(): User?
    suspend fun updateProfileImage(userId: String, imageUrl: String): Result<Unit>
    suspend fun getUserById(userId: String): Result<User>
}
