package com.example.localconnect.util

import android.content.Context
import android.util.Log
import com.example.localconnect.data.model.Post
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Detects trending issues in the user's area based on rapid engagement
 */
class TrendingIssueDetector(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val notificationManager = NotificationManager(context)

    companion object {
        private const val TAG = "TrendingIssueDetector"
        private const val MIN_UPVOTES_FOR_TRENDING = 10 // Minimum likes to be considered trending
        private const val TRENDING_TIME_WINDOW_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val PROXIMITY_RADIUS_KM = 30.0 // 30 km radius for "your area"
        private const val NOTIFICATION_COOLDOWN_MS = 7 * 24 * 60 * 60 * 1000L // 7 days per post
    }

    /**
     * Check if a post has become trending after receiving a new like
     * @param post The post that was just liked
     */
    suspend fun checkIfTrending(post: Post) {
        try {
            // Only check for issue-type posts
            if (post.type?.equals("issue", ignoreCase = true) != true) {
                return
            }

            // Check if post meets trending criteria
            if (!isTrending(post)) {
                return
            }

            // Find nearby users to notify
            val nearbyUsers = findNearbyUsers(post.latitude, post.longitude)

            // Notify each user (except the post owner)
            nearbyUsers.forEach { userId ->
                if (userId != post.userId && !wasRecentlyNotified(post.postId, userId)) {
                    notificationManager.sendTrendingIssueNotification(
                        userId = userId,
                        postId = post.postId,
                        postTitle = post.title ?: post.caption ?: "an issue",
                        upvoteCount = post.likes.toInt()
                    )

                    // Record this notification
                    recordNotification(post.postId, userId)
                    Log.d(TAG, "Trending issue notification sent: ${post.postId} to user $userId")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for trending issue", e)
        }
    }

    /**
     * Check if a post meets the criteria for trending
     */
    private fun isTrending(post: Post): Boolean {
        // Must have minimum upvotes
        if (post.likes < MIN_UPVOTES_FOR_TRENDING) return false

        // Must be relatively recent (within time window)
        val timeSincePost = System.currentTimeMillis() - (post.timestamp ?: 0L)
        if (timeSincePost > TRENDING_TIME_WINDOW_MS) return false

        // Calculate engagement rate (likes per hour)
        val hoursOld = timeSincePost / (60 * 60 * 1000.0)
        val engagementRate = post.likes / hoursOld.coerceAtLeast(1.0)

        // Trending if getting at least 0.5 likes per hour
        return engagementRate >= 0.5
    }

    /**
     * Find users who have posts or activity near the given location
     */
    private suspend fun findNearbyUsers(latitude: Double, longitude: Double): List<String> {
        return try {
            // Get all posts within the area
            val posts = firestore.collection("posts")
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Post::class.java) }

            // Filter by proximity and get unique user IDs
            posts
                .filter {
                    val distance = LocationUtils.calculateDistance(
                        latitude, longitude,
                        it.latitude, it.longitude
                    ) / 1000.0 // Convert to km
                    distance <= PROXIMITY_RADIUS_KM
                }
                .map { it.userId }
                .distinct()
        } catch (e: Exception) {
            Log.e(TAG, "Error finding nearby users", e)
            emptyList()
        }
    }

    /**
     * Check if we've recently notified this user about this trending post
     */
    private suspend fun wasRecentlyNotified(postId: String, userId: String): Boolean {
        return try {
            val doc = firestore.collection("trending_notifications")
                .document("${userId}_${postId}")
                .get()
                .await()

            if (!doc.exists()) return false

            val timestamp = doc.getLong("timestamp") ?: 0L
            val timeSinceNotification = System.currentTimeMillis() - timestamp

            timeSinceNotification < NOTIFICATION_COOLDOWN_MS
        } catch (e: Exception) {
            Log.e(TAG, "Error checking notification cooldown", e)
            false
        }
    }

    /**
     * Record that we've sent a notification about this trending post
     */
    private suspend fun recordNotification(postId: String, userId: String) {
        try {
            firestore.collection("trending_notifications")
                .document("${userId}_${postId}")
                .set(
                    mapOf(
                        "postId" to postId,
                        "userId" to userId,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error recording notification", e)
        }
    }
}

