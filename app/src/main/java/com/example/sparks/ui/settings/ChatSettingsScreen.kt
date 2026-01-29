package com.example.sparks.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsScreen(navController: NavController) {
    // Dummy state
    var linkPreviews by remember { mutableStateOf(true) }
    var addressBookPhotos by remember { mutableStateOf(false) }
    var keepMutedArchived by remember { mutableStateOf(false) }
    var useSystemEmoji by remember { mutableStateOf(false) }
    var sendWithEnter by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats") },
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
            // General Options
            SettingsSwitchItem(
                title = "Generate link previews",
                subtitle = "Retrieve link previews directly from websites for messages you send.",
                checked = linkPreviews,
                onCheckedChange = { linkPreviews = it }
            )

            SettingsSwitchItem(
                title = "Use address book photos",
                subtitle = "Display contact photos from your address book if available",
                checked = addressBookPhotos,
                onCheckedChange = { addressBookPhotos = it }
            )

            SettingsSwitchItem(
                title = "Keep muted chats archived",
                subtitle = "Muted chats that are archived will remain archived when a new message arrives.",
                checked = keepMutedArchived,
                onCheckedChange = { keepMutedArchived = it }
            )

            HorizontalDivider()

            SettingsCategoryHeader("Chat folders")
            SettingsActionItem(title = "Add a chat folder", onClick = {})

            HorizontalDivider()

            SettingsCategoryHeader("Keyboard")
            SettingsSwitchItem(
                title = "Use system emoji",
                checked = useSystemEmoji,
                onCheckedChange = { useSystemEmoji = it }
            )
            SettingsSwitchItem(
                title = "Send with enter",
                checked = sendWithEnter,
                onCheckedChange = { sendWithEnter = it }
            )
        }
    }
}