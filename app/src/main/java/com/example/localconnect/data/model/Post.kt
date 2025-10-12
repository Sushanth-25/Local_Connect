package com.example.localconnect.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Post(
    @PropertyName("postId") val postId: String = "",
    @PropertyName("userId") val userId: String = "",
    @PropertyName("caption") val caption: String? = null,
    @PropertyName("description") val description: String? = null,
    @PropertyName("title") val title: String? = null,
    @PropertyName("category") val category: String? = null,
    @PropertyName("status") val status: String? = null,
    @PropertyName("location") val location: String? = null,
    @PropertyName("hasImage") val hasImage: Boolean = false,
    @PropertyName("imageUrl") val imageUrl: String? = null,
    @PropertyName("videoUrl") val videoUrl: String? = null,
    @PropertyName("mediaUrls") val mediaUrls: List<String> = emptyList(),
    @PropertyName("tags") val tags: List<String> = emptyList(),
    @PropertyName("localOnly") val isLocalOnly: Boolean = true,
    @PropertyName("timestamp") val timestamp: Long? = null,
    @PropertyName("updatedAt") val updatedAt: Long? = null,
    @PropertyName("likes") val likes: Int = 0,
    @PropertyName("comments") val comments: Int = 0,
    @PropertyName("upvotes") val upvotes: Int = 0,
    @PropertyName("views") val views: Int = 0,
    @PropertyName("priority") val priority: Int? = null,
    @PropertyName("type") val type: String? = null
) {
    // No-argument constructor for Firestore
    constructor() : this(
        postId = "",
        userId = "",
        caption = null,
        description = null,
        title = null,
        category = null,
        status = null,
        location = null,
        hasImage = false,
        imageUrl = null,
        videoUrl = null,
        mediaUrls = emptyList(),
        tags = emptyList(),
        isLocalOnly = false,
        timestamp = null,
        updatedAt = null,
        likes = 0,
        comments = 0,
        upvotes = 0,
        views = 0,
        priority = null,
        type = null
    )
}

// Post types enum for better type safety
enum class PostType(val value: String) {
    ISSUE("ISSUE"),
    POST("POST"),
    EVENT("EVENT");

    companion object {
        fun fromString(value: String?): PostType? {
            return values().find { it.value == value }
        }
    }
}
