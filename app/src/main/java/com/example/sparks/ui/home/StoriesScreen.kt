package com.example.sparks.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.sparks.model.StoryType
import com.example.sparks.viewmodel.StoriesViewModel
import com.example.sparks.viewmodel.UserStoryGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoriesScreen(
    navController: NavController,
    viewModel: StoriesViewModel = viewModel()
) {
    val myStories by viewModel.myStories.collectAsState()
    val otherStories by viewModel.storyGroups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Media Picker for "Add Story"
    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            // For MVP, we upload immediately as IMAGE.
            // Later we can add a preview/crop screen.
            viewModel.uploadStory(uri, StoryType.IMAGE)
        }
    }

    // Gradient for the "Story Ring" (Instagram style)
    val storyGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF750AA2), Color(0xFF6E51B2), Color(0xFF0097A7))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sparkies", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Add Story")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {

                // 1. MY STORY SECTION
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (myStories.isNotEmpty()) {
                                    // Navigate to Viewer (To be implemented)
                                    navController.navigate("story_viewer/${viewModel.getCurrentUserId()}")
                                } else {
                                    mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar Box
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    // Show colorful border if I have stories
                                    .then(if (myStories.isNotEmpty()) Modifier.border(2.dp, storyGradient, CircleShape) else Modifier)
                                    .background(Color.LightGray)
                            ) {
                                // Show my latest story thumbnail if exists, else generic avatar
                                if (myStories.isNotEmpty()) {
                                    AsyncImage(model = myStories.last().mediaUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                } else {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.align(Alignment.Center), tint = Color.Gray)
                                }
                            }

                            // Plus Badge (if no stories)
                            if (myStories.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text("My Sparks", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(
                                text = if (myStories.isNotEmpty()) "${myStories.size} active updates" else "Tap to add to your Spark",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }

                // 2. RECENT UPDATES HEADER
                if (otherStories.isNotEmpty()) {
                    item {
                        Text(
                            "Recent updates",
                            modifier = Modifier.padding(16.dp),
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }

                // 3. FRIENDS' STORIES LIST
                items(otherStories) { group ->
                    StoryUserItem(
                        group = group,
                        borderBrush = storyGradient,
                        currentUserId = viewModel.getCurrentUserId() // <--- Pass ID here
                    ) {
                        navController.navigate("story_viewer/${group.user.uid}")
                    }
                }
            }
        }
    }
}

@Composable
fun StoryUserItem(
    group: UserStoryGroup,
    borderBrush: Brush,
    currentUserId: String, // <--- We need this to check 'viewers'
    onClick: () -> Unit
) {
    // 1. Calculate Unseen Count
    val unseenCount = group.stories.count { !it.viewers.contains(currentUserId) }
    val hasUnseen = unseenCount > 0

    // 2. Determine Style based on state
    val ringBrush = if (hasUnseen) borderBrush else androidx.compose.ui.graphics.SolidColor(Color.LightGray)
    val ringWidth = if (hasUnseen) 2.5.dp else 1.5.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar Ring
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .border(ringWidth, ringBrush, CircleShape) // Dynamic Border
                .padding(4.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
        ) {
            AsyncImage(
                model = group.user.profileImageUrl ?: "",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = "${group.user.firstName} ${group.user.lastName}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )

            // Dynamic Status Text
            if (hasUnseen) {
                Text(
                    text = "$unseenCount New Sparks",
                    color = MaterialTheme.colorScheme.primary, // Blue/Primary color for new
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            } else {
                Text(
                    text = "Viewed", // Or timestamp
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}