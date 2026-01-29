package com.example.sparks.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StoriesScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            // "My Stories" Item
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with Plus Badge
                Box(modifier = Modifier.size(50.dp)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Gray), // Placeholder for self avatar
                        contentAlignment = Alignment.Center
                    ) {
                        Text("M", color = Color.White) // 'M' for Me
                    }

                    // The small Plus Icon overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.White) // Border effect
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text("My Stories", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("Tap to add", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Empty State Message
            Text(
                text = "No recent updates to show right now.",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 200.dp),
                color = Color.Gray
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        // Floating Action Button for Stories (Camera)
        FloatingActionButton(
            onClick = { /* TODO */ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = "Camera")
        }
    }
}