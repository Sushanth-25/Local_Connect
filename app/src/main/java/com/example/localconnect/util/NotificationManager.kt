package com.example.localconnect.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.localconnect.data.model.Notification
import com.example.localconnect.data.model.NotificationType
import com.example.localconnect.data.model.UserDevice
import com.example.localconnect.data.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class NotificationManager(private val context: Context) {

    private val notificationRepository = NotificationRepository()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "NotificationManager"
        private const val PREFS_NAME = "notification_prefs"
        private const val KEY_DEVICE_ID = "device_id"
    }

    /**
     * Initialize FCM and register device
     */
    suspend fun initializeFCM() {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "FCM Token: $token")

            val userId = auth.currentUser?.uid
            if (userId != null) {
                saveDeviceInfo(userId, token)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting FCM token", e)
        }
    }

    /**
     * Save device information
     */
    private suspend fun saveDeviceInfo(userId: String, fcmToken: String) {
        try {
            val deviceId = getOrCreateDeviceId()
            val device = UserDevice(
                deviceId = deviceId,
                userId = userId,
                deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
                deviceModel = Build.MODEL,
                osVersion = Build.VERSION.RELEASE,
                fcmToken = fcmToken,
                lastLoginTimestamp = System.currentTimeMillis(),
                isCurrentDevice = true
            )

            firestore.collection("user_devices")
                .document(deviceId)
                .set(device)
                .await()

            // Update user's FCM token
            firestore.collection("users")
                .document(userId)
                .update("fcmToken", fcmToken)
                .await()

            Log.d(TAG, "Device info saved")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving device info", e)
        }
    }

    /**
     * Get or create a unique device ID
     */
    private fun getOrCreateDeviceId(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)

        if (deviceId == null) {
            deviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: java.util.UUID.randomUUID().toString()

            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }

        return deviceId
    }

    /**
     * Check for new device login
     */
    suspend fun checkForNewDeviceLogin(userId: String) {
        try {
            val currentDeviceId = getOrCreateDeviceId()

            val devices = firestore.collection("user_devices")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val isNewDevice = devices.documents.none {
                it.id == currentDeviceId
            }

            if (isNewDevice && devices.documents.isNotEmpty()) {
                // This is a new device, send notification
                sendNewDeviceLoginNotification(userId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for new device", e)
        }
    }

    /**
     * Send notification for status update
     */
    suspend fun sendStatusUpdateNotification(
        userId: String,
        postId: String,
        oldStatus: String,
        newStatus: String,
        postTitle: String
    ) {
        try {
            val notificationType = when {
                oldStatus.equals("submitted", true) && newStatus.equals("in_progress", true) ->
                    NotificationType.STATUS_SUBMITTED_TO_IN_PROGRESS
                oldStatus.equals("in_progress", true) && newStatus.equals("resolved", true) ->
                    NotificationType.STATUS_IN_PROGRESS_TO_RESOLVED
                oldStatus.equals("resolved", true) && newStatus.equals("closed", true) ->
                    NotificationType.STATUS_RESOLVED_TO_CLOSED
                else -> return
            }

            val notification = Notification(
                userId = userId,
                type = notificationType,
                title = "Issue Status Updated",
                message = "Your issue \"$postTitle\" has been updated to: $newStatus",
                postId = postId,
                timestamp = System.currentTimeMillis()
            )

            notificationRepository.createNotification(notification).fold(
                onSuccess = { Log.d(TAG, "Status notification created: $it") },
                onFailure = { Log.e(TAG, "Failed to create status notification", it) }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in sendStatusUpdateNotification", e)
        }
    }

    /**
     * Send notification for new comment
     */
    suspend fun sendNewCommentNotification(
        postOwnerId: String,
        postId: String,
        postTitle: String,
        commenterId: String,
        commenterName: String,
        commenterProfileUrl: String?,
        commentText: String
    ) {
        try {
            // Don't send notification if commenting on own post
            if (postOwnerId == commenterId) {
                Log.d(TAG, "Skipping comment notification: user commented on own post")
                return
            }

            val notification = Notification(
                userId = postOwnerId,
                type = NotificationType.NEW_COMMENT,
                title = "New Comment",
                message = "$commenterName commented on your post: \"${commentText.take(50)}${if (commentText.length > 50) "..." else ""}\"",
                postId = postId,
                senderId = commenterId,
                senderName = commenterName,
                senderProfileUrl = commenterProfileUrl,
                timestamp = System.currentTimeMillis()
            )

            Log.d(TAG, "Creating comment notification for user: $postOwnerId")
            notificationRepository.createNotification(notification).fold(
                onSuccess = { Log.d(TAG, "Comment notification created: $it") },
                onFailure = { Log.e(TAG, "Failed to create comment notification", it) }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in sendNewCommentNotification", e)
        }
    }

    /**
     * Send notification for upvote/like
     */
    suspend fun sendLikeNotification(
        postOwnerId: String,
        postId: String,
        postTitle: String,
        likerId: String,
        likerName: String,
        likerProfileUrl: String?,
        isUpvote: Boolean = false
    ) {
        // Don't send notification if liking own post
        if (postOwnerId == likerId) return

        val notification = Notification(
            userId = postOwnerId,
            type = if (isUpvote) NotificationType.POST_UPVOTED else NotificationType.POST_LIKED,
            title = if (isUpvote) "New Upvote" else "New Like",
            message = "$likerName ${if (isUpvote) "upvoted" else "liked"} your post: \"$postTitle\"",
            postId = postId,
            senderId = likerId,
            senderName = likerName,
            senderProfileUrl = likerProfileUrl,
            timestamp = System.currentTimeMillis()
        )

        notificationRepository.createNotification(notification)
    }

    /**
     * Send notification for similar issue nearby
     */
    suspend fun sendSimilarIssueNotification(
        userId: String,
        userPostId: String,
        similarPostId: String,
        similarPostTitle: String,
        distance: Double
    ) {
        val notification = Notification(
            userId = userId,
            type = NotificationType.SIMILAR_ISSUE_NEARBY,
            title = "Similar Issue Found",
            message = "A similar issue \"$similarPostTitle\" was reported ${distance.toInt()}m away from your location",
            postId = similarPostId,
            metadata = mapOf(
                "userPostId" to userPostId,
                "distance" to distance.toString()
            ),
            timestamp = System.currentTimeMillis()
        )

        notificationRepository.createNotification(notification)
    }

    /**
     * Send notification for trending issue
     */
    suspend fun sendTrendingIssueNotification(
        userId: String,
        postId: String,
        postTitle: String,
        upvoteCount: Int
    ) {
        val notification = Notification(
            userId = userId,
            type = NotificationType.TRENDING_ISSUE,
            title = "Trending Issue Alert",
            message = "\"$postTitle\" is trending in your area with $upvoteCount upvotes",
            postId = postId,
            timestamp = System.currentTimeMillis()
        )

        notificationRepository.createNotification(notification)
    }

    /**
     * Send notification for new device login
     */
    private suspend fun sendNewDeviceLoginNotification(userId: String) {
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

        val notification = Notification(
            userId = userId,
            type = NotificationType.NEW_DEVICE_LOGIN,
            title = "New Device Login",
            message = "Your account was accessed from a new device: $deviceName",
            timestamp = System.currentTimeMillis()
        )

        notificationRepository.createNotification(notification)
    }

    /**
     * Send notification for profile update
     */
    suspend fun sendProfileUpdateNotification(
        userId: String,
        updateType: String,
        oldValue: String,
        newValue: String
    ) {
        val notificationType = when (updateType) {
            "email" -> NotificationType.EMAIL_CHANGED
            "phone" -> NotificationType.PHONE_CHANGED
            else -> NotificationType.PROFILE_UPDATE
        }

        val notification = Notification(
            userId = userId,
            type = notificationType,
            title = "Profile Updated",
            message = when (updateType) {
                "email" -> "Your email was changed from $oldValue to $newValue"
                "phone" -> "Your phone number was updated"
                else -> "Your profile was updated successfully"
            },
            timestamp = System.currentTimeMillis()
        )

        notificationRepository.createNotification(notification)
    }
}

