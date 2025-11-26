package com.example.localconnect.data.repository

import android.content.Context
import android.util.Log
import com.example.localconnect.data.model.Post
import com.example.localconnect.data.model.Staff
import com.example.localconnect.util.NotificationManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StaffRepository(private val context: Context? = null) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationManager = context?.let { NotificationManager(it) }

    companion object {
        private const val TAG = "StaffRepository"
    }

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

            // Get current post to retrieve old status
            val postRef = firestore.collection("posts").document(postId)
            val postSnapshot = postRef.get().await()
            val post = postSnapshot.toObject(Post::class.java)
            val oldStatus = post?.status ?: ""

            val updates = hashMapOf<String, Any>(
                "status" to newStatus,
                "updatedAt" to System.currentTimeMillis()
            )

            postRef.update(updates).await()

            // Send notification for status update (in background)
            if (notificationManager != null && post != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        notificationManager.sendStatusUpdateNotification(
                            userId = post.userId,
                            postId = postId,
                            oldStatus = oldStatus,
                            newStatus = newStatus,
                            postTitle = post.title ?: post.caption ?: "your post"
                        )
                        Log.d(TAG, "Status update notification sent for post $postId: $oldStatus -> $newStatus")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending status update notification", e)
                    }
                }
            }

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

            // Try with ordering first, if it fails due to missing index, fallback to client-side sorting
            val posts = try {
                val snapshot = firestore.collection("posts")
                    .whereEqualTo("status", status)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()

                snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Post::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
            } catch (e: Exception) {
                // If ordering fails (missing index), fetch without ordering and sort client-side
                val snapshot = firestore.collection("posts")
                    .whereEqualTo("status", status)
                    .get()
                    .await()

                snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Post::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.timestamp ?: 0L }
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
