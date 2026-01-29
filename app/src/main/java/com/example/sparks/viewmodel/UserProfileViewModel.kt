package com.example.sparks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sparks.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _user = MutableStateFlow<User?>(null)
    val user = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        fetchUserProfile()
    }

    private fun fetchUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val document = firestore.collection("users").document(uid).get().await()
                _user.value = document.toObject(User::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Function to update Name and Bio
    fun updateUserProfile(firstName: String, lastName: String, bio: String, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val updates = mapOf(
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "bio" to bio
                )
                firestore.collection("users").document(uid).update(updates).await()
                // Refresh local data
                fetchUserProfile()
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    // Note: Photo update logic remains separate as you've already implemented it.
}