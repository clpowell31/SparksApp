package com.example.sparks.ui.chat

import android.media.MediaPlayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun AudioPlayerBubble(audioUrl: String, tint: androidx.compose.ui.graphics.Color) {
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Cleanup when bubble leaves screen
    DisposableEffect(audioUrl) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(8.dp).width(200.dp)
    ) {
        // Play/Pause Button
        Surface(
            shape = CircleShape,
            color = tint.copy(alpha = 0.2f),
            modifier = Modifier.size(40.dp).clickable {
                if (isPlaying) {
                    mediaPlayer?.pause()
                    isPlaying = false
                } else {
                    if (mediaPlayer == null) {
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(audioUrl)
                            prepareAsync()
                            setOnPreparedListener {
                                start()
                                isPlaying = true
                            }
                            setOnCompletionListener {
                                isPlaying = false
                            }
                        }
                    } else {
                        mediaPlayer?.start()
                        isPlaying = true
                    }
                }
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play Audio",
                    tint = tint
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Fake visualization/progress line
        LinearProgressIndicator(
            progress = { if (isPlaying) 0.6f else 0.0f }, // Static demo progress, real progress needs a looped state
            modifier = Modifier.weight(1f),
            color = tint,
            trackColor = tint.copy(alpha = 0.3f),
        )
    }
}