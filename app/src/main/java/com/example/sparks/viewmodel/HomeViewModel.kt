package com.example.sparks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sparks.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatListItem(
    val id: String,
    val username: String,
    val lastMessage: String,
    val timestamp: String,
    val timestampLong: Long, // For sorting
    val profilePictureUrl: String?,
    val archivedFor: List<String> = emptyList(),
    val isGroup: Boolean = false
)

class HomeViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _chats = MutableStateFlow<List<ChatListItem>>(emptyList())
    val chats = _chats.asStateFlow()

    // 1. Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    // 2. Update activeChats to include filtering
    // Replace the EXISTING 'activeChats' definition with this one:
    val activeChats = combine(_chats, _searchQuery) { chats, query ->
        chats.filter { chat ->
            val uid = auth.currentUser?.uid ?: ""
            val isNotArchived = !chat.archivedFor.contains(uid)
            val matchesQuery = chat.username.contains(query, ignoreCase = true) ||
                    chat.lastMessage.contains(query, ignoreCase = true)

            isNotArchived && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val archivedChats = _chats.map { list ->
        list.filter {
            val uid = auth.currentUser?.uid ?: ""
            it.archivedFor.contains(uid)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    init {
        listenToChats()
    }

    private fun listenToChats() {
        val currentUser = auth.currentUser ?: return

        // Listen to any conversation where the current user is a participant
        firestore.collection("conversations")
            .whereArrayContains("users", currentUser.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                if (snapshot != null) {
                    viewModelScope.launch {
                        val chatItems = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.id
                                val lastMessage = doc.getString("lastMessage") ?: ""
                                val timestampLong = doc.getLong("timestamp") ?: 0L
                                val timestamp = formatTime(timestampLong)
                                val isGroup = doc.getBoolean("isGroup") == true

                                // FIX: Fetch the archived list safely
                                val archivedFor = doc.get("archivedFor") as? List<String> ?: emptyList()

                                var name = "Chat"
                                var image: String? = null

                                if (isGroup) {
                                    // 1. GROUP CHAT LOGIC
                                    name = doc.getString("groupName") ?: "Group"
                                } else {
                                    // 2. DIRECT CHAT LOGIC
                                    val users = doc.get("users") as? List<String> ?: emptyList()
                                    val otherId = users.firstOrNull { it != currentUser.uid }

                                    if (otherId != null) {
                                        val userDoc = firestore.collection("users").document(otherId).get().await()
                                        val user = userDoc.toObject(User::class.java)
                                        name = user?.firstName ?: "Unknown"
                                        if (user?.lastName?.isNotEmpty() == true) name += " ${user.lastName}"
                                        image = user?.profileImageUrl
                                    } else {
                                        name = "Note to Self"
                                    }
                                }

                                // FIX: Pass variables in correct order: id, name, msg, time, timeLong, img, ARCHIVED, isGroup
                                ChatListItem(id, name, lastMessage, timestamp, timestampLong, image, archivedFor, isGroup)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        // Sort by newest first
                        _chats.value = chatItems.sortedByDescending { it.timestampLong }
                    }
                }
            }
    }

    fun deleteChat(chatId: String, forEveryone: Boolean) {
        val currentUser = auth.currentUser ?: return

        if (forEveryone) {
            firestore.collection("conversations").document(chatId).delete()
        } else {
            firestore.collection("conversations").document(chatId)
                .update("users", com.google.firebase.firestore.FieldValue.arrayRemove(currentUser.uid))
        }
    }

    // 2. Toggle Archive Action
    fun toggleArchive(chatId: String, archive: Boolean) {
        val currentUser = auth.currentUser ?: return
        val docRef = firestore.collection("conversations").document(chatId)

        if (archive) {
            docRef.update("archivedFor", com.google.firebase.firestore.FieldValue.arrayUnion(currentUser.uid))
        } else {
            docRef.update("archivedFor", com.google.firebase.firestore.FieldValue.arrayRemove(currentUser.uid))
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}