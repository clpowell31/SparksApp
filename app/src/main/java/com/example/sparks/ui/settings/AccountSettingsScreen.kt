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
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(navController: NavController) {
    // Dummy state for UI demonstration
    var pinReminders by remember { mutableStateOf(true) }
    var registrationLock by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account") },
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
            SettingsCategoryHeader("Sparks PIN")

            SettingsActionItem(title = "Change your PIN", onClick = {})

            SettingsSwitchItem(
                title = "PIN reminders",
                subtitle = "You'll be asked less frequently over time",
                checked = pinReminders,
                onCheckedChange = { pinReminders = it }
            )

            SettingsSwitchItem(
                title = "Registration Lock",
                subtitle = "Require your Sparks PIN to register your phone number with Sparks again",
                checked = registrationLock,
                onCheckedChange = { registrationLock = it }
            )

            SettingsActionItem(title = "Advanced PIN settings", onClick = {})

            HorizontalDivider()

            SettingsCategoryHeader("Account")

            SettingsActionItem(title = "Change phone number", onClick = {})
            SettingsActionItem(
                title = "Transfer account",
                subtitle = "Transfer account to a new Android device",
                onClick = {}
            )
            SettingsActionItem(title = "Your account data", onClick = {})

            // Delete Account (Red)
            SettingsActionItem(
                title = "Delete account",
                color = Color.Red,
                onClick = {}
            )
        }
    }
}