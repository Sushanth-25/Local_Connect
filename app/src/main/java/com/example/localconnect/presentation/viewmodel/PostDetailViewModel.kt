package com.example.localconnect.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.localconnect.data.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared ViewModel to hold the selected post for detail view
 * This avoids the need to pass Post through navigation which requires Parcelable
 */
class PostDetailViewModel : ViewModel() {
    private val _selectedPost = MutableStateFlow<Post?>(null)
    val selectedPost: StateFlow<Post?> = _selectedPost.asStateFlow()

    fun setSelectedPost(post: Post) {
        _selectedPost.value = post
    }

    fun clearSelectedPost() {
        _selectedPost.value = null
    }
}

