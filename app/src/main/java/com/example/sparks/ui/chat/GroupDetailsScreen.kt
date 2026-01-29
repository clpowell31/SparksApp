package com.example.sparks.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sparks.viewmodel.ChatViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    navController: NavController,
    participantIds: String, // Comma-separated IDs passed from nav
    viewModel: ChatViewModel = viewModel()
) {
    var groupName by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    val ids = remember { participantIds.split(",").filter { it.isNotBlank() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New group") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (groupName.isNotBlank()) {
                FloatingActionButton(
                    onClick = {
                        isCreating = true
                        val ids = participantIds.split(",")
                        viewModel.createGroup(groupName, ids) { chatId ->
                            // On Success: Go to the new Chat
                            navController.navigate("chat/$chatId") {
                                // Remove the creation screens from backstack so "Back" goes to Home
                                popUpTo("home")
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Check, contentDescription = "Create")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Group Avatar Placeholder
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Groups,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Name Input
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group name (required)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Members: ${participantIds.split(",").size}", color = Color.Gray)
        }
    }
}