package com.example.localconnect.data.repository

import android.content.Context
import android.util.Log
import com.example.localconnect.data.model.User
import com.example.localconnect.repository.AuthRepository
import com.example.localconnect.util.NotificationManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FirebaseAuthRepository(private val context: Context? = null) : AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val notificationManager = context?.let { NotificationManager(it) }

    companion object {
        private const val TAG = "FirebaseAuthRepository"
    }

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
        return User(
            userId = firebaseUser.uid,
            name = firebaseUser.displayName ?: "",
            email = firebaseUser.email ?: ""
        )
    }

    override suspend fun updateProfileImage(userId: String, imageUrl: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .update("profileImage", imageUrl)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserById(userId: String): Result<User> {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            val user = doc.toObject(User::class.java) ?: throw Exception("User not found")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user email with notification
     */
    suspend fun updateUserEmail(userId: String, newEmail: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("User not logged in")

            // Get old email
            val oldEmail = currentUser.email ?: ""

            // Update Firebase Auth email
            currentUser.updateEmail(newEmail).await()

            // Update Firestore user document
            firestore.collection("users")
                .document(userId)
                .update("email", newEmail)
                .await()

            // Send notification (in background)
            if (notificationManager != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        notificationManager.sendProfileUpdateNotification(
                            userId = userId,
                            updateType = "email",
                            oldValue = oldEmail,
                            newValue = newEmail
                        )
                        Log.d(TAG, "Email update notification sent for user $userId")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending email update notification", e)
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating email", e)
            Result.failure(e)
        }
    }

    /**
     * Update user phone number with notification
     */
    suspend fun updateUserPhone(userId: String, newPhone: String): Result<Unit> {
        return try {
            // Get old phone (if exists)
            val doc = firestore.collection("users").document(userId).get().await()
            val oldPhone = doc.getString("phone") ?: ""

            // Update Firestore user document
            firestore.collection("users")
                .document(userId)
                .update("phone", newPhone)
                .await()

            // Send notification (in background)
            if (notificationManager != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        notificationManager.sendProfileUpdateNotification(
                            userId = userId,
                            updateType = "phone",
                            oldValue = oldPhone,
                            newValue = newPhone
                        )
                        Log.d(TAG, "Phone update notification sent for user $userId")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending phone update notification", e)
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating phone", e)
            Result.failure(e)
        }
    }
}
