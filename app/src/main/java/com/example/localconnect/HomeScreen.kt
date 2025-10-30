package com.example.localconnect

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localconnect.util.UserLocationManager
import com.example.localconnect.data.model.Post
import kotlinx.coroutines.delay
import com.example.localconnect.presentation.viewmodel.PostDetailViewModel
import android.location.Geocoder
import java.util.Locale

enum class PostType {
    ISSUE, CELEBRATION, GENERAL, LOST_FOUND
}

enum class SortBy {
    RECENT, MOST_VOTED, MOST_VIEWED, PRIORITY
}

data class CommunityPost(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val type: PostType,
    val status: String,
    val upvotes: Int,
    val comments: Int,
    val views: Int,
    val priority: Int = 0, // For issues - higher number = higher priority
    val timeAgo: String,
    val location: String,
    val authorName: String,
    val isLocal: Boolean = true, // Whether it's from local community
    val hasImage: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("unused")
val dummyCommunityPosts = listOf(
    CommunityPost("1", "Major Pothole on Main Street", "Large pothole causing traffic issues near the school zone", "Roads", PostType.ISSUE, "Open", 25, 8, 120, 8, "2 hours ago", "Main Street", "John Doe", true, true),
    CommunityPost("2", "Community Diwali Celebration", "Join us for community Diwali celebration this weekend at the community hall", "Events", PostType.CELEBRATION, "Upcoming", 45, 12, 200, 0, "4 hours ago", "Community Hall", "Priya Sharma", true, true),
    CommunityPost("3", "Overflowing Garbage Bins", "Multiple bins overflowing near central park, creating hygiene issues", "Waste", PostType.ISSUE, "In Progress", 18, 5, 85, 6, "6 hours ago", "Central Park", "Mike Wilson", true),
    CommunityPost("4", "Lost Golden Retriever", "Missing since yesterday evening, answers to 'Buddy'", "Lost & Found", PostType.LOST_FOUND, "Active", 12, 3, 65, 0, "1 day ago", "Elm Street", "Sarah Johnson", true, true),
    CommunityPost("5", "Free Yoga Classes Starting", "Free community yoga classes every morning at 6 AM", "Health", PostType.GENERAL, "Active", 35, 15, 150, 0, "2 days ago", "Community Center", "Dr. Patel", true),
    CommunityPost("6", "Broken Street Light", "Dark area unsafe for evening walks, needs urgent attention", "Infrastructure", PostType.ISSUE, "Reported", 22, 6, 95, 7, "3 days ago", "Oak Avenue", "Lisa Chen", true),
    CommunityPost("7", "Blood Donation Drive", "Urgent need for blood donations at city hospital", "Health", PostType.GENERAL, "Urgent", 50, 20, 300, 9, "5 hours ago", "City Hospital", "Red Cross Society", true), // Non-local
    CommunityPost("8", "Children's Art Exhibition", "Display of local children's artwork this weekend", "Culture", PostType.CELEBRATION, "This Weekend", 28, 9, 110, 0, "1 day ago", "Library Hall", "Art Teacher Mary", true),
    CommunityPost("9", "Water Leakage Issue", "Continuous water leakage on residential street", "Water Supply", PostType.ISSUE, "Open", 15, 4, 75, 5, "8 hours ago", "Residential Area", "Community Member", true),
    CommunityPost("10", "Cricket Tournament", "Annual community cricket tournament registration open", "Sports", PostType.CELEBRATION, "Registration Open", 42, 18, 180, 0, "12 hours ago", "Sports Ground", "Sports Committee", true)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedFilterBar(
    selectedSort: SortBy,
    onSortSelected: (SortBy) -> Unit
) {
    // We removed the category chips (they were above). Keep only the Sort chip (the one below "Recent").
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        var sortMenuExpanded by remember { mutableStateOf(false) }

        FilterChip(
            selected = false,
            onClick = { sortMenuExpanded = true },
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(when(selectedSort) {
                        SortBy.RECENT -> "Recent"
                        SortBy.MOST_VOTED -> "Most Voted"
                        SortBy.MOST_VIEWED -> "Most Viewed"
                        SortBy.PRIORITY -> "Priority"
                    })
                }
            }
        )

        DropdownMenu(
            expanded = sortMenuExpanded,
            onDismissRequest = { sortMenuExpanded = false }
        ) {
            // Use Enum.entries for Kotlin 1.9+
            SortBy.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(when(sort) {
                        SortBy.RECENT -> "Recent"
                        SortBy.MOST_VOTED -> "Most Voted"
                        SortBy.MOST_VIEWED -> "Most Viewed"
                        SortBy.PRIORITY -> "Priority"
                    }) },
                    onClick = {
                        onSortSelected(sort)
                        sortMenuExpanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(navController: NavHostController, postDetailViewModel: PostDetailViewModel) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf("Explore", "Local Community")

    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var selectedSort by rememberSaveable { mutableStateOf(SortBy.RECENT) }

    // Connect to Firebase posts via ViewModel
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory())
    val homeUiState by homeViewModel.uiState.collectAsState()

    // Location permission launcher for community posts
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            // Get current location and update posts
            getCurrentLocationForCommunity(context, homeViewModel)
        } else {
            Toast.makeText(context, "Location permission is required for community posts", Toast.LENGTH_LONG).show()
        }
    }


    // Load posts based on selected tab and handle location permission
    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            0 -> homeViewModel.loadPosts(context) // Explore - all posts with location filtering
            1 -> {
                // Local Community tab - check if user has location first
                val userLocation = UserLocationManager.getUserLocation(context)
                if (userLocation == null) {
                    // No user location saved, request permission
                    val hasLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                            androidx.core.content.ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    if (hasLocationPermission) {
                        // Has permission but no saved location, get current location
                        getCurrentLocationForCommunity(context, homeViewModel)
                    } else {
                        // No permission, request it
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                } else {
                    // User has location, load community posts
                    homeViewModel.loadCommunityPosts(context)
                }
            }
        }
    }

    // Function to refresh posts based on current tab
    val refreshPosts = {
        Toast.makeText(context, "Refreshing posts...", Toast.LENGTH_SHORT).show()
        when (selectedTab) {
            0 -> homeViewModel.loadPosts(context)
            1 -> homeViewModel.loadCommunityPosts(context)
        }
    }

    // Dialog state
    var showPhotoDialog by remember { mutableStateOf(false) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // Handle captured photo bitmap
        Toast.makeText(context, "Photo captured!", Toast.LENGTH_SHORT).show()
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        // Handle selected photo URI
        Toast.makeText(context, "Photo selected from gallery!", Toast.LENGTH_SHORT).show()
    }

    // Pull-to-refresh state
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            refreshPosts()
            delay(1000) // Simulate network delay
            isRefreshing = false
        }
    }

    LaunchedEffect(homeUiState.isLoading) {
        if (!homeUiState.isLoading) {
            isRefreshing = false
        }
    }

    // Show error message if any
    LaunchedEffect(homeUiState.error) {
        homeUiState.error?.let {
            Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LocalConnect", fontWeight = FontWeight.Bold)
                        Text("Building Better Communities", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Badge(containerColor = Color.Red) {
                            Text("3", color = Color.White, fontSize = 10.sp)
                        }
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        navController.navigate("map") // Navigate to map screen
                    },
                    icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                    label = { Text("Map") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        navController.navigate("my_posts")
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "My Posts") },
                    label = { Text("My Posts") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        navController.navigate("profile") // Navigate to profile screen
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") }
                )
            }
        },
        floatingActionButton = {
            // keep the FAB positioned bottom-end; avoid fillMaxSize so it can't affect layout
            Box(
                modifier = Modifier
                    .padding(end = 20.dp, bottom = 20.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                FloatingActionButton(
                    onClick = { navController.navigate("create_post") },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant, // soft background tone
                    elevation = FloatingActionButtonDefaults.elevation(3.dp),
                    shape = RoundedCornerShape(12.dp) // smooth corners to match UI
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Post",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Create Post",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }



    ) { paddingValues ->
        // Replace nested static columns with a single LazyColumn so the whole screen (header, filters, posts, bottom nav)
        // scrolls together. This preserves the previous behavior but makes the UI elements move up when the user scrolls.
        val listState = rememberLazyListState()
        val realPosts = homeUiState.posts
        val filteredRealPosts: List<Post> = getFilteredRealPosts(
            posts = realPosts,
            category = selectedCategory,
            localOnly = selectedTab == 1
        )

        // We'll compute a simple fade using the first visible item index and scroll offset
        // Avoid reading frequently-changing scroll state directly in the composition body.
        // Use derivedStateOf so recompositions happen only when these values change.
        val scrollInfo by remember {
            derivedStateOf { Pair(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) }
        }

        // Wrap posts area in a Box with pullRefresh so users can pull down to refresh the list.
        val pullRefreshState = rememberPullRefreshState(isRefreshing, { isRefreshing = true })

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                // add extra bottom padding so the last item isn't obscured by the bottom bar / FAB
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp)
            ) {
                // Tab row (will scroll away)
                item {
                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title) },
                                icon = {
                                    when (index) {
                                        0 -> Icon(Icons.Default.Explore, contentDescription = null)
                                        1 -> Icon(Icons.Default.LocationCity, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                }

                // Header card + filters + quick actions
                item {
                   // Fade header smoothly as user scrolls; keep a faint minimum alpha so it doesn't fully disappear.
                   val (firstIndex, firstOffset) = scrollInfo
                   val fadeDistanceHeader = 300f
                   val minAlpha = 0.12f
                   val headerIndex = 1 // header block is the second item (index 1) in the LazyColumn
                   val headerAlpha = when {
                       firstIndex > headerIndex -> minAlpha
                       firstIndex == headerIndex -> ((1f - (firstOffset / fadeDistanceHeader)).coerceIn(minAlpha, 1f))
                       else -> 1f
                   }

                   Column(modifier = Modifier.fillMaxWidth().graphicsLayer(alpha = headerAlpha)) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.EmojiPeople, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (selectedTab == 0) "Explore Communities" else "Your Local Community",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = if (selectedTab == 0) "Discover what's happening around you" else "Stay connected with your neighborhood",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        EnhancedFilterBar(
                            selectedSort = selectedSort,
                            onSortSelected = { selectedSort = it }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Replace boxed chips with a horizontal scrollable row of icon buttons for each category
                        val quickCategories = listOf(
                            Triple("All", Icons.AutoMirrored.Filled.List, Color(0xFF9E9E9E)),
                            Triple("Health", Icons.Default.LocalHospital, Color(0xFF4CAF50)),
                            Triple("Roads", Icons.Default.Route, Color(0xFF607D8B)),
                            Triple("Infrastructure", Icons.Default.Construction, Color(0xFF795548)),
                            Triple("Lost & Found", Icons.Default.Pets, Color(0xFF81C784)),
                            Triple("Events", Icons.Default.Event, Color(0xFF64B5F6)),
                            Triple("Emergency", Icons.Default.Warning, Color(0xFFFF8A65)),
                            Triple("General", Icons.Default.Info, Color(0xFF90A4AE))
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            items(quickCategories) { (label, icon, color) ->
                                CompactQuickActionButton(
                                    label = label,
                                    icon = icon,
                                    color = color,
                                    isSelected = selectedCategory.trim().equals(label, ignoreCase = true)
                                ) {
                                    // toggle selection
                                    if (selectedCategory.trim().equals(label, ignoreCase = true)) {
                                        selectedCategory = "All"
                                    } else {
                                        selectedCategory = label
                                    }
                                }
                            }
                        }
                    }
                }

               // Posts header
               item {
                   Row(
                       modifier = Modifier.fillMaxWidth(),
                       horizontalArrangement = Arrangement.SpaceBetween,
                       verticalAlignment = Alignment.CenterVertically
                   ) {
                       Text(
                           text = "📍 Community Posts (${filteredRealPosts.size})",
                           fontSize = 18.sp,
                           fontWeight = FontWeight.SemiBold
                       )

                       Text(
                           text = if (selectedTab == 0) "All communities" else "Local only",
                           fontSize = 12.sp,
                           color = MaterialTheme.colorScheme.primary
                       )
                   }
               }

               // Posts list (each post is an item)
               if (homeUiState.isLoading && realPosts.isEmpty()) {
                   item {
                       Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                           CircularProgressIndicator()
                       }
                   }
               } else if (filteredRealPosts.isEmpty()) {
                   item {
                       Card(
                           modifier = Modifier.fillMaxWidth(),
                           colors = CardDefaults.cardColors(
                               containerColor = if (selectedTab == 1 && homeUiState.needsLocationForCommunity)
                                   MaterialTheme.colorScheme.errorContainer
                               else
                                   MaterialTheme.colorScheme.surfaceVariant
                           )
                       ) {
                           Column(
                               modifier = Modifier.padding(24.dp),
                               horizontalAlignment = Alignment.CenterHorizontally
                           ) {
                               Icon(
                                   imageVector = if (selectedTab == 1 && homeUiState.needsLocationForCommunity)
                                       Icons.Default.LocationOff
                                   else
                                       Icons.Default.PostAdd,
                                   contentDescription = null,
                                   modifier = Modifier.size(48.dp),
                                   tint = if (selectedTab == 1 && homeUiState.needsLocationForCommunity)
                                       MaterialTheme.colorScheme.onErrorContainer
                                   else
                                       MaterialTheme.colorScheme.onSurfaceVariant
                               )
                               Spacer(modifier = Modifier.height(8.dp))
                               Text(
                                   text = if (selectedTab == 1 && homeUiState.needsLocationForCommunity)
                                       "Location Required"
                                   else
                                       "No posts yet",
                                   style = MaterialTheme.typography.titleMedium,
                                   color = if (selectedTab == 1 && homeUiState.needsLocationForCommunity)
                                       MaterialTheme.colorScheme.onErrorContainer
                                   else
                                       MaterialTheme.colorScheme.onSurfaceVariant
                               )
                               Text(
                                   text = if (selectedTab == 1 && homeUiState.needsLocationForCommunity)
                                       "Please enable location permission to see posts within 30km of your location"
                                   else if (selectedTab == 1)
                                       "No posts found within 30km of your location. Try the Explore tab!"
                                   else
                                       "Be the first to share something with your community!",
                                   style = MaterialTheme.typography.bodySmall,
                                   color = if (selectedTab == 1 && homeUiState.needsLocationForCommunity)
                                       MaterialTheme.colorScheme.onErrorContainer
                                   else
                                       MaterialTheme.colorScheme.onSurfaceVariant,
                                   textAlign = androidx.compose.ui.text.style.TextAlign.Center
                               )

                               if (selectedTab == 1 && homeUiState.needsLocationForCommunity) {
                                   Spacer(modifier = Modifier.height(16.dp))
                                   Button(
                                       onClick = {
                                           locationPermissionLauncher.launch(
                                               arrayOf(
                                                   Manifest.permission.ACCESS_FINE_LOCATION,
                                                   Manifest.permission.ACCESS_COARSE_LOCATION
                                               )
                                           )
                                       }
                                   ) {
                                       Icon(Icons.Default.LocationOn, contentDescription = null)
                                       Spacer(modifier = Modifier.width(8.dp))
                                       Text("Enable Location")
                                   }
                               }
                           }
                       }
                   }
               } else {
                   itemsIndexed(filteredRealPosts) { _, post: Post ->
                       // Keep posts fully opaque while scrolling so they don't visually vanish (Instagram-like behavior).
                       // Header still fades above; posts should scroll normally without alpha changes.
                       val alpha = 1f

                        RealPostCard(
                            post = post,
                            modifier = Modifier.graphicsLayer(alpha = alpha.coerceIn(0f, 1f)),
                            onClick = {
                                postDetailViewModel.setSelectedPost(post)
                                navController.navigate("post_detail/${post.postId}")
                            }
                        )
                    }
                }

               // Removed duplicate bottom navigation and duplicate Create Post FAB here.
               // The Scaffold's `bottomBar` and `floatingActionButton` are used instead to avoid overlapping UI.
               item {
                   Spacer(modifier = Modifier.height(8.dp))
               }
            }

            // Pull-to-refresh indicator sits above the content
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter).zIndex(1f)
            )

        }

        // Photo selection dialog remains unchanged
        if (showPhotoDialog) {
            AlertDialog(
                onDismissRequest = { showPhotoDialog = false },
                title = { Text("Select Photo") },
                text = { Text("Choose an option to add a photo") },
                confirmButton = {
                    TextButton(onClick = {
                        showPhotoDialog = false
                        cameraLauncher.launch(null)
                    }) { Text("Camera") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showPhotoDialog = false
                        galleryLauncher.launch("image/*")
                    }) { Text("Gallery") }
                }
            )
        }
    }
}


