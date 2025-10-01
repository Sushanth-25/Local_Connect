package com.localconnect.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localconnect.data.model.Post
import com.localconnect.data.model.PostType
import com.localconnect.domain.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class CreatePostViewModel(
    private val postRepository: PostRepository,
    private val firebaseAuth: FirebaseAuth,
    private val storage: FirebaseStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState

    fun onPostTypeChange(type: PostType) {
        _uiState.value = _uiState.value.copy(
            postType = type,
            // Reset type-specific fields when changing type
            title = if (type == PostType.ISSUE) _uiState.value.title else "",
            description = if (type == PostType.ISSUE) _uiState.value.description else "",
            priority = if (type == PostType.ISSUE) _uiState.value.priority else null,
            status = if (type == PostType.ISSUE) _uiState.value.status else null
        )
    }

    fun onCategoryChange(category: String) {
        _uiState.value = _uiState.value.copy(category = category)
    }

    fun onCaptionChange(caption: String) {
        _uiState.value = _uiState.value.copy(caption = caption)
    }

    fun onTitleChange(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun onDescriptionChange(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun onTagsChange(tags: String) {
        _uiState.value = _uiState.value.copy(tags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() })
    }

    fun onLocationChange(location: String) {
        _uiState.value = _uiState.value.copy(location = location)
    }

    fun onLocalOnlyToggle(isLocalOnly: Boolean) {
        _uiState.value = _uiState.value.copy(isLocalOnly = isLocalOnly)
    }

    fun onPriorityChange(priority: Int) {
        _uiState.value = _uiState.value.copy(priority = priority)
    }

    fun onStatusChange(status: String) {
        _uiState.value = _uiState.value.copy(status = status)
    }

    fun onImageUriChange(uri: Uri?) {
        _uiState.value = _uiState.value.copy(imageUri = uri, hasImage = uri != null)
    }

    fun onVideoUriChange(uri: Uri?) {
        _uiState.value = _uiState.value.copy(videoUri = uri)
    }

    fun createPost() {
        val currentState = _uiState.value
        val currentUser = firebaseAuth.currentUser

        if (currentUser == null) {
            _uiState.value = currentState.copy(error = "Please log in to create a post")
            return
        }

        // Validation
        when (currentState.postType) {
            PostType.ISSUE -> {
                if (currentState.title.isBlank()) {
                    _uiState.value = currentState.copy(error = "Issue title is required")
                    return
                }
                if (currentState.description.isBlank()) {
                    _uiState.value = currentState.copy(error = "Issue description is required")
                    return
                }
            }
            else -> {
                if (currentState.caption.isBlank()) {
                    _uiState.value = currentState.copy(error = "Caption is required")
                    return
                }
            }
        }

        if (currentState.category.isBlank()) {
            _uiState.value = currentState.copy(error = "Category is required")
            return
        }

        _uiState.value = currentState.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val postId = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()

                // Upload media if present
                val mediaUrls = mutableListOf<String>()
                var imageUrl: String? = null
                var videoUrl: String? = null

                currentState.imageUri?.let { uri ->
                    val uploadedUrl = uploadFile(uri, "images/$postId.jpg")
                    imageUrl = uploadedUrl
                    mediaUrls.add(uploadedUrl)
                }

                currentState.videoUri?.let { uri ->
                    val uploadedUrl = uploadFile(uri, "videos/$postId.mp4")
                    videoUrl = uploadedUrl
                    mediaUrls.add(uploadedUrl)
                }

                val post = Post(
                    postId = postId,
                    userId = currentUser.uid,
                    caption = if (currentState.postType != PostType.ISSUE) currentState.caption else null,
                    description = if (currentState.postType == PostType.ISSUE) currentState.description else null,
                    title = if (currentState.postType == PostType.ISSUE) currentState.title else null,
                    category = currentState.category,
                    status = if (currentState.postType == PostType.ISSUE) currentState.status else null,
                    location = currentState.location.ifBlank { null },
                    hasImage = currentState.hasImage,
                    imageUrl = imageUrl,
                    videoUrl = videoUrl,
                    mediaUrls = mediaUrls,
                    tags = currentState.tags,
                    isLocalOnly = currentState.isLocalOnly,
                    timestamp = timestamp,
                    updatedAt = timestamp,
                    priority = if (currentState.postType == PostType.ISSUE) currentState.priority else null,
                    type = currentState.postType.value
                )

                val result = postRepository.createPost(post)

                if (result.isSuccess) {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                } else {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to create post"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isLoading = false,
                    error = e.message ?: "An error occurred"
                )
            }
        }
    }

    private suspend fun uploadFile(uri: Uri, path: String): String {
        val ref = storage.reference.child(path)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class CreatePostUiState(
    val postType: PostType = PostType.POST,
    val category: String = "",
    val caption: String = "",
    val title: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val location: String = "",
    val isLocalOnly: Boolean = false, // Changed default to false (community posts)
    val priority: Int? = null,
    val status: String? = "Open", // Make this nullable to fix the compilation error
    val imageUri: Uri? = null,
    val videoUri: Uri? = null,
    val hasImage: Boolean = false,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
