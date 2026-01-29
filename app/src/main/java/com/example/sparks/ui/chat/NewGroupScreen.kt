package com.example.sparks.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
fun NewGroupScreen(
    navController: NavController,
    onNextClick: (List<String>) -> Unit // Pass selected User IDs to next screen
) {
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    // Track selected User IDs
    val selectedIds = remember { mutableStateListOf<String>() }
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Fetch users (Same logic as NewChat)
    LaunchedEffect(Unit) {
        val snapshot = FirebaseFirestore.getInstance().collection("users").get().await()
        val allUsers = snapshot.toObjects(User::class.java)
        users = allUsers.filter { it.uid != currentUser?.uid }
            .sortedBy { it.firstName }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("New group")
                        Text("${selectedIds.size} selected", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            // Only show Next button if at least 1 person is selected
            if (selectedIds.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { onNextClick(selectedIds) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(users) { user ->
                val isSelected = selectedIds.contains(user.uid)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isSelected) selectedIds.remove(user.uid) else selectedIds.add(user.uid)
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (user.profileImageUrl != null) {
                            AsyncImage(model = user.profileImageUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                        } else {
                            Text(user.firstName.take(1), color = Color.White)
                        }

                        // Selection Checkmark Overlay
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "${user.firstName} ${user.lastName}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )

                    // Checkbox Circle
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Circle,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray
                    )
                }
            }
        }
    }
}