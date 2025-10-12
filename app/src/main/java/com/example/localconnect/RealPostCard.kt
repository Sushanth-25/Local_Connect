package com.example.localconnect

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localconnect.data.model.Post
import com.example.localconnect.data.model.PostType
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RealPostCard(post: Post) {
    val postType = PostType.fromString(post.type)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (postType) {
                PostType.ISSUE -> if ((post.priority ?: 0) > 7) Color(0xFFFFEBEE) else Color(0xFFFFF3E0)
                PostType.EVENT -> Color(0xFFF3E5F5)
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
                        // Type and category indicators
                        when (postType) {
                            PostType.ISSUE -> Icon(Icons.Default.Warning, contentDescription = null,
                                tint = if ((post.priority ?: 0) > 7) Color.Red else Color(0xFFFF9800),
                                modifier = Modifier.size(16.dp))
                            PostType.EVENT -> Icon(Icons.Default.Event, contentDescription = null,
                                tint = Color(0xFF9C27B0), modifier = Modifier.size(16.dp))
                            else -> when (post.category) {
                                "Health" -> Icon(Icons.Default.LocalHospital, contentDescription = null,
                                    tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                "Emergency" -> Icon(Icons.Default.Warning, contentDescription = null,
                                    tint = Color.Red, modifier = Modifier.size(16.dp))
                                "Infrastructure" -> Icon(Icons.Default.Construction, contentDescription = null,
                                    tint = Color(0xFF9C27B0), modifier = Modifier.size(16.dp))
                                "Roads" -> Icon(Icons.Default.Route, contentDescription = null,
                                    tint = Color(0xFFFF9800), modifier = Modifier.size(16.dp))
                                "Lost & Found" -> Icon(Icons.Default.Pets, contentDescription = null,
                                    tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                else -> Icon(Icons.Default.Info, contentDescription = null,
                                    tint = Color.Blue, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))
                        post.category?.let { category ->
                            Text(category, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        }

                        // Priority badge for high priority issues
                        if (postType == PostType.ISSUE && (post.priority ?: 0) > 7) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(containerColor = Color.Red) {
                                Text("HIGH", color = Color.White, fontSize = 10.sp)
                            }
                        }

                        // Local badge
                        if (post.isLocalOnly) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(containerColor = Color(0xFF4CAF50)) {
                                Text("LOCAL", color = Color.White, fontSize = 10.sp)
                            }
                        }

                        // Media indicators
                        if (post.hasImage || post.mediaUrls.any { it.contains("image", ignoreCase = true) }) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        }

                        if (post.videoUrl != null || post.mediaUrls.any { it.contains("video", ignoreCase = true) }) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Display title for issues or caption for posts
                    val displayText = when (postType) {
                        PostType.ISSUE -> post.title ?: post.caption ?: "Untitled Issue"
                        else -> post.caption ?: post.title ?: "No caption"
                    }

                    Text(
                        text = displayText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 2
                    )

                    // Show description for issues
                    if (postType == PostType.ISSUE && !post.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = post.description,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            maxLines = 2
                        )
                    }

                    // Tags
                    if (post.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "#${post.tags.joinToString(" #")}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = formatTimeAgo(post.timestamp ?: 0L),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Location
            if (!post.location.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(post.location, fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Interaction metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // For issues, show upvotes; for posts, show likes
                    if (postType == PostType.ISSUE) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("${post.upvotes}", fontSize = 12.sp)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("${post.likes}", fontSize = 12.sp)
                        }
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

                // Status for issues
                if (postType == PostType.ISSUE && !post.status.isNullOrBlank()) {
                    Text(
                        text = "Status: ${post.status}",
                        fontSize = 11.sp,
                        color = when(post.status) {
                            "Open", "Active", "Reported" -> Color.Red
                            "In Progress" -> Color(0xFFFF9800)
                            "Resolved", "Closed" -> Color.Green
                            else -> Color.Blue
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
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
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}
