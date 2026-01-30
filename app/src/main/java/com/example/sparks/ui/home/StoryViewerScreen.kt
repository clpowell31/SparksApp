package com.example.sparks.ui.home

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.sparks.model.StoryType
import com.example.sparks.ui.common.VideoPlayer
import com.example.sparks.viewmodel.StoriesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun StoryViewerScreen(
    navController: NavController,
    userId: String,
    viewModel: StoriesViewModel = viewModel()
) {
    // 1. Fetch Stories for this User
    val storyGroups by viewModel.storyGroups.collectAsState()
    val myStories by viewModel.myStories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState() // <--- HERE

    // Determine if we are viewing MY stories or SOMEONE ELSE'S
    val stories = remember(userId, storyGroups, myStories) {
        if (userId == viewModel.getCurrentUserId()) {
            myStories
        } else {
            storyGroups.find { it.user.uid == userId }?.stories ?: emptyList()
        }
    }

    // Handle empty state INTELLIGENTLY
    LaunchedEffect(stories, isLoading) {
        // Only close if we are NOT loading and still have no stories
        if (stories.isEmpty() && !isLoading) {
            navController.popBackStack()
        }
    }

    // Show loading spinner if waiting for data
    if (stories.isEmpty()) {
        if (isLoading) {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }
        return // Stop here while loading or if empty
    }

    // 2. State Management
    var currentStoryIndex by remember { mutableIntStateOf(0) }
    // SAFE ACCESS: Define currentStory here
    val currentStory = stories.getOrNull(currentStoryIndex)
    if (currentStory == null) return

    // 1. NEW: MARK AS VIEWED LOGIC
    // Whenever 'currentStory' changes, we check if we've seen it. If not, mark it.
    LaunchedEffect(currentStory.id) {
        val myId = viewModel.getCurrentUserId()
        if (!currentStory.viewers.contains(myId)) {
            viewModel.markStoryAsViewed(currentStory.id)
        }
    }
    val isVideo = currentStory.type == StoryType.VIDEO

    // Timer State
    var progress by remember { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }

    // Constants
    val storyDuration = 5000L // 5 seconds per slide
    val stepFrequency = 50L   // Update progress every 50ms

    // 3. Timer Logic
    LaunchedEffect(currentStoryIndex, isPaused) {
        if (isPaused) return@LaunchedEffect

        // IF VIDEO: We wait for the player to finish (see onVideoEnded below)
        if (isVideo) {
            progress = 0f
            // We just hang here. The VideoPlayer's onVideoEnded callback triggers the next step.
        } else {
            // IF IMAGE: We run the timer manually
            progress = 0f
            val steps = storyDuration / stepFrequency
            val increment = 1f / steps
            while (isActive && progress < 1f) {
                delay(stepFrequency)
                progress += increment
            }
            // Time's up -> Next Story
            if (currentStoryIndex < stories.lastIndex) {
                currentStoryIndex++
            } else {
                navController.popBackStack()
            }
        }
    }

    // 4. UI Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        tryAwaitRelease()
                        isPaused = false
                    },
                    onTap = { offset ->
                        val screenWidthPx = size.width
                        if (offset.x < screenWidthPx / 3) {
                            if (currentStoryIndex > 0) {
                                currentStoryIndex--
                                progress = 0f
                            }
                        } else {
                            if (currentStoryIndex < stories.lastIndex) {
                                currentStoryIndex++
                                progress = 0f
                            } else {
                                navController.popBackStack()
                            }
                        }
                    }
                )
            }
    ) {
        // A. Content Switcher
        if (isVideo) {
            // VIDEO PLAYER
            VideoPlayer(
                uri = Uri.parse(currentStory.mediaUrl),
                autoPlay = true,
                useController = false, // Hides pause/play buttons for clean look
                onVideoEnded = {
                    // Video finished -> Auto Advance
                    if (currentStoryIndex < stories.lastIndex) {
                        currentStoryIndex++
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        } else {
            // IMAGE VIEWER
            AsyncImage(
                model = currentStory.mediaUrl,
                contentDescription = "Story",
                contentScale = ContentScale.Fit, // Ensure whole image is seen
                modifier = Modifier.fillMaxSize().align(Alignment.Center)
            )
        }

        // Overlay Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)))
        )

        // B. Progress Bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            stories.forEachIndexed { index, _ ->
                val barProgress = when {
                    index < currentStoryIndex -> 1f
                    index == currentStoryIndex -> progress
                    else -> 0f
                }

                LinearProgressIndicator(
                    progress = barProgress, // FIXED: Removed { } lambda
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                )
            }
        }

        // C. Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = currentStory.userAvatar ?: "",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(currentStory.userName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(formatStoryTime(currentStory.timestamp), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        // D. Caption
        if (!currentStory.caption.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                Text(currentStory.caption, color = Color.White, fontSize = 16.sp, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

fun formatStoryTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "Yesterday"
    }
}