package com.example.sparks.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sparks.model.Message
import com.example.sparks.model.MessageStatus
import com.example.sparks.model.MessageType
import com.example.sparks.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// Fallback color if not defined in your theme
val SignalBlue = Color(0xFF007AFF)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isFromMe: Boolean,
    senderName: String? = null,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    onLongClick: () -> Unit = {},
    onImageClick: (String) -> Unit = {},
    viewModel: ChatViewModel // <--- REQUIRED FOR E2EE
) {
    val context = LocalContext.current
    val decryptedCache = viewModel.decryptedContentCache

    val bubbleColor = if (isFromMe) SignalBlue else MaterialTheme.colorScheme.tertiary
    val textColor = if (isFromMe) Color.White else MaterialTheme.colorScheme.onSurface
    val alignment = if (isFromMe) Alignment.End else Alignment.Start
    val finalShape = shape ?: RoundedCornerShape(18.dp)

    // Timestamp & Status Logic
    val timeText = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    val statusIcon = when (message.status) {
        MessageStatus.SENT -> Icons.Default.Done
        MessageStatus.DELIVERED -> Icons.Default.DoneAll
        MessageStatus.READ -> Icons.Default.DoneAll
        else -> Icons.Default.Done
    }
    val statusTint = if (message.status == MessageStatus.READ) Color.White else Color.White.copy(alpha = 0.7f)
    val timeColor = if (isFromMe) Color.White.copy(alpha = 0.7f) else Color.Gray

    // Helper to generate a consistent color for a name
    fun getNameColor(name: String): Color {
        val colors = listOf(
            Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
            Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF009688),
            Color(0xFFFF5722), Color(0xFF795548)
        )
        return colors[abs(name.hashCode()) % colors.size]
    }

    // 1. CHECK FOR JUMBO EMOJI
    val isJumboEmoji = remember(message.text) {
        message.type == MessageType.TEXT && isEmojiOnly(message.text)
    }

    // --- REACTION LOGIC ---
    val reactionCounts = remember(message.reactions) {
        message.reactions.values.groupingBy { it }.eachCount()
    }
    val hasReactions = reactionCounts.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalAlignment = alignment
    ) {

        // PARENT BOX (Holds Bubble + Reaction Pill)
        Box {
            // 1. THE MESSAGE BUBBLE
            Surface(
                shape = finalShape,
                // Transparent background if Jumbo Emoji
                color = if (isJumboEmoji) Color.Transparent else bubbleColor,
                shadowElevation = if (isJumboEmoji) 0.dp else 1.dp,
                modifier = Modifier
                    .padding(bottom = if (hasReactions) 10.dp else 0.dp) // Make room for reactions
                    .widthIn(min = 80.dp, max = 300.dp)
                    .combinedClickable(
                        onClick = {
                            // If cached image, click opens it
                            if (message.type == MessageType.IMAGE && viewModel.decryptedContentCache[message.id] != null) {
                                onImageClick(viewModel.decryptedContentCache[message.id].toString())
                            }
                        },
                        onLongClick = onLongClick
                    )
            ) {
                // CONTENT COLUMN
                Column(
                    modifier = if (message.type == MessageType.IMAGE) Modifier else Modifier.padding(
                        horizontal = if (isJumboEmoji) 0.dp else 12.dp,
                        vertical = if (isJumboEmoji) 0.dp else 8.dp
                    )
                ) {
                    // A. SENDER NAME (Groups only)
                    if (senderName != null && !isJumboEmoji) {
                        Text(
                            text = senderName,
                            color = getNameColor(senderName),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // B. QUOTED REPLY BLOCK
                    if (message.replyToId != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(modifier = Modifier.padding(8.dp).height(IntrinsicSize.Min)) {
                                Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(Color.White.copy(0.7f)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = message.replyToName ?: "User",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor.copy(alpha = 0.9f)
                                    )
                                    Text(
                                        text = if (message.replyToImage != null) "Photo" else message.replyToText ?: "",
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = textColor.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    // C. MAIN CONTENT SWITCHER
                    when (message.type) {
                        MessageType.IMAGE -> {
                            if (message.imageUrl != null) {
                                // --- SECURITY CHECK ---
                                val decryptedUri = decryptedCache[message.id]

                                if (decryptedUri != null) {
                                    // CASE 1: Decrypted & Ready -> Show Image
                                    Box {
                                        AsyncImage(
                                            model = decryptedUri,
                                            contentDescription = "Sent image",
                                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                        // Image Timestamp Overlay
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp)
                                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = timeText, fontSize = 10.sp, color = Color.White)
                                                if (isFromMe) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(statusIcon, contentDescription = null, tint = statusTint, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // CASE 2: Encrypted -> Show Lock Spinner
                                    LaunchedEffect(message.id) {
                                        viewModel.resolveMedia(message, context)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .background(Color.Black.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                            }
                        }

                        MessageType.VIDEO -> {
                            if (message.imageUrl != null) {
                                val decryptedUri = viewModel.decryptedContentCache[message.id]

                                if (decryptedUri != null) {
                                    // CASE 1: Decrypted & Ready -> Show Thumbnail + Play Button
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black)
                                            .clickable {
                                                // CLICK: Open Full Screen Player
                                                // We need to pass the LOCAL URI
                                                onImageClick(decryptedUri.toString())
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Optional: You could generate a thumbnail here,
                                        // but for now we just show a Play Icon on black
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(48.dp)
                                        )

                                        // Timestamp Overlay
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp)
                                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = timeText, fontSize = 10.sp, color = Color.White)
                                                if (isFromMe) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(statusIcon, contentDescription = null, tint = statusTint, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // CASE 2: Encrypted -> Show Spinner
                                    LaunchedEffect(message.id) {
                                        viewModel.resolveMedia(message, context)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .background(Color.Black.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                            }
                        }

                        MessageType.AUDIO -> {
                            if (message.imageUrl != null) {
                                // --- AUDIO SECURITY CHECK ---
                                val decryptedUri = viewModel.decryptedContentCache[message.id]

                                if (decryptedUri != null) {
                                    // CASE 1: Decrypted & Ready -> Show Player
                                    Column {
                                        // Pass the LOCAL decrypted file URI to the player
                                        ChatAudioBubble(audioUrl = decryptedUri.toString(), tint = textColor)

                                        // Audio Timestamp
                                        Row(
                                            modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = timeText, fontSize = 10.sp, color = timeColor)
                                            if (isFromMe) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(statusIcon, contentDescription = null, tint = statusTint, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                } else {
                                    // CASE 2: Encrypted -> Trigger Decrypt
                                    LaunchedEffect(message.id) {
                                        viewModel.resolveMedia(message, context)
                                    }

                                    // Show "Decrypting Audio..." placeholder
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            color = textColor,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Decrypting voice...", color = textColor, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        else -> {
                            // TEXT MESSAGE (Standard or Jumbo)
                            if (isJumboEmoji) {
                                Text(
                                    text = message.text,
                                    fontSize = 48.sp, // JUMBO SIZE
                                    lineHeight = 56.sp
                                )
                            } else {
                                Box {
                                    val paddingSpace = "\u00A0".repeat(if (isFromMe) 14 else 12)
                                    Text(
                                        text = message.text + paddingSpace,
                                        color = textColor,
                                        fontSize = 16.sp,
                                        modifier = Modifier.align(Alignment.TopStart)
                                    )
                                    // Timestamp Floating
                                    Row(
                                        modifier = Modifier.align(Alignment.BottomEnd),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = timeText, fontSize = 11.sp, color = timeColor)
                                        if (isFromMe) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(imageVector = statusIcon, contentDescription = "Status", tint = statusTint, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. THE REACTION PILL (Overlay)
            if (hasReactions) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(y = 4.dp, x = (-4).dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        reactionCounts.entries.sortedByDescending { it.value }.take(3).forEach { (emoji, count) ->
                            Text("$emoji $count", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } // End Box (Bubble + Reaction)

        // 3. JUMBO EMOJI TIMESTAMP (Outside Bubble)
        if (isJumboEmoji) {
            Row(
                modifier = Modifier.padding(top = 2.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeText,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                if (isFromMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(statusIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

// --- HELPER FUNCTION: DETECT 1-3 EMOJIS ---
fun isEmojiOnly(text: String): Boolean {
    if (text.isBlank()) return false
    val trimmed = text.trim()
    val length = trimmed.length
    var i = 0
    var emojiCount = 0

    while (i < length) {
        val codePoint = trimmed.codePointAt(i)
        val charCount = Character.charCount(codePoint)
        val type = Character.getType(codePoint)

        // Very basic check for symbols/surrogates
        val isSymbol = type == Character.SURROGATE.toInt() ||
                type == Character.OTHER_SYMBOL.toInt() ||
                type == Character.MATH_SYMBOL.toInt() ||
                type == Character.MODIFIER_SYMBOL.toInt()

        if (!isSymbol) return false // Found a letter/digit

        emojiCount++
        i += charCount
    }
    return length < 12 // Arbitrary limit for "Jumbo" sizing
}

// FIX: Renamed function to avoid conflict
@Composable
fun ChatAudioBubble(audioUrl: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = tint)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Voice Message", color = tint)
    }
}