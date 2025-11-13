package com.example.localconnect.data.repository

import android.util.Log
import com.example.localconnect.data.model.Comment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CommentRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val postsCollection = firestore.collection("posts")

    companion object {
        private const val TAG = "CommentRepository"
    }

    /**
     * Add a comment to a post
     * This will:
     * 1. Read the current post data (MUST happen first)
     * 2. Add the comment to the comments subcollection
     * 3. Increment the comment counter on the post document
     *
     * IMPORTANT: All reads must happen before any writes in Firestore transactions
     */
    suspend fun addComment(comment: Comment): Result<String> {
        return try {
            val postRef = postsCollection.document(comment.postId)
            val commentRef = postRef.collection("comments").document()

            val commentWithId = comment.copy(commentId = commentRef.id)

            // Use a transaction to ensure atomic updates
            firestore.runTransaction { transaction ->
                // STEP 1: ALL READS FIRST (before any writes)
                val snapshot = transaction.get(postRef)
                val currentComments = snapshot.getLong("comments") ?: 0L

                // STEP 2: NOW DO ALL WRITES
                // Add comment to subcollection
                transaction.set(commentRef, commentWithId)

                // Increment comment counter on post
                transaction.update(postRef, "comments", currentComments + 1)
                transaction.update(postRef, "updatedAt", System.currentTimeMillis())

                commentRef.id
            }.await()

            Log.d(TAG, "Comment added successfully: ${commentRef.id}")
            Result.success(commentRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding comment: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get all comments for a post
     */
    suspend fun getComments(postId: String): List<Comment> {
        return try {
            val snapshot = postsCollection.document(postId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Comment::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing comment document ${document.id}: ${e.message}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching comments: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Observe comments in real-time
     */
    fun observeComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val registration = postsCollection.document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing comments: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val comments = snapshot?.documents?.mapNotNull { document ->
                    try {
                        document.toObject(Comment::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing comment document ${document.id}: ${e.message}", e)
                        null
                    }
                } ?: emptyList()

                trySend(comments)
            }

        awaitClose { registration.remove() }
    }

    /**
     * Delete a comment
     * This will:
     * 1. Read the current post data (MUST happen first)
     * 2. Delete the comment from the subcollection
     * 3. Decrement the comment counter on the post document
     *
     * IMPORTANT: All reads must happen before any writes in Firestore transactions
     */
    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        return try {
            val postRef = postsCollection.document(postId)
            val commentRef = postRef.collection("comments").document(commentId)

            // Use a transaction to ensure atomic updates
            firestore.runTransaction { transaction ->
                // STEP 1: ALL READS FIRST (before any writes/deletes)
                val snapshot = transaction.get(postRef)
                val currentComments = snapshot.getLong("comments") ?: 0L
                val newCount = (currentComments - 1).coerceAtLeast(0)

                // STEP 2: NOW DO ALL WRITES/DELETES
                // Delete comment from subcollection
                transaction.delete(commentRef)

                // Decrement comment counter on post
                transaction.update(postRef, "comments", newCount)
                transaction.update(postRef, "updatedAt", System.currentTimeMillis())
            }.await()

            Log.d(TAG, "Comment deleted successfully: $commentId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting comment: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Toggle like on a comment (like if not liked, unlike if already liked)
     * Uses subcollection: /posts/{postId}/comments/{commentId}/likes/{userId}
     *
     * @return Result with Boolean - true if liked, false if unliked
     */
    suspend fun toggleCommentLike(postId: String, commentId: String, userId: String): Result<Boolean> {
        return try {
            val commentRef = postsCollection.document(postId)
                .collection("comments")
                .document(commentId)

            val likeRef = commentRef.collection("likes").document(userId)

            val isLiked = firestore.runTransaction { transaction ->
                // STEP 1: ALL READS FIRST
                val commentSnapshot = transaction.get(commentRef)
                val likeSnapshot = transaction.get(likeRef)

                val currentLikes = commentSnapshot.getLong("likes") ?: 0L
                val alreadyLiked = likeSnapshot.exists()

                // STEP 2: ALL WRITES
                if (alreadyLiked) {
                    // Unlike: remove like document and decrement counter
                    transaction.delete(likeRef)
                    val newCount = (currentLikes - 1).coerceAtLeast(0)
                    transaction.update(commentRef, "likes", newCount)
                    false // Return false (unliked)
                } else {
                    // Like: add like document and increment counter
                    val likeData = hashMapOf(
                        "userId" to userId,
                        "timestamp" to System.currentTimeMillis()
                    )
                    transaction.set(likeRef, likeData)
                    transaction.update(commentRef, "likes", currentLikes + 1)
                    true // Return true (liked)
                }
            }.await()

            Log.d(TAG, "Comment like toggled: commentId=$commentId, isLiked=$isLiked")
            Result.success(isLiked)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling comment like: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if user has liked a comment
     */
    suspend fun hasUserLikedComment(postId: String, commentId: String, userId: String): Boolean {
        return try {
            val likeRef = postsCollection.document(postId)
                .collection("comments")
                .document(commentId)
                .collection("likes")
                .document(userId)

            val snapshot = likeRef.get().await()
            snapshot.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking comment like status: ${e.message}", e)
            false
        }
    }

    /**
     * Get comment count for a post (from the aggregate counter)
     */
    suspend fun getCommentCount(postId: String): Int {
        return try {
            val snapshot = postsCollection.document(postId).get().await()
            snapshot.getLong("comments")?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting comment count: ${e.message}", e)
            0
        }
    }

    /**
     * Get replies to a specific comment
     */
    suspend fun getReplies(postId: String, parentCommentId: String): List<Comment> {
        return try {
            val snapshot = postsCollection.document(postId)
                .collection("comments")
                .whereEqualTo("parentCommentId", parentCommentId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Comment::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing reply document ${document.id}: ${e.message}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching replies: ${e.message}", e)
            emptyList()
        }
    }
}

