package com.example.localconnect

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.localconnect.data.repository.FirebasePostRepository
import com.example.localconnect.data.model.Post
import com.example.localconnect.util.LocationUtils
import com.example.localconnect.util.UserLocationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val postRepository: FirebasePostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _paginatedPosts = MutableStateFlow<Flow<PagingData<Post>>?>(null)
    val paginatedPosts: StateFlow<Flow<PagingData<Post>>?> = _paginatedPosts.asStateFlow()

    init {
        // Don't load posts in init - let the screen trigger it based on tab
    }

    /**
     * Load posts with pagination for Explore tab
     * This is the optimized version that drastically reduces Firestore reads
     */
    fun loadPostsPaginated(category: String? = null, sortBy: String = "timestamp") {
        viewModelScope.launch {
            println("HomeViewModel: Loading paginated posts - category=$category, sortBy=$sortBy")
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val flow = postRepository.getPostsPaginated(
                    pageSize = 20,
                    category = if (category == "All") null else category,
                    sortBy = sortBy
                ).cachedIn(viewModelScope) // Cache the paging data

                _paginatedPosts.value = flow
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    usePagination = true
                )
                println("HomeViewModel: Paginated posts flow created successfully")
            } catch (e: Exception) {
                println("HomeViewModel: Error creating paginated flow: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * Legacy method - kept for backward compatibility
     * Consider using loadPostsPaginated() for better performance
     */
    @Deprecated("Use loadPostsPaginated() for better performance")
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
                    error = null,
                    usePagination = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * Load community posts with pagination and location filtering
     * This is the optimized version that reduces Firestore reads
     */
    fun loadCommunityPostsPaginated(
        context: Context,
        category: String? = null,
        sortBy: String = "timestamp"
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val userLocation = UserLocationManager.getUserLocation(context)
                println("HomeViewModel: User location for paginated community posts: $userLocation")

                if (userLocation == null) {
                    println("HomeViewModel: No user location available - cannot show community posts")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null,
                        needsLocationForCommunity = true,
                        usePagination = false
                    )
                    _paginatedPosts.value = null
                    return@launch
                }

                println("HomeViewModel: Creating paginated flow for posts within 30km")
                val flow = postRepository.getCommunityPostsPaginated(
                    userLat = userLocation.latitude,
                    userLon = userLocation.longitude,
                    radiusKm = 30.0,
                    pageSize = 20,
                    category = if (category == "All") null else category,
                    sortBy = sortBy
                ).cachedIn(viewModelScope)

                _paginatedPosts.value = flow
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null,
                    needsLocationForCommunity = false,
                    usePagination = true
                )
                println("HomeViewModel: Paginated community posts flow created successfully")
            } catch (e: Exception) {
                println("HomeViewModel: Error creating paginated community flow: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * Legacy method - kept for backward compatibility
     * Consider using loadCommunityPostsPaginated() for better performance
     */
    @Deprecated("Use loadCommunityPostsPaginated() for better performance")
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
                        needsLocationForCommunity = true,
                        usePagination = false
                    )
                    return@launch
                }

                println("HomeViewModel: Getting posts within 30km of ${userLocation.latitude}, ${userLocation.longitude}")
                // Get posts within 30km radius for community tab
                val posts = postRepository.getCommunityPosts(userLocation.latitude, userLocation.longitude)

                println("HomeViewModel: Community posts loaded: ${posts.size} posts within 30km")
                posts.forEach { post ->
                    println("HomeViewModel: Post ${post.postId}: location=${post.locationName}, title=${post.title}")
                }

                _uiState.value = _uiState.value.copy(
                    posts = posts,
                    isLoading = false,
                    error = null,
                    needsLocationForCommunity = false,
                    usePagination = false
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
    val posts: List<Post> = emptyList(), // Legacy - used when not paginating
    val isLoading: Boolean = false,
    val error: String? = null,
    val needsLocationForCommunity: Boolean = false,
    val usePagination: Boolean = true // Flag to indicate if using pagination
)
