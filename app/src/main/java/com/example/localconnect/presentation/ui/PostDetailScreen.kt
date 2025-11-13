package com.example.localconnect.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import com.example.localconnect.data.model.Post
import com.example.localconnect.data.model.PostType
import java.text.SimpleDateFormat
import java.util.*

/**
 * Post Detail Screen - Displays detailed information about a post
 * - Lazy loads full images/videos when opened
 * - Supports multiple media items with horizontal scroll
 * - Handles both media and non-media posts
 * - Smooth animations and error handling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    post: Post,
    viewModel: com.example.localconnect.presentation.viewmodel.PostDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val postType = PostType.fromString(post.type)
    var selectedMediaIndex by remember { mutableStateOf(0) }

    val comments by viewModel.comments.collectAsState()
    val postStats by viewModel.postStats.collectAsState()
    val isLiked by viewModel.isLiked.collectAsState()
    val likedComments by viewModel.likedComments.collectAsState()
    val error by viewModel.error.collectAsState()

    // Handle system back gesture/key
    BackHandler { onBackClick() }

    // Show error snackbar
    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            // Show error to user (you can add a Snackbar here)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share functionality */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { /* More options */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Media Section (Images/Videos) - Lazy loaded
            if (post.mediaUrls.isNotEmpty()) {
                item {
                    MediaCarousel(
                        mediaUrls = post.mediaUrls,
                        selectedIndex = selectedMediaIndex,
                        onIndexChanged = { selectedMediaIndex = it }
                    )
                }
            }

            // Post Header
            item {
                PostDetailHeader(post = post, postType = postType)
            }

            // Post Content
            item {
                PostDetailContent(post = post, postType = postType)
            }

            // Post Metrics
            item {
                PostDetailMetrics(
                    postStats = postStats,
                    postType = postType
                )
            }

            // Action Buttons
            item {
                PostDetailActions(
                    isLiked = isLiked,
                    onLikeClick = { viewModel.togglePostLike() },
                    onCommentClick = { /* Scroll to comment section */ },
                    postType = postType
                )
            }

            // Comments Section
            item {
                CommentsSection(
                    comments = comments,
                    likedComments = likedComments,
                    currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid,
                    onAddComment = { text -> viewModel.addComment(text) },
                    onDeleteComment = { commentId -> viewModel.deleteComment(commentId) },
                    onLikeComment = { commentId -> viewModel.toggleCommentLike(commentId) }
                )
            }
        }
    }
}

@Composable
private fun MediaCarousel(
    mediaUrls: List<String>,
    selectedIndex: Int,
    onIndexChanged: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Main Media Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.Black)
        ) {
            val currentUrl = mediaUrls.getOrNull(selectedIndex) ?: ""

            when {
                isVideoUrl(currentUrl) -> {
                    VideoPlayer(videoUrl = currentUrl)
                }
                isImageUrl(currentUrl) -> {
                    FullImageView(imageUrl = currentUrl)
                }
                else -> {
                    // Fallback for unknown media type
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Unsupported media format",
                            color = Color.White
                        )
                    }
                }
            }

            // Media Counter
            if (mediaUrls.size > 1) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "${selectedIndex + 1}/${mediaUrls.size}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Media Thumbnails (if multiple)
        if (mediaUrls.size > 1) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(mediaUrls.size) { index ->
                    MediaThumbnail(
                        mediaUrl = mediaUrls[index],
                        isSelected = index == selectedIndex,
                        onClick = { onIndexChanged(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FullImageView(imageUrl: String) {
    SubcomposeAsyncImage(
        model = imageUrl,
        contentDescription = "Post image",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit,
        loading = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = Color.White
                )
            }
        },
        error = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = "Failed to load image",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Failed to load image",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    )
}

@Composable
private fun VideoPlayer(videoUrl: String) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Create ExoPlayer lazily
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.Builder().setUri(videoUrl).build())
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun MediaThumbnail(
    mediaUrl: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray)
            .padding(if (isSelected) 2.dp else 0.dp)
            .clickable(onClick = onClick)
    ) {
        SubcomposeAsyncImage(
            model = mediaUrl,
            contentDescription = "Media thumbnail",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(if (isSelected) 6.dp else 8.dp)),
            contentScale = ContentScale.Crop,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        )

        // Play icon for videos
        if (isVideoUrl(mediaUrl)) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = "Video",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
            )
        }
    }
}

