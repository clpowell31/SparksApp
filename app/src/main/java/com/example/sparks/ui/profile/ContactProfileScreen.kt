package com.example.sparks.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.sparks.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactProfileScreen(
    navController: NavController,
    userId: String
) {
    var user by remember { mutableStateOf<User?>(null) }
    val context = LocalContext.current

    // States
    var showBlockDialog by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showDisappearingDialog by remember { mutableStateOf(false) }

    var nicknameText by remember { mutableStateOf("") }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val firestore = FirebaseFirestore.getInstance()

    // Disappearing Messages State (Default: Off)
    var selectedDisappearingLabel by remember { mutableStateOf("Off") }

    // Defines the options you requested
    val disappearingOptions = listOf(
        "Off",
        "4 weeks",
        "1 week",
        "96 hours", // 4 days
        "24 hours", // 1 day
        "1 hour",
        "30 minutes",
        "10 minutes"
    )

    // Add this helper to generate the Chat ID
    fun getChatId(user1: String, user2: String): String {
        return if (user1 < user2) "${user1}_${user2}" else "${user2}_${user1}"
    }

    // Fetch User Data
    LaunchedEffect(userId) {
        try {
            val snapshot = FirebaseFirestore.getInstance().collection("users").document(userId).get().await()
            user = snapshot.toObject(User::class.java)
            nicknameText = user?.firstName ?: ""
        } catch (e: Exception) {
            // Handle error
        }
    }

    // --- DIALOGS ---

    // 1. Block Dialog
    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Block ${user?.firstName}?") },
            text = { Text("Blocked contacts will not be able to call you or send you messages.") },
            confirmButton = {
                TextButton(onClick = {
                    showBlockDialog = false
                    Toast.makeText(context, "${user?.firstName} blocked", Toast.LENGTH_SHORT).show()
                }) { Text("Block", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") }
            }
        )
    }

    // 2. Nickname Dialog
    if (showNicknameDialog) {
        AlertDialog(
            onDismissRequest = { showNicknameDialog = false },
            title = { Text("Set nickname") },
            text = {
                OutlinedTextField(
                    value = nicknameText,
                    onValueChange = { nicknameText = it },
                    label = { Text("First Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showNicknameDialog = false
                    Toast.makeText(context, "Nickname saved", Toast.LENGTH_SHORT).show()
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNicknameDialog = false }) { Text("Cancel") }
            }
        )
    }

    // 3. Disappearing Messages Dialog (PERSISTENCE ADDED)
    if (showDisappearingDialog) {
        AlertDialog(
            onDismissRequest = { showDisappearingDialog = false },
            title = { Text("Disappearing messages") },
            text = {
                Column {
                    disappearingOptions.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedDisappearingLabel = option
                                    showDisappearingDialog = false

                                    // --- PERSISTENCE LOGIC START ---
                                    if (currentUser != null) {
                                        val chatId = getChatId(currentUser.uid, userId)

                                        // Save to Firestore
                                        val data = hashMapOf("disappearingMessages" to option)
                                        firestore.collection("conversations").document(chatId)
                                            .set(data, SetOptions.merge())
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Timer set to $option", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(context, "Failed to save setting", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    // --- PERSISTENCE LOGIC END ---
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (selectedDisappearingLabel == option) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (selectedDisappearingLabel == option) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(text = option, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDisappearingDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Fetch existing conversation settings
    LaunchedEffect(userId) {
        if (currentUser != null) {
            val chatId = getChatId(currentUser.uid, userId)
            val doc = firestore.collection("conversations").document(chatId).get().await()
            val savedTimer = doc.getString("disappearingMessages")
            if (savedTimer != null) {
                selectedDisappearingLabel = savedTimer
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Avatar
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                if (user?.profileImageUrl != null) {
                    AsyncImage(
                        model = user!!.profileImageUrl,
                        contentDescription = "Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = user?.firstName?.firstOrNull()?.toString() ?: "?",
                        fontSize = 40.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Name
            Text(
                text = "${user?.firstName} ${user?.lastName}".trim(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ContactActionButton(
                    icon = Icons.Default.Videocam,
                    label = "Video",
                    onClick = { Toast.makeText(context, "Starting Video Call...", Toast.LENGTH_SHORT).show() }
                )
                ContactActionButton(
                    icon = Icons.Default.Call,
                    label = "Audio",
                    onClick = { Toast.makeText(context, "Starting Audio Call...", Toast.LENGTH_SHORT).show() }
                )
                ContactActionButton(
                    icon = Icons.Outlined.Notifications,
                    label = "Mute",
                    onClick = { Toast.makeText(context, "Notifications muted", Toast.LENGTH_SHORT).show() }
                )
                ContactActionButton(
                    icon = Icons.Default.Search,
                    label = "Search",
                    onClick = { /* Navigate to search */ }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))

            // 4. Settings List
            Column(modifier = Modifier.padding(vertical = 8.dp)) {

                // UPDATED: Now shows the selected time in the subtitle
                ContactOptionItem(
                    icon = Icons.Default.Timer,
                    title = "Disappearing messages",
                    subtitle = selectedDisappearingLabel, // Shows "Off", "10 minutes", etc.
                    onClick = { showDisappearingDialog = true }
                )

                ContactOptionItem(
                    icon = Icons.Default.Edit,
                    title = "Nickname",
                    subtitle = null,
                    onClick = { showNicknameDialog = true }
                )

                ContactOptionItem(
                    icon = Icons.Default.Palette,
                    title = "Chat color & wallpaper",
                    subtitle = null,
                    onClick = { navController.navigate("settings_appearance_chat_color") }
                )

                ContactOptionItem(
                    icon = Icons.Default.Notifications,
                    title = "Sounds & notifications",
                    subtitle = null,
                    onClick = {}
                )

                ContactOptionItem(
                    icon = Icons.Default.Person,
                    title = "Phone contact info",
                    subtitle = null,
                    onClick = {}
                )

                ContactOptionItem(
                    icon = Icons.Default.Person, // Placeholder for safety number icon
                    title = "View safety number",
                    subtitle = null,
                    onClick = {}
                )
            }

            HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))

            // 5. Destructive Actions
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    "Block",
                    color = Color.Red,
                    fontSize = 17.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBlockDialog = true }
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                )

                Text(
                    "Report spam",
                    color = Color.Red,
                    fontSize = 17.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { Toast.makeText(context, "Reported as spam", Toast.LENGTH_SHORT).show() }
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Custom Square-ish Button Style
@Composable
fun ContactActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(16.dp), // Squared corners
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), // Light purple/blue
            modifier = Modifier
                .size(64.dp) // Slightly larger
                .clickable { onClick() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ContactOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.width(24.dp))
        Column {
            Text(text = title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}