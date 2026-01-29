package com.example.sparks.ui.chat

import android.R.attr.onClick
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.sparks.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(navController: NavController) {
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Fetch users
    LaunchedEffect(Unit) {
        val snapshot = FirebaseFirestore.getInstance().collection("users").get().await()
        val allUsers = snapshot.toObjects(User::class.java)
        // Sort alphabetically by first name
        users = allUsers.filter { it.uid != currentUser?.uid }
            .sortedBy { it.firstName.uppercase() }
    }

    // Filter users based on search
    val filteredUsers = if (searchQuery.isBlank()) {
        users
    } else {
        users.filter {
            it.firstName.contains(searchQuery, ignoreCase = true) ||
                    it.lastName.contains(searchQuery, ignoreCase = true) ||
                    it.username.contains(searchQuery, ignoreCase = true)
        }
    }

    // Group users by first letter
    val groupedUsers = filteredUsers.groupBy { it.firstName.firstOrNull()?.uppercase() ?: "#" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New message") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, contentDescription = "Menu") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // 1. Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Name, username or number", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // 2. Static Options (Only show if not searching, or keep them? Signal keeps them usually)
                if (searchQuery.isBlank()) {
                    item {
                        ActionItem(
                            icon = Icons.Default.GroupAdd,
                            title = "New group",
                            // ADD CLICK HANDLER:
                            onClick = { navController.navigate("new_group_select") }
                        )
                        ActionItem(Icons.Default.AlternateEmail, "Find by username")
                        ActionItem(Icons.Default.Dialpad, "Find by phone number")

                        // "Note to Self" is effectively a chat with yourself
                        UserItem(
                            user = User(
                                uid = currentUser?.uid ?: "",
                                firstName = "Note to Self",
                                username = "note_to_self"
                            ),
                            isNoteToSelf = true,
                            onClick = {
                                // Chat ID for self is just "myUid_myUid" or handled specifically
                                val myId = currentUser?.uid ?: ""
                                navController.navigate("chat/${myId}_${myId}")
                            }
                        )
                    }
                }

                // 3. User List with Headers
                groupedUsers.forEach { (initial, userList) ->
                    item {
                        Text(
                            text = initial.toString(),
                            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(userList) { user ->
                        UserItem(
                            user = user,
                            onClick = {
                                val myId = currentUser?.uid ?: ""
                                val otherId = user.uid
                                val chatId = if (myId < otherId) "${myId}_${otherId}" else "${otherId}_${myId}"
                                navController.navigate("chat/$chatId")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit = {} // Added default empty lambda so other items don't break
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() } // Use the passed click handler
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun UserItem(user: User, isNoteToSelf: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isNoteToSelf) MaterialTheme.colorScheme.primaryContainer else Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            if (isNoteToSelf) {
                Icon(Icons.Default.Note, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            } else if (user.profileImageUrl != null) {
                AsyncImage(
                    model = user.profileImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = user.firstName.firstOrNull()?.toString() ?: "?",
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isNoteToSelf) "Note to Self" else "${user.firstName} ${user.lastName}".trim(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                if (isNoteToSelf) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("(You)", fontSize = 12.sp, color = Color.Gray)
                }
            }

            if (!isNoteToSelf && user.username.isNotBlank()) {
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}