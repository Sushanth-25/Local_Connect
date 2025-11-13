package com.example.localconnect.data.model

import com.google.firebase.firestore.PropertyName

data class Comment(
    @PropertyName("commentId") val commentId: String = "",
    @PropertyName("postId") val postId: String = "",
    @PropertyName("userId") val userId: String = "",
    @PropertyName("userName") val userName: String = "",
    @PropertyName("userProfileUrl") val userProfileUrl: String? = null,
    @PropertyName("text") val text: String = "",
    @PropertyName("timestamp") val timestamp: Long = 0L,
    @PropertyName("likes") val likes: Int = 0,
    @PropertyName("parentCommentId") val parentCommentId: String? = null // For nested replies
) {
    // No-argument constructor for Firestore
    constructor() : this(
        commentId = "",
        postId = "",
        userId = "",
        userName = "",
        userProfileUrl = null,
        text = "",
        timestamp = 0L,
        likes = 0,
        parentCommentId = null
    )
}

