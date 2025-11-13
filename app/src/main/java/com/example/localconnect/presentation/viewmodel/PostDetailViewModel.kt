package com.example.localconnect.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.localconnect.data.model.Comment
import com.example.localconnect.data.model.Post
import com.example.localconnect.data.repository.CommentRepository
import com.example.localconnect.data.repository.FirebasePostRepository
import com.example.localconnect.data.repository.PostStats
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Post Detail Screen
 * Handles post display, comments, likes, and view tracking
 */
class PostDetailViewModel : ViewModel() {
    private val _selectedPost = MutableStateFlow<Post?>(null)
    val selectedPost: StateFlow<Post?> = _selectedPost.asStateFlow()

    private val commentRepository = CommentRepository()
    private val postRepository = FirebasePostRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _postStats = MutableStateFlow<PostStats?>(null)
    val postStats: StateFlow<PostStats?> = _postStats.asStateFlow()

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _likedComments = MutableStateFlow<Set<String>>(emptySet())
    val likedComments: StateFlow<Set<String>> = _likedComments.asStateFlow()

    companion object {
        private const val TAG = "PostDetailViewModel"
    }

    fun setSelectedPost(post: Post) {
        _selectedPost.value = post
        val currentUserId = auth.currentUser?.uid
        // Load comments and stats when post is set
        post.postId.takeIf { it.isNotEmpty() }?.let { postId ->
            observeComments(postId)
            loadPostStats(postId)

            // Track view only once per user
            if (currentUserId != null) {
                trackPostViewOnce(postId, currentUserId)
                loadInitialLikeState(postId, currentUserId)
            }
        }
    }

    fun clearSelectedPost() {
        _selectedPost.value = null
        _comments.value = emptyList()
        _postStats.value = null
        _isLiked.value = false
        _likedComments.value = emptySet()
    }

    private fun observeComments(postId: String) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid
                commentRepository.observeComments(postId).collect { comments ->
                    _comments.value = comments
                    Log.d(TAG, "Loaded ${comments.size} comments for post $postId")

                    // Load like states for comments when they change
                    if (userId != null && comments.isNotEmpty()) {
                        loadCommentLikeStates(postId, userId)
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to load comments: ${e.message}"
                Log.e(TAG, "Error observing comments", e)
            }
        }
    }

    private fun loadPostStats(postId: String) {
        viewModelScope.launch {
            try {
                val stats = postRepository.getPostStats(postId)
                _postStats.value = stats
                Log.d(TAG, "Loaded stats for post $postId: $stats")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading post stats", e)
            }
        }
    }

    private fun trackPostViewOnce(postId: String, userId: String) {
        viewModelScope.launch {
            try {
                postRepository.incrementPostViewOnce(postId, userId).onSuccess { wasCounted ->
                    if (wasCounted) {
                        Log.d(TAG, "View tracked for post $postId (first time)")
                        // Reload stats to show updated view count
                        loadPostStats(postId)
                    } else {
                        Log.d(TAG, "View already tracked for post $postId")
                    }
                }.onFailure { exception ->
                    Log.e(TAG, "Error tracking view", exception)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error tracking view", e)
            }
        }
    }

    private fun loadInitialLikeState(postId: String, userId: String) {
        viewModelScope.launch {
            try {
                val isLiked = postRepository.hasUserLikedPost(postId, userId)
                _isLiked.value = isLiked
                Log.d(TAG, "Initial like state loaded: $isLiked")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading like state", e)
            }
        }
    }

    fun addComment(text: String) {
        val postId = _selectedPost.value?.postId ?: return
        val currentUser = auth.currentUser ?: run {
            _error.value = "You must be logged in to comment"
            return
        }

        if (text.isBlank()) {
            _error.value = "Comment cannot be empty"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                val comment = Comment(
                    postId = postId,
                    userId = currentUser.uid,
                    userName = currentUser.displayName ?: "Anonymous",
                    userProfileUrl = currentUser.photoUrl?.toString(),
                    text = text.trim(),
                    timestamp = System.currentTimeMillis()
                )

                commentRepository.addComment(comment).onSuccess {
                    Log.d(TAG, "Comment added successfully")
                    // Reload stats to show updated count
                    loadPostStats(postId)
                }.onFailure { exception ->
                    _error.value = "Failed to add comment: ${exception.message}"
                    Log.e(TAG, "Error adding comment", exception)
                }
            } catch (e: Exception) {
                _error.value = "Failed to add comment: ${e.message}"
                Log.e(TAG, "Error adding comment", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteComment(commentId: String) {
        val postId = _selectedPost.value?.postId ?: return

        viewModelScope.launch {
            try {
                commentRepository.deleteComment(postId, commentId).onSuccess {
                    Log.d(TAG, "Comment deleted successfully")
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
     * Toggle like on a comment
     * Uses Firestore subcollection to track if user has liked
     * The toggle is handled server-side atomically
     */
    fun toggleCommentLike(commentId: String) {
        val postId = _selectedPost.value?.postId ?: return
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                commentRepository.toggleCommentLike(postId, commentId, userId).onSuccess { isNowLiked ->
                    // Update local state for immediate UI feedback
                    _likedComments.value = if (isNowLiked) {
                        _likedComments.value + commentId
                    } else {
                        _likedComments.value - commentId
                    }
                    Log.d(TAG, "Comment like toggled: commentId=$commentId, isLiked=$isNowLiked")
                }.onFailure { exception ->
                    _error.value = "Failed to toggle comment like"
                    Log.e(TAG, "Error toggling comment like", exception)
                }
            } catch (e: Exception) {
                _error.value = "Failed to update comment like"
                Log.e(TAG, "Error toggling comment like", e)
            }
        }
    }

    /**
     * Load initial like states for all comments
     */
    private fun loadCommentLikeStates(postId: String, userId: String) {
        viewModelScope.launch {
            try {
                val comments = _comments.value
                val likedSet = mutableSetOf<String>()

                comments.forEach { comment ->
                    val isLiked = commentRepository.hasUserLikedComment(postId, comment.commentId, userId)
                    if (isLiked) {
                        likedSet.add(comment.commentId)
                    }
                }

                _likedComments.value = likedSet
                Log.d(TAG, "Loaded like states for ${likedSet.size} comments")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading comment like states", e)
            }
        }
    }

    fun togglePostLike() {
        val postId = _selectedPost.value?.postId ?: return
        val userId = auth.currentUser?.uid ?: run {
            _error.value = "You must be logged in to like posts"
            return
        }

        viewModelScope.launch {
            try {
                postRepository.togglePostLike(postId, userId).onSuccess { isNowLiked ->
                    _isLiked.value = isNowLiked
                    Log.d(TAG, "Post like toggled: postId=$postId, isLiked=$isNowLiked")
                    loadPostStats(postId)
                }.onFailure { exception ->
                    _error.value = "Failed to toggle post like"
                    Log.e(TAG, "Error toggling post like", exception)
                }
            } catch (e: Exception) {
                _error.value = "Failed to update post like"
                Log.e(TAG, "Error toggling post like", e)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

