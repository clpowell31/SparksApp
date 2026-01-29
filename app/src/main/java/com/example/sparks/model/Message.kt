package com.example.sparks.model

enum class MessageType {
    TEXT, IMAGE, AUDIO // New Type
}

// 1. Add Status Enum
enum class MessageStatus {
    SENT, DELIVERED, READ
}

data class Message(
    val id: String = "",
    val text: String = "",
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
    val replyToImage: String? = null // If replying to an image
)