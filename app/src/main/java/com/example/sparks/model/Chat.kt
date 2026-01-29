package com.example.sparks.model

data class Chat(
    val id: String,
    val username: String,
    val lastMessage: String,
    val timestamp: String,
    val profilePictureUrl: String? = null,
    val unreadCount: Int = 0
)