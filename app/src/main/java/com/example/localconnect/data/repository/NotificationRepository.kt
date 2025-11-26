package com.example.localconnect.data.repository

import android.util.Log
import com.example.localconnect.data.model.Notification
import com.example.localconnect.data.model.NotificationPreferences
import com.example.localconnect.data.model.NotificationType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class NotificationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val notificationsCollection = firestore.collection("notifications")
    private val preferencesCollection = firestore.collection("notification_preferences")

    companion object {
        private const val TAG = "NotificationRepository"
    }

    /**
     * Create and save a notification
     */
    suspend fun createNotification(notification: Notification): Result<String> {
        return try {
            val notificationId = notification.notificationId.ifEmpty { UUID.randomUUID().toString() }
            val notificationWithId = notification.copy(notificationId = notificationId)

            Log.d(TAG, "Creating notification for user: ${notificationWithId.userId}")
            Log.d(TAG, "Notification type: ${notificationWithId.type}")
            Log.d(TAG, "Notification title: ${notificationWithId.title}")

            notificationsCollection
                .document(notificationId)
                .set(notificationWithId)
                .await()

            Log.d(TAG, "✅ Notification created successfully: $notificationId")
            Result.success(notificationId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating notification: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get notifications for a user with real-time updates
     */
    fun getUserNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        val listener = notificationsCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to notifications", error)
                    close(error)
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Notification::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing notification: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                trySend(notifications)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get unread notification count
     */
    fun getUnreadCount(userId: String): Flow<Int> = callbackFlow {
        val listener = notificationsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting unread count", error)
                    close(error)
                    return@addSnapshotListener
                }

                trySend(snapshot?.size() ?: 0)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Mark notification as read
     */
    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection
                .document(notificationId)
                .update("isRead", true)
                .await()

            Log.d(TAG, "Notification marked as read: $notificationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as read", e)
            Result.failure(e)
        }
    }

    /**
     * Mark all notifications as read for a user
     */
    suspend fun markAllAsRead(userId: String): Result<Unit> {
        return try {
            val unreadNotifications = notificationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            val batch = firestore.batch()
            unreadNotifications.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()

            Log.d(TAG, "All notifications marked as read for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking all as read", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a notification
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection
                .document(notificationId)
                .delete()
                .await()

            Log.d(TAG, "Notification deleted: $notificationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting notification", e)
            Result.failure(e)
        }
    }

    /**
     * Delete all notifications for a user
     */
    suspend fun deleteAllNotifications(userId: String): Result<Unit> {
        return try {
            val notifications = notificationsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val batch = firestore.batch()
            notifications.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()

            Log.d(TAG, "All notifications deleted for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting all notifications", e)
            Result.failure(e)
        }
    }

    /**
     * Get notification preferences
     */
    suspend fun getNotificationPreferences(userId: String): Result<NotificationPreferences> {
        return try {
            val doc = preferencesCollection
                .document(userId)
                .get()
                .await()

            val prefs = if (doc.exists()) {
                doc.toObject(NotificationPreferences::class.java)
                    ?: NotificationPreferences(userId = userId)
            } else {
                NotificationPreferences(userId = userId)
            }

            Result.success(prefs)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting notification preferences", e)
            Result.failure(e)
        }
    }

    /**
     * Update notification preferences
     */
    suspend fun updateNotificationPreferences(preferences: NotificationPreferences): Result<Unit> {
        return try {
            preferencesCollection
                .document(preferences.userId)
                .set(preferences)
                .await()

            Log.d(TAG, "Notification preferences updated for user: ${preferences.userId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification preferences", e)
            Result.failure(e)
        }
    }

    /**
     * Get notification preferences with real-time updates
     */
    fun getNotificationPreferencesFlow(userId: String): Flow<NotificationPreferences> = callbackFlow {
        val listener = preferencesCollection
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to preferences", error)
                    close(error)
                    return@addSnapshotListener
                }

                val prefs = if (snapshot?.exists() == true) {
                    snapshot.toObject(NotificationPreferences::class.java)
                        ?: NotificationPreferences(userId = userId)
                } else {
                    NotificationPreferences(userId = userId)
                }

                trySend(prefs)
            }

        awaitClose { listener.remove() }
    }
}

