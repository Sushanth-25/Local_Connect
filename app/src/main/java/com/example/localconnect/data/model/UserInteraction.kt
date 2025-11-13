package com.example.localconnect.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Represents a user's like on a post or comment
 */
data class PostLike(
    @PropertyName("userId") val userId: String = "",
    @PropertyName("timestamp") val timestamp: Long = 0L
) {
    constructor() : this("", 0L)
}

/**
 * Represents a user's view on a post
 */
data class PostView(
    @PropertyName("userId") val userId: String = "",
    @PropertyName("timestamp") val timestamp: Long = 0L
) {
    constructor() : this("", 0L)
}

/**
 * Represents a user's like on a comment
 */
data class CommentLike(
    @PropertyName("userId") val userId: String = "",
    @PropertyName("timestamp") val timestamp: Long = 0L
) {
    constructor() : this("", 0L)
}

