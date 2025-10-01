package com.localconnect.data.repository

import com.localconnect.data.model.User
import com.localconnect.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository : AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override suspend fun register(name: String, email: String, password: String, bio: String, profileImage: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("No user")
            val user = User(
                userId = firebaseUser.uid,
                name = name,
                email = email,
                profileImage = profileImage,
                bio = bio,
                createdAt = System.currentTimeMillis()
            )
            firestore.collection("users").document(user.userId).set(user).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("No user")
            val doc = firestore.collection("users").document(firebaseUser.uid).get().await()
            val user = doc.toObject(User::class.java) ?: throw Exception("User not found")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        auth.signOut()
    }

    override fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        // This is a stub; in ViewModel, fetch from Firestore if needed
        return User(userId = firebaseUser.uid, name = firebaseUser.displayName ?: "", email = firebaseUser.email ?: "")
    }
}

