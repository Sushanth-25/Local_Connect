package com.example.localconnect.data.model

import com.google.firebase.firestore.PropertyName

data class Notification(
    @PropertyName("notificationId") val notificationId: String = "",
    @PropertyName("userId") val userId: String = "", // Recipient user ID
    @PropertyName("type") val type: NotificationType = NotificationType.OTHER,
    @PropertyName("title") val title: String = "",
    @PropertyName("message") val message: String = "",
    @PropertyName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @PropertyName("isRead") val isRead: Boolean = false,
    @PropertyName("imageUrl") val imageUrl: String? = null,

    // Reference data for navigation
    @PropertyName("postId") val postId: String? = null,
    @PropertyName("commentId") val commentId: String? = null,
    @PropertyName("senderId") val senderId: String? = null,
    @PropertyName("senderName") val senderName: String? = null,
    @PropertyName("senderProfileUrl") val senderProfileUrl: String? = null,

    // Additional data
    @PropertyName("actionUrl") val actionUrl: String? = null,
    @PropertyName("metadata") val metadata: Map<String, String> = emptyMap()
) {
    // No-argument constructor for Firestore
    constructor() : this(
        notificationId = "",
        userId = "",
        type = NotificationType.OTHER,
        title = "",
        message = "",
        timestamp = System.currentTimeMillis(),
        isRead = false,
        imageUrl = null,
        postId = null,
        commentId = null,
        senderId = null,
        senderName = null,
        senderProfileUrl = null,
        actionUrl = null,
        metadata = emptyMap()
    )
}

enum class NotificationType(val value: String) {
    // Status updates
    STATUS_CHANGE("status_change"),
    STATUS_SUBMITTED_TO_IN_PROGRESS("status_submitted_to_in_progress"),
    STATUS_IN_PROGRESS_TO_RESOLVED("status_in_progress_to_resolved"),
    STATUS_RESOLVED_TO_CLOSED("status_resolved_to_closed"),

    // Comments
    NEW_COMMENT("new_comment"),
    COMMENT_REPLY("comment_reply"),

    // Likes/Upvotes
    POST_LIKED("post_liked"),
    POST_UPVOTED("post_upvoted"),
    COMMENT_LIKED("comment_liked"),

    // Trending and Similar
    SIMILAR_ISSUE_NEARBY("similar_issue_nearby"),
    TRENDING_ISSUE("trending_issue"),

    // Security
    NEW_DEVICE_LOGIN("new_device_login"),
    SUSPICIOUS_LOGIN("suspicious_login"),

    // Profile
    PROFILE_UPDATE("profile_update"),
    EMAIL_CHANGED("email_changed"),
    PHONE_CHANGED("phone_changed"),

    // System
    SYSTEM_ANNOUNCEMENT("system_announcement"),
    OTHER("other");

    companion object {
        fun fromString(value: String?): NotificationType {
            return entries.find { it.value == value } ?: OTHER
        }
    }
}

// User device info for tracking logins
data class UserDevice(
    @PropertyName("deviceId") val deviceId: String = "",
    @PropertyName("userId") val userId: String = "",
    @PropertyName("deviceName") val deviceName: String = "",
    @PropertyName("deviceModel") val deviceModel: String = "",
    @PropertyName("osVersion") val osVersion: String = "",
    @PropertyName("fcmToken") val fcmToken: String = "",
    @PropertyName("lastLoginTimestamp") val lastLoginTimestamp: Long = System.currentTimeMillis(),
    @PropertyName("lastLoginLocation") val lastLoginLocation: String? = null,
    @PropertyName("isCurrentDevice") val isCurrentDevice: Boolean = false
) {
    constructor() : this("", "", "", "", "", "", System.currentTimeMillis(), null, false)
}

// Notification preferences
data class NotificationPreferences(
    @PropertyName("userId") val userId: String = "",
    @PropertyName("statusUpdates") val statusUpdates: Boolean = true,
    @PropertyName("comments") val comments: Boolean = true,
    @PropertyName("likes") val likes: Boolean = true,
    @PropertyName("trending") val trending: Boolean = true,
    @PropertyName("security") val security: Boolean = true,
    @PropertyName("profileChanges") val profileChanges: Boolean = true,
    @PropertyName("pushEnabled") val pushEnabled: Boolean = true,
    @PropertyName("emailEnabled") val emailEnabled: Boolean = false
) {
    constructor() : this("", true, true, true, true, true, true, true, false)
}
