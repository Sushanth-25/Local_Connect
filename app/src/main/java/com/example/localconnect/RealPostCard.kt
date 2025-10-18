package com.example.localconnect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.localconnect.data.model.Post
import com.example.localconnect.data.model.PostType
import java.text.SimpleDateFormat
import java.util.*

/**
 * Optimized Post Card Component
 * - Displays thumbnail on the home screen for performance
 * - Lazy loads full media only when post is clicked
 * - Handles media and non-media posts gracefully
 * - Includes error handling for missing URLs
 */
@Composable
fun RealPostCard(
    post: Post,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val postType = PostType.fromString(post.type)
    val hasThumbnail = post.thumbnailUrls.isNotEmpty()
    val hasMedia = post.mediaUrls.isNotEmpty()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (postType) {
                PostType.ISSUE -> if ((post.priority ?: 0) > 7) Color(0xFFFFEBEE) else Color(0xFFFFF3E0)
                PostType.EVENT -> Color(0xFFF3E5F5)
                else -> Color.White
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row - Category, Type, and Time
            PostCardHeader(post = post, postType = postType)

            Spacer(modifier = Modifier.height(8.dp))

            // Thumbnail Image (if available) - Only load thumbnail on home screen
            if (hasThumbnail) {
                ThumbnailImage(
                    thumbnailUrl = post.thumbnailUrls.firstOrNull() ?: "",
                    hasMultipleMedia = post.thumbnailUrls.size > 1
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Content - Title and Description
            PostCardContent(post = post, postType = postType)

            Spacer(modifier = Modifier.height(8.dp))

            // Location
            if (!post.location.isNullOrBlank()) {
                PostCardLocation(location = post.location)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Footer - Metrics and Status
            PostCardFooter(post = post, postType = postType, hasMedia = hasMedia)
        }
    }
}

@Composable
private fun PostCardHeader(post: Post, postType: PostType) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Type and Category Icon
            PostTypeIcon(postType = postType, category = post.category, priority = post.priority)

            Spacer(modifier = Modifier.width(4.dp))

            // Category Text
            post.category?.let { category ->
                Text(
                    text = category,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }

            // Priority Badge for high priority issues
            if (postType == PostType.ISSUE && (post.priority ?: 0) > 7) {
                Spacer(modifier = Modifier.width(6.dp))
                Badge(containerColor = Color.Red) {
                    Text("HIGH", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Local Badge
            if (post.isLocalOnly) {
                Spacer(modifier = Modifier.width(6.dp))
                Badge(containerColor = Color(0xFF4CAF50)) {
                    Text("LOCAL", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Time Ago
        Text(
            text = formatTimeAgo(post.timestamp ?: 0L),
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun PostTypeIcon(postType: PostType, category: String?, priority: Int?) {
    val icon = when (postType) {
        PostType.ISSUE -> Icons.Default.Warning
        PostType.EVENT -> Icons.Default.Event
        else -> when (category) {
            "Health" -> Icons.Default.LocalHospital
            "Emergency" -> Icons.Default.Warning
            "Infrastructure" -> Icons.Default.Construction
            "Roads" -> Icons.Default.Route
            "Lost & Found" -> Icons.Default.Pets
            else -> Icons.Default.Info
        }
    }

    val tint = when (postType) {
        PostType.ISSUE -> if ((priority ?: 0) > 7) Color.Red else Color(0xFFFF9800)
        PostType.EVENT -> Color(0xFF9C27B0)
        else -> when (category) {
            "Health" -> Color(0xFF4CAF50)
            "Emergency" -> Color.Red
            "Infrastructure" -> Color(0xFF9C27B0)
            "Roads" -> Color(0xFFFF9800)
            "Lost & Found" -> Color(0xFF4CAF50)
            else -> Color.Blue
        }
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(16.dp)
    )
}

@Composable
private fun ThumbnailImage(thumbnailUrl: String, hasMultipleMedia: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        SubcomposeAsyncImage(
            model = thumbnailUrl,
            contentDescription = "Post thumbnail",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.LightGray),
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
                        .background(Color(0xFFEEEEEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = "Failed to load thumbnail",
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        )

        // Multiple media indicator
        if (hasMultipleMedia) {
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
                    text = "Multiple",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PostCardContent(post: Post, postType: PostType) {
    // Title or Caption
    val displayText = when (postType) {
        PostType.ISSUE -> post.title ?: post.caption ?: "Untitled Issue"
        else -> post.caption ?: post.title ?: "No caption"
    }

    Text(
        text = displayText,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )

    // Description for issues
    if (postType == PostType.ISSUE && !post.description.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = post.description,
            fontSize = 14.sp,
            color = Color.Gray,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }

    // Tags
    if (post.tags.isNotEmpty()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "#${post.tags.take(3).joinToString(" #")}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PostCardLocation(location: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = location,
            fontSize = 12.sp,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PostCardFooter(post: Post, postType: PostType, hasMedia: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Upvotes/Likes
            MetricItem(
                icon = if (postType == PostType.ISSUE) Icons.Default.ThumbUp else Icons.Default.Favorite,
                count = if (postType == PostType.ISSUE) post.upvotes else post.likes,
                tint = if (postType == PostType.ISSUE) Color.Gray else Color(0xFFE91E63)
            )

            // Comments
            MetricItem(
                icon = Icons.Default.Comment,
                count = post.comments,
                tint = Color.Gray
            )

            // Views
            MetricItem(
                icon = Icons.Default.Visibility,
                count = post.views,
                tint = Color.Gray
            )

            // Media indicator
            if (hasMedia) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Has media",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Status for issues
        if (postType == PostType.ISSUE && !post.status.isNullOrBlank()) {
            Text(
                text = post.status,
                fontSize = 11.sp,
                color = when (post.status) {
                    "Open", "Active", "Reported" -> Color.Red
                    "In Progress" -> Color(0xFFFF9800)
                    "Resolved", "Closed" -> Color(0xFF4CAF50)
                    else -> Color.Blue
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MetricItem(icon: androidx.compose.ui.graphics.vector.ImageVector, count: Int, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = tint
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = formatCount(count),
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

// Helper Functions
private fun formatTimeAgo(timestamp: Long): String {
    if (timestamp == 0L) return "Unknown"

    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m"
        diff < 86400000 -> "${diff / 3600000}h"
        diff < 604800000 -> "${diff / 86400000}d"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1000000 -> String.format(Locale.US, "%.1fM", count / 1000000.0)
        count >= 1000 -> String.format(Locale.US, "%.1fK", count / 1000.0)
        else -> count.toString()
    }
}
