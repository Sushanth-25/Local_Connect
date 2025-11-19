package com.example.localconnect.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.localconnect.data.model.Post
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await

/**
 * PagingSource for loading posts efficiently with pagination
 * This reduces Firestore reads by loading only what's needed
 */
class PostsPagingSource(
    private val firestore: FirebaseFirestore,
    private val pageSize: Int = 20,
    private val userLat: Double? = null,
    private val userLon: Double? = null,
    private val radiusKm: Double = 30.0,
    private val category: String? = null,
    private val sortBy: String = "timestamp" // timestamp, likes, views, priority
) : PagingSource<QuerySnapshot, Post>() {

    companion object {
        private const val TAG = "PostsPagingSource"
    }

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, Post> {
        return try {
            Log.d(TAG, "Loading posts page, key=${params.key != null}, pageSize=$pageSize")

            // Build the base query
            var query: Query = firestore.collection("posts")
                .orderBy(sortBy, Query.Direction.DESCENDING)
                .limit(pageSize.toLong())

            // Add category filter if specified
            if (!category.isNullOrEmpty() && category != "All") {
                query = firestore.collection("posts")
                    .whereEqualTo("category", category)
                    .orderBy(sortBy, Query.Direction.DESCENDING)
                    .limit(pageSize.toLong())
            }

            // If we have a previous page, start after it
            val snapshot = if (params.key == null) {
                query.get().await()
            } else {
                val lastDocument = params.key!!.documents.lastOrNull()
                if (lastDocument != null) {
                    query.startAfter(lastDocument).get().await()
                } else {
                    query.get().await()
                }
            }

            Log.d(TAG, "Loaded ${snapshot.documents.size} posts from Firestore")

            // Parse posts from documents
            var posts = snapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Post::class.java)?.copy(postId = document.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing post ${document.id}: ${e.message}")
                    null
                }
            }

            // Apply location filtering if coordinates are provided
            if (userLat != null && userLon != null) {
                posts = posts.filter { post ->
                    val distance = calculateDistance(
                        userLat, userLon,
                        post.latitude, post.longitude
                    )
                    distance <= radiusKm
                }
                Log.d(TAG, "After location filtering: ${posts.size} posts within ${radiusKm}km")
            }

            // Determine next key for pagination
            val nextKey = if (snapshot.documents.size < pageSize) {
                null // No more pages
            } else {
                snapshot // Use this snapshot as the key for the next page
            }

            Log.d(TAG, "Page loaded successfully, hasNextPage=${nextKey != null}")

            LoadResult.Page(
                data = posts,
                prevKey = null, // We only support forward pagination
                nextKey = nextKey
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading posts page: ${e.message}", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<QuerySnapshot, Post>): QuerySnapshot? {
        // Return null to always start from the beginning on refresh
        return null
    }

    /**
     * Calculate distance between two coordinates using Haversine formula
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // kilometers

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c
    }
}

