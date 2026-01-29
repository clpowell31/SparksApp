package com.example.sparks.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CallsScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            // "Create a Call Link" Item
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = "Link",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Create a Call Link", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Share a link for a Spark call",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }

        // Floating Action Button for Calls
        FloatingActionButton(
            onClick = { /* TODO */ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(Icons.Default.Phone, contentDescription = "New Call")
        }
    }
}