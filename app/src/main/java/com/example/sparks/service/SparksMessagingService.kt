package com.example.sparks.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.sparks.MainActivity
import com.example.sparks.R
import com.example.sparks.util.CryptoManager
import com.example.sparks.util.PresenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class SparksMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 1. DATA PAYLOAD (Real Chat Messages)
        if (remoteMessage.data.isNotEmpty()) {
            val data = remoteMessage.data

            val chatId = data["chatId"] ?: return
            val senderId = data["senderId"] ?: return
            val senderName = data["senderName"] ?: "Someone"
            val encryptedContent = data["encryptedContent"] ?: ""
            val encryptedKey = data["encryptedKey"] ?: ""

            // Launch Coroutine to decrypt and check blocking settings
            CoroutineScope(Dispatchers.IO).launch {
                handleIncomingMessage(chatId, senderId, senderName, encryptedContent, encryptedKey)
            }
        }

        // 2. NOTIFICATION PAYLOAD (Firebase Console Tests)
        remoteMessage.notification?.let {
            val title = it.title ?: "Sparks"
            val body = it.body ?: "New Message"
            val chatId = remoteMessage.data["chatId"] ?: "test_chat_id"
            showNotification(chatId, title, body)
        }
    }

    private suspend fun handleIncomingMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        encryptedContent: String,
        encryptedKey: String
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        // If not logged in, show generic notification
        if (currentUser == null) {
            showNotification(chatId, "Sparks", "You have a new message")
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        var shouldShow = true

        // 1. CHECK BLOCKING & MUTING (Fail-Safe)
        try {
            // Check Blocking
            val myDoc = firestore.collection("users").document(currentUser.uid).get().await()
            val blockedIds = myDoc.get("blockedIds") as? List<String> ?: emptyList()
            if (blockedIds.contains(senderId)) {
                Log.d("FCM", "Blocked user $senderId attempted to message.")
                shouldShow = false
            }

            // Check Muting
            if (shouldShow) {
                val chatDoc = firestore.collection("conversations").document(chatId).get().await()
                if (chatDoc.exists()) {
                    val mutedBy = chatDoc.get("mutedBy") as? List<String> ?: emptyList()
                    if (mutedBy.contains(currentUser.uid)) {
                        Log.d("FCM", "Chat $chatId is muted.")
                        shouldShow = false
                    }
                }
            }
        } catch (e: Exception) {
            // If DB check fails (e.g. offline), DEFAULT TO SHOWING NOTIFICATION
            Log.e("FCM", "Failed to check block/mute status", e)
            shouldShow = true
        }

        if (!shouldShow) return

        // 2. DECRYPT CONTENT
        var bodyText = "🔒 Encrypted Message"

        if (encryptedKey.isNotEmpty() && encryptedContent.isNotEmpty()) {
            try {
                // IMPORTANT: Use the key sent specifically for ME
                val decrypted = CryptoManager.decryptMessage(encryptedContent, encryptedKey)
                if (!decrypted.startsWith("Error")) {
                    bodyText = decrypted
                }
            } catch (e: Exception) {
                Log.e("FCM", "Decryption failed", e)
                bodyText = "🔒 New Message (Decryption Error)"
            }
        } else {
            // Fallback if backend didn't send keys
            bodyText = "🔒 New Message"
        }

        // 3. SHOW IT
        showNotification(chatId, senderName, bodyText)
    }

    private fun showNotification(chatId: String, title: String, messageBody: String) {
        // Foreground Check
        if (PresenceManager.isAppInForeground && PresenceManager.currentChatId == chatId) {
            return
        }

        val channelId = "sparks_chat_channel"
        val notificationId = Random.nextInt() // Unique ID

        // Deep Link Intent
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("chatId", chatId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ensure this resource exists
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun saveTokenToFirestore(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .update("fcmToken", token)
            .addOnFailureListener { Log.e("FCM", "Token save failed", it) }
    }
}