package com.example.sparks.model

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "", // Unique handle (e.g. @chris)
    val firstName: String = "", // Display Name
    val lastName: String = "",
    val profileImageUrl: String? = null,
    val bio: String = "",
    // Privacy & Security
    val publicKey: String? = null,
    val blockedIds: List<String> = emptyList(),

    // Notifications
    val fcmToken: String? = null, // <--- ADD THIS

    // NEW FIELDS (Fixes the Warnings)
    val followingIds: List<String> = emptyList(),
    @field:JvmField // Helps Firestore map "is" booleans correctly
    val isOnline: Boolean = false,
    val lastActive: Long = 0L
)