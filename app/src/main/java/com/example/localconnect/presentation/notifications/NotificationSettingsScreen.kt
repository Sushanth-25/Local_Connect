package com.example.localconnect.presentation.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localconnect.data.model.NotificationPreferences
import com.example.localconnect.viewmodel.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationViewModel = viewModel()
) {
    val preferences by viewModel.preferences.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            preferences?.let { prefs ->
                // Master Switch
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Push Notifications",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SwitchPreference(
                            title = "Enable push notifications",
                            subtitle = "Receive notifications on this device",
                            checked = prefs.pushEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.updatePreferences(prefs.copy(pushEnabled = enabled))
                            }
                        )
                    }
                }

                // Issue Status Updates
                PreferenceSection(
                    title = "Issue Updates",
                    preferences = prefs,
                    enabled = prefs.pushEnabled,
                    items = listOf(
                        PreferenceItem(
                            title = "Status updates",
                            subtitle = "When your issue status changes",
                            checked = prefs.statusUpdates,
                            onCheckedChange = { enabled ->
                                viewModel.updatePreferences(prefs.copy(statusUpdates = enabled))
                            }
                        )
                    )
                )

                // Comments
                PreferenceSection(
                    title = "Comments & Replies",
                    preferences = prefs,
                    enabled = prefs.pushEnabled,
                    items = listOf(
                        PreferenceItem(
                            title = "Comments",
                            subtitle = "When someone comments on your post",
                            checked = prefs.comments,
                            onCheckedChange = { enabled ->
                                viewModel.updatePreferences(prefs.copy(comments = enabled))
                            }
                        )
                    )
                )

                // Likes & Upvotes
                PreferenceSection(
                    title = "Engagement",
                    preferences = prefs,
                    enabled = prefs.pushEnabled,
                    items = listOf(
                        PreferenceItem(
                            title = "Likes & upvotes",
                            subtitle = "When someone likes or upvotes your post",
                            checked = prefs.likes,
                            onCheckedChange = { enabled ->
                                viewModel.updatePreferences(prefs.copy(likes = enabled))
                            }
                        )
                    )
                )

                // Trending & Similar
                PreferenceSection(
                    title = "Discovery",
                    preferences = prefs,
                    enabled = prefs.pushEnabled,
                    items = listOf(
                        PreferenceItem(
                            title = "Trending issues",
                            subtitle = "Similar issues nearby and trending alerts",
                            checked = prefs.trending,
                            onCheckedChange = { enabled ->
                                viewModel.updatePreferences(prefs.copy(trending = enabled))
                            }
                        )
                    )
                )

                // Security
                PreferenceSection(
                    title = "Security",
                    preferences = prefs,
                    enabled = prefs.pushEnabled,
                    items = listOf(
                        PreferenceItem(
                            title = "Security alerts",
                            subtitle = "New device logins and suspicious activity",
                            checked = prefs.security,
                            onCheckedChange = { enabled ->
                                viewModel.updatePreferences(prefs.copy(security = enabled))
                            }
                        )
                    )
                )

                // Profile Changes
                PreferenceSection(
                    title = "Account",
                    preferences = prefs,
                    enabled = prefs.pushEnabled,
                    items = listOf(
                        PreferenceItem(
                            title = "Profile changes",
                            subtitle = "Email, phone, and profile updates",
                            checked = prefs.profileChanges,
                            onCheckedChange = { enabled ->
                                viewModel.updatePreferences(prefs.copy(profileChanges = enabled))
                            }
                        )
                    )
                )

                // Email Notifications
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Email Notifications",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SwitchPreference(
                            title = "Email notifications",
                            subtitle = "Receive notifications via email",
                            checked = prefs.emailEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.updatePreferences(prefs.copy(emailEnabled = enabled))
                            }
                        )
                    }
                }

                // Info card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "About Notifications",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "You can manage which notifications you receive. Some security notifications cannot be disabled for your account protection.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PreferenceSection(
    title: String,
    preferences: NotificationPreferences,
    enabled: Boolean,
    items: List<PreferenceItem>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            items.forEach { item ->
                SwitchPreference(
                    title = item.title,
                    subtitle = item.subtitle,
                    checked = item.checked,
                    enabled = enabled,
                    onCheckedChange = item.onCheckedChange
                )

                if (item != items.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SwitchPreference(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

private data class PreferenceItem(
    val title: String,
    val subtitle: String,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)

