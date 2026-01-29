package com.example.sparks.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sparks.viewmodel.ThemeViewModel
import com.example.sparks.viewmodel.Wallpaper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperPickerScreen(
    navController: NavController,
    themeViewModel: ThemeViewModel
) {
    // Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                themeViewModel.setWallpaper(Wallpaper.Image(uri))
                navController.popBackStack()
            }
        }
    )

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

            // 1. Choose from Photos
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Choose from photos", style = MaterialTheme.typography.bodyLarge)
            }

            HorizontalDivider()

            // 2. Presets Header
            Text(
                "Presets",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            // 3. Color Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3), // 3 columns like screenshot
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(themeViewModel.wallpaperPresets) { color ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f) // Square
                            .clip(RoundedCornerShape(12.dp))
                            .background(color)
                            .clickable {
                                themeViewModel.setWallpaper(Wallpaper.SolidColor(color))
                                navController.popBackStack()
                            }
                    )
                }
            }
        }
    }
}