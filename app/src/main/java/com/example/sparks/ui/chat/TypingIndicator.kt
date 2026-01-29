package com.example.sparks.ui.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier,
    dotSize: Dp = 8.dp,
    dotColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val dots = listOf(
        remember { Animatable(0f) },
        remember { Animatable(0f) },
        remember { Animatable(0f) }
    )

    LaunchedEffect(Unit) {
        // Staggered animation loop
        while (true) {
            dots.forEachIndexed { index, animatable ->
                launch {
                    animatable.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(300, easing = LinearEasing)
                    )
                    animatable.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(300, easing = LinearEasing)
                    )
                }
                delay(150) // Delay between dots starting
            }
            delay(1000) // Pause before next loop
        }
    }

    Row(
        modifier = modifier
            .padding(8.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        dots.forEach { animatable ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(dotSize)
                    .graphicsLayer {
                        translationY = -animatable.value * 10f // Bounce up by 10 pixels
                    }
                    .background(dotColor, CircleShape)
            )
        }
    }
}