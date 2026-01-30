package com.example.sparks.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sparks.model.Story
import com.example.sparks.model.StoryType
import com.example.sparks.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

// Helper class to group stories by user
data class UserStoryGroup(
    val user: User,
    val stories: List<Story>
)

class StoriesViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // --- STATE ---
    private val _storyGroups = MutableStateFlow<List<UserStoryGroup>>(emptyList())
    val storyGroups = _storyGroups.asStateFlow()

    private val _myStories = MutableStateFlow<List<Story>>(emptyList())
    val myStories = _myStories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Listeners (to avoid leaks)
    private var storiesListener: ListenerRegistration? = null
    private var followingListener: ListenerRegistration? = null

    // Cache
    private var myFollowingIds = emptyList<String>()

    init {
        listenToMyFollowingList()
    }

    // 1. Listen to who I am following (Real-time)
    private fun listenToMyFollowingList() {
        val currentUser = auth.currentUser ?: return

        followingListener = firestore.collection("users").document(currentUser.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                if (snapshot != null && snapshot.exists()) {
                    val following = snapshot.get("followingIds") as? List<String> ?: emptyList()
                    myFollowingIds = following
                    // Restart story listener with new follow list
                    listenToStories()
                }
            }
    }

    // 2. Listen to Active Stories (Real-time)
    private fun listenToStories() {
        val currentUser = auth.currentUser ?: return
        val now = System.currentTimeMillis()

        // Remove old listener if exists
        storiesListener?.remove()

        _isLoading.value = true

        storiesListener = firestore.collection("stories")
            .whereGreaterThan("expiresAt", now)
            .orderBy("expiresAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("StoriesViewModel", "Listen failed.", e)
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val allStories = snapshot.toObjects(Story::class.java)
                    processStories(allStories, currentUser.uid)
                }
                _isLoading.value = false
            }
    }

    // 3. Process and Group
    private fun processStories(allStories: List<Story>, myId: String) {
        viewModelScope.launch {
            // A. Separate My Stories
            val mine = allStories.filter { it.userId == myId }
            _myStories.value = mine.sortedBy { it.timestamp }

            // B. Filter "Others" based on Following
            val others = allStories.filter {
                it.userId != myId && (myFollowingIds.contains(it.userId))
            }

            // C. Group by User ID
            val grouped = others.groupBy { it.userId }
            val groupsList = mutableListOf<UserStoryGroup>()

            // D. Fetch User Profiles for the groups
            grouped.forEach { (uid, userStories) ->
                try {
                    val userDoc = firestore.collection("users").document(uid).get().await()
                    var user = userDoc.toObject(User::class.java)

                    // FALLBACK: If user profile missing/error, create a dummy one
                    if (user == null) {
                        user = User(uid, "Unknown", "User", profileImageUrl = null)
                    }

                    groupsList.add(UserStoryGroup(user, userStories.sortedBy { it.timestamp }))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            _storyGroups.value = groupsList
        }
    }

    fun uploadStory(uri: Uri, type: StoryType, caption: String? = null) {
        val currentUser = auth.currentUser ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get User Details
                val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                val user = userDoc.toObject(User::class.java)
                val userName = if (user != null) "${user.firstName} ${user.lastName}" else "User"

                // Upload Media
                val ext = if (type == StoryType.VIDEO) "mp4" else "jpg"
                val filename = "${UUID.randomUUID()}.$ext"
                val ref = storage.reference.child("stories/${currentUser.uid}/$filename")

                ref.putFile(uri).await()
                val downloadUrl = ref.downloadUrl.await().toString()

                // Create Story
                val now = System.currentTimeMillis()
                val expiresAt = now + (24 * 60 * 60 * 1000) // 24 Hours

                val story = Story(
                    id = UUID.randomUUID().toString(),
                    userId = currentUser.uid,
                    userName = userName,
                    userAvatar = user?.profileImageUrl,
                    mediaUrl = downloadUrl,
                    type = type,
                    caption = caption,
                    timestamp = now,
                    expiresAt = expiresAt
                )

                firestore.collection("stories").document(story.id).set(story).await()

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // NEW: Mark a specific story as viewed by the current user
    fun markStoryAsViewed(storyId: String) {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            // "arrayUnion" adds the ID only if it's not already there
            firestore.collection("stories").document(storyId)
                .update("viewers", com.google.firebase.firestore.FieldValue.arrayUnion(uid))
                .addOnFailureListener { e -> e.printStackTrace() }
        }
    }

    fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""

    override fun onCleared() {
        super.onCleared()
        storiesListener?.remove()
        followingListener?.remove()
    }
}