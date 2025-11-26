package com.example.localconnect.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.localconnect.data.model.Notification
import com.example.localconnect.data.model.NotificationPreferences
import com.example.localconnect.data.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class NotificationUiState {
    object Loading : NotificationUiState()
    data class Success(val notifications: List<Notification>) : NotificationUiState()
    data class Error(val message: String) : NotificationUiState()
}

class NotificationViewModel : ViewModel() {

    private val notificationRepository = NotificationRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow<NotificationUiState>(NotificationUiState.Loading)
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _preferences = MutableStateFlow<NotificationPreferences?>(null)
    val preferences: StateFlow<NotificationPreferences?> = _preferences.asStateFlow()

    companion object {
        private const val TAG = "NotificationViewModel"
    }

    init {
        loadNotifications()
        loadUnreadCount()
        loadPreferences()
    }

    /**
     * Load user notifications
     */
    private fun loadNotifications() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                notificationRepository.getUserNotifications(userId).collect { notifications ->
                    _uiState.value = NotificationUiState.Success(notifications)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Job was cancelled, this is normal during navigation
                Log.d(TAG, "Notification loading cancelled (normal during navigation)")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading notifications", e)
                _uiState.value = NotificationUiState.Error(e.message ?: "Failed to load notifications")
            }
        }
    }

    /**
     * Load unread notification count
     */
    private fun loadUnreadCount() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                notificationRepository.getUnreadCount(userId).collect { count ->
                    _unreadCount.value = count
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Job was cancelled, this is normal during navigation
                Log.d(TAG, "Unread count loading cancelled (normal during navigation)")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading unread count", e)
            }
        }
    }

    /**
     * Load notification preferences
     */
    private fun loadPreferences() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                notificationRepository.getNotificationPreferencesFlow(userId).collect { prefs ->
                    _preferences.value = prefs
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Job was cancelled, this is normal during navigation - just ignore
                Log.d(TAG, "Preference loading cancelled (normal during navigation)")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading preferences", e)
            }
        }
    }

    /**
     * Mark notification as read
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }

    /**
     * Mark all notifications as read
     */
    fun markAllAsRead() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            notificationRepository.markAllAsRead(userId)
        }
    }

    /**
     * Delete notification
     */
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
        }
    }

    /**
     * Delete all notifications
     */
    fun deleteAllNotifications() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            notificationRepository.deleteAllNotifications(userId)
        }
    }

    /**
     * Update notification preferences
     */
    fun updatePreferences(preferences: NotificationPreferences) {
        viewModelScope.launch {
            notificationRepository.updateNotificationPreferences(preferences)
        }
    }

    /**
     * Refresh notifications
     */
    fun refresh() {
        loadNotifications()
    }
}

