package com.example.localconnect.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable empty state component
 * Shows when no content is available with helpful messages and actions
 */
@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    message: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = iconTint
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text(actionText)
                }
            }
        }
    }
}

/**
 * Error state with retry button
 */
@Composable
fun ErrorStateView(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, title, message) = when {
        error.contains("network", ignoreCase = true) ||
        error.contains("internet", ignoreCase = true) ->
            Triple(
                Icons.Default.CloudOff,
                "No Internet Connection",
                "Please check your internet connection and try again"
            )

        error.contains("timeout", ignoreCase = true) ->
            Triple(
                Icons.Default.HourglassEmpty,
                "Request Timed Out",
                "The request took too long. Please try again"
            )

        error.contains("permission", ignoreCase = true) ->
            Triple(
                Icons.Default.Block,
                "Permission Required",
                error
            )

        else ->
            Triple(
                Icons.Default.ErrorOutline,
                "Something Went Wrong",
                error
            )
    }

    EmptyStateView(
        icon = icon,
        title = title,
        message = message,
        actionText = "Try Again",
        onActionClick = onRetry,
        modifier = modifier,
        iconTint = MaterialTheme.colorScheme.error
    )
}

/**
 * No posts found state
 */
@Composable
fun NoPostsView(
    isLocalTab: Boolean,
    needsLocation: Boolean,
    onEnableLocation: (() -> Unit)? = null,
    onCreatePost: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    when {
        needsLocation -> {
            EmptyStateView(
                icon = Icons.Default.LocationOff,
                title = "Location Required",
                message = "Enable location to see posts within 30km of your area",
                actionText = "Enable Location",
                onActionClick = onEnableLocation,
                modifier = modifier,
                iconTint = MaterialTheme.colorScheme.error
            )
        }

        isLocalTab -> {
            EmptyStateView(
                icon = Icons.Default.NearMe,
                title = "No Local Posts",
                message = "No posts found within 30km. Be the first to post in your area!",
                actionText = "Create Post",
                onActionClick = onCreatePost,
                modifier = modifier
            )
        }

        else -> {
            EmptyStateView(
                icon = Icons.Default.PostAdd,
                title = "No Posts Yet",
                message = "Be the first to share something with the community",
                actionText = "Create First Post",
                onActionClick = onCreatePost,
                modifier = modifier
            )
        }
    }
}

/**
 * Loading state with message
 */
@Composable
fun LoadingStateView(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * No search results
 */
@Composable
fun NoSearchResultsView(
    query: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateView(
        icon = Icons.Default.SearchOff,
        title = "No Results Found",
        message = "We couldn't find any posts matching \"$query\"",
        actionText = "Clear Search",
        onActionClick = onClearSearch,
        modifier = modifier
    )
}

/**
 * No notifications
 */
@Composable
fun NoNotificationsView(
    modifier: Modifier = Modifier
) {
    EmptyStateView(
        icon = Icons.Default.NotificationsNone,
        title = "All Caught Up!",
        message = "You don't have any notifications right now",
        modifier = modifier
    )
}

/**
 * Generic success state with icon and message
 */
@Composable
fun SuccessStateView(
    icon: ImageVector = Icons.Default.CheckCircle,
    title: String,
    message: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    EmptyStateView(
        icon = icon,
        title = title,
        message = message,
        actionText = actionText,
        onActionClick = onActionClick,
        modifier = modifier,
        iconTint = Color(0xFF4CAF50) // Success green
    )
}

