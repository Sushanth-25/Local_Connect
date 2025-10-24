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

                // Fetch posts from Firestore where userId matches current user
                val postsSnapshot = firestore.collection("posts")
                    .whereEqualTo("userId", currentUser.uid)
                    .get()
                    .await()

                val posts = postsSnapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Post::class.java)
                    } catch (e: Exception) {
                        println("Error parsing post: ${e.message}")
                        null
                    }
                }.sortedByDescending { it.timestamp ?: 0L }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userPosts = posts
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}

