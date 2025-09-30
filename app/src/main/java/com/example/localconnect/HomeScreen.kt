package com.example.localconnect

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

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

val dummyCommunityPosts = listOf(
    CommunityPost("1", "Major Pothole on Main Street", "Large pothole causing traffic issues near the school zone", "Roads", PostType.ISSUE, "Open", 25, 8, 120, 8, "2 hours ago", "Main Street", "John Doe", true, true),
    CommunityPost("2", "Community Diwali Celebration", "Join us for community Diwali celebration this weekend at the community hall", "Events", PostType.CELEBRATION, "Upcoming", 45, 12, 200, 0, "4 hours ago", "Community Hall", "Priya Sharma", true, true),
    CommunityPost("3", "Overflowing Garbage Bins", "Multiple bins overflowing near central park, creating hygiene issues", "Waste", PostType.ISSUE, "In Progress", 18, 5, 85, 6, "6 hours ago", "Central Park", "Mike Wilson", true),
    CommunityPost("4", "Lost Golden Retriever", "Missing since yesterday evening, answers to 'Buddy'", "Lost & Found", PostType.LOST_FOUND, "Active", 12, 3, 65, 0, "1 day ago", "Elm Street", "Sarah Johnson", true, true),
    CommunityPost("5", "Free Yoga Classes Starting", "Free community yoga classes every morning at 6 AM", "Health", PostType.GENERAL, "Active", 35, 15, 150, 0, "2 days ago", "Community Center", "Dr. Patel", true),
    CommunityPost("6", "Broken Street Light", "Dark area unsafe for evening walks, needs urgent attention", "Infrastructure", PostType.ISSUE, "Reported", 22, 6, 95, 7, "3 days ago", "Oak Avenue", "Lisa Chen", true),
    CommunityPost("7", "Blood Donation Drive", "Urgent need for blood donations at city hospital", "Health", PostType.GENERAL, "Urgent", 50, 20, 300, 9, "5 hours ago", "City Hospital", "Red Cross Society", false), // Non-local
    CommunityPost("8", "Children's Art Exhibition", "Display of local children's artwork this weekend", "Culture", PostType.CELEBRATION, "This Weekend", 28, 9, 110, 0, "1 day ago", "Library Hall", "Art Teacher Mary", true),
    CommunityPost("9", "Water Leakage Issue", "Continuous water leakage on residential street", "Water Supply", PostType.ISSUE, "Open", 15, 4, 75, 5, "8 hours ago", "Residential Area", "Community Member", true),
    CommunityPost("10", "Cricket Tournament", "Annual community cricket tournament registration open", "Sports", PostType.CELEBRATION, "Registration Open", 42, 18, 180, 0, "12 hours ago", "Sports Ground", "Sports Committee", true)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedFilterBar(
    selectedCategory: String,
    selectedSort: SortBy,
    onCategorySelected: (String) -> Unit,
    onSortSelected: (SortBy) -> Unit
) {
    val categories = listOf("All", "Issues", "Celebrations", "General", "Lost & Found")

    Column {
        // Main category filters
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = { Text(category) },
                    leadingIcon = {
                        when (category) {
                            "Issues" -> Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            "Celebrations" -> Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.size(16.dp))
                            "General" -> Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            "Lost & Found" -> Icon(Icons.Default.Pets, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                )
            }
        }

        // Sort options only (removed local toggle)
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
                SortBy.values().forEach { sort ->
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Explore", "Local Community")

    var selectedCategory by remember { mutableStateOf("All") }
    var selectedSort by remember { mutableStateOf(SortBy.RECENT) }

    LaunchedEffect(Unit) {
        Toast.makeText(context, "Welcome to LocalConnect!", Toast.LENGTH_SHORT).show()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (cameraGranted && locationGranted) {
            Toast.makeText(context, "Permissions granted! You can now report issues.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permissions needed to report issues", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LocalConnect", fontWeight = FontWeight.Bold)
                        Text("Building Better Communities", fontSize = 12.sp, color = Color.Gray)
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
                    onClick = { },
                    icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                    label = { Text("Map") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { },
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
            FloatingActionButton(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Post")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row for Explore vs Local
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Compact header section (25% of space)
                Column(
                    modifier = Modifier.weight(0.25f)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Compact welcome message
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
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Enhanced Filter Bar
                    EnhancedFilterBar(
                        selectedCategory = selectedCategory,
                        selectedSort = selectedSort,
                        onCategorySelected = { selectedCategory = it },
                        onSortSelected = { selectedSort = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Compact Quick Actions
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CompactQuickActionButton("Report", Icons.Default.Report, Color(0xFFE57373))
                        CompactQuickActionButton("Lost & Found", Icons.Default.Pets, Color(0xFF81C784))
                        CompactQuickActionButton("Events", Icons.Default.Event, Color(0xFF64B5F6))
                        CompactQuickActionButton("Emergency", Icons.Default.Emergency, Color(0xFFFF8A65))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Posts section (75% of space)
                Column(
                    modifier = Modifier.weight(0.75f)
                ) {
                    val filteredPosts = getFilteredPosts(
                        posts = dummyCommunityPosts,
                        category = selectedCategory,
                        sortBy = selectedSort,
                        localOnly = selectedTab == 1
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📍 Community Posts (${filteredPosts.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = if (selectedTab == 0) "All communities" else "Local only",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredPosts) { post ->
                            EnhancedPostCard(post)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionsSection() {
    Text(
        text = "Quick Actions",
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        QuickActionButton("Report Issue", Icons.Default.Report, Color(0xFFE57373))
        QuickActionButton("Lost & Found", Icons.Default.Pets, Color(0xFF81C784))
        QuickActionButton("Events", Icons.Default.Event, Color(0xFF64B5F6))
        QuickActionButton("Emergency", Icons.Default.Emergency, Color(0xFFFF8A65))
    }
}

@Composable
fun QuickActionButton(label: String, icon: ImageVector, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CompactQuickActionButton(label: String, icon: ImageVector, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { }
    ) {
        Box(
            modifier = Modifier
                .size(45.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun EnhancedPostCard(post: CommunityPost) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (post.type) {
                PostType.ISSUE -> if (post.priority > 7) Color(0xFFFFEBEE) else Color.White
                PostType.CELEBRATION -> Color(0xFFF3E5F5)
                else -> Color.White
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Type indicator
                        when (post.type) {
                            PostType.ISSUE -> Icon(Icons.Default.Warning, contentDescription = null, tint = if (post.priority > 7) Color.Red else Color(0xFFFF9800), modifier = Modifier.size(16.dp))
                            PostType.CELEBRATION -> Icon(Icons.Default.Celebration, contentDescription = null, tint = Color(0xFF9C27B0), modifier = Modifier.size(16.dp))
                            PostType.GENERAL -> Icon(Icons.Default.Info, contentDescription = null, tint = Color.Blue, modifier = Modifier.size(16.dp))
                            PostType.LOST_FOUND -> Icon(Icons.Default.Pets, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(post.category, fontSize = 12.sp, color = Color.Gray)

                        if (post.priority > 7) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(containerColor = Color.Red) {
                                Text("HIGH", color = Color.White, fontSize = 10.sp)
                            }
                        }

                        if (!post.isLocal) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(containerColor = Color.Blue) {
                                Text("NEARBY", color = Color.White, fontSize = 10.sp)
                            }
                        }

                        if (post.hasImage) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = post.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = post.description,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        maxLines = 2
                    )
                }

                Text(
                    text = post.timeAgo,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(post.location, fontSize = 12.sp, color = Color.Gray)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("${post.upvotes}", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Comment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("${post.comments}", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("${post.views}", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("By ${post.authorName}", fontSize = 11.sp, color = Color.Gray)
                Text("Status: ${post.status}", fontSize = 11.sp,
                    color = when(post.status) {
                        "Open", "Active", "Reported" -> Color.Red
                        "In Progress" -> Color(0xFFFF9800)
                        "Resolved" -> Color.Green
                        else -> Color.Blue
                    },
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

fun getFilteredPosts(
    posts: List<CommunityPost>,
    category: String,
    sortBy: SortBy,
    localOnly: Boolean
): List<CommunityPost> {
    var filtered = posts

    // Filter by local/nearby
    if (localOnly) {
        filtered = filtered.filter { it.isLocal }
    }

    // Filter by category
    if (category != "All") {
        filtered = when (category) {
            "Issues" -> filtered.filter { it.type == PostType.ISSUE }
            "Celebrations" -> filtered.filter { it.type == PostType.CELEBRATION }
            "General" -> filtered.filter { it.type == PostType.GENERAL }
            "Lost & Found" -> filtered.filter { it.type == PostType.LOST_FOUND }
            else -> filtered
        }
    }

    // Sort posts
    return when (sortBy) {
        SortBy.RECENT -> filtered // Already in recent order
        SortBy.MOST_VOTED -> filtered.sortedByDescending { it.upvotes }
        SortBy.MOST_VIEWED -> filtered.sortedByDescending { it.views }
        SortBy.PRIORITY -> filtered.sortedByDescending { it.priority }
    }
}

// Legacy data models for backward compatibility
data class Issue(
    val title: String,
    val category: String,
    val status: String,
    val upvotes: Int,
    val comments: Int
)

val dummyIssues = listOf(
    Issue("Pothole on Main St", "Roads", "Open", 12, 3),
    Issue("Overflowing Garbage", "Waste", "In Progress", 8, 2),
    Issue("Lost Dog", "Lost & Found", "Resolved", 5, 1)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf("All events", "Problems", "Community celebration")
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        filters.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter) }
            )
        }
    }
}

@Composable
fun IssueCard(issue: Issue) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(issue.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(issue.category, fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Status: ${issue.status}", fontSize = 13.sp)
                Row {
                    Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("${issue.upvotes}  ")
                    Icon(Icons.Default.Comment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("${issue.comments}")
                }
            }
        }
    }
}
