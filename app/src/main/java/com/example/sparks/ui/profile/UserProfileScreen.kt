package com.example.sparks.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.sparks.viewmodel.UserProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    navController: NavController,
    // Assume you have a separate viewmodel/function for photo upload from previous steps
    onPhotoClicked: () -> Unit = {},
    viewModel: UserProfileViewModel = viewModel()
) {
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Local state for editing form fields
    var firstName by remember(user) { mutableStateOf(user?.firstName ?: "") }
    var lastName by remember(user) { mutableStateOf(user?.lastName ?: "") }
    var bio by remember(user) { mutableStateOf(user?.bio ?: "") }

    // Toggle between "View Mode" and "Edit Mode"
    var isEditing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditing) {
                        // SAVE BUTTON
                        IconButton(enabled = !isLoading, onClick = {
                            viewModel.updateUserProfile(firstName, lastName, bio) {
                                isEditing = false // Exit edit mode on success
                            }
                        }) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Check, contentDescription = "Save")
                            }
                        }
                    } else {
                        // EDIT BUTTON
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (user == null && isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- Profile Picture Section ---
                Box(contentAlignment = Alignment.BottomEnd) {
                    if (user?.profileImageUrl != null) {
                        AsyncImage(
                            model = user?.profileImageUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.size(120.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.Gray)
                        }
                    }
                    // Edit photo button
                    SmallFloatingActionButton(
                        onClick = onPhotoClicked,
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit photo", modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                SuggestionChip(onClick = onPhotoClicked, label = { Text("Edit photo") })
                Spacer(modifier = Modifier.height(32.dp))

                // --- Editable Fields ---

                // 1. Name Field(s)
                if (isEditing) {
                    OutlinedTextField(
                        value = firstName, onValueChange = { firstName = it },
                        label = { Text("First Name") }, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = lastName, onValueChange = { lastName = it },
                        label = { Text("Last Name") }, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                } else {
                    ProfileItem(
                        icon = Icons.Default.Person,
                        title = "${user?.firstName} ${user?.lastName}".trim().ifBlank { "No Name Set" }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Bio / About Field
                if (isEditing) {
                    OutlinedTextField(
                        value = bio, onValueChange = { bio = it },
                        label = { Text("About") },
                        placeholder = { Text("Write a few words about yourself") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                } else {
                    ProfileItem(
                        icon = Icons.Outlined.Info, // Using Info icon for bio
                        title = bio.ifBlank { "Write a few words about yourself" },
                        subtitle = if (bio.isBlank()) "Tap edit to add a bio" else null
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Read-Only Fields ---

                // Badges (Static placeholder)
                ProfileItem(icon = Icons.Outlined.Badge, title = "Badges")

                Divider(modifier = Modifier.padding(vertical = 24.dp))

                // Username
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Text("@", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(24.dp))
                    Column {
                        Text("Username", style = MaterialTheme.typography.bodyLarge)
                        Text("@${user?.username ?: "none"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "People can now message you using your optional username so you don't have to give out your phone number.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

// Helper Composable for static profile items
@Composable
fun ProfileItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(24.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}