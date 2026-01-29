package com.example.sparks.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun StickerPicker(onStickerSelected: (String) -> Unit) {
    // Mock Data: List of "Sticker" URLs
    // Using reliable placeholders. in a real app, these would be local drawables or Firebase Storage URLs
    val stickers = listOf(
        "https://cdn-icons-png.flaticon.com/512/742/742751.png", // Laughing
        "https://cdn-icons-png.flaticon.com/512/742/742752.png", // Heart eyes
        "https://cdn-icons-png.flaticon.com/512/742/742921.png", // Cool
        "https://cdn-icons-png.flaticon.com/512/742/742760.png", // Crying
        "https://cdn-icons-png.flaticon.com/512/197/197374.png", // UK Flag (random fun)
        "https://cdn-icons-png.flaticon.com/512/260/260185.png", // Star
        "https://cdn-icons-png.flaticon.com/512/742/742823.png", // Angry
        "https://cdn-icons-png.flaticon.com/512/742/742745.png", // Happy

    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().height(250.dp)
    ) {
        Column {
            Text(
                "Stickers",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleSmall
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(stickers) { url ->
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .aspectRatio(1f)
                            .clickable { onStickerSelected(url) },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Sticker",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}