@Composable
private fun PostDetailHeader(post: Post, postType: PostType) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category and Type
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = when (postType) {
                    PostType.ISSUE -> Icons.Default.Warning
                    PostType.EVENT -> Icons.Default.Event
                    else -> Icons.Default.Info
                }

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = when (postType) {
                        PostType.ISSUE -> if ((post.priority ?: 0) > 7) Color.Red else Color(0xFFFF9800)
                        PostType.EVENT -> Color(0xFF9C27B0)
                        else -> Color.Blue
                    },
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                post.category?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Time
            Text(
                text = formatDetailedTimeAgo(post.timestamp ?: 0L),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        // Badges
        if (post.isLocalOnly || (postType == PostType.ISSUE && (post.priority ?: 0) > 7)) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (post.isLocalOnly) {
                    AssistChip(
                        onClick = { },
                        label = { Text("LOCAL", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
                if (postType == PostType.ISSUE && (post.priority ?: 0) > 7) {
                    AssistChip(
                        onClick = { },
                        label = { Text("HIGH PRIORITY", fontSize = 11.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.Red.copy(alpha = 0.1f),
                            labelColor = Color.Red
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PostDetailContent(post: Post, postType: PostType) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Title
        val title = when (postType) {
            PostType.ISSUE -> post.title ?: post.caption
            else -> post.caption ?: post.title
        }

        title?.let {
            Text(
                text = it,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Description
        post.description?.let {
            Text(
                text = it,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Location
        if (post.locationName.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = post.locationName,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Tags
        if (post.tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                post.tags.forEach { tag ->
                    SuggestionChip(
                        onClick = { },
                        label = { Text("#$tag", fontSize = 12.sp) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Status (for issues)
        if (postType == PostType.ISSUE && !post.status.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when (post.status) {
                    "Open", "Active", "Reported" -> Color.Red.copy(alpha = 0.1f)
                    "In Progress" -> Color(0xFFFF9800).copy(alpha = 0.1f)
                    "Resolved", "Closed" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                    else -> Color.Blue.copy(alpha = 0.1f)
                }
            ) {
                Text(
                    text = "Status: ${post.status}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when (post.status) {
                        "Open", "Active", "Reported" -> Color.Red
                        "In Progress" -> Color(0xFFFF9800)
                        "Resolved", "Closed" -> Color(0xFF4CAF50)
                        else -> Color.Blue
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PostDetailMetrics(postStats: com.example.localconnect.data.repository.PostStats?, postType: PostType) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MetricColumn(
                icon = if (postType == PostType.ISSUE) Icons.Default.ThumbUp else Icons.Default.Favorite,
                count = postStats?.likes ?: 0,
                label = if (postType == PostType.ISSUE) "Upvotes" else "Likes"
            )

            MetricColumn(
                icon = Icons.AutoMirrored.Filled.Comment,
                count = postStats?.comments ?: 0,
                label = "Comments"
            )

            MetricColumn(
                icon = Icons.Default.Visibility,
                count = postStats?.views ?: 0,
                label = "Views"
            )
        }
    }
}

@Composable
private fun MetricColumn(icon: androidx.compose.ui.graphics.vector.ImageVector, count: Int, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatCount(count),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun PostDetailActions(
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    postType: PostType
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onLikeClick,
            modifier = Modifier.weight(1f),
            colors = if (isLiked) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            } else {
                ButtonDefaults.buttonColors()
            }
        ) {
            Icon(
                imageVector = if (postType == PostType.ISSUE) Icons.Default.ThumbUp else Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (postType == PostType.ISSUE) {
                if (isLiked) "Upvoted" else "Upvote"
            } else {
                if (isLiked) "Liked" else "Like"
            })
        }

        OutlinedButton(
            onClick = onCommentClick,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Comment,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Comment")
        }
    }
}

@Composable
private fun CommentsSection(
    comments: List<com.example.localconnect.data.model.Comment>,
    likedComments: Set<String>,
    currentUserId: String?,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onLikeComment: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Comments (${comments.size})",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Comment input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                placeholder = { Text("Add a comment...") },
                modifier = Modifier.weight(1f),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (commentText.isNotBlank()) {
                        onAddComment(commentText)
                        commentText = ""
                    }
                },
                enabled = commentText.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send comment",
                    tint = if (commentText.isNotBlank())
                        MaterialTheme.colorScheme.primary
                    else
                        Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Comments list
        if (comments.isEmpty()) {
            Text(
                text = "No comments yet. Be the first to comment!",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 32.dp)
            )
        } else {
            comments.forEach { comment ->
                CommentItem(
                    comment = comment,
                    isLiked = likedComments.contains(comment.commentId),
                    isOwnComment = comment.userId == currentUserId,
                    onLikeClick = { onLikeComment(comment.commentId) },
                    onDeleteClick = { onDeleteComment(comment.commentId) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: com.example.localconnect.data.model.Comment,
    isLiked: Boolean,
    isOwnComment: Boolean,
    onLikeClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = comment.userName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = formatDetailedTimeAgo(comment.timestamp),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Comment text
            Text(
                text = comment.text,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onLikeClick)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        modifier = Modifier.size(18.dp),
                        tint = if (isLiked) Color.Red else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${comment.likes}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Delete button (only for own comments)
                if (isOwnComment) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// Helper Functions
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        // Measure children with given constraints
        val placeables = measurables.map { it.measure(constraints) }

        val maxLineWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else placeables.maxOfOrNull { it.width } ?: 0
        val spacing = 8 // px spacing between items (kept same as before)

        var xPos = 0
        var yPos = 0
        var lineHeight = 0

        // Track positions for placement
        val positions = ArrayList<Pair<Int, Int>>(placeables.size)

        placeables.forEach { placeable ->
            val pw = placeable.width
            val ph = placeable.height

            if (xPos > 0 && xPos + pw > maxLineWidth) {
                // Wrap to next line
                xPos = 0
                yPos += lineHeight + spacing
                lineHeight = 0
            }

            positions.add(Pair(xPos, yPos))
            xPos += pw + spacing
            lineHeight = maxOf(lineHeight, ph)
        }

        val calculatedWidth = maxLineWidth.coerceAtLeast(constraints.minWidth)
        val calculatedHeight = (yPos + lineHeight).coerceAtLeast(constraints.minHeight)

        // Cap both dimensions within constraints and Compose hard limit
        val maxComposeDim = 16_777_215 // Compose hard limit for a dimension
        val layoutWidth = calculatedWidth.coerceIn(constraints.minWidth, constraints.maxWidth.coerceAtMost(maxComposeDim))
        val layoutHeight = calculatedHeight.coerceIn(constraints.minHeight, constraints.maxHeight.coerceAtMost(maxComposeDim))

        layout(layoutWidth, layoutHeight) {
            placeables.forEachIndexed { index, placeable ->
                val (px, py) = positions[index]
                placeable.place(px, py)
            }
        }
    }
}

private fun isVideoUrl(url: String): Boolean {
    return url.contains("video", ignoreCase = true) ||
            url.endsWith(".mp4", ignoreCase = true) ||
            url.endsWith(".mov", ignoreCase = true) ||
            url.endsWith(".avi", ignoreCase = true)
}

private fun isImageUrl(url: String): Boolean {
    return url.contains("image", ignoreCase = true) ||
            url.endsWith(".jpg", ignoreCase = true) ||
            url.endsWith(".jpeg", ignoreCase = true) ||
            url.endsWith(".png", ignoreCase = true) ||
            url.endsWith(".webp", ignoreCase = true)
}

private fun formatDetailedTimeAgo(timestamp: Long): String {
    if (timestamp == 0L) return "Unknown time"

    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000} minute${if (diff / 60000 > 1) "s" else ""} ago"
        diff < 86400000 -> "${diff / 3600000} hour${if (diff / 3600000 > 1) "s" else ""} ago"
        diff < 604800000 -> "${diff / 86400000} day${if (diff / 86400000 > 1) "s" else ""} ago"
        else -> SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1000000 -> String.format(Locale.US, "%.1fM", count / 1000000.0)
        count >= 1000 -> String.format(Locale.US, "%.1fK", count / 1000.0)
        else -> count.toString()
    }
}
