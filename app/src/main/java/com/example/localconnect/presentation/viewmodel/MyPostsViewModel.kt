package com.example.localconnect.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.localconnect.data.model.Post
import com.example.localconnect.data.repository.FirebasePostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class MyPostsUiState(
    val isLoading: Boolean = false,
    val userPosts: List<Post> = emptyList(),
    val userData: Pair<String, String>? = null, // (name, profilePicUrl)
    val error: String? = null
)

class MyPostsViewModel : ViewModel() {
    private val postRepository = FirebasePostRepository()
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(MyPostsUiState())
    val uiState: StateFlow<MyPostsUiState> = _uiState

    // Pagination state
    private var currentPage = 0
    private val pageSize = 10
    private var hasMorePosts = true
    private var isLoadingMore = false

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val userDoc = firestore.collection("users")
                        .document(currentUser.uid)
                        .get()
                        .await()

                    val name = userDoc.getString("name") ?: "User"
                    val profilePic = userDoc.getString("profileImage") ?: ""

                    _uiState.value = _uiState.value.copy(userData = Pair(name, profilePic))
                }
            } catch (e: Exception) {
                // Use default values if error
                _uiState.value = _uiState.value.copy(userData = Pair("User", ""))
            }
        }
    }

    fun loadUserPosts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Not logged in"
                    )
                    return@launch
                }

                println("MyPostsViewModel: Loading posts for user: ${currentUser.uid}")

                // Try using the repository method that works for other screens
                val allPosts = postRepository.getPostsByUser(currentUser.uid)
                println("MyPostsViewModel: Repository returned ${allPosts.size} posts")

                // If repository method doesn't work, fall back to direct Firestore query
                val posts = if (allPosts.isNotEmpty()) {
                    allPosts.take(pageSize) // Take first page
                } else {
                    println("MyPostsViewModel: Repository returned empty, trying direct query...")

                    // Reset pagination for fresh load
                    currentPage = 0
                    hasMorePosts = true

                    // First, let's check if there are any posts at all for debugging
                    val allPostsSnapshot = firestore.collection("posts")
                        .get()
                        .await()

                    println("MyPostsViewModel: Total posts in database: ${allPostsSnapshot.documents.size}")

                    // Check which posts have this userId
                    val userIdMatches = allPostsSnapshot.documents.filter { doc ->
                        val postUserId = doc.getString("userId")
                        println("MyPostsViewModel: Post ${doc.id} has userId: $postUserId, current user: ${currentUser.uid}")
                        postUserId == currentUser.uid
                    }
                    println("MyPostsViewModel: Found ${userIdMatches.size} posts matching current user")

                    // Try without orderBy first to see if indexing is the issue
                    val postsSnapshot = try {
                        firestore.collection("posts")
                            .whereEqualTo("userId", currentUser.uid)
                            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .limit(pageSize.toLong())
                            .get()
                            .await()
                    } catch (indexError: Exception) {
                        println("MyPostsViewModel: Index error, trying without orderBy: ${indexError.message}")
                        // If ordering fails due to missing index, try without ordering
                        firestore.collection("posts")
                            .whereEqualTo("userId", currentUser.uid)
                            .limit(pageSize.toLong())
                            .get()
                            .await()
                    }

                    println("MyPostsViewModel: Query returned ${postsSnapshot.documents.size} documents")

                    postsSnapshot.documents.mapNotNull { doc ->
                        try {
                            val post = doc.toObject(Post::class.java)?.copy(postId = doc.id)
                            println("MyPostsViewModel: Successfully parsed post: ${post?.postId}")
                            post
                        } catch (e: Exception) {
                            println("MyPostsViewModel: Error parsing post ${doc.id}: ${e.message}")
                            null
                        }
                    }
                }

                hasMorePosts = posts.size == pageSize
                currentPage = 1

                println("MyPostsViewModel: Final posts list size: ${posts.size}")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userPosts = posts
                )
            } catch (e: Exception) {
                println("MyPostsViewModel: Error loading posts: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun loadMorePosts() {
        if (isLoadingMore || !hasMorePosts) return

        viewModelScope.launch {
            isLoadingMore = true
            try {
                val currentUser = auth.currentUser ?: return@launch
                val currentPosts = _uiState.value.userPosts

                if (currentPosts.isEmpty()) return@launch

                val lastPost = currentPosts.last()
                val lastTimestamp = lastPost.timestamp ?: 0L

                val postsSnapshot = firestore.collection("posts")
                    .whereEqualTo("userId", currentUser.uid)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .whereLessThan("timestamp", lastTimestamp)
                    .limit(pageSize.toLong())
                    .get()
                    .await()

                val newPosts = postsSnapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Post::class.java)?.copy(postId = doc.id)
                    } catch (e: Exception) {
                        println("Error parsing post: ${e.message}")
                        null
                    }
                }

                hasMorePosts = newPosts.size == pageSize
                currentPage++

                _uiState.value = _uiState.value.copy(
                    userPosts = currentPosts + newPosts
                )
            } catch (e: Exception) {
                println("Error loading more posts: ${e.message}")
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                val result = postRepository.deletePost(postId)
                if (result.isSuccess) {
                    // Remove the post from the current list
                    val updatedPosts = _uiState.value.userPosts.filter { it.postId != postId }
                    _uiState.value = _uiState.value.copy(userPosts = updatedPosts)
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to delete post: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error deleting post: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun canLoadMore(): Boolean = hasMorePosts && !isLoadingMore
}
