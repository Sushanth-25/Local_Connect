package com.example.localconnect.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.localconnect.data.model.Notification
import com.example.localconnect.data.model.NotificationType
import com.example.localconnect.viewmodel.NotificationUiState
import com.example.localconnect.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPost: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: NotificationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()

    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Notifications")
                        if (unreadCount > 0) {
                            Text(
                                text = "$unreadCount unread",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mark all as read") },
                            onClick = {
                                viewModel.markAllAsRead()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.DoneAll, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                onNavigateToSettings()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Settings, contentDescription = null)
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Clear all", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                viewModel.deleteAllNotifications()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is NotificationUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is NotificationUiState.Success -> {
                    if (state.notifications.isEmpty()) {
                        EmptyNotificationsView(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        NotificationsList(
                            notifications = state.notifications,
                            onNotificationClick = { notification ->
                                viewModel.markAsRead(notification.notificationId)
                                notification.postId?.let { onNavigateToPost(it) }
                            },
                            onDeleteClick = { notificationId ->
                                viewModel.deleteNotification(notificationId)
                            }
                        )
                    }
                }

                is NotificationUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationsList(
    notifications: List<Notification>,
    onNotificationClick: (Notification) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = notifications,
            key = { it.notificationId }
        ) { notification ->
            NotificationItem(
                notification = notification,
                onClick = { onNotificationClick(notification) },
                onDeleteClick = { onDeleteClick(notification.notificationId) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            }
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Notification icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(getNotificationColor(notification.type).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (notification.senderProfileUrl != null) {
                    AsyncImage(
                        model = notification.senderProfileUrl,
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = getNotificationIcon(notification.type),
                        contentDescription = null,
                        tint = getNotificationColor(notification.type),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatTimestamp(notification.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete button
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete notification") },
            text = { Text("Are you sure you want to delete this notification?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmptyNotificationsView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No notifications",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You're all caught up!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Error loading notifications",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun getNotificationIcon(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.STATUS_SUBMITTED_TO_IN_PROGRESS,
        NotificationType.STATUS_IN_PROGRESS_TO_RESOLVED,
        NotificationType.STATUS_RESOLVED_TO_CLOSED -> Icons.Default.Update

        NotificationType.NEW_COMMENT,
        NotificationType.COMMENT_REPLY -> Icons.Default.Comment

        NotificationType.POST_LIKED -> Icons.Default.Favorite
        NotificationType.POST_UPVOTED -> Icons.Default.ThumbUp
        NotificationType.COMMENT_LIKED -> Icons.Default.FavoriteBorder

        NotificationType.SIMILAR_ISSUE_NEARBY -> Icons.Default.LocationOn
        NotificationType.TRENDING_ISSUE -> Icons.Default.TrendingUp

        NotificationType.NEW_DEVICE_LOGIN,
        NotificationType.SUSPICIOUS_LOGIN -> Icons.Default.Security

        NotificationType.PROFILE_UPDATE,
        NotificationType.EMAIL_CHANGED,
        NotificationType.PHONE_CHANGED -> Icons.Default.Person

        NotificationType.SYSTEM_ANNOUNCEMENT -> Icons.Default.Announcement
        NotificationType.OTHER -> Icons.Default.Notifications
    }
}

@Composable
private fun getNotificationColor(type: NotificationType): androidx.compose.ui.graphics.Color {
    return when (type) {
        NotificationType.STATUS_SUBMITTED_TO_IN_PROGRESS,
        NotificationType.STATUS_IN_PROGRESS_TO_RESOLVED,
        NotificationType.STATUS_RESOLVED_TO_CLOSED -> MaterialTheme.colorScheme.primary

        NotificationType.NEW_COMMENT,
        NotificationType.COMMENT_REPLY -> MaterialTheme.colorScheme.tertiary

        NotificationType.POST_LIKED,
        NotificationType.POST_UPVOTED,
        NotificationType.COMMENT_LIKED -> MaterialTheme.colorScheme.error

        NotificationType.SIMILAR_ISSUE_NEARBY,
        NotificationType.TRENDING_ISSUE -> MaterialTheme.colorScheme.secondary

        NotificationType.NEW_DEVICE_LOGIN,
        NotificationType.SUSPICIOUS_LOGIN -> MaterialTheme.colorScheme.error

        NotificationType.PROFILE_UPDATE,
        NotificationType.EMAIL_CHANGED,
        NotificationType.PHONE_CHANGED -> MaterialTheme.colorScheme.primary

        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

