package com.example.localconnect.repository

import com.example.localconnect.data.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    suspend fun getAllPosts(): List<Post>
    fun observeAllPosts(limit: Long = 100): Flow<List<Post>>
    suspend fun createPost(post: Post): Result<Unit>
    suspend fun likePost(postId: String): Result<Unit>
}
