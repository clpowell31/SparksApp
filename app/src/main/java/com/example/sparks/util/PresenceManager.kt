package com.example.sparks.util

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PresenceManager {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // NEW: Track where the user is
    var isAppInForeground: Boolean = false
    var currentChatId: String? = null // Null if not in a chat

    fun setOnlineStatus(isOnline: Boolean) {
        isAppInForeground = isOnline // Sync these
        val uid = auth.currentUser?.uid ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestore.collection("users").document(uid)
                    .update(
                        mapOf(
                            "isOnline" to isOnline,
                            "lastActive" to System.currentTimeMillis()
                        )
                    )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}