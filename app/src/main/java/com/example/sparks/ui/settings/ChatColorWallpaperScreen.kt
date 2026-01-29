package com.example.sparks.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.sparks.viewmodel.ThemeViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.sparks.viewmodel.Wallpaper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatColorWallpaperScreen(
    navController: NavController,
    themeViewModel: ThemeViewModel
) {
    val currentWallpaper by themeViewModel.currentWallpaper.collectAsState()
    val dimInDark by themeViewModel.dimWallpaperInDarkMode.collectAsState()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme() // Simple check for now
    val currentChatColor by themeViewModel.chatColor.collectAsState()
    var showColorPicker by remember { mutableStateOf(false) }

    // Bottom Sheet for Color Picker
    if (showColorPicker) {
        ModalBottomSheet(
            onDismissRequest = { showColorPicker = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Chat color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(themeViewModel.presetColors) { color ->
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(color)
                                .clickable {
                                    themeViewModel.setChatColor(color)
                                    // Don't close immediately so they can see the change in background if visible
                                    // showColorPicker = false
                                }
                                .border(
                                    width = if (currentChatColor == color) 3.dp else 0.dp,
                                    color = if (currentChatColor == color) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat color & wallpaper") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // 1. Preview Area (Mock Chat)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                // Mock Phone Frame
                Surface(
                    modifier = Modifier
                        .width(200.dp)
                        .height(360.dp) // Made it taller to match screenshot
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.background // Fallback
                ) {
                    // --- WALLPAPER LAYER ---
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (val wp = currentWallpaper) {
                            is Wallpaper.SolidColor -> {
                                Box(modifier = Modifier.fillMaxSize().background(wp.color))
                            }
                            is Wallpaper.Image -> {
                                AsyncImage(
                                    model = wp.uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Wallpaper.None -> { /* Default Theme Background */ }
                        }

                        // Dimming Layer
                        if (dimInDark && isSystemDark) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                        }
                    }
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.Gray))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Contact name", fontSize = 10.sp)
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Bubbles
                        // Received
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp, bottom = 8.dp)
                                .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(12.dp))
                                .padding(8.dp)
                                .width(80.dp)
                                .height(12.dp)
                        )

                        // Sent (Dynamic Color!)
                        Box(
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(end = 8.dp, bottom = 16.dp)
                                .background(currentChatColor, RoundedCornerShape(12.dp))
                                .padding(8.dp)
                                .width(100.dp)
                                .height(12.dp)
                        )
                    }
                }
            }

            // 2. Settings Controls
            SettingsActionItem(
                title = "Chat color",
                onClick = { showColorPicker = true }
            )
            // Color circle preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .offset(y = (-40).dp) // Hacky way to put the dot next to the text above if not using custom layout
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(currentChatColor)
                )
            }

            SettingsActionItem(title = "Reset chat color", onClick = { /* TODO */ })

            HorizontalDivider()

            SettingsActionItem(
                title = "Set wallpaper",
                onClick = { navController.navigate("settings_wallpaper_picker") } // New Route
            )

            // UPDATE: Dimming Switch
            SettingsSwitchItem(
                title = "Dark mode dims wallpaper",
                checked = dimInDark,
                onCheckedChange = { themeViewModel.setDimWallpaper(it) }
            )

            SettingsActionItem(title = "Reset wallpaper", onClick = { themeViewModel.setWallpaper(Wallpaper.None) })
        }
    }
}