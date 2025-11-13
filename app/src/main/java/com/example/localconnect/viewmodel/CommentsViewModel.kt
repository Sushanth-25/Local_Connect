package com.example.localconnect.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.localconnect.data.model.Comment
import com.example.localconnect.data.repository.CommentRepository
import com.example.localconnect.data.repository.FirebasePostRepository
import com.example.localconnect.data.repository.PostStats
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing comments and post statistics
 * Updated to use toggle-based likes and single-view tracking
 */
class CommentsViewModel : ViewModel() {
    private val commentRepository = CommentRepository()
    private val postRepository = FirebasePostRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    private val _postStats = MutableStateFlow<PostStats?>(null)
    val postStats: StateFlow<PostStats?> = _postStats

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    companion object {
        private const val TAG = "CommentsViewModel"
    }

    /**
     * Load comments for a post with real-time updates
     */
    fun observeComments(postId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                commentRepository.observeComments(postId)
                    .collect { comments ->
                        _comments.value = comments
                        _isLoading.value = false
                        Log.d(TAG, "Loaded ${comments.size} comments for post $postId")
                    }
            } catch (e: Exception) {
                _error.value = "Failed to load comments: ${e.message}"
                _isLoading.value = false
                Log.e(TAG, "Error observing comments", e)
            }
        }
    }

    /**
     * Load post statistics
     */
    fun loadPostStats(postId: String) {
        viewModelScope.launch {
            try {
                val stats = postRepository.getPostStats(postId)
                _postStats.value = stats
                Log.d(TAG, "Loaded stats for post $postId: $stats")
            } catch (e: Exception) {
                _error.value = "Failed to load post stats: ${e.message}"
                Log.e(TAG, "Error loading post stats", e)
            }
        }
    }

    /**
     * Add a new comment
     * This automatically increments the post's comment counter
     */
    fun addComment(
        postId: String,
        userId: String,
        userName: String,
        text: String,
        userProfileUrl: String? = null,
        parentCommentId: String? = null
    ) {
        if (text.isBlank()) {
            _error.value = "Comment text cannot be empty"
            return
        }

        viewModelScope.launch {
            try {
                val comment = Comment(
                    postId = postId,
                    userId = userId,
                    userName = userName,
                    userProfileUrl = userProfileUrl,
                    text = text.trim(),
                    timestamp = System.currentTimeMillis(),
                    parentCommentId = parentCommentId
                )

                commentRepository.addComment(comment).onSuccess { commentId ->
                    Log.d(TAG, "Comment added successfully: $commentId")
                    // Reload stats to show updated count
                    loadPostStats(postId)
                }.onFailure { exception ->
                    _error.value = "Failed to add comment: ${exception.message}"
                    Log.e(TAG, "Error adding comment", exception)
                }
            } catch (e: Exception) {
                _error.value = "Failed to add comment: ${e.message}"
                Log.e(TAG, "Error adding comment", e)
            }
        }
    }

    /**
     * Delete a comment
     * This automatically decrements the post's comment counter
     */
    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            try {
                commentRepository.deleteComment(postId, commentId).onSuccess {
                    Log.d(TAG, "Comment deleted successfully: $commentId")
                    // Reload stats to show updated count
                    loadPostStats(postId)
                }.onFailure { exception ->
                    _error.value = "Failed to delete comment: ${exception.message}"
                    Log.e(TAG, "Error deleting comment", exception)
                }
            } catch (e: Exception) {
                _error.value = "Failed to delete comment: ${e.message}"
                Log.e(TAG, "Error deleting comment", e)
            }
        }
    }

    /**
     * Toggle like on a comment (like if not liked, unlike if already liked)
     */
    fun toggleCommentLike(postId: String, commentId: String) {
        val userId = auth.currentUser?.uid ?: run {
            _error.value = "You must be logged in to like comments"
            return
        }

        viewModelScope.launch {
            try {
                commentRepository.toggleCommentLike(postId, commentId, userId).onSuccess { isLiked ->
                    Log.d(TAG, "Comment like toggled: commentId=$commentId, isLiked=$isLiked")
                }.onFailure { exception ->
                    _error.value = "Failed to toggle comment like: ${exception.message}"
                    Log.e(TAG, "Error toggling comment like", exception)
                }
            } catch (e: Exception) {
                _error.value = "Failed to toggle comment like: ${e.message}"
                Log.e(TAG, "Error toggling comment like", e)
            }
        }
    }

    /**
     * Toggle like on a post (like if not liked, unlike if already liked)
     */
    fun togglePostLike(postId: String) {
        val userId = auth.currentUser?.uid ?: run {
            _error.value = "You must be logged in to like posts"
            return
        }

        viewModelScope.launch {
            try {
                postRepository.togglePostLike(postId, userId).onSuccess { isLiked ->
                    Log.d(TAG, "Post like toggled: postId=$postId, isLiked=$isLiked")
                    // Reload stats to show updated count
                    loadPostStats(postId)
                }.onFailure { exception ->
                    _error.value = "Failed to toggle post like: ${exception.message}"
                    Log.e(TAG, "Error toggling post like", exception)
                }
            } catch (e: Exception) {
                _error.value = "Failed to toggle post like: ${e.message}"
                Log.e(TAG, "Error toggling post like", e)
            }
        }
    }

    /**
     * Track when a user views a post (only once per user)
     * Call this when the post detail screen is opened
     */
    fun trackPostViewOnce(postId: String) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                postRepository.incrementPostViewOnce(postId, userId).onSuccess { wasCounted ->
                    if (wasCounted) {
                        Log.d(TAG, "Post view tracked: $postId (first time)")
                        // Reload stats to show updated view count
                        loadPostStats(postId)
                    } else {
                        Log.d(TAG, "Post view already tracked: $postId")
                    }
                }.onFailure { exception ->
                    // Don't show error to user for view tracking
                    Log.e(TAG, "Error tracking view", exception)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error tracking view", e)
            }
        }
    }

    /**
     * Get replies to a specific comment
     */
    fun loadReplies(postId: String, parentCommentId: String) {
        viewModelScope.launch {
            try {
                val replies = commentRepository.getReplies(postId, parentCommentId)
                Log.d(TAG, "Loaded ${replies.size} replies for comment $parentCommentId")
                // You can emit this to a separate StateFlow if needed
            } catch (e: Exception) {
                _error.value = "Failed to load replies: ${e.message}"
                Log.e(TAG, "Error loading replies", e)
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}

