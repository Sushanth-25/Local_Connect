package com.example.localconnect.presentation.staff

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.example.localconnect.data.model.Post
import com.example.localconnect.data.model.PostType
import com.example.localconnect.viewmodel.PostUpdateState
import com.example.localconnect.viewmodel.StaffViewModel
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

    var selectedFilter by remember { mutableStateOf("All") }
    var showStatusDialog by remember { mutableStateOf(false) }
    var selectedPost by remember { mutableStateOf<Post?>(null) }
    var expandedFilters by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Staff Dashboard", fontWeight = FontWeight.Bold)
                        Text(
                            text = "${posts.size} Posts",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(onClick = { viewModel.loadAllPosts() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                    // Logout button
                    IconButton(onClick = { viewModel.logout(); onNavigateBack() }) {
                        Icon(Icons.Default.ExitToApp, "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Filter Toggle Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedFilters = !expandedFilters },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filters",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (expandedFilters) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Filters"
                        )
                    }

                    // Filter Chips (shown when expanded)
                    if (expandedFilters) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "By Status:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedFilter == "All",
                                onClick = {
                                    selectedFilter = "All"
                                    viewModel.loadAllPosts()
                                },
                                label = { Text("All") }
                            )
                            FilterChip(
                                selected = selectedFilter == "Open",
                                onClick = {
                                    selectedFilter = "Open"
                                    viewModel.loadPostsByStatus("Open")
                                },
                                label = { Text("Open") }
                            )
                            FilterChip(
                                selected = selectedFilter == "In Progress",
                                onClick = {
                                    selectedFilter = "In Progress"
                                    viewModel.loadPostsByStatus("In Progress")
                                },
                                label = { Text("In Progress") }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedFilter == "Resolved",
                                onClick = {
                                    selectedFilter = "Resolved"
                                    viewModel.loadPostsByStatus("Resolved")
                                },
                                label = { Text("Resolved") }
                            )
                            FilterChip(
                                selected = selectedFilter == "Closed",
                                onClick = {
                                    selectedFilter = "Closed"
                                    viewModel.loadPostsByStatus("Closed")
                                },
                                label = { Text("Closed") }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "By Type:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedFilter == "ISSUE",
                                onClick = {
                                    selectedFilter = "ISSUE"
                                    viewModel.loadPostsByType("ISSUE")
                                },
                                label = { Text("Issues") }
                            )
                            FilterChip(
                                selected = selectedFilter == "EVENT",
                                onClick = {
                                    selectedFilter = "EVENT"
                                    viewModel.loadPostsByType("EVENT")
                                },
                                label = { Text("Events") }
                            )
                            FilterChip(
                                selected = selectedFilter == "POST",
                                onClick = {
                                    selectedFilter = "POST"
                                    viewModel.loadPostsByType("POST")
                                },
                                label = { Text("Posts") }
                            )
                        }
                    }
                }
            }

            // Posts List
            if (posts.isEmpty()) {
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
                            text = "No posts found",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(posts) { post ->
                        StaffPostCard(
                            post = post,
                            onStatusClick = {
                                selectedPost = post
                                showStatusDialog = true
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
    onStatusClick: () -> Unit
) {
    val postType = PostType.fromString(post.type)

    Card(
        modifier = Modifier.fillMaxWidth(),
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

            // Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetricItem(icon = Icons.Default.ThumbUp, count = post.likes)
                MetricItem(icon = Icons.Default.Comment, count = post.comments)
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
