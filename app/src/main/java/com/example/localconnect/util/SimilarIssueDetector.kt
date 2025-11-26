package com.example.localconnect.util

import android.content.Context
import android.util.Log
import com.example.localconnect.data.model.Post
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Detects similar issues around the user based on location, category, and tags
 */
class SimilarIssueDetector(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val notificationManager = NotificationManager(context)

    companion object {
        private const val TAG = "SimilarIssueDetector"
        private const val PROXIMITY_RADIUS_METERS = 500.0 // 500 meters radius
        private const val MIN_TAG_MATCH_COUNT = 1 // Minimum matching tags
        private const val NOTIFICATION_COOLDOWN_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    /**
     * Check for similar issues when a new post is created
     * @param newPost The newly created post
     * @param userId The user who should receive notifications about similar issues
     */
    suspend fun checkForSimilarIssues(newPost: Post, userId: String) {
        try {
            // Only check for issue-type posts
            if (newPost.type?.equals("issue", ignoreCase = true) != true) {
                return
            }

            // Get user's recent posts to compare
            val userPosts = firestore.collection("posts")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", "issue")
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Post::class.java) }

            // Check each user post against the new post
            userPosts.forEach { userPost ->
                if (isSimilarIssue(userPost, newPost)) {
                    // Check if we've recently notified about this pair
                    if (!wasRecentlyNotified(userPost.postId, newPost.postId, userId)) {
                        val distance = LocationUtils.calculateDistance(
                            userPost.latitude, userPost.longitude,
                            newPost.latitude, newPost.longitude
                        )

                        notificationManager.sendSimilarIssueNotification(
                            userId = userId,
                            userPostId = userPost.postId,
                            similarPostId = newPost.postId,
                            similarPostTitle = newPost.title ?: newPost.caption ?: "an issue",
                            distance = distance
                        )

                        // Record this notification
                        recordNotification(userPost.postId, newPost.postId, userId)
                        Log.d(TAG, "Similar issue notification sent: ${newPost.postId} to user $userId")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for similar issues", e)
        }
    }

    /**
     * Check if two posts are similar based on location, category, and tags
     */
    private fun isSimilarIssue(post1: Post, post2: Post): Boolean {
        // Don't compare a post with itself
        if (post1.postId == post2.postId) return false

        // Check proximity
        val distance = LocationUtils.calculateDistance(
            post1.latitude, post1.longitude,
            post2.latitude, post2.longitude
        )
        if (distance > PROXIMITY_RADIUS_METERS) return false

        // Check category match
        val categoryMatches = post1.category?.equals(post2.category, ignoreCase = true) == true

        // Check tag overlap
        val commonTags = post1.tags.intersect(post2.tags.toSet())
        val tagMatches = commonTags.size >= MIN_TAG_MATCH_COUNT

        // Similar if category matches OR sufficient tag overlap
        return categoryMatches || tagMatches
    }

    /**
     * Check if we've recently notified about this pair of posts
     */
    private suspend fun wasRecentlyNotified(
        userPostId: String,
        similarPostId: String,
        userId: String
    ): Boolean {
        return try {
            val notificationKey = "${userPostId}_${similarPostId}"
            val doc = firestore.collection("similar_issue_notifications")
                .document("${userId}_${notificationKey}")
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
     * Record that we've sent a notification about this pair
     */
    private suspend fun recordNotification(
        userPostId: String,
        similarPostId: String,
        userId: String
    ) {
        try {
            val notificationKey = "${userPostId}_${similarPostId}"
            firestore.collection("similar_issue_notifications")
                .document("${userId}_${notificationKey}")
                .set(
                    mapOf(
                        "userPostId" to userPostId,
                        "similarPostId" to similarPostId,
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

