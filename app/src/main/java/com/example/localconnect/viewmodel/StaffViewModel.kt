package com.example.localconnect.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.localconnect.AuthResult
import com.example.localconnect.data.model.Post
import com.example.localconnect.data.repository.StaffRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class StaffAuthState {
    object Idle : StaffAuthState()
    object Loading : StaffAuthState()
    object Success : StaffAuthState()
    data class Error(val message: String) : StaffAuthState()
    object NotStaff : StaffAuthState()
}

sealed class PostUpdateState {
    object Idle : PostUpdateState()
    object Loading : PostUpdateState()
    object Success : PostUpdateState()
    data class Error(val message: String) : PostUpdateState()
}

class StaffViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val staffRepository = StaffRepository(application.applicationContext)

    private val _authState = MutableStateFlow<StaffAuthState>(StaffAuthState.Idle)
    val authState: StateFlow<StaffAuthState> = _authState

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    private val _updateState = MutableStateFlow<PostUpdateState>(PostUpdateState.Idle)
    val updateState: StateFlow<PostUpdateState> = _updateState

    private val _isStaff = MutableStateFlow(false)
    val isStaff: StateFlow<Boolean> = _isStaff

    // Staff login with custom claim verification
    fun staffLogin(email: String, password: String) {
        _authState.value = StaffAuthState.Loading

        viewModelScope.launch {
            try {
                // Sign in with Firebase Auth
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        // Force refresh token to get latest custom claims
                        val user = auth.currentUser
                        user?.getIdToken(true)
                            ?.addOnSuccessListener { tokenResult ->
                                val isStaff = tokenResult.claims["staff"] as? Boolean ?: false
                                if (isStaff) {
                                    _isStaff.value = true
                                    _authState.value = StaffAuthState.Success
                                    // Load posts after successful staff login
                                    loadAllPosts()
                                } else {
                                    // Not a staff member, sign out
                                    auth.signOut()
                                    _authState.value = StaffAuthState.NotStaff
                                }
                            }
                            ?.addOnFailureListener { e ->
                                auth.signOut()
                                _authState.value = StaffAuthState.Error("Failed to verify credentials: ${e.message}")
                            }
                    }
                    .addOnFailureListener { e ->
                        _authState.value = StaffAuthState.Error(e.message ?: "Login failed")
                    }
            } catch (e: Exception) {
                _authState.value = StaffAuthState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Load all posts (staff only)
    fun loadAllPosts() {
        viewModelScope.launch {
            try {
                val result = staffRepository.getAllPosts()
                result.onSuccess { postList ->
                    _posts.value = postList
                }.onFailure { e ->
                    _authState.value = StaffAuthState.Error(e.message ?: "Failed to load posts")
                }
            } catch (e: Exception) {
                _authState.value = StaffAuthState.Error(e.message ?: "Failed to load posts")
            }
        }
    }

    // Load posts by status
    fun loadPostsByStatus(status: String) {
        viewModelScope.launch {
            try {
                val result = staffRepository.getPostsByStatus(status)
                result.onSuccess { postList ->
                    _posts.value = postList
                }.onFailure { e ->
                    _authState.value = StaffAuthState.Error(e.message ?: "Failed to load posts")
                }
            } catch (e: Exception) {
                _authState.value = StaffAuthState.Error(e.message ?: "Failed to load posts")
            }
        }
    }

    // Load posts by type
    fun loadPostsByType(type: String) {
        viewModelScope.launch {
            try {
                val result = staffRepository.getPostsByType(type)
                result.onSuccess { postList ->
                    _posts.value = postList
                }.onFailure { e ->
                    _authState.value = StaffAuthState.Error(e.message ?: "Failed to load posts")
                }
            } catch (e: Exception) {
                _authState.value = StaffAuthState.Error(e.message ?: "Failed to load posts")
            }
        }
    }

    // Update post status
    fun updatePostStatus(postId: String, newStatus: String) {
        _updateState.value = PostUpdateState.Loading

        viewModelScope.launch {
            try {
                val result = staffRepository.updatePostStatus(postId, newStatus)
                result.onSuccess {
                    _updateState.value = PostUpdateState.Success
                    // Reload posts to reflect changes
                    loadAllPosts()
                }.onFailure { e ->
                    _updateState.value = PostUpdateState.Error(e.message ?: "Failed to update status")
                }
            } catch (e: Exception) {
                _updateState.value = PostUpdateState.Error(e.message ?: "Failed to update status")
            }
        }
    }

    // Reset states
    fun resetAuthState() {
        _authState.value = StaffAuthState.Idle
    }

    fun resetUpdateState() {
        _updateState.value = PostUpdateState.Idle
    }

    // Logout
    fun logout() {
        auth.signOut()
        _isStaff.value = false
        _posts.value = emptyList()
        _authState.value = StaffAuthState.Idle
    }
}
