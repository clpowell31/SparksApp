package com.example.sparks.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sparks.model.Message

@Composable
fun ReactionOverlay(
    selectedMessage: Message?,
    isGroupChat: Boolean, // To re-render the bubble correctly
    onDismiss: () -> Unit,
    onReactionSelect: (String) -> Unit,
    onOptionSelect: (String) -> Unit
) {
    if (selectedMessage == null) return

    // 1. DIMMED BACKGROUND
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }, // Tap outside to close
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {

            // 2. REACTION PILL (❤️ 👍 👎 😂 😮 😢)
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val emojis = listOf("❤️", "👍", "👎", "😂", "😮", "😢")
                    emojis.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 28.sp,
                            modifier = Modifier.clickable { onReactionSelect(emoji) }
                        )
                    }
                    // "More" dots (Visual only for now)
                    Text("...", fontSize = 28.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. THE MESSAGE CLONE (Visual Reference)
            // We re-use MessageBubble but disable click interactions on it
            // Note: In a production app, you calculate the exact screen position to line it up.
            // For now, centering it is a clean, standard look.
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                // Pass 'false' for interactions or create a simplified view
                // Simple Text for the clone to save complexity:
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if(selectedMessage.senderId == "me") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = selectedMessage.text.take(50) + if(selectedMessage.text.length > 50) "..." else "",
                        modifier = Modifier.padding(12.dp),
                        color = if(selectedMessage.senderId == "me") Color.White else Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. OPTIONS MENU (Reply, Copy, etc.)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(250.dp)
            ) {
                Column {
                    MenuOption(Icons.Outlined.Reply, "Reply") { onOptionSelect("Reply") }
                    MenuOption(Icons.Outlined.Forward, "Forward") { onOptionSelect("Forward") }
                    MenuOption(Icons.Outlined.ContentCopy, "Copy") { onOptionSelect("Copy") }
                    MenuOption(Icons.Outlined.CheckCircle, "Select") { onOptionSelect("Select") }
                    MenuOption(Icons.Outlined.Info, "Info") { onOptionSelect("Info") }
                    Divider(color = Color.LightGray.copy(alpha=0.2f))
                    MenuOption(Icons.Outlined.Delete, "Delete", isDestructive = true) { onOptionSelect("Delete") }
                }
            }
        }
    }
}

@Composable
fun MenuOption(icon: ImageVector, text: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) Color.Red else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            fontSize = 16.sp,
            color = if (isDestructive) Color.Red else MaterialTheme.colorScheme.onSurface
        )
    }
}