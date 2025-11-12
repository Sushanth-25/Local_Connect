package com.example.localconnect.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.localconnect.data.model.Post
import com.example.localconnect.data.model.PostType
import com.example.localconnect.repository.PostRepository
import com.example.localconnect.util.CloudinaryManager
import com.example.localconnect.util.UriUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.*

class CreatePostViewModel(
    private val postRepository: PostRepository,
    private val firebaseAuth: FirebaseAuth,
    private val storage: FirebaseStorage
) : ViewModel() {

    companion object {
        private const val TAG = "CreatePostViewModel"
    }

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

    fun onLocationChangeWithCoords(locationName: String, latitude: Double?, longitude: Double?) {
        _uiState.value = _uiState.value.copy(
            location = locationName,
            latitude = latitude,
            longitude = longitude
        )
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

    // Support multiple images
    fun onImageUrisChange(uris: List<Uri>) {
        _uiState.value = _uiState.value.copy(imageUris = uris, hasImage = uris.isNotEmpty())
    }

    fun removeImageAt(index: Int) {
        val current = _uiState.value
        if (index < 0 || index >= current.imageUris.size) return
        val mutable = current.imageUris.toMutableList()
        mutable.removeAt(index)
        _uiState.value = current.copy(imageUris = mutable, hasImage = mutable.isNotEmpty())
    }

    // Keep existing single image method for backward compatibility
    fun onImageUriChange(uri: Uri?) {
        val uris = if (uri != null) listOf(uri) else emptyList()
        onImageUrisChange(uris)
    }

    fun onVideoUriChange(uri: Uri?) {
        _uiState.value = _uiState.value.copy(videoUri = uri)
    }

    fun createPost(context: Context) {
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

        if (currentState.location.isBlank() || currentState.latitude == null || currentState.longitude == null) {
            _uiState.value = currentState.copy(error = "Location is required for all posts")
            return
        }

        _uiState.value = currentState.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting post creation process")
                val currentUser = firebaseAuth.currentUser
                    ?: throw IllegalStateException("User not authenticated")

                val postId = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()

                // Collect all media files (images and videos)
                val allMediaFiles = mutableListOf<File>()
                val mediaUrls = mutableListOf<String>()
                val thumbnailUrls = mutableListOf<String>()

                try {
                    // Convert URIs to temp files
                    withContext(Dispatchers.IO) {
                        Log.d(TAG, "Converting ${currentState.imageUris.size} image URIs to files")
                        currentState.imageUris.forEach { uri ->
                            try {
                                val file = UriUtils.uriToFile(context, uri)
                                allMediaFiles.add(file)
                                Log.d(TAG, "Image file created: ${file.absolutePath}")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error converting image URI to file: ${e.message}", e)
                            }
                        }

                        // Convert video URI to temp file if present
                        currentState.videoUri?.let { uri ->
                            try {
                                val file = UriUtils.uriToFile(context, uri, "video_")
                                allMediaFiles.add(file)
                                Log.d(TAG, "Video file created: ${file.absolutePath}")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error converting video URI to file: ${e.message}", e)
                            }
                        }
                    }

                    // Upload all media files to Cloudinary if any exist
                    if (allMediaFiles.isNotEmpty()) {
                        Log.d(TAG, "Uploading ${allMediaFiles.size} files to Cloudinary")
                        val paths = allMediaFiles.map { it.absolutePath }

                        val uploadResults = withContext(Dispatchers.IO) {
                            CloudinaryManager.uploadMediaWithThumbnails(paths)
                        }

                        Log.d(TAG, "Upload completed. Processing ${uploadResults.size} results")

                        // Collect URLs from upload results
                        uploadResults.forEachIndexed { index, (originalUrl, thumbnailUrl) ->
                            if (originalUrl != null) {
                                mediaUrls.add(originalUrl)
                                Log.d(TAG, "Added media URL $index: $originalUrl")
                            } else {
                                Log.w(TAG, "Media URL $index is null")
                            }

                            if (thumbnailUrl != null) {
                                thumbnailUrls.add(thumbnailUrl)
                                Log.d(TAG, "Added thumbnail URL $index: $thumbnailUrl")
                            }
                        }

                        Log.d(TAG, "Total media URLs collected: ${mediaUrls.size}")
                        Log.d(TAG, "Total thumbnail URLs collected: ${thumbnailUrls.size}")
                    } else {
                        Log.d(TAG, "No media files to upload")
                    }

                } finally {
                    // Clean up temp files
                    Log.d(TAG, "Cleaning up ${allMediaFiles.size} temp files")
                    allMediaFiles.forEach { file ->
                        try {
                            if (file.delete()) {
                                Log.d(TAG, "Deleted temp file: ${file.absolutePath}")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to delete temp file: ${file.absolutePath}", e)
                        }
                    }
                }

                // Create the post object
                val post = Post(
                    postId = postId,
                    userId = currentUser.uid,
                    caption = if (currentState.postType != PostType.ISSUE) currentState.caption else null,
                    description = if (currentState.postType == PostType.ISSUE) currentState.description else null,
                    title = if (currentState.postType == PostType.ISSUE) currentState.title else null,
                    category = currentState.category,
                    status = if (currentState.postType == PostType.ISSUE) currentState.status else null,
                    hasImage = mediaUrls.isNotEmpty(),
                    mediaUrls = mediaUrls,
                    thumbnailUrls = thumbnailUrls,
                    tags = currentState.tags,
                    isLocalOnly = currentState.isLocalOnly,
                    timestamp = timestamp,
                    updatedAt = timestamp,
                    priority = if (currentState.postType == PostType.ISSUE) currentState.priority else null,
                    type = currentState.postType.value,
                    latitude = currentState.latitude ?: 0.0,
                    longitude = currentState.longitude ?: 0.0,
                    locationName = currentState.location.ifBlank { "Unknown Location" }
                )

                Log.d(TAG, "Created post object with ${post.mediaUrls.size} media URLs and ${post.thumbnailUrls.size} thumbnail URLs")
                Log.d(TAG, "Media URLs: ${post.mediaUrls}")
                Log.d(TAG, "Thumbnail URLs: ${post.thumbnailUrls}")

                // Save to Firestore
                Log.d(TAG, "Saving post to Firestore with ID: $postId")
                val result = postRepository.createPost(post)

                if (result.isSuccess) {
                    Log.d(TAG, "Post created successfully")
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Failed to create post"
                    Log.e(TAG, "Failed to create post: $errorMessage")
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating post: ${e.message}", e)
                _uiState.value = currentState.copy(
                    isLoading = false,
                    error = e.message ?: "An error occurred"
                )
            }
        }
    }

    // Keep existing createPost() for backward compatibility
    fun createPost() {
        // This will fail without context, but we'll keep it to avoid breaking existing calls
        _uiState.value = _uiState.value.copy(error = "Context required for image upload")
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
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isLocalOnly: Boolean = true,
    val priority: Int? = null,
    val status: String? = "Open",
    // Support multiple images
    val imageUris: List<Uri> = emptyList(),
    val videoUri: Uri? = null,
    val hasImage: Boolean = false,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
) {
    // Backward compatibility property
    val imageUri: Uri? get() = imageUris.firstOrNull()
}
