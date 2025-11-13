package com.example.localconnect.data.repository

import android.util.Log
import com.example.localconnect.data.model.Post
import com.example.localconnect.repository.PostRepository
import com.example.localconnect.util.LocationUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebasePostRepository : PostRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val postsCollection = firestore.collection("posts")

    companion object {
        private const val TAG = "FirebasePostRepository"
    }

    override suspend fun getAllPosts(): List<Post> {
        return try {
            val snapshot = postsCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Post::class.java)?.copy(
                        postId = document.id // Ensure postId matches document ID
                    )
                } catch (e: Exception) {
                    // Log the error and skip this document
                    Log.e(TAG, "Error parsing post document ${document.id}: ${e.message}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching posts: ${e.message}", e)
            emptyList()
        }
    }

    override fun observeAllPosts(limit: Long): Flow<List<Post>> = callbackFlow {
        val registration = postsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing posts: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents?.mapNotNull { document ->
                    try {
                        document.toObject(Post::class.java)?.copy(
                            postId = document.id
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing post document ${document.id}: ${e.message}", e)
                        null
                    }
                } ?: emptyList()

                trySend(posts)
            }

        awaitClose { registration.remove() }
    }

    override suspend fun createPost(post: Post): Result<Unit> {
        return try {
            Log.d(TAG, "Creating post with ID: ${post.postId}")
            Log.d(TAG, "Post has ${post.mediaUrls.size} media URLs: ${post.mediaUrls}")
            Log.d(TAG, "Post has ${post.thumbnailUrls.size} thumbnail URLs: ${post.thumbnailUrls}")

            // Ensure all required fields have valid values
            val sanitizedPost = post.copy(
                timestamp = post.timestamp ?: System.currentTimeMillis(),
                updatedAt = post.updatedAt ?: System.currentTimeMillis(),
                // Ensure empty strings are converted to null for optional fields
                caption = post.caption?.takeIf { it.isNotBlank() },
                description = post.description?.takeIf { it.isNotBlank() },
                title = post.title?.takeIf { it.isNotBlank() },
                status = post.status?.takeIf { it.isNotBlank() },
                category = post.category?.takeIf { it.isNotBlank() },
                type = post.type?.takeIf { it.isNotBlank() },
                // Ensure location fields are not blank
                locationName = post.locationName.ifBlank { "Unknown Location" },
                // Handle lists properly - keep them as is since they're already validated
                mediaUrls = post.mediaUrls,
                thumbnailUrls = post.thumbnailUrls,
                tags = post.tags
            )

            // Convert to Map to ensure proper serialization
            val postData = hashMapOf(
                "postId" to sanitizedPost.postId,
                "userId" to sanitizedPost.userId,
                "caption" to sanitizedPost.caption,
                "description" to sanitizedPost.description,
                "title" to sanitizedPost.title,
                "category" to sanitizedPost.category,
                "status" to sanitizedPost.status,
                "hasImage" to sanitizedPost.hasImage,
                "mediaUrls" to sanitizedPost.mediaUrls,
                "thumbnailUrls" to sanitizedPost.thumbnailUrls,
                "tags" to sanitizedPost.tags,
                "localOnly" to sanitizedPost.isLocalOnly,
                "timestamp" to sanitizedPost.timestamp,
                "updatedAt" to sanitizedPost.updatedAt,
                "likes" to sanitizedPost.likes,
                "comments" to sanitizedPost.comments,
                "views" to sanitizedPost.views,
                "priority" to sanitizedPost.priority,
                "type" to sanitizedPost.type,
                "latitude" to sanitizedPost.latitude,
                "longitude" to sanitizedPost.longitude,
                "locationName" to sanitizedPost.locationName
            )

            Log.d(TAG, "Saving post data to Firestore: $postData")
            postsCollection.document(sanitizedPost.postId).set(postData).await()
            Log.d(TAG, "Post saved successfully to Firestore")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating post: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Toggle like on a post (like if not liked, unlike if already liked)
     * Uses subcollection: /posts/{postId}/likes/{userId}
     *
     * @return Result with Boolean - true if liked, false if unliked
     */
    suspend fun togglePostLike(postId: String, userId: String): Result<Boolean> {
        return try {
            val postRef = postsCollection.document(postId)
            val likeRef = postRef.collection("likes").document(userId)

            val isLiked = firestore.runTransaction { transaction ->
                // STEP 1: ALL READS FIRST
                val postSnapshot = transaction.get(postRef)
                val likeSnapshot = transaction.get(likeRef)

                val currentLikes = postSnapshot.getLong("likes") ?: 0L
                val alreadyLiked = likeSnapshot.exists()

                // STEP 2: ALL WRITES
                if (alreadyLiked) {
                    // Unlike: remove like document and decrement counter
                    transaction.delete(likeRef)
                    val newCount = (currentLikes - 1).coerceAtLeast(0)
                    transaction.update(postRef, "likes", newCount)
                    transaction.update(postRef, "updatedAt", System.currentTimeMillis())
                    false // Return false (unliked)
                } else {
                    // Like: add like document and increment counter
                    val likeData = hashMapOf(
                        "userId" to userId,
                        "timestamp" to System.currentTimeMillis()
                    )
                    transaction.set(likeRef, likeData)
                    transaction.update(postRef, "likes", currentLikes + 1)
                    transaction.update(postRef, "updatedAt", System.currentTimeMillis())
                    true // Return true (liked)
                }
            }.await()

            Log.d(TAG, "Post like toggled: postId=$postId, isLiked=$isLiked")
            Result.success(isLiked)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling post like: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if user has liked a post
     */
    suspend fun hasUserLikedPost(postId: String, userId: String): Boolean {
        return try {
            val likeRef = postsCollection.document(postId)
                .collection("likes")
                .document(userId)

            val snapshot = likeRef.get().await()
            snapshot.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking like status: ${e.message}", e)
            false
        }
    }

    /**
     * Increment view counter for a post ONLY ONCE per user
     * Uses subcollection: /posts/{postId}/views/{userId}
     *
     * @return Result with Boolean - true if view was counted, false if already viewed
     */
    suspend fun incrementPostViewOnce(postId: String, userId: String): Result<Boolean> {
        return try {
            val postRef = postsCollection.document(postId)
            val viewRef = postRef.collection("views").document(userId)

            val wasCounted = firestore.runTransaction { transaction ->
                // STEP 1: ALL READS FIRST
                val postSnapshot = transaction.get(postRef)
                val viewSnapshot = transaction.get(viewRef)

                val currentViews = postSnapshot.getLong("views") ?: 0L
                val alreadyViewed = viewSnapshot.exists()

                // STEP 2: ALL WRITES
                if (alreadyViewed) {
                    // User already viewed this post - don't increment
                    false
                } else {
                    // First time view - add view document and increment counter
                    val viewData = hashMapOf(
                        "userId" to userId,
                        "timestamp" to System.currentTimeMillis()
                    )
                    transaction.set(viewRef, viewData)
                    transaction.update(postRef, "views", currentViews + 1)
                    true
                }
            }.await()

            Log.d(TAG, "Post view tracking: postId=$postId, wasCounted=$wasCounted")
            Result.success(wasCounted)
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking post view: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if user has viewed a post
     */
    suspend fun hasUserViewedPost(postId: String, userId: String): Boolean {
        return try {
            val viewRef = postsCollection.document(postId)
                .collection("views")
                .document(userId)

            val snapshot = viewRef.get().await()
            snapshot.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking view status: ${e.message}", e)
            false
        }
    }

    // Legacy methods - kept for backward compatibility but deprecated
    @Deprecated("Use togglePostLike() instead", ReplaceWith("togglePostLike(postId, userId)"))
    override suspend fun likePost(postId: String): Result<Unit> {
        Log.w(TAG, "likePost() is deprecated, use togglePostLike() instead")
        return Result.success(Unit)
    }

    @Deprecated("Use togglePostLike() instead", ReplaceWith("togglePostLike(postId, userId)"))
    suspend fun unlikePost(postId: String): Result<Unit> {
        Log.w(TAG, "unlikePost() is deprecated, use togglePostLike() instead")
        return Result.success(Unit)
    }

    @Deprecated("Use incrementPostViewOnce() instead", ReplaceWith("incrementPostViewOnce(postId, userId)"))
    suspend fun incrementViews(postId: String): Result<Unit> {
        Log.w(TAG, "incrementViews() is deprecated, use incrementPostViewOnce() instead")
        return Result.success(Unit)
    }

    /**
     * Increment comment counter for a post
     * Note: This is typically called automatically by CommentRepository when adding comments
     * Use CommentRepository.addComment() instead of calling this directly
     */
    @Deprecated("Use CommentRepository.addComment() instead", ReplaceWith("CommentRepository().addComment(comment)"))
    suspend fun incrementComments(postId: String): Result<Unit> {
        return try {
            val postRef = postsCollection.document(postId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                val currentComments = snapshot.getLong("comments") ?: 0L
                transaction.update(postRef, "comments", currentComments + 1)
                transaction.update(postRef, "updatedAt", System.currentTimeMillis())
            }.await()
            Log.d(TAG, "Comment count incremented for post $postId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing comments: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get aggregate stats for a post (likes, comments, views)
     */
    suspend fun getPostStats(postId: String): PostStats? {
        return try {
            val snapshot = postsCollection.document(postId).get().await()
            PostStats(
                likes = snapshot.getLong("likes")?.toInt() ?: 0,
                comments = snapshot.getLong("comments")?.toInt() ?: 0,
                views = snapshot.getLong("views")?.toInt() ?: 0,
                upvotes = snapshot.getLong("upvotes")?.toInt() ?: 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting post stats: ${e.message}", e)
            null
        }
    }

    suspend fun updatePostStatus(postId: String, status: String): Result<Unit> {
        return try {
            val postRef = postsCollection.document(postId)
            postRef.update(
                mapOf(
                    "status" to status,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            println("Error updating post status: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Delete a post and its associated media from Cloudinary
     */
    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Deleting post: $postId")

            // First, get the post to retrieve media URLs
            val postSnapshot = postsCollection.document(postId).get().await()
            val post = postSnapshot.toObject(Post::class.java)

            if (post != null && post.mediaUrls.isNotEmpty()) {
                Log.d(TAG, "Post has ${post.mediaUrls.size} media files to delete from Cloudinary")

                // Delete media files from Cloudinary
                try {
                    val deletionSuccess = com.example.localconnect.util.CloudinaryManager.deleteMediaFiles(post.mediaUrls)
                    if (!deletionSuccess) {
                        Log.w(TAG, "Some media files could not be deleted from Cloudinary")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting media from Cloudinary: ${e.message}", e)
                    // Continue with post deletion even if Cloudinary deletion fails
                }
            }

            // Delete the post document from Firestore
            postsCollection.document(postId).delete().await()
            Log.d(TAG, "Post deleted successfully from Firestore")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting post: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getPostsByCategory(category: String): List<Post> {
        return try {
            val snapshot = postsCollection
                .whereEqualTo("category", category)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Post::class.java)?.copy(postId = document.id)
                } catch (e: Exception) {
                    println("Error parsing post document ${document.id}: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            println("Error fetching posts by category: ${e.message}")
            emptyList()
        }
    }

    suspend fun getPostsByType(type: String): List<Post> {
        return try {
            val snapshot = postsCollection
                .whereEqualTo("type", type)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Post::class.java)?.copy(postId = document.id)
                } catch (e: Exception) {
                    println("Error parsing post document ${document.id}: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            println("Error fetching posts by type: ${e.message}")
            emptyList()
        }
    }

    suspend fun getPostsByUser(userId: String): List<Post> {
        return try {
            val snapshot = postsCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Post::class.java)?.copy(postId = document.id)
                } catch (e: Exception) {
                    println("Error parsing post document ${document.id}: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            println("Error fetching posts by user: ${e.message}")
            emptyList()
        }
    }

    suspend fun getCommunityPosts(
        userLat: Double?,
        userLon: Double?
    ): List<Post> {
        return try {
            println("FirebasePostRepository: Getting community posts for location: $userLat, $userLon")

            // User location is REQUIRED for community posts
            if (userLat == null || userLon == null) {
                println("FirebasePostRepository: No user location provided - returning empty list")
                return emptyList()
            }

            // Get ALL posts (not just type "POST") for community tab
            val snapshot = postsCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val posts = snapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Post::class.java)?.copy(postId = document.id)
                } catch (e: Exception) {
                    println("Error parsing post document ${document.id}: ${e.message}")
                    null
                }
            }

            println("FirebasePostRepository: Total posts fetched from Firebase: ${posts.size}")

            // Apply strict location-based filtering (30km radius)
            val filteredPosts = LocationUtils.filterPostsByLocation(posts, userLat, userLon)
            println("FirebasePostRepository: After 30km radius filtering: ${filteredPosts.size} posts")

            filteredPosts
        } catch (e: Exception) {
            println("FirebasePostRepository: Error fetching community posts: ${e.message}")
            emptyList()
        }
    }

    suspend fun getPostsNearLocation(
        userLat: Double,
        userLon: Double,
        radiusKm: Double = 30.0
    ): List<Post> {
        return try {
            val allPosts = getAllPosts()
            allPosts.filter { post ->
                // Always include local-only posts
                if (post.isLocalOnly) return@filter true

                // For community posts, check distance using coordinates
                val distance = LocationUtils.calculateDistance(
                    userLat, userLon, post.latitude, post.longitude
                )
                distance <= radiusKm
            }
        } catch (e: Exception) {
            println("Error fetching posts near location: ${e.message}")
            emptyList()
        }
    }

    suspend fun getLocalPosts(): List<Post> {
        return try {
            val snapshot = postsCollection
                .whereEqualTo("localOnly", true)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Post::class.java)?.copy(postId = document.id)
                } catch (e: Exception) {
                    println("Error parsing post document ${document.id}: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            println("Error fetching local posts: ${e.message}")
            emptyList()
        }
    }
}

/**
 * Data class to hold aggregate statistics for a post
 */
data class PostStats(
    val likes: Int = 0,
    val comments: Int = 0,
    val views: Int = 0,
    val upvotes: Int = 0
)

