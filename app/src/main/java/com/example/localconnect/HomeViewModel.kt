package com.example.localconnect

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.localconnect.data.repository.FirebasePostRepository
import com.example.localconnect.data.model.Post
import com.example.localconnect.util.LocationUtils
import com.example.localconnect.util.UserLocationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val postRepository: FirebasePostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadPosts()
    }

    fun loadPosts(context: Context? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Explore tab should show ALL posts without any location filtering
                val allPosts = postRepository.getAllPosts()

                println("HomeViewModel: Explore tab - loaded ${allPosts.size} total posts")

                _uiState.value = _uiState.value.copy(
                    posts = allPosts,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun loadCommunityPosts(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val userLocation = UserLocationManager.getUserLocation(context)
                println("HomeViewModel: User location for community posts: $userLocation")

                if (userLocation == null) {
                    println("HomeViewModel: No user location available - cannot show community posts")
                    // User MUST have location enabled for community posts
                    _uiState.value = _uiState.value.copy(
                        posts = emptyList(),
                        isLoading = false,
                        error = null,
                        needsLocationForCommunity = true
                    )
                    return@launch
                }

                println("HomeViewModel: Getting posts within 30km of ${userLocation.latitude}, ${userLocation.longitude}")
                // Get posts within 30km radius for community tab
                val posts = postRepository.getCommunityPosts(userLocation.latitude, userLocation.longitude)

                println("HomeViewModel: Community posts loaded: ${posts.size} posts within 30km")
                posts.forEach { post ->
                    println("HomeViewModel: Post ${post.postId}: location=${post.location}, title=${post.title}")
                }

                _uiState.value = _uiState.value.copy(
                    posts = posts,
                    isLoading = false,
                    error = null,
                    needsLocationForCommunity = false
                )
            } catch (e: Exception) {
                println("HomeViewModel: Error loading community posts: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun loadLocalPosts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val posts = postRepository.getLocalPosts()
                _uiState.value = _uiState.value.copy(
                    posts = posts,
                    isLoading = false,
                    error = null,
                    needsLocationForCommunity = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun refreshPosts(context: Context? = null) {
        loadPosts(context)
    }

    fun updateUserLocation(context: Context, latitude: Double, longitude: Double, locationName: String?) {
        UserLocationManager.saveUserLocation(context, latitude, longitude, locationName)
        // Reload posts with new location
        loadPosts(context)
    }
}

data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val needsLocationForCommunity: Boolean = false
)
