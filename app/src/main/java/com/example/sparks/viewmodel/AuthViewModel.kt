package com.example.sparks.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sparks.model.User
import com.example.sparks.util.CryptoManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // --- NEW: ROBUST TOKEN SAVER ---
    private fun fetchAndSaveFcmToken() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val uid = auth.currentUser?.uid ?: return@addOnCompleteListener

                // Try to update existing doc
                firestore.collection("users").document(uid)
                    .update("fcmToken", token)
                    .addOnFailureListener {
                        // If doc missing, create it with just the token
                        firestore.collection("users").document(uid)
                            .set(mapOf("fcmToken" to token), SetOptions.merge())
                    }
            }
        }
    }

    // Helper function to save/update user in Firestore
    private suspend fun saveUserToFirestore(user: FirebaseUser) {
        val userMap = hashMapOf(
            "uid" to user.uid,
            "email" to user.email,
            "username" to (user.email?.substringBefore("@") ?: "User")
        )

        try {
            firestore.collection("users")
                .document(user.uid)
                .set(userMap, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error saving user", e)
        }
    }

    // 1. Function to check if a username exists
    suspend fun isUsernameAvailable(username: String): Boolean {
        val snapshot = FirebaseFirestore.getInstance().collection("users")
            .whereEqualTo("username", username)
            .get()
            .await()
        return snapshot.isEmpty
    }

    // 2. Function to save the full profile
    fun createProfile(firstName: String, lastName: String, username: String, onSuccess: () -> Unit) {
        val user = auth.currentUser ?: return

        viewModelScope.launch {
            _isLoading.value = true

            // Check availability first
            if (!isUsernameAvailable(username)) {
                _errorMessage.value = "Username already taken."
                _isLoading.value = false
                return@launch
            }

            // Generate E2EE Keys for new user
            val myPublicKey = CryptoManager.checkOrGenerateKeys()

            val newUser = User(
                uid = user.uid,
                email = user.email ?: "",
                username = username,
                firstName = firstName,
                lastName = lastName,
                publicKey = myPublicKey
            )

            try {
                // Save to Firestore
                // NOTE: This .set() overwrites data, so we must re-save token after!
                FirebaseFirestore.getInstance().collection("users")
                    .document(user.uid)
                    .set(newUser)
                    .await()

                // RE-SAVE TOKEN NOW (Crucial step)
                fetchAndSaveFcmToken()

                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Function to Upload Profile Picture
    fun uploadProfilePicture(imageUri: Uri) {
        val user = auth.currentUser ?: return
        val uid = user.uid

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val storageRef = storage.reference.child("profile_images/$uid.jpg")
                storageRef.putFile(imageUri).await()
                val downloadUrl = storageRef.downloadUrl.await()

                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .update("profileImageUrl", downloadUrl.toString())
                    .await()

            } catch (e: Exception) {
                _errorMessage.value = "Upload failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signUp(email: String, pass: String, onSignUpSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                auth.createUserWithEmailAndPassword(email, pass).await()

                // SAVE TOKEN IMMEDIATELY
                fetchAndSaveFcmToken()

                _currentUser.value = auth.currentUser
                onSignUpSuccess()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                val user = result.user
                if (user != null) {
                    saveUserToFirestore(user)
                    _currentUser.value = user

                    // SAVE TOKEN ON LOGIN
                    fetchAndSaveFcmToken()

                    // Ensure E2EE Keys exist
                    val publicKey = CryptoManager.checkOrGenerateKeys()
                    if (publicKey != null) {
                        firestore.collection("users").document(user.uid)
                            .update("publicKey", publicKey)
                            .addOnFailureListener {
                                // If user doc doesn't exist for some reason, create partial
                                firestore.collection("users").document(user.uid)
                                    .set(mapOf("publicKey" to publicKey), SetOptions.merge())
                            }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Login Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        auth.signOut()
        _currentUser.value = null
    }
}