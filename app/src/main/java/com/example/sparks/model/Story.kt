package com.example.sparks.model

enum class StoryType { IMAGE, VIDEO }

data class Story(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",     // Denormalized for faster loading
    val userAvatar: String? = null,
    val mediaUrl: String = "",
    val type: StoryType = StoryType.IMAGE,
    val caption: String? = null,
    val timestamp: Long = 0L,
    val expiresAt: Long = 0L,

    // Privacy / Engagement
    val viewers: List<String> = emptyList() // List of User IDs who viewed it
)