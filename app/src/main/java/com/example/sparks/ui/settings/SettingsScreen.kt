package com.example.sparks.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
// REMOVED: import androidx.compose.material.icons.filled.Appearance (This was the error)
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Brightness4 // Correct Icon for Appearance
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.sparks.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val user = authViewModel.currentUser.collectAsState().value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Header (Profile)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("profile") }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Avatar
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user?.email?.first()?.toString()?.uppercase() ?: "U",
                        color = Color.White,
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Christopher", // Placeholder until full fetch
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = user?.email ?: "(229) 555-0123",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp)

            // 2. Settings Items
            SettingsItem(
                icon = Icons.Default.AccountCircle,
                title = "Account",
                onClick = { navController.navigate("settings_account") } // Update this
            )
            SettingsItem(Icons.Default.Devices, "Linked devices")
            SettingsItem(Icons.Default.FavoriteBorder, "Donate to Sparks")

            HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            // FIXED: Used Brightness4 instead of Appearance
            SettingsItem(
                icon = Icons.Default.Brightness4,
                title = "Appearance",
                onClick = { navController.navigate("settings_appearance") } // Link here
            )
            SettingsItem(
                icon = Icons.Default.ChatBubbleOutline,
                title = "Chats",
                onClick = { navController.navigate("settings_chats") } // Update this
            )
            SettingsItem(Icons.Outlined.PhotoCamera, "Stories")
            SettingsItem(Icons.Default.NotificationsNone, "Notifications")
            SettingsItem(Icons.Default.Lock, "Privacy")
            SettingsItem(Icons.Default.Backup, "Backups", badge = "BETA")
            SettingsItem(Icons.Default.DataUsage, "Data and storage")

            HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            SettingsItem(Icons.Default.CreditCard, "Payments")

            // Sign Out
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        authViewModel.logout()
                        navController.navigate("auth") {
                            popUpTo(0)
                        }
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Log Out (Debug)", color = Color.Red)
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    badge: String? = null,
    onClick: () -> Unit = {} // Add this parameter
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() } // Use it here
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        if (badge != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    text = badge,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}