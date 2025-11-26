package com.example.localconnect.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.localconnect.MainActivity
import com.example.localconnect.R
import com.example.localconnect.data.model.Notification
import com.example.localconnect.data.model.NotificationType
import com.example.localconnect.data.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LocalConnectMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationRepository = NotificationRepository()

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "local_connect_notifications"
        private const val CHANNEL_NAME = "Local Connect Notifications"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")

        // Save the new token to Firestore
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            saveTokenToFirestore(userId, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Handle data payload
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataPayload(remoteMessage.data)
        }

        // Handle notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message notification body: ${it.body}")
            showNotification(
                title = it.title ?: "Local Connect",
                message = it.body ?: "",
                data = remoteMessage.data
            )
        }
    }

    private fun handleDataPayload(data: Map<String, String>) {
        val userId = data["userId"] ?: return
        val type = data["type"] ?: NotificationType.OTHER.value
        val title = data["title"] ?: ""
        val message = data["message"] ?: ""
        val postId = data["postId"]
        val commentId = data["commentId"]
        val senderId = data["senderId"]
        val senderName = data["senderName"]

        // Create notification in Firestore
        serviceScope.launch {
            val notification = Notification(
                userId = userId,
                type = NotificationType.fromString(type),
                title = title,
                message = message,
                postId = postId,
                commentId = commentId,
                senderId = senderId,
                senderName = senderName,
                timestamp = System.currentTimeMillis()
            )

            notificationRepository.createNotification(notification)
        }

        // Show local notification
        showNotification(title, message, data)
    }

    private fun showNotification(title: String, message: String, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

            // Add navigation data
            data["postId"]?.let { putExtra("postId", it) }
            data["notificationId"]?.let { putExtra("notificationId", it) }
            data["type"]?.let { putExtra("notificationType", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(notificationSound)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Local Connect updates"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(
            System.currentTimeMillis().toInt(),
            notificationBuilder.build()
        )
    }

    private fun saveTokenToFirestore(userId: String, token: String) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d(TAG, "FCM token saved to Firestore")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving FCM token", e)
            }
    }
}

