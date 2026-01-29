package com.example.sparks.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sparks.model.Message
import com.example.sparks.model.MessageStatus
import com.example.sparks.model.MessageType
import com.example.sparks.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class ChatViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    // State for the Chat Header
    private val _chatMetadata = MutableStateFlow<Pair<String, String?>>("Chat" to null) // Name, ImageUrl
    val chatMetadata = _chatMetadata.asStateFlow()

    // Typing State
    private val _remoteTypingUsers = MutableStateFlow<List<String>>(emptyList())
    val remoteTypingUsers = _remoteTypingUsers.asStateFlow()

    private var typingJob: Job? = null // To handle debouncing

    // 1. Store Member Names (Key: UserID, Value: FirstName)
    private val _groupMembers = MutableStateFlow<Map<String, String>>(emptyMap())
    val groupMembers = _groupMembers.asStateFlow()

    // State to track if we are currently composing a reply
    private val _replyingTo = MutableStateFlow<Message?>(null)
    val replyingTo = _replyingTo.asStateFlow()

    fun setReplyingTo(message: Message?) {
        _replyingTo.value = message
    }

    // --- THE SINGLE, CORRECT SEND MESSAGE FUNCTION ---
    fun sendMessage(chatId: String, text: String, chatName: String) {
        val currentUser = auth.currentUser ?: return
        val replyMessage = _replyingTo.value

        viewModelScope.launch {
            try {
                // 1. Ensure the parent chat document exists (Fixes "Ghost Document" issues)
                val chatParentData = hashMapOf("lastActive" to System.currentTimeMillis())
                firestore.collection("chats").document(chatId)
                    .set(chatParentData, com.google.firebase.firestore.SetOptions.merge())

                // 2. Create the Message Object (With Reply Data if available)
                val messageId = firestore.collection("chats").document(chatId).collection("messages").document().id
                val message = Message(
                    id = messageId,
                    senderId = currentUser.uid,
                    text = text,
                    timestamp = System.currentTimeMillis(),
                    status = MessageStatus.SENT,
                    type = MessageType.TEXT,
                    // Attach Reply Data
                    replyToId = replyMessage?.id,
                    replyToName = if (replyMessage != null) chatName else null,
                    replyToText = replyMessage?.text,
                    replyToImage = replyMessage?.imageUrl
                )

                // 3. Save Message to Firestore
                firestore.collection("chats").document(chatId).collection("messages")
                    .document(messageId)
                    .set(message)

                // 4. Update Conversation Summary (Last Message)
                val conversationData = hashMapOf<String, Any>(
                    "lastMessage" to text,
                    "timestamp" to message.timestamp
                )

                // FIX: Only update 'users' for Direct Chats (check for underscore).
                // For Groups, we MUST NOT touch the 'users' field, or we wipe out the members!
                if (chatId.contains("_")) {
                    conversationData["users"] = listOf(currentUser.uid, getOtherUserId(chatId, currentUser.uid))
                }

                firestore.collection("conversations").document(chatId)
                    .set(conversationData, com.google.firebase.firestore.SetOptions.merge())

                // 5. Clear reply state
                _replyingTo.value = null

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 2. Fetch Members (Call this when opening a group chat)
    fun fetchGroupMembers(chatId: String) {
        viewModelScope.launch {
            try {
                val convDoc = firestore.collection("conversations").document(chatId).get().await()
                val userIds = convDoc.get("users") as? List<String> ?: emptyList()

                if (userIds.isNotEmpty()) {
                    val usersMap = mutableMapOf<String, String>()
                    userIds.forEach { uid ->
                        val userDoc = firestore.collection("users").document(uid).get().await()
                        val firstName = userDoc.getString("firstName") ?: "Unknown"
                        usersMap[uid] = firstName
                    }
                    _groupMembers.value = usersMap
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 1. LISTEN: Who is typing?
    fun listenToTyping(chatId: String) {
        firestore.collection("chats").document(chatId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val typingList = snapshot.get("typing") as? List<String> ?: emptyList()
                val currentUser = auth.currentUser?.uid ?: return@addSnapshotListener
                _remoteTypingUsers.value = typingList.filter { it != currentUser }
            }
    }

    // 2. ACT: I am typing (with debounce)
    fun onUserInputChanged(chatId: String, text: String) {
        if (text.isEmpty()) {
            updateTypingStatus(chatId, false)
            return
        }
        typingJob?.cancel()
        updateTypingStatus(chatId, true)
        typingJob = viewModelScope.launch {
            delay(3000)
            updateTypingStatus(chatId, false)
        }
    }

    private fun updateTypingStatus(chatId: String, isTyping: Boolean) {
        val currentUser = auth.currentUser ?: return
        val docRef = firestore.collection("chats").document(chatId)

        if (isTyping) {
            docRef.update("typing", com.google.firebase.firestore.FieldValue.arrayUnion(currentUser.uid))
                .addOnFailureListener {
                    docRef.set(hashMapOf("typing" to listOf(currentUser.uid)), com.google.firebase.firestore.SetOptions.merge())
                }
        } else {
            docRef.update("typing", com.google.firebase.firestore.FieldValue.arrayRemove(currentUser.uid))
        }
    }

    fun listenToMessages(chatId: String) {
        firestore.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val msgs = snapshot.toObjects(Message::class.java)
                    _messages.value = msgs
                    markMessagesAsRead(chatId, msgs)
                }
            }
    }

    private fun markMessagesAsRead(chatId: String, messages: List<Message>) {
        val currentUser = auth.currentUser ?: return
        val unreadMessages = messages.filter {
            it.senderId != currentUser.uid && it.status != MessageStatus.READ
        }

        if (unreadMessages.isNotEmpty()) {
            val batch = firestore.batch()
            for (msg in unreadMessages) {
                val ref = firestore.collection("chats").document(chatId)
                    .collection("messages").document(msg.id)
                batch.update(ref, "status", MessageStatus.READ)
            }
            batch.commit()
        }
    }

    fun sendImageMessage(chatId: String, imageUri: Uri, otherUserName: String) {
        val currentUser = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                val filename = "${System.currentTimeMillis()}_${currentUser.uid}.jpg"
                val storageRef = storage.reference.child("chat_images/$chatId/$filename")
                storageRef.putFile(imageUri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                val messageId = firestore.collection("chats").document(chatId).collection("messages").document().id
                val message = Message(
                    id = messageId,
                    text = "Photo",
                    senderId = currentUser.uid,
                    timestamp = System.currentTimeMillis(),
                    type = MessageType.IMAGE,
                    imageUrl = downloadUrl,
                    status = MessageStatus.SENT
                )

                firestore.collection("chats").document(chatId).collection("messages")
                    .document(messageId).set(message)

                updateConversationSummary(chatId, "Photo", message.timestamp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendAudioMessage(chatId: String, audioFile: File, chatName: String) {
        val currentUser = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                val filename = "${System.currentTimeMillis()}_${currentUser.uid}.mp3"
                val storageRef = storage.reference.child("chat_audio/$chatId/$filename")
                val uri = Uri.fromFile(audioFile)

                storageRef.putFile(uri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                val messageId = firestore.collection("chats").document(chatId).collection("messages").document().id
                val message = Message(
                    id = messageId,
                    text = "Voice Message",
                    senderId = currentUser.uid,
                    timestamp = System.currentTimeMillis(),
                    type = MessageType.AUDIO,
                    status = MessageStatus.SENT,
                    imageUrl = downloadUrl
                )

                firestore.collection("chats").document(chatId).collection("messages")
                    .document(messageId).set(message)

                updateConversationSummary(chatId, "Voice Message", message.timestamp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createGroup(groupName: String, participantIds: List<String>, onSuccess: (String) -> Unit) {
        val currentUser = auth.currentUser ?: return

        viewModelScope.launch {
            try {
                val groupId = firestore.collection("conversations").document().id
                val allParticipants = participantIds + currentUser.uid

                val groupData = hashMapOf(
                    "id" to groupId,
                    "isGroup" to true,
                    "groupName" to groupName,
                    "groupOwnerId" to currentUser.uid,
                    "users" to allParticipants,
                    "lastMessage" to "Group created",
                    "timestamp" to System.currentTimeMillis()
                )
                firestore.collection("conversations").document(groupId).set(groupData).await()

                firestore.collection("chats").document(groupId).set(
                    hashMapOf("created" to System.currentTimeMillis())
                ).await()

                val systemMsg = Message(
                    id = firestore.collection("chats").document().id,
                    text = "You created the group \"$groupName\"",
                    senderId = "system",
                    timestamp = System.currentTimeMillis(),
                    status = MessageStatus.READ
                )
                firestore.collection("chats").document(groupId).collection("messages").add(systemMsg)

                onSuccess(groupId)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchChatDetails(chatId: String) {
        val currentUser = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                val doc = firestore.collection("conversations").document(chatId).get().await()
                val isGroup = doc.getBoolean("isGroup") == true

                if (isGroup) {
                    val name = doc.getString("groupName") ?: "Group"
                    _chatMetadata.value = name to null
                } else {
                    val users = doc.get("users") as? List<String> ?: emptyList()
                    val otherId = users.firstOrNull { it != currentUser.uid }

                    if (otherId != null) {
                        val userDoc = firestore.collection("users").document(otherId).get().await()
                        val user = userDoc.toObject(User::class.java)
                        val name = "${user?.firstName} ${user?.lastName}".trim()
                        val image = user?.profileImageUrl
                        _chatMetadata.value = name to image
                    } else {
                        _chatMetadata.value = "Note to Self" to null
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun updateConversationSummary(chatId: String, text: String, timestamp: Long) {
        val conversationRef = firestore.collection("conversations").document(chatId)
        val data = hashMapOf(
            "lastMessage" to text,
            "timestamp" to timestamp
        )
        conversationRef.set(data, com.google.firebase.firestore.SetOptions.merge())
    }

    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }

    private fun getOtherUserId(chatId: String, currentUserId: String): String {
        val parts = chatId.split("_")
        return if (parts.size == 2) {
            if (parts[0] == currentUserId) parts[1] else parts[0]
        } else {
            chatId
        }
    }

    fun toggleReaction(chatId: String, messageId: String, emoji: String) {
        val currentUser = auth.currentUser ?: return
        val msgRef = firestore.collection("chats").document(chatId).collection("messages").document(messageId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(msgRef)
            val currentReactions = snapshot.get("reactions") as? Map<String, String> ?: emptyMap()

            val newReactions = currentReactions.toMutableMap()
            if (newReactions[currentUser.uid] == emoji) {
                newReactions.remove(currentUser.uid)
            } else {
                newReactions[currentUser.uid] = emoji
            }

            transaction.update(msgRef, "reactions", newReactions)
        }
    }

    fun sendSticker(chatId: String, stickerUrl: String) {
        val currentUser = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                val messageId = firestore.collection("chats").document(chatId).collection("messages").document().id
                val message = Message(
                    id = messageId,
                    text = "Sticker", // Fallback text
                    senderId = currentUser.uid,
                    timestamp = System.currentTimeMillis(),
                    type = MessageType.IMAGE, // Treat as image
                    imageUrl = stickerUrl,
                    status = MessageStatus.SENT
                )

                // Save to Firestore
                firestore.collection("chats").document(chatId).collection("messages")
                    .document(messageId).set(message)

                // Update Last Message
                updateConversationSummary(chatId, "Sticker", message.timestamp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteMessage(chatId: String, message: Message, forEveryone: Boolean) {
        val currentUser = auth.currentUser ?: return
        val docRef = firestore.collection("chats").document(chatId).collection("messages").document(message.id)

        if (forEveryone) {
            val updates = mapOf(
                "text" to "🚫 This message was deleted",
                "type" to "TEXT",
                "imageUrl" to null
            )
            docRef.update(updates)
        } else {
            docRef.update("deletedFor", com.google.firebase.firestore.FieldValue.arrayUnion(currentUser.uid))
        }
    }
}