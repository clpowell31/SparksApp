package com.example.sparks.viewmodel

import android.util.Log
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.sparks.model.User

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

    // Helper function to save/update user in Firestore
    private suspend fun saveUserToFirestore(user: FirebaseUser) {
        val userMap = hashMapOf(
            "uid" to user.uid,
            "email" to user.email,
            "username" to (user.email?.substringBefore("@") ?: "User")
        )

        // Use SetOptions.merge() so we don't overwrite existing data if we add more fields later
        try {
            firestore.collection("users")
                .document(user.uid)
                .set(userMap, SetOptions.merge())
                .await()
            Log.d("AuthViewModel", "User saved to Firestore: ${user.email}")
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

            val newUser = User(
                uid = user.uid,
                email = user.email ?: "",
                username = username,
                firstName = firstName,
                lastName = lastName
            )

            try {
                // Save to Firestore
                FirebaseFirestore.getInstance().collection("users")
                    .document(user.uid)
                    .set(newUser)
                    .await()

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
                // 1. Create a reference in Storage: "profile_images/uid.jpg"
                val storageRef = storage.reference.child("profile_images/$uid.jpg")

                // 2. Upload the file
                storageRef.putFile(imageUri).await()

                // 3. Get the Download URL
                val downloadUrl = storageRef.downloadUrl.await()

                // 4. Update Firestore User Document
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .update("profileImageUrl", downloadUrl.toString())
                    .await()

                // 5. Update Local State (Optional, but good for immediate UI feedback)
                // We rely on the UI observing Firestore or reloading, but let's refresh current user context if needed.
                // For now, Firestore real-time listeners in the UI will handle the visual update.

            } catch (e: Exception) {
                _errorMessage.value = "Upload failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // MODIFY Sign Up: Don't write to Firestore yet!
    // We will let the Profile Screen do that.
    fun signUp(email: String, pass: String, onSignUpSuccess: () -> Unit) { // Added callback
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                auth.createUserWithEmailAndPassword(email, pass).await()
                // Do NOT create Firestore doc here anymore.
                // Just update state and trigger callback
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
                    saveUserToFirestore(user) // Save to DB (Fixes missing users)
                    _currentUser.value = user
                }
            } catch (e: Exception) {
                _errorMessage.value = "Login Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
    }
}