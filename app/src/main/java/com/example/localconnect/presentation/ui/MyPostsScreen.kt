package com.example.localconnect.presentation.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.localconnect.presentation.viewmodel.MyPostsViewModel
import com.example.localconnect.presentation.viewmodel.MyPostsViewModelFactory
import com.example.localconnect.data.model.Post
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MyPostsScreen(navController: NavHostController) {
    val myPostsViewModel: MyPostsViewModel = viewModel(factory = MyPostsViewModelFactory())
    val uiState by myPostsViewModel.uiState.collectAsState()

    var selectedCategory by remember { mutableStateOf("All") }
    var isFilterExpanded by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var postToDelete by remember { mutableStateOf<Post?>(null) }

    // Load user's posts when screen loads
    LaunchedEffect(Unit) {
        myPostsViewModel.loadUserPosts()
    }

    // Handle refresh
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            myPostsViewModel.loadUserPosts()
            delay(1000)
            isRefreshing = false
        }
    }

    // Show error snackbar
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            // Show snackbar or toast
            delay(3000)
            myPostsViewModel.clearError()
        }
    }

    // Hide filter when scrolling
    val listState = rememberLazyListState()
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && isFilterExpanded) {
            isFilterExpanded = false
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && postToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                postToDelete = null
            },
            title = { Text("Delete Post") },
            text = { Text("Are you sure you want to delete this post? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        postToDelete?.let { myPostsViewModel.deletePost(it.postId) }
                        showDeleteDialog = false
                        postToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    postToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("My Posts", fontWeight = FontWeight.Bold)
                        Text(
                            "${uiState.userPosts.size} posts",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Filter toggle button
                    IconButton(onClick = { isFilterExpanded = !isFilterExpanded }) {
                        Icon(
                            if (isFilterExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.FilterList,
                            contentDescription = "Toggle Filter"
                        )
                    }
                    // Refresh button
                    IconButton(onClick = { isRefreshing = true }) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Expandable Filter Section
            if (isFilterExpanded) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Filter by Category",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val categories = listOf(
                            "All", "Health", "Roads", "Infrastructure",
                            "Lost & Found", "Events", "Emergency", "General"
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categories) { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                    label = { Text(category) }
                                )
                            }
                        }
                    }
                }
                HorizontalDivider()
            }

            // Posts List with lazy loading
            val filteredPosts = if (selectedCategory == "All") {
                uiState.userPosts
            } else {
                uiState.userPosts.filter { it.category == selectedCategory }
            }

            if (uiState.isLoading && uiState.userPosts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredPosts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.PostAdd,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No posts yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Start sharing with your community!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(filteredPosts) { index, post ->
                        MyPostCard(
                            post = post,
                            userData = uiState.userData,
                            onPostClick = {
                                navController.navigate("post_detail/${post.postId}")
                            },
                            onDeleteRequest = {
                                postToDelete = post
                                showDeleteDialog = true
                            }
                        )

                        // Trigger loading more posts when near the end
                        if (index >= filteredPosts.size - 3 && myPostsViewModel.canLoadMore()) {
                            LaunchedEffect(Unit) {
                                myPostsViewModel.loadMorePosts()
                            }
                        }
                    }

                    // Loading indicator for pagination
                    if (myPostsViewModel.canLoadMore()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyPostCard(
    post: Post,
    userData: Pair<String, String>?, // (name, profilePicUrl)
    onPostClick: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    var showFullImages by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onPostClick,
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDeleteRequest()
                }
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top section: User name and Category in same line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User profile section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Profile picture
                    AsyncImage(
                        model = userData?.second ?: "",
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // User name
                    Text(
                        text = userData?.first ?: "User",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Category badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = getCategoryColor(post.category),
                    tonalElevation = 1.dp
                ) {
                    Text(
                        text = post.category ?: "General",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Images section with lazy loading and thumbnail optimization
            if (post.mediaUrls.isNotEmpty()) {
                Box {
                    val pagerState = rememberPagerState(pageCount = {
                        if (showFullImages) post.mediaUrls.size else (post.thumbnailUrls.takeIf { it.isNotEmpty() }?.size ?: post.mediaUrls.size)
                    })

                    // Use thumbnails initially, load full images on interaction
                    val imageUrls = if (showFullImages) {
                        post.mediaUrls
                    } else {
                        post.thumbnailUrls.takeIf { it.isNotEmpty() } ?: post.mediaUrls
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                // Load full images on interaction
                                showFullImages = true
                            }
                    ) { page ->
                        AsyncImage(
                            model = imageUrls[page],
                            contentDescription = "Post Image ${page + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onSuccess = {
                                // Preload next images when current loads successfully
                                if (!showFullImages && page == 0) {
                                    showFullImages = true
                                }
                            }
                        )
                    }

                    // Multiple images indicator (top right corner of first image)
                    if (post.mediaUrls.size > 1) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Collections,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "${pagerState.currentPage + 1}/${post.mediaUrls.size}",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Quality indicator
                    if (!showFullImages && post.thumbnailUrls.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.5f)
                        ) {
                            Text(
                                "Tap for HD",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Engagement icons (likes, comments, shares)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Likes
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ThumbUp,
                        contentDescription = "Likes",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "${post.likes}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Comments
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Comment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "${post.comments}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Shares
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Shares",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "0", // Shares not implemented yet
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            if (!post.description.isNullOrEmpty()) {
                Text(
                    text = post.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Status and timestamp row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = getStatusColor(post.status)
                ) {
                    Text(
                        text = "Status: ${post.status ?: "Open"}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                // Timestamp
                Text(
                    text = getTimeAgo(post.timestamp ?: 0L),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Helper functions
private fun getCategoryColor(category: String?): Color {
    return when (category) {
        "Health" -> Color(0xFFE91E63)
        "Roads" -> Color(0xFF9C27B0)
        "Infrastructure" -> Color(0xFF673AB7)
        "Lost & Found" -> Color(0xFF4CAF50)
        "Events" -> Color(0xFF2196F3)
        "Emergency" -> Color(0xFFF44336)
        "General" -> Color(0xFF607D8B)
        else -> Color(0xFF9E9E9E)
    }
}

private fun getStatusColor(status: String?): Color {
    return when (status) {
        "Open", "Active", "Reported" -> Color(0xFFF44336) // Red
        "In Progress" -> Color(0xFFFF9800) // Orange
        "Resolved", "Closed" -> Color(0xFF4CAF50) // Green
        else -> Color(0xFF2196F3) // Blue
    }
}

private fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        diff < 604800000 -> "${diff / 86400000}d ago"
        else -> "${diff / 604800000}w ago"
    }
}
