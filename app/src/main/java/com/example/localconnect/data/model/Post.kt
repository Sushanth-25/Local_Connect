package com.example.localconnect.data.model

import com.cloudinary.Url
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.Exclude
import java.util.Date

data class Post(
    @PropertyName("postId") val postId: String = "",
    @PropertyName("userId") val userId: String = "",
    @PropertyName("caption") val caption: String? = null,
    @PropertyName("description") val description: String? = null,
    @PropertyName("title") val title: String? = null,
    @PropertyName("category") val category: String? = null,
    @PropertyName("status") val status: String? = null,
    @PropertyName("hasImage") val hasImage: Boolean = false,
    @PropertyName("mediaUrls") val mediaUrls: List<String> = emptyList(),
    @PropertyName("thumbnailUrls") val thumbnailUrls: List<String> = emptyList(),
    @PropertyName("tags") val tags: List<String> = emptyList(),
    @PropertyName("localOnly") val isLocalOnly: Boolean = true,
    @PropertyName("timestamp") val timestamp: Long? = null,
    @PropertyName("updatedAt") val updatedAt: Long? = null,
    @PropertyName("likes") val likes: Int = 0,
    @PropertyName("comments") val comments: Int = 0,
    @PropertyName("upvotes") val upvotes: Int = 0,
    @PropertyName("views") val views: Int = 0,
    @PropertyName("priority") val priority: Int? = null,
    @PropertyName("type") val type: String? = null,

    // Location - required for all posts
    @PropertyName("latitude") val latitude: Double = 0.0,
    @PropertyName("longitude") val longitude: Double = 0.0,
    @PropertyName("locationName") val locationName: String = "",

    // Legacy fields - excluded from serialization but kept for deserialization compatibility
    @Exclude @get:Exclude var imageUrl: String? = null,
    @Exclude @get:Exclude var videoUrl: String? = null
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
        hasImage = false,
        mediaUrls = emptyList(),
        tags = emptyList(),
        thumbnailUrls = emptyList(),
        isLocalOnly = false,
        timestamp = null,
        updatedAt = null,
        likes = 0,
        comments = 0,
        upvotes = 0,
        views = 0,
        priority = null,
        type = null,
        imageUrl = null,
        videoUrl = null,
        latitude = 0.0,
        longitude = 0.0,
        locationName = ""
    )
}

// Post types enum for better type safety
enum class PostType(val value: String) {
    ISSUE("ISSUE"),
    POST("POST"),
    EVENT("EVENT");

    companion object {
        fun fromString(value: String?): PostType {
            return entries.find { it.value == value } ?: POST
        }
    }
}
