package com.example.localconnect.data.repository

import com.example.localconnect.data.model.Post
import com.example.localconnect.data.model.Staff
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class StaffRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Check if current user is staff by verifying custom claims
    suspend fun isStaff(): Boolean {
        return try {
            val user = auth.currentUser ?: return false
            val tokenResult = user.getIdToken(true).await()
            tokenResult.claims["staff"] as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    // Get staff info from Firestore
    suspend fun getStaffInfo(uid: String): Staff? {
        return try {
            val doc = firestore.collection("staffs")
                .document(uid)
                .get()
                .await()
            doc.toObject(Staff::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Update post status (staff only)
    suspend fun updatePostStatus(postId: String, newStatus: String): Result<Unit> {
        return try {
            // Verify staff claim first
            if (!isStaff()) {
                return Result.failure(Exception("Unauthorized: User is not staff"))
            }

            val updates = hashMapOf<String, Any>(
                "status" to newStatus,
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection("posts")
                .document(postId)
                .update(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get all posts for staff dashboard
    suspend fun getAllPosts(): Result<List<Post>> {
        return try {
            if (!isStaff()) {
                return Result.failure(Exception("Unauthorized: User is not staff"))
            }

            val snapshot = firestore.collection("posts")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val posts = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Post::class.java)
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get posts by status
    suspend fun getPostsByStatus(status: String): Result<List<Post>> {
        return try {
            if (!isStaff()) {
                return Result.failure(Exception("Unauthorized: User is not staff"))
            }

            val snapshot = firestore.collection("posts")
                .whereEqualTo("status", status)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val posts = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Post::class.java)
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get posts by type (ISSUE, EVENT, POST)
    suspend fun getPostsByType(type: String): Result<List<Post>> {
        return try {
            if (!isStaff()) {
                return Result.failure(Exception("Unauthorized: User is not staff"))
            }

            val snapshot = firestore.collection("posts")
                .whereEqualTo("type", type)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val posts = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Post::class.java)
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

