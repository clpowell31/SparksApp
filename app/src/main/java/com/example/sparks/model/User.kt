package com.example.sparks.model

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "", // Unique handle (e.g. @chris)
    val firstName: String = "", // Display Name
    val lastName: String = "",
    val profileImageUrl: String? = null,
    val bio: String = "",
    val publicKey: String? = null // <--- NEW FIELD
)