// Helper function to get current location for community posts
@Suppress("DEPRECATION")
private fun getCurrentLocationForCommunity(
    context: android.content.Context,
    homeViewModel: HomeViewModel
) {
    try {
        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager

        // Check if location services are enabled
        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) &&
            !locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
            Toast.makeText(context, "Please enable location services", Toast.LENGTH_LONG).show()
            return
        }

        // Check permissions
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Location permission required", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(context, "Getting your location for community posts...", Toast.LENGTH_SHORT).show()

        // Get last known location first (faster)
        val lastKnownLocation = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)

        if (lastKnownLocation != null) {
            var locationName = "Current Location"

            try {
                // Use Android Geocoder only, prefer POI + locality if available
                resolveReadablePlaceName(context, lastKnownLocation.latitude, lastKnownLocation.longitude)?.let { best ->
                    locationName = best
                }
            } catch (_: Exception) { /* keep fallback */ }

            // Save user location and update posts
            homeViewModel.updateUserLocation(
                context,
                lastKnownLocation.latitude,
                lastKnownLocation.longitude,
                locationName
            )

            Toast.makeText(context, "Location updated! Community posts filtered by 30km radius", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Unable to get current location. Please try again.", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error getting location: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// Helper to build a concise place name from Android Geocoder only
private fun resolveReadablePlaceName(context: android.content.Context, lat: Double, lon: Double): String? {
    return try {
        @Suppress("DEPRECATION")
        val list = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1)
        list?.firstOrNull()?.getAddressLine(0)
    } catch (_: Exception) { null }
}

// Filter and sort helper function
fun getFilteredRealPosts(
    posts: List<Post>,
    category: String,
    localOnly: Boolean
): List<Post> {
    var filtered = posts

    // DON'T filter by isLocalOnly here - the ViewModel already handles location-based filtering
    // for the Local Community tab using actual distance calculation (30km radius)
    // This was the bug: we were showing ALL posts with isLocalOnly=true regardless of distance

    val requestedCategory = category.trim()
    if (!requestedCategory.equals("All", ignoreCase = true)) {
        filtered = filtered.filter { post ->
            post.category?.trim()?.equals(requestedCategory, ignoreCase = true) == true
        }
    }

    // show newest first
    return filtered.sortedByDescending { it.timestamp ?: 0L }
}

@Composable
fun CompactQuickActionButton(label: String, icon: ImageVector, color: Color, isSelected: Boolean = false, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(45.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (isSelected) Modifier.border(width = 2.dp, color = MaterialTheme.colorScheme.primary, shape = CircleShape) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}
