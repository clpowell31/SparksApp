package com.example.sparks.model

enum class MessageType {
    TEXT, IMAGE, AUDIO, VIDEO // New Type
}

// 1. Add Status Enum
enum class MessageStatus {
    SENT, DELIVERED, READ
}

data class Message(
    val id: String = "",
    val text: String = "", // This will now hold Encrypted Content (IV:Cipher)
    val senderId: String = "",
    val timestamp: Long = 0L,
    val type: MessageType = MessageType.TEXT,
    val imageUrl: String? = null,
    val status: MessageStatus = MessageStatus.SENT,
    val reactions: Map<String, String> = emptyMap(),
    val deletedFor: List<String> = emptyList(),
    val replyToId: String? = null,
    val replyToName: String? = null,
    val replyToText: String? = null,
    val replyToImage: String? = null, // If replying to an image
    // NEW: Map of UserID -> Encrypted AES Key
    val encryptionKeys: Map<String, String> = emptyMap(),
    // NEW: Disappearing Messages
    val expiresAt: Long? = null // Timestamp when this message should vanish
)