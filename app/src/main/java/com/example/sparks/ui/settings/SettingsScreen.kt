package com.example.sparks.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.sparks.model.User
import com.example.sparks.viewmodel.AuthViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val firebaseUser by authViewModel.currentUser.collectAsState()
    var userProfile by remember { mutableStateOf<User?>(null) }

    // 1. Fetch Full User Profile from Firestore
    LaunchedEffect(firebaseUser) {
        if (firebaseUser != null) {
            try {
                val doc = FirebaseFirestore.getInstance().collection("users")
                    .document(firebaseUser!!.uid).get().await()
                userProfile = doc.toObject(User::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {

            // 2. USER PROFILE HEADER (Dynamic)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("profile") } // Navigate to edit profile
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar Logic
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userProfile?.profileImageUrl != null) {
                            AsyncImage(
                                model = userProfile!!.profileImageUrl,
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Fallback: Initials
                            val initial = userProfile?.firstName?.take(1)?.uppercase()
                                ?: firebaseUser?.email?.take(1)?.uppercase()
                                ?: "?"
                            Text(
                                text = initial,
                                fontSize = 28.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Name Logic
                    Column {
                        val displayName = if (userProfile != null) {
                            "${userProfile!!.firstName} ${userProfile!!.lastName}"
                        } else {
                            "Loading..."
                        }

                        Text(
                            text = displayName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = firebaseUser?.email ?: "",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
                HorizontalDivider()
            }

            // 3. SETTINGS ITEMS
            item {
                SettingsItem(Icons.Default.AccountCircle, "Account") { navController.navigate("settings_account") }
                SettingsItem(Icons.Default.Devices, "Linked devices")
                SettingsItem(Icons.Default.FavoriteBorder, "Donate to Sparks")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                SettingsItem(Icons.Default.Brightness6, "Appearance") { navController.navigate("settings_appearance") }
                SettingsItem(Icons.Default.Chat, "Chats") { navController.navigate("settings_chats") }
                SettingsItem(Icons.Default.PhotoCamera, "Stories")
                SettingsItem(Icons.Default.Notifications, "Notifications")
                SettingsItem(Icons.Default.Lock, "Privacy")
                SettingsItem(Icons.Default.Backup, "Backups") { /* Backup Logic */ }
                SettingsItem(Icons.Default.DataUsage, "Data and storage")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                SettingsItem(Icons.Default.CreditCard, "Payments")
            }

            // 4. LOGOUT BUTTON
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = {
                        authViewModel.logout()
                        navController.navigate("auth") {
                            popUpTo(0) // Clear back stack
                        }
                    }) {
                        Text("Log Out (Debug)", color = Color.Red)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray)
        Spacer(modifier = Modifier.width(24.dp))
        Text(title, fontSize = 16.sp)
    }
}