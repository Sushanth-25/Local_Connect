package com.example.localconnect

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localconnect.data.repository.FirebasePostRepository
import com.localconnect.data.model.Post
import com.localconnect.util.LocationUtils
import com.localconnect.util.UserLocationManager
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
                val allPosts = postRepository.getAllPosts()

                // Apply location-based filtering if user location is available
                val filteredPosts = if (context != null) {
                    val userLocation = UserLocationManager.getUserLocation(context)
                    if (userLocation != null) {
                        LocationUtils.filterPostsByLocation(
                            allPosts,
                            userLocation.latitude,
                            userLocation.longitude
                        )
                    } else {
                        // If no user location, show all local posts and posts without location coordinates
                        allPosts.filter { post ->
                            post.isLocalOnly || post.location.isNullOrBlank()
                        }
                    }
                } else {
                    allPosts
                }

                _uiState.value = _uiState.value.copy(
                    posts = filteredPosts,
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

                val posts = if (userLocation != null) {
                    println("HomeViewModel: Getting posts within 30km of ${userLocation.latitude}, ${userLocation.longitude}")
                    // Get posts within 30km radius for community tab
                    postRepository.getCommunityPosts(userLocation.latitude, userLocation.longitude)
                } else {
                    println("HomeViewModel: No user location available, requesting location permission")
                    // If no user location, show only posts without location coordinates or local posts
                    val allPosts = postRepository.getAllPosts()
                    allPosts.filter { post ->
                        post.isLocalOnly || post.location.isNullOrBlank()
                    }
                }

                println("HomeViewModel: Community posts loaded: ${posts.size} posts")
                posts.forEach { post ->
                    println("HomeViewModel: Post ${post.postId}: location=${post.location}, isLocalOnly=${post.isLocalOnly}, type=${post.type}")
                }

                _uiState.value = _uiState.value.copy(
                    posts = posts,
                    isLoading = false,
                    error = null,
                    needsLocationForCommunity = userLocation == null
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
