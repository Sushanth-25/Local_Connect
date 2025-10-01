package com.localconnect.data.repository

import com.localconnect.data.model.Post
import com.localconnect.domain.repository.PostRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebasePostRepository : PostRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val postsCollection = firestore.collection("posts")

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
                    println("Error parsing post document ${document.id}: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            println("Error fetching posts: ${e.message}")
            emptyList()
        }
    }

    override fun observeAllPosts(limit: Long): Flow<List<Post>> = callbackFlow {
        val registration = postsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Error observing posts: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents?.mapNotNull { document ->
                    try {
                        document.toObject(Post::class.java)?.copy(
                            postId = document.id
                        )
                    } catch (e: Exception) {
                        println("Error parsing post document ${document.id}: ${e.message}")
                        null
                    }
                } ?: emptyList()

                trySend(posts)
            }

        awaitClose { registration.remove() }
    }

    override suspend fun createPost(post: Post): Result<Unit> {
        return try {
            // Ensure all required fields have valid values
            val sanitizedPost = post.copy(
                timestamp = post.timestamp ?: System.currentTimeMillis(),
                updatedAt = post.updatedAt ?: System.currentTimeMillis(),
                // Ensure empty strings are converted to null for optional fields
                caption = post.caption?.takeIf { it.isNotBlank() },
                description = post.description?.takeIf { it.isNotBlank() },
                title = post.title?.takeIf { it.isNotBlank() },
                location = post.location?.takeIf { it.isNotBlank() },
                imageUrl = post.imageUrl?.takeIf { it.isNotBlank() },
                videoUrl = post.videoUrl?.takeIf { it.isNotBlank() },
                status = post.status?.takeIf { it.isNotBlank() },
                category = post.category?.takeIf { it.isNotBlank() },
                type = post.type?.takeIf { it.isNotBlank() }
            )

            postsCollection.document(sanitizedPost.postId).set(sanitizedPost).await()
            Result.success(Unit)
        } catch (e: Exception) {
            println("Error creating post: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun likePost(postId: String): Result<Unit> {
        return try {
            val postRef = postsCollection.document(postId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                val currentLikes = snapshot.getLong("likes") ?: 0L
                transaction.update(postRef, "likes", currentLikes + 1)
                transaction.update(postRef, "updatedAt", System.currentTimeMillis())
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            println("Error liking post: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun upvotePost(postId: String): Result<Unit> {
        return try {
            val postRef = postsCollection.document(postId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                val currentUpvotes = snapshot.getLong("upvotes") ?: 0L
                transaction.update(postRef, "upvotes", currentUpvotes + 1)
                transaction.update(postRef, "updatedAt", System.currentTimeMillis())
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            println("Error upvoting post: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun incrementViews(postId: String): Result<Unit> {
        return try {
            val postRef = postsCollection.document(postId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                val currentViews = snapshot.getLong("views") ?: 0L
                transaction.update(postRef, "views", currentViews + 1)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            println("Error incrementing views: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun incrementComments(postId: String): Result<Unit> {
        return try {
            val postRef = postsCollection.document(postId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                val currentComments = snapshot.getLong("comments") ?: 0L
                transaction.update(postRef, "comments", currentComments + 1)
                transaction.update(postRef, "updatedAt", System.currentTimeMillis())
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            println("Error incrementing comments: ${e.message}")
            Result.failure(e)
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

            println("FirebasePostRepository: Total posts fetched: ${posts.size}")

            // Filter by location if user coordinates are available
            val filteredPosts = if (userLat != null && userLon != null) {
                val result = com.localconnect.util.LocationUtils.filterPostsByLocation(posts, userLat, userLon)
                println("FirebasePostRepository: After location filtering: ${result.size} posts")
                result
            } else {
                // If no user location, show only local-only posts and posts without coordinates
                val result = posts.filter { post ->
                    post.isLocalOnly || post.location.isNullOrBlank()
                }
                println("FirebasePostRepository: No user location, showing ${result.size} local/no-location posts")
                result
            }

            filteredPosts
        } catch (e: Exception) {
            println("Error fetching community posts: ${e.message}")
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

                // For community posts, check distance using location string
                val postLat = com.localconnect.util.LocationUtils.parseLatitudeFromLocation(post.location)
                val postLon = com.localconnect.util.LocationUtils.parseLongitudeFromLocation(post.location)

                if (postLat != null && postLon != null) {
                    val distance = com.localconnect.util.LocationUtils.calculateDistance(
                        userLat, userLon, postLat, postLon
                    )
                    distance <= radiusKm
                } else {
                    // Include posts without valid coordinates in community feed
                    true
                }
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
