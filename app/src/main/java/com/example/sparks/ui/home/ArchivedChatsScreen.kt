package com.example.sparks.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sparks.viewmodel.ChatListItem
import com.example.sparks.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedChatsScreen(
    navController: NavController,
    viewModel: HomeViewModel // Pass the SAME ViewModel instance
) {
    val archivedList by viewModel.archivedChats.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archived Chats") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (archivedList.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No archived chats", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(items = archivedList, key = { it.id }) { chat ->

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                                // Swipe ANY direction to Unarchive
                                viewModel.toggleArchive(chat.id, archive = false)
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Gray)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Icon(Icons.Default.Unarchive, contentDescription = "Unarchive", tint = Color.White)
                            }
                        },
                        content = {
                            Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                                ChatListItem(
                                    username = chat.username,
                                    lastMessage = chat.lastMessage,
                                    timestamp = chat.timestamp,
                                    profileUrl = chat.profilePictureUrl,
                                    onClick = { /* Optional: Navigate to chat? */ }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}