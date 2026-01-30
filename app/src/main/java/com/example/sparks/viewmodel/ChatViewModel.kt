package com.example.sparks.viewmodel

import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sparks.model.Message
import com.example.sparks.model.MessageStatus
import com.example.sparks.model.MessageType
import com.example.sparks.model.User
import com.example.sparks.util.CryptoManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

class ChatViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // --- STATE ---
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private var allMessagesCache: List<Message> = emptyList()

    private val _chatMetadata = MutableStateFlow<Pair<String, String?>>("Chat" to null)
    val chatMetadata = _chatMetadata.asStateFlow()

    private val _remoteTypingUsers = MutableStateFlow<List<String>>(emptyList())
    val remoteTypingUsers = _remoteTypingUsers.asStateFlow()
    private var typingJob: Job? = null

    private val _groupMembers = MutableStateFlow<Map<String, String>>(emptyMap())
    val groupMembers = _groupMembers.asStateFlow()
    private val _memberKeys = mutableMapOf<String, String>()

    private val _replyingTo = MutableStateFlow<Message?>(null)
    val replyingTo = _replyingTo.asStateFlow()

    private val _disappearingDuration = MutableStateFlow<Long>(0L)
    val disappearingDuration = _disappearingDuration.asStateFlow()

    private var expirationTickerJob: Job? = null
    private var conversationListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null
    private var typingListener: ListenerRegistration? = null

    private val _decryptedContentCache = mutableStateMapOf<String, Uri>()
    val decryptedContentCache = _decryptedContentCache

    fun setReplyingTo(message: Message?) {
        _replyingTo.value = message
    }

    private fun startExpirationTicker() {
        expirationTickerJob?.cancel()
        expirationTickerJob = viewModelScope.launch {
            while (isActive) {
                filterExpiredMessages()
                delay(5000)
            }
        }
    }

    private fun stopExpirationTicker() {
        expirationTickerJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        stopExpirationTicker()
        conversationListener?.remove()
        messagesListener?.remove()
        typingListener?.remove()
    }

    @Suppress("UNCHECKED_CAST")
    fun listenToMessages(chatId: String) {
        startExpirationTicker()

        conversationListener = firestore.collection("conversations").document(chatId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val durationSeconds = snapshot.getLong("disappearingDuration") ?: 0L
                _disappearingDuration.value = durationSeconds * 1000L
            }

        messagesListener = firestore.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val currentUser = auth.currentUser ?: return@addSnapshotListener
                    val msgs = snapshot.toObjects(Message::class.java).map { msg ->
                        decryptMessageIfNeeded(msg, currentUser.uid)
                    }
                    allMessagesCache = msgs
                    filterExpiredMessages()
                    markMessagesAsRead(chatId, msgs)
                }
            }
    }

    @Suppress("UNCHECKED_CAST")
    fun listenToTyping(chatId: String) {
        typingListener = firestore.collection("chats").document(chatId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val typingList = snapshot.get("typing") as? List<String> ?: emptyList()
                val currentUser = auth.currentUser?.uid ?: return@addSnapshotListener
                _remoteTypingUsers.value = typingList.filter { it != currentUser }
            }
    }

    private fun filterExpiredMessages() {
        val now = System.currentTimeMillis()
        val validMessages = allMessagesCache.filter { msg ->
            msg.expiresAt == null || msg.expiresAt > now
        }
        _messages.value = validMessages
    }

    private fun decryptMessageIfNeeded(msg: Message, currentUserId: String): Message {
        if (msg.encryptionKeys.containsKey(currentUserId)) {
            return try {
                val encryptedAesKey = msg.encryptionKeys[currentUserId]!!
                val decryptedText = CryptoManager.decryptMessage(msg.text, encryptedAesKey)
                msg.copy(text = decryptedText)
            } catch (e: Exception) {
                msg.copy(text = "⚠️ Decryption Error")
            }
        }
        return msg
    }

    fun fetchChatDetails(chatId: String) {
        viewModelScope.launch {
            val currentUser = auth.currentUser ?: return@launch

            val isOneOnOne = chatId.contains("_") && !chatId.startsWith("group_")

            if (isOneOnOne) {
                val ids = chatId.split("_")
                val otherUserId = ids.firstOrNull { it != currentUser.uid }

                if (otherUserId != null) {
                    firestore.collection("users").document(otherUserId).get()
                        .addOnSuccessListener { document ->
                            val user = document.toObject(User::class.java)
                            if (user != null) {
                                val fullName = "${user.firstName} ${user.lastName}"
                                _chatMetadata.value = Pair(fullName, user.profileImageUrl)
                            }
                        }
                }
            } else {
                val doc = firestore.collection("conversations").document(chatId).get().await()
                if (doc.exists()) {
                    val name = doc.getString("groupName") ?: "Chat"
                    val image = doc.getString("imageUrl")
                    _chatMetadata.value = Pair(name, image)
                }
            }
        }
    }

    fun sendMessage(chatId: String, text: String, chatName: String) {
        val currentUser = auth.currentUser ?: return
        val replyMessage = _replyingTo.value
        val currentDuration = _disappearingDuration.value

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // A. Encryption
                val aesKey = CryptoManager.generateAesKey()
                val encryptedText = CryptoManager.encryptMessageWithAes(text, aesKey)
                val encryptionKeysMap = mutableMapOf<String, String>()

                val myPubKeyStr = CryptoManager.getMyPublicKeyString()
                if (myPubKeyStr != null) {
                    val myPubKey = CryptoManager.parsePublicKey(myPubKeyStr)
                    encryptionKeysMap[currentUser.uid] = CryptoManager.wrapAesKeyForRecipient(aesKey, myPubKey)
                }

                _memberKeys.forEach { (userId, pubKeyStr) ->
                    if (userId != currentUser.uid) {
                        val otherPubKey = CryptoManager.parsePublicKey(pubKeyStr)
                        encryptionKeysMap[userId] = CryptoManager.wrapAesKeyForRecipient(aesKey, otherPubKey)
                    }
                }

                // B. Expiration
                val expiresAt = if (currentDuration > 0) System.currentTimeMillis() + currentDuration else null

                // C. Construct Message
                val messageId = firestore.collection("chats").document(chatId).collection("messages").document().id
                val message = Message(
                    id = messageId,
                    senderId = currentUser.uid,
                    text = encryptedText,
                    timestamp = System.currentTimeMillis(),
                    status = MessageStatus.SENT,
                    type = MessageType.TEXT,
                    encryptionKeys = encryptionKeysMap,
                    replyToId = replyMessage?.id,
                    replyToName = if (replyMessage != null) chatName else null,
                    replyToText = replyMessage?.text,
                    replyToImage = replyMessage?.imageUrl,
                    expiresAt = expiresAt
                )

                // D. Upload Message
                firestore.collection("chats").document(chatId).collection("messages").document(messageId).set(message)

                // E. FIX: Update Parent Conversation
                // we must NOT save 'text' here, or the preview will be readable!
                val conversationRef = firestore.collection("conversations").document(chatId)
                val ids = if (chatId.contains("_")) chatId.split("_") else listOf(currentUser.uid)

                val updates = hashMapOf<String, Any>(
                    "lastMessage" to "🔒 Encrypted Message", // <--- CHANGED FROM 'text'
                    "timestamp" to System.currentTimeMillis()
                )
                if (chatId.contains("_")) {
                    updates["users"] = ids
                }

                conversationRef.set(updates, SetOptions.merge())
                _replyingTo.value = null

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateDisappearingDuration(chatId: String, seconds: Long) {
        firestore.collection("conversations").document(chatId)
            .update("disappearingDuration", seconds)
    }

    @Suppress("UNCHECKED_CAST")
    fun fetchGroupMembers(chatId: String) {
        viewModelScope.launch {
            try {
                val convDoc = firestore.collection("conversations").document(chatId).get().await()
                val userIds = convDoc.get("users") as? List<String> ?: emptyList()
                if (userIds.isNotEmpty()) {
                    val usersMap = mutableMapOf<String, String>()
                    userIds.forEach { uid ->
                        val userDoc = firestore.collection("users").document(uid).get().await()
                        usersMap[uid] = userDoc.getString("firstName") ?: "Unknown"
                        val pubKey = userDoc.getString("publicKey")
                        if (pubKey != null) _memberKeys[uid] = pubKey
                    }
                    _groupMembers.value = usersMap
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun sendImageMessage(chatId: String, imageUri: Uri, context: android.content.Context) {
        val currentUser = auth.currentUser ?: return
        val currentDuration = _disappearingDuration.value

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mediaKey = CryptoManager.generateAesKey()
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val imageBytes = inputStream?.readBytes() ?: return@launch
                inputStream.close()

                val encryptedBytes = CryptoManager.encryptBytes(imageBytes, mediaKey)
                val filename = "${System.currentTimeMillis()}_enc.jpg"
                val storageRef = storage.reference.child("chat_images/$chatId/$filename")
                storageRef.putBytes(encryptedBytes).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                val encryptionKeysMap = mutableMapOf<String, String>()
                val myPubKeyStr = CryptoManager.getMyPublicKeyString()
                if (myPubKeyStr != null) {
                    val myPubKey = CryptoManager.parsePublicKey(myPubKeyStr)
                    encryptionKeysMap[currentUser.uid] = CryptoManager.wrapAesKeyForRecipient(mediaKey, myPubKey)
                }

                _memberKeys.forEach { (userId, pubKeyStr) ->
                    val otherPubKey = CryptoManager.parsePublicKey(pubKeyStr)
                    encryptionKeysMap[userId] = CryptoManager.wrapAesKeyForRecipient(mediaKey, otherPubKey)
                }

                val expiresAt = if (currentDuration > 0) System.currentTimeMillis() + currentDuration else null

                val messageId = firestore.collection("chats").document(chatId).collection("messages").document().id
                val message = Message(
                    id = messageId,
                    text = CryptoManager.encryptMessageWithAes("📷 Encrypted Photo", mediaKey),
                    senderId = currentUser.uid,
                    timestamp = System.currentTimeMillis(),
                    type = MessageType.IMAGE,
                    imageUrl = downloadUrl,
                    status = MessageStatus.SENT,
                    encryptionKeys = encryptionKeysMap,
                    expiresAt = expiresAt
                )

                firestore.collection("chats").document(chatId).collection("messages").document(messageId).set(message)
                updateConversationSummary(chatId, "📷 Photo", message.timestamp)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resolveMedia(message: Message, context: android.content.Context) {
        if (_decryptedContentCache.containsKey(message.id)) return
        val currentUser = auth.currentUser ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!message.encryptionKeys.containsKey(currentUser.uid)) return@launch
                val encryptedAesKey = message.encryptionKeys[currentUser.uid]!!
                val mediaKey = CryptoManager.unwrapAesKey(encryptedAesKey) ?: return@launch

                val httpsReference = storage.getReferenceFromUrl(message.imageUrl!!)
                val tempEncryptedFile = File.createTempFile("enc_", ".tmp", context.cacheDir)
                httpsReference.getFile(tempEncryptedFile).await()

                val encryptedBytes = tempEncryptedFile.readBytes()
                val decryptedBytes = CryptoManager.decryptBytes(encryptedBytes, mediaKey)

                val ext = if (message.type == MessageType.VIDEO) "mp4" else "jpg"
                val decryptedFile = File(context.cacheDir, "dec_${message.id}.$ext")
                decryptedFile.writeBytes(decryptedBytes)

                tempEncryptedFile.delete()
                _decryptedContentCache[message.id] = Uri.fromFile(decryptedFile)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendAudioMessage(chatId: String, audioFile: File, chatName: String) {
        val currentUser = auth.currentUser ?: return
        val currentDuration = _disappearingDuration.value

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mediaKey = CryptoManager.generateAesKey()
                val audioBytes = audioFile.readBytes()
                val encryptedBytes = CryptoManager.encryptBytes(audioBytes, mediaKey)

                val filename = "${System.currentTimeMillis()}_enc.mp3"
                val storageRef = storage.reference.child("chat_audio/$chatId/$filename")
                storageRef.putBytes(encryptedBytes).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                val encryptionKeysMap = mutableMapOf<String, String>()
                val myPubKeyStr = CryptoManager.getMyPublicKeyString()
                if (myPubKeyStr != null) {
                    val myPubKey = CryptoManager.parsePublicKey(myPubKeyStr)
                    encryptionKeysMap[currentUser.uid] = CryptoManager.wrapAesKeyForRecipient(mediaKey, myPubKey)
                }
                _memberKeys.forEach { (userId, pubKeyStr) ->
                    val otherPubKey = CryptoManager.parsePublicKey(pubKeyStr)
                    encryptionKeysMap[userId] = CryptoManager.wrapAesKeyForRecipient(mediaKey, otherPubKey)
                }

                val expiresAt = if (currentDuration > 0) System.currentTimeMillis() + currentDuration else null

                val messageId = firestore.collection("chats").document(chatId).collection("messages").document().id
                val message = Message(
                    id = messageId,
                    text = CryptoManager.encryptMessageWithAes("🎤 Encrypted Audio", mediaKey),
                    senderId = currentUser.uid,
                    timestamp = System.currentTimeMillis(),
                    type = MessageType.AUDIO,
                    status = MessageStatus.SENT,
                    imageUrl = downloadUrl,
                    encryptionKeys = encryptionKeysMap,
                    expiresAt = expiresAt
                )

                firestore.collection("chats").document(chatId).collection("messages").document(messageId).set(message)
                updateConversationSummary(chatId, "🎤 Voice Message", message.timestamp)

            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun sendVideoMessage(chatId: String, videoUri: Uri, context: android.content.Context) {
        val currentUser = auth.currentUser ?: return
        val currentDuration = _disappearingDuration.value

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mediaKey = CryptoManager.generateAesKey()
                val inputStream = context.contentResolver.openInputStream(videoUri)
                val videoBytes = inputStream?.readBytes() ?: return@launch
                inputStream.close()
                val encryptedBytes = CryptoManager.encryptBytes(videoBytes, mediaKey)

                val filename = "${System.currentTimeMillis()}_enc.mp4"
                val storageRef = storage.reference.child("chat_videos/$chatId/$filename")
                storageRef.putBytes(encryptedBytes).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                val encryptionKeysMap = mutableMapOf<String, String>()
                val myPubKeyStr = CryptoManager.getMyPublicKeyString()
                if (myPubKeyStr != null) {
                    val myPubKey = CryptoManager.parsePublicKey(myPubKeyStr)
                    encryptionKeysMap[currentUser.uid] = CryptoManager.wrapAesKeyForRecipient(mediaKey, myPubKey)
                }
                _memberKeys.forEach { (userId, pubKeyStr) ->
                    val otherPubKey = CryptoManager.parsePublicKey(pubKeyStr)
                    encryptionKeysMap[userId] = CryptoManager.wrapAesKeyForRecipient(mediaKey, otherPubKey)
                }

                val messageId = firestore.collection("chats").document(chatId).collection("messages").document().id
                val message = Message(
                    id = messageId,
                    text = CryptoManager.encryptMessageWithAes("🎥 Encrypted Video", mediaKey),
                    senderId = currentUser.uid,
                    timestamp = System.currentTimeMillis(),
                    type = MessageType.VIDEO,
                    status = MessageStatus.SENT,
                    imageUrl = downloadUrl,
                    encryptionKeys = encryptionKeysMap,
                    expiresAt = if (currentDuration > 0) System.currentTimeMillis() + currentDuration else null
                )

                firestore.collection("chats").document(chatId).collection("messages").document(messageId).set(message)
                updateConversationSummary(chatId, "🎥 Video", message.timestamp)

            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun sendSticker(chatId: String, stickerUrl: String) {
        val currentUser = auth.currentUser ?: return
        val currentDuration = _disappearingDuration.value
        viewModelScope.launch {
            try {
                val expiresAt = if (currentDuration > 0) System.currentTimeMillis() + currentDuration else null

                val messageId = firestore.collection("chats").document(chatId).collection("messages").document().id
                val message = Message(
                    id = messageId, text = "Sticker", senderId = currentUser.uid, timestamp = System.currentTimeMillis(),
                    type = MessageType.IMAGE, imageUrl = stickerUrl, status = MessageStatus.SENT,
                    expiresAt = expiresAt
                )
                firestore.collection("chats").document(chatId).collection("messages").document(messageId).set(message)
                updateConversationSummary(chatId, "Sticker", message.timestamp)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // 1. FIXED: Made Public
    fun onUserInputChanged(chatId: String, text: String) {
        if (text.isEmpty()) { updateTypingStatus(chatId, false); return }
        typingJob?.cancel()
        updateTypingStatus(chatId, true)
        typingJob = viewModelScope.launch { delay(3000); updateTypingStatus(chatId, false) }
    }

    private fun updateTypingStatus(chatId: String, isTyping: Boolean) {
        val currentUser = auth.currentUser ?: return
        val update = if (isTyping) FieldValue.arrayUnion(currentUser.uid) else FieldValue.arrayRemove(currentUser.uid)
        firestore.collection("chats").document(chatId).update("typing", update)
            .addOnFailureListener {
                if (isTyping) {
                    firestore.collection("chats").document(chatId).set(hashMapOf("typing" to listOf(currentUser.uid)), SetOptions.merge())
                }
            }
    }

    private fun markMessagesAsRead(chatId: String, messages: List<Message>) {
        val currentUser = auth.currentUser ?: return
        val unreadMessages = messages.filter { it.senderId != currentUser.uid && it.status != MessageStatus.READ }
        if (unreadMessages.isNotEmpty()) {
            val batch = firestore.batch()
            for (msg in unreadMessages) {
                val ref = firestore.collection("chats").document(chatId).collection("messages").document(msg.id)
                batch.update(ref, "status", MessageStatus.READ)
            }
            batch.commit()
        }
    }

    private fun updateConversationSummary(chatId: String, text: String, timestamp: Long) {
        val conversationRef = firestore.collection("conversations").document(chatId)
        val data = hashMapOf("lastMessage" to text, "timestamp" to timestamp)
        conversationRef.set(data, SetOptions.merge())
    }

    fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""

    fun toggleReaction(chatId: String, messageId: String, emoji: String) {
        val currentUser = auth.currentUser ?: return
        val msgRef = firestore.collection("chats").document(chatId).collection("messages").document(messageId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(msgRef)
            @Suppress("UNCHECKED_CAST")
            val currentReactions = snapshot.get("reactions") as? Map<String, String> ?: emptyMap()
            val newReactions = currentReactions.toMutableMap()
            if (newReactions[currentUser.uid] == emoji) newReactions.remove(currentUser.uid) else newReactions[currentUser.uid] = emoji
            transaction.update(msgRef, "reactions", newReactions)
        }
    }

    fun deleteMessage(chatId: String, message: Message, forEveryone: Boolean) {
        val currentUser = auth.currentUser ?: return
        val docRef = firestore.collection("chats").document(chatId).collection("messages").document(message.id)
        if (forEveryone) {
            val updates = mapOf("text" to "🚫 This message was deleted", "type" to "TEXT", "imageUrl" to null)
            docRef.update(updates)
        } else {
            docRef.update("deletedFor", FieldValue.arrayUnion(currentUser.uid))
        }
    }

    // 2. FIXED: Added Public 'createGroup' function
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
                    "timestamp" to System.currentTimeMillis(),
                    "disappearingDuration" to 0L
                )

                // Create Conversations Doc
                firestore.collection("conversations").document(groupId).set(groupData).await()
                // Create Chat Parent Doc
                firestore.collection("chats").document(groupId).set(hashMapOf("created" to System.currentTimeMillis())).await()

                onSuccess(groupId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}