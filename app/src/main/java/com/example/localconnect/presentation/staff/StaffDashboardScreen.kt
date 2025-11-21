package com.example.localconnect.presentation.staff

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.example.localconnect.data.model.Comment
import com.example.localconnect.data.model.Post
import com.example.localconnect.data.model.PostType
import com.example.localconnect.viewmodel.PostUpdateState
import com.example.localconnect.viewmodel.StaffViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffDashboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: StaffViewModel = viewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val context = LocalContext.current

    // Changed to support multiple filters
    var selectedFilters by remember { mutableStateOf(setOf<String>()) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var selectedPost by remember { mutableStateOf<Post?>(null) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    var showCommentDialog by remember { mutableStateOf(false) }
    var postForComments by remember { mutableStateOf<Post?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showExpandedPost by remember { mutableStateOf(false) }
    var expandedPost by remember { mutableStateOf<Post?>(null) }

    // State for scroll-based filter visibility
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Hide filter menu when scrolling
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && showFilterMenu) {
            showFilterMenu = false
        }
    }

    // Auto-load posts when dashboard opens
    LaunchedEffect(Unit) {
        viewModel.loadAllPosts()
    }

    // Calculate filter counts
    val allPosts = remember(posts) { posts }
    val filterCounts = remember(allPosts) {
        mapOf(
            // Status counts
            "All" to allPosts.size,
            "Open" to allPosts.count { it.status == "Open" },
            "In Progress" to allPosts.count { it.status == "In Progress" },
            "Resolved" to allPosts.count { it.status == "Resolved" },
            "Closed" to allPosts.count { it.status == "Closed" },
            "Under Review" to allPosts.count { it.status == "Under Review" },
            // Type counts
            "ISSUE" to allPosts.count { it.type == "ISSUE" },
            "EVENT" to allPosts.count { it.type == "EVENT" },
            "POST" to allPosts.count { it.type == "POST" }
        )
    }

    // Filter posts based on multiple selected filters
    val filteredPosts = remember(posts, selectedFilters) {
        if (selectedFilters.isEmpty()) {
            posts
        } else {
            val statusFilters = selectedFilters.filter { it in listOf("Open", "In Progress", "Resolved", "Closed", "Under Review") }
            val typeFilters = selectedFilters.filter { it in listOf("ISSUE", "EVENT", "POST") }

            posts.filter { post ->
                val matchesStatus = statusFilters.isEmpty() || post.status in statusFilters
                val matchesType = typeFilters.isEmpty() || post.type in typeFilters
                matchesStatus && matchesType
            }
        }
    }

    // Calculate active filter count and display text
    val activeFilterCount = selectedFilters.size
    val filterDisplayText = if (selectedFilters.isEmpty()) {
        "All • ${posts.size} Posts"
    } else {
        "${activeFilterCount} Filter${if (activeFilterCount > 1) "s" else ""} • ${filteredPosts.size} Posts"
    }

    // Handle system back button
    BackHandler {
        showLogoutConfirmation = true
    }

    // Handle update state
    LaunchedEffect(updateState) {
        when (updateState) {
            is PostUpdateState.Success -> {
                Toast.makeText(context, "Status updated successfully", Toast.LENGTH_SHORT).show()
                viewModel.resetUpdateState()
                showStatusDialog = false
            }
            is PostUpdateState.Error -> {
                Toast.makeText(context, (updateState as PostUpdateState.Error).message, Toast.LENGTH_SHORT).show()
                viewModel.resetUpdateState()
            }
            else -> {}
        }
    }

    // Logout confirmation dialog
    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F)
                )
            },
            title = { Text("Logout Confirmation") },
            text = { Text("Are you sure you want to logout from the Staff Portal?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmation = false
                        viewModel.logout()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F)
                    )
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Status dialog
    if (showStatusDialog && selectedPost != null) {
        StatusUpdateDialog(
            post = selectedPost!!,
            onDismiss = { showStatusDialog = false },
            onStatusSelected = { newStatus ->
                viewModel.updatePostStatus(selectedPost!!.postId, newStatus)
            }
        )
    }

    // Comment dialog
    if (showCommentDialog && postForComments != null) {
        CommentsDialog(
            post = postForComments!!,
            onDismiss = { showCommentDialog = false }
        )
    }

    // Expanded Post Dialog
    if (showExpandedPost && expandedPost != null) {
        ExpandedPostDialog(
            post = expandedPost!!,
            onDismiss = { showExpandedPost = false },
            onStatusClick = {
                selectedPost = expandedPost
                showStatusDialog = true
            },
            onCommentsClick = {
                postForComments = expandedPost
                showCommentDialog = true
            }
        )
    }

    // Edge-to-edge layout matching login screen exactly
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 0.dp) // Remove statusBarsPadding to extend upward
        ) {
            // Custom Top Bar with integrated filter
            Surface(
                modifier = Modifier
                    .fillMaxWidth(), // Move statusBarsPadding to Surface
                color = Color(0xFF1976D2),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp), // Fixed height for the actual content
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back button
                    IconButton(
                        onClick = { showLogoutConfirmation = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Title
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Staff Dashboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = filterDisplayText,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }

                    // Actions
                    Row(horizontalArrangement = Arrangement.End) {
                        // Filter button with dropdown
                        Box {
                            IconButton(
                                onClick = { showFilterMenu = !showFilterMenu },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box {
                                    Icon(
                                        Icons.Default.FilterList,
                                        "Filters",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    // Show badge with count when filters are active
                                    if (activeFilterCount > 0) {
                                        Badge(
                                            containerColor = Color(0xFFFF9800),
                                            contentColor = Color.White,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 4.dp, y = (-4).dp)
                                        ) {
                                            Text(
                                                text = activeFilterCount.toString(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Filter Dropdown Menu
                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .width(220.dp)
                            ) {
                                // Clear All / Select All option
                                if (selectedFilters.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = Color(0xFFD32F2F)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Clear All Filters",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFD32F2F)
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedFilters = setOf()
                                        }
                                    )
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                }

                                // Status Filters Section
                                Text(
                                    text = "BY STATUS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )

                                FilterMenuItem(
                                    label = "Open",
                                    count = filterCounts["Open"] ?: 0,
                                    isSelected = "Open" in selectedFilters,
                                    onClick = {
                                        selectedFilters = if ("Open" in selectedFilters) {
                                            selectedFilters - "Open"
                                        } else {
                                            selectedFilters + "Open"
                                        }
                                    }
                                )

                                FilterMenuItem(
                                    label = "In Progress",
                                    count = filterCounts["In Progress"] ?: 0,
                                    isSelected = "In Progress" in selectedFilters,
                                    onClick = {
                                        selectedFilters = if ("In Progress" in selectedFilters) {
                                            selectedFilters - "In Progress"
                                        } else {
                                            selectedFilters + "In Progress"
                                        }
                                    }
                                )

                                FilterMenuItem(
                                    label = "Resolved",
                                    count = filterCounts["Resolved"] ?: 0,
                                    isSelected = "Resolved" in selectedFilters,
                                    onClick = {
                                        selectedFilters = if ("Resolved" in selectedFilters) {
                                            selectedFilters - "Resolved"
                                        } else {
                                            selectedFilters + "Resolved"
                                        }
                                    }
                                )

                                FilterMenuItem(
                                    label = "Closed",
                                    count = filterCounts["Closed"] ?: 0,
                                    isSelected = "Closed" in selectedFilters,
                                    onClick = {
                                        selectedFilters = if ("Closed" in selectedFilters) {
                                            selectedFilters - "Closed"
                                        } else {
                                            selectedFilters + "Closed"
                                        }
                                    }
                                )

                                FilterMenuItem(
                                    label = "Under Review",
                                    count = filterCounts["Under Review"] ?: 0,
                                    isSelected = "Under Review" in selectedFilters,
                                    onClick = {
                                        selectedFilters = if ("Under Review" in selectedFilters) {
                                            selectedFilters - "Under Review"
                                        } else {
                                            selectedFilters + "Under Review"
                                        }
                                    }
                                )

                                Divider(modifier = Modifier.padding(vertical = 4.dp))

                                // Type Filters Section
                                Text(
                                    text = "BY TYPE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )

                                FilterMenuItem(
                                    label = "Issues",
                                    count = filterCounts["ISSUE"] ?: 0,
                                    isSelected = "ISSUE" in selectedFilters,
                                    onClick = {
                                        selectedFilters = if ("ISSUE" in selectedFilters) {
                                            selectedFilters - "ISSUE"
                                        } else {
                                            selectedFilters + "ISSUE"
                                        }
                                    }
                                )

                                FilterMenuItem(
                                    label = "Events",
                                    count = filterCounts["EVENT"] ?: 0,
                                    isSelected = "EVENT" in selectedFilters,
                                    onClick = {
                                        selectedFilters = if ("EVENT" in selectedFilters) {
                                            selectedFilters - "EVENT"
                                        } else {
                                            selectedFilters + "EVENT"
                                        }
                                    }
                                )

                                FilterMenuItem(
                                    label = "Posts",
                                    count = filterCounts["POST"] ?: 0,
                                    isSelected = "POST" in selectedFilters,
                                    onClick = {
                                        selectedFilters = if ("POST" in selectedFilters) {
                                            selectedFilters - "POST"
                                        } else {
                                            selectedFilters + "POST"
                                        }
                                    }
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                selectedFilters = setOf()  // Clear all filters
                                viewModel.loadAllPosts()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                "Refresh",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { showLogoutConfirmation = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                "Logout",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Posts List - no filter card anymore
            if (filteredPosts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedFilters.isEmpty()) "No posts found" else "No posts match the selected filters",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        if (selectedFilters.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { selectedFilters = setOf() }) {
                                Text("Clear Filters")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = filteredPosts,
                        key = { post -> post.postId }
                    ) { post ->
                        StaffPostCard(
                            post = post,
                            onStatusClick = {
                                selectedPost = post
                                showStatusDialog = true
                            },
                            onCommentsClick = {
                                postForComments = post
                                showCommentDialog = true
                            },
                            onPostClick = {
                                expandedPost = post
                                showExpandedPost = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StaffPostCard(
    post: Post,
    onStatusClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onPostClick: () -> Unit
) {
    val postType = PostType.fromString(post.type)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPostClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (postType) {
                PostType.ISSUE -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                PostType.EVENT -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Type badge and timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(
                        containerColor = when (postType) {
                            PostType.ISSUE -> Color(0xFFD32F2F)
                            PostType.EVENT -> Color(0xFF7B1FA2)
                            else -> Color(0xFF1976D2)
                        }
                    ) {
                        Text(
                            text = postType.value,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    post.category?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = it,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Text(
                    text = formatTimeAgo(post.timestamp ?: 0L),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = post.title ?: post.caption ?: "No title",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Description for issues
            if (postType == PostType.ISSUE && !post.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = post.description,
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Thumbnail Image (Display thumbnails if available)
            if (post.thumbnailUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    SubcomposeAsyncImage(
                        model = post.thumbnailUrls.first(),
                        contentDescription = "Post thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = "Failed to load thumbnail",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    )

                    // Multiple media indicator
                    if (post.thumbnailUrls.size > 1 || post.mediaUrls.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Collections,
                                contentDescription = "Multiple media",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${maxOf(post.thumbnailUrls.size, post.mediaUrls.size)}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Location
            if (post.locationName.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = post.locationName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Metrics with clickable comments
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetricItem(icon = Icons.Default.ThumbUp, count = post.likes)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onCommentsClick)
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF1976D2)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = post.comments.toString(),
                        fontSize = 12.sp,
                        color = Color(0xFF1976D2),
                        fontWeight = FontWeight.Bold
                    )
                }
                MetricItem(icon = Icons.Default.Visibility, count = post.views)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider()

            Spacer(modifier = Modifier.height(12.dp))

            // Status Change Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Current Status:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = post.status ?: "Not Set",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = getStatusColor(post.status)
                    )
                }

                Button(
                    onClick = onStatusClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Change Status")
                }
            }
        }
    }
}

@Composable
fun StatusUpdateDialog(
    post: Post,
    onDismiss: () -> Unit,
    onStatusSelected: (String) -> Unit
) {
    val statusOptions = listOf("Open", "In Progress", "Resolved", "Closed", "Under Review")
    var selectedStatus by remember { mutableStateOf(post.status ?: "Open") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Status") },
        text = {
            Column {
                Text(
                    text = post.title ?: post.caption ?: "Post",
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Select new status:", fontSize = 14.sp)

                Spacer(modifier = Modifier.height(8.dp))

                statusOptions.forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStatus = status }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = status,
                            color = getStatusColor(status),
                            fontWeight = if (selectedStatus == status) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onStatusSelected(selectedStatus) }) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MetricItem(icon: androidx.compose.ui.graphics.vector.ImageVector, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = count.toString(),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

private fun getStatusColor(status: String?): Color {
    return when (status) {
        "Open", "Reported" -> Color(0xFFD32F2F)
        "In Progress", "Under Review" -> Color(0xFFFF9800)
        "Resolved", "Closed" -> Color(0xFF4CAF50)
        else -> Color.Gray
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    if (timestamp == 0L) return "Unknown"

    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        diff < 604800000 -> "${diff / 86400000}d ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
fun CommentsDialog(
    post: Post,
    onDismiss: () -> Unit
) {
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()

    // Get screen configuration for responsive sizing
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    // Adaptive sizing based on screen size
    val dialogWidthFraction = if (screenWidth < 360.dp) 0.98f else 0.95f
    val dialogHeightFraction = if (screenHeight < 600.dp) 0.90f else 0.85f

    // Load comments when dialog opens
    LaunchedEffect(post.postId) {
        isLoading = true
        try {
            val commentsSnapshot = firestore.collection("posts")
                .document(post.postId)
                .collection("comments")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            comments = commentsSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Comment::class.java)
            }
        } catch (e: Exception) {
            // Handle error silently or show toast
        } finally {
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(dialogWidthFraction)
                .fillMaxHeight(dialogHeightFraction),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header - Adaptive padding based on screen size
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF1976D2),
                                    Color(0xFF2196F3)
                                )
                            )
                        )
                        .padding(
                            horizontal = if (screenWidth < 360.dp) 16.dp else 20.dp,
                            vertical = if (screenHeight < 600.dp) 12.dp else 20.dp
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Comments",
                                fontSize = if (screenWidth < 360.dp) 20.sp else 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${comments.size} ${if (comments.size == 1) "comment" else "comments"}",
                                fontSize = if (screenWidth < 360.dp) 12.sp else 14.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Post Info - Adaptive padding
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = if (screenWidth < 360.dp) 12.dp else 16.dp,
                            vertical = if (screenHeight < 600.dp) 12.dp else 16.dp
                        )
                    ) {
                        Text(
                            text = post.title ?: post.caption ?: "Post",
                            fontSize = if (screenWidth < 360.dp) 14.sp else 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!post.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = post.description,
                                fontSize = if (screenWidth < 360.dp) 12.sp else 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Comments List
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(if (screenWidth < 360.dp) 40.dp else 48.dp),
                                color = Color(0xFF1976D2),
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading comments...",
                                fontSize = if (screenWidth < 360.dp) 12.sp else 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else if (comments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(
                                if (screenWidth < 360.dp) 24.dp else 32.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(
                                    if (screenWidth < 360.dp) 56.dp else 72.dp
                                ),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No comments yet",
                                fontSize = if (screenWidth < 360.dp) 16.sp else 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Be the first to share your thoughts!",
                                fontSize = if (screenWidth < 360.dp) 12.sp else 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            if (screenWidth < 360.dp) 12.dp else 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(
                            if (screenHeight < 600.dp) 8.dp else 12.dp
                        )
                    ) {
                        items(
                            items = comments,
                            key = { comment -> comment.commentId }
                        ) { comment ->
                            StaffCommentItem(
                                comment = comment,
                                isCompact = screenWidth < 360.dp || screenHeight < 600.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StaffCommentItem(
    comment: Comment,
    isCompact: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (isCompact) 12.dp else 16.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompact) 12.dp else 16.dp)
        ) {
            // User Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Avatar
                Box(
                    modifier = Modifier
                        .size(if (isCompact) 36.dp else 40.dp)
                        .clip(CircleShape)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF2196F3),
                                    Color(0xFF1976D2)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (comment.userProfileUrl.isNullOrBlank()) {
                        Text(
                            text = comment.userName.firstOrNull()?.uppercase() ?: "U",
                            fontSize = if (isCompact) 16.sp else 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        SubcomposeAsyncImage(
                            model = comment.userProfileUrl,
                            contentDescription = "User avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.width(if (isCompact) 10.dp else 12.dp))

                // User Name and Time
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comment.userName,
                        fontSize = if (isCompact) 14.sp else 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatCommentTime(comment.timestamp),
                        fontSize = if (isCompact) 11.sp else 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Likes
                if (comment.likes > 0) {
                    Surface(
                        shape = RoundedCornerShape(if (isCompact) 10.dp else 12.dp),
                        color = Color(0xFFFFEBEE)
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = if (isCompact) 6.dp else 8.dp,
                                vertical = if (isCompact) 3.dp else 4.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFE91E63),
                                modifier = Modifier.size(if (isCompact) 12.dp else 14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = comment.likes.toString(),
                                fontSize = if (isCompact) 11.sp else 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFE91E63)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 12.dp))

            // Comment Text
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(if (isCompact) 10.dp else 12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    text = comment.text,
                    fontSize = if (isCompact) 13.sp else 14.sp,
                    lineHeight = if (isCompact) 18.sp else 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(if (isCompact) 10.dp else 12.dp)
                )
            }
        }
    }
}

private fun formatCommentTime(timestamp: Long): String {
    if (timestamp == 0L) return "Unknown time"

    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        diff < 604800000 -> "${diff / 86400000}d ago"
        diff < 2592000000 -> "${diff / 604800000}w ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
fun FilterMenuItem(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF1976D2)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Spacer(modifier = Modifier.width(24.dp))
                    }
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF1976D2) else MaterialTheme.colorScheme.onSurface
                    )
                }
                // Count badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFF1976D2) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = count.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        onClick = onClick,
        modifier = Modifier.background(
            if (isSelected) Color(0xFF1976D2).copy(alpha = 0.1f) else Color.Transparent
        )
    )
}

@Composable
fun ExpandedPostDialog(
    post: Post,
    onDismiss: () -> Unit,
    onStatusClick: () -> Unit,
    onCommentsClick: () -> Unit
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    // Fetch user details
    var userName by remember { mutableStateOf("Loading...") }
    var userPhoneNumber by remember { mutableStateOf<String?>(null) }
    var userProfileUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingUser by remember { mutableStateOf(true) }

    // Current image index for multiple images
    var currentImageIndex by remember { mutableStateOf(0) }

    // Fetch user data
    LaunchedEffect(post.userId) {
        isLoadingUser = true
        try {
            val userDoc = firestore.collection("users")
                .document(post.userId)
                .get()
                .await()

            userName = userDoc.getString("name") ?: "Unknown User"
            userPhoneNumber = userDoc.getString("phoneNumber")
            userProfileUrl = userDoc.getString("profileImage")
        } catch (e: Exception) {
            userName = "Unknown User"
        } finally {
            isLoadingUser = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Bar with close button
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1976D2),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = "Post Details",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        // Phone icon
                        IconButton(
                            onClick = {
                                if (userPhoneNumber != null) {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                        data = android.net.Uri.parse("tel:$userPhoneNumber")
                                    }
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, "Phone number not provided", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Call User",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Scrollable content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // Post Type Badge
                    item {
                        val postType = PostType.fromString(post.type)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Badge(
                                containerColor = when (postType) {
                                    PostType.ISSUE -> Color(0xFFD32F2F)
                                    PostType.EVENT -> Color(0xFF7B1FA2)
                                    else -> Color(0xFF1976D2)
                                }
                            ) {
                                Text(
                                    text = postType.value,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }

                            post.category?.let {
                                Text(
                                    text = it,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // User Info Section
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // User Avatar
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(
                                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFF2196F3),
                                                    Color(0xFF1976D2)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (userProfileUrl.isNullOrBlank()) {
                                        Text(
                                            text = userName.firstOrNull()?.uppercase() ?: "U",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    } else {
                                        SubcomposeAsyncImage(
                                            model = userProfileUrl,
                                            contentDescription = "User avatar",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = userName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatTimeAgo(post.timestamp ?: 0L),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Title
                    item {
                        Text(
                            text = post.title ?: post.caption ?: "No title",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Description
                    item {
                        if (!post.description.isNullOrBlank()) {
                            Text(
                                text = post.description,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Images Section
                    item {
                        if (post.mediaUrls.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column {
                                    // Full Image Display
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(400.dp)
                                    ) {
                                        SubcomposeAsyncImage(
                                            model = post.mediaUrls[currentImageIndex],
                                            contentDescription = "Post image ${currentImageIndex + 1}",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit,
                                            loading = {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator()
                                                }
                                            },
                                            error = {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.BrokenImage,
                                                            contentDescription = "Failed to load",
                                                            modifier = Modifier.size(64.dp),
                                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text(
                                                            text = "Failed to load image",
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                        )
                                                    }
                                                }
                                            }
                                        )

                                        // Image counter
                                        if (post.mediaUrls.size > 1) {
                                            Surface(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(12.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                color = Color.Black.copy(alpha = 0.6f)
                                            ) {
                                                Text(
                                                    text = "${currentImageIndex + 1}/${post.mediaUrls.size}",
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        }

                                        // Navigation arrows
                                        if (post.mediaUrls.size > 1) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .align(Alignment.Center)
                                                    .padding(horizontal = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                if (currentImageIndex > 0) {
                                                    IconButton(
                                                        onClick = { currentImageIndex-- },
                                                        modifier = Modifier
                                                            .clip(CircleShape)
                                                            .background(Color.Black.copy(alpha = 0.5f))
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowBack,
                                                            contentDescription = "Previous",
                                                            tint = Color.White
                                                        )
                                                    }
                                                } else {
                                                    Spacer(modifier = Modifier.size(48.dp))
                                                }

                                                if (currentImageIndex < post.mediaUrls.size - 1) {
                                                    IconButton(
                                                        onClick = { currentImageIndex++ },
                                                        modifier = Modifier
                                                            .clip(CircleShape)
                                                            .background(Color.Black.copy(alpha = 0.5f))
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowForward,
                                                            contentDescription = "Next",
                                                            tint = Color.White
                                                        )
                                                    }
                                                } else {
                                                    Spacer(modifier = Modifier.size(48.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        } else {
                            // No image message
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ImageNotSupported,
                                        contentDescription = "No image",
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No images attached",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Location Section
                    item {
                        if (post.locationName.isNotBlank()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Open map with location
                                        val uri = android.net.Uri.parse(
                                            "geo:${post.latitude},${post.longitude}?q=${post.latitude},${post.longitude}(${post.locationName})"
                                        )
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                        intent.setPackage("com.google.android.apps.maps")

                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // Fallback to browser if Google Maps is not installed
                                            val browserIntent = android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=${post.latitude},${post.longitude}")
                                            )
                                            context.startActivity(browserIntent)
                                        }
                                    },
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = Color(0xFF1976D2)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Location",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = post.locationName,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Tap to view on map",
                                            fontSize = 11.sp,
                                            color = Color(0xFF1976D2),
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Metrics and Actions
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Metrics
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.ThumbUp,
                                            contentDescription = "Likes",
                                            tint = Color(0xFF1976D2),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = post.likes.toString(),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Likes",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable(onClick = onCommentsClick)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Comment,
                                            contentDescription = "Comments",
                                            tint = Color(0xFF1976D2),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = post.comments.toString(),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Comments",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = "Views",
                                            tint = Color(0xFF1976D2),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = post.views.toString(),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Views",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Status Section
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when (post.status) {
                                    "Open" -> Color(0xFFD32F2F).copy(alpha = 0.1f)
                                    "In Progress" -> Color(0xFFFF9800).copy(alpha = 0.1f)
                                    "Resolved", "Closed" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Current Status",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = post.status ?: "Not Set",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = getStatusColor(post.status)
                                    )
                                }

                                Button(
                                    onClick = onStatusClick,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1976D2)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Update", fontSize = 16.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
