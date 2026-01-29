package com.example.sparks.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sparks.viewmodel.AppThemeMode
import com.example.sparks.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    navController: NavController,
    themeViewModel: ThemeViewModel
) {
    val currentTheme by themeViewModel.themeMode.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Theme") },
            text = {
                Column {
                    ThemeOption(
                        text = "System default",
                        selected = currentTheme == AppThemeMode.SYSTEM,
                        onClick = { themeViewModel.setThemeMode(AppThemeMode.SYSTEM); showThemeDialog = false }
                    )
                    ThemeOption(
                        text = "Light",
                        selected = currentTheme == AppThemeMode.LIGHT,
                        onClick = { themeViewModel.setThemeMode(AppThemeMode.LIGHT); showThemeDialog = false }
                    )
                    ThemeOption(
                        text = "Dark",
                        selected = currentTheme == AppThemeMode.DARK,
                        onClick = { themeViewModel.setThemeMode(AppThemeMode.DARK); showThemeDialog = false }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Theme Selector
            SettingsActionItem(
                title = "Theme",
                subtitle = when(currentTheme) {
                    AppThemeMode.SYSTEM -> "System default"
                    AppThemeMode.LIGHT -> "Light"
                    AppThemeMode.DARK -> "Dark"
                },
                onClick = { showThemeDialog = true }
            )

            // Chat Color & Wallpaper Link
            SettingsActionItem(
                title = "Chat color & wallpaper",
                onClick = { navController.navigate("settings_appearance_chat_color") }
            )

            HorizontalDivider()

            SettingsActionItem(title = "App icon", onClick = {})
            SettingsActionItem(title = "Message font size", subtitle = "Normal", onClick = {})
            SettingsActionItem(title = "Navigation bar size", subtitle = "Normal", onClick = {})
        }
    }
}

@Composable
fun ThemeOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}