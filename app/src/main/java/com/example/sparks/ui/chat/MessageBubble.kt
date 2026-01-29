package com.example.sparks.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.sparks.model.Message
import com.example.sparks.model.MessageStatus
import com.example.sparks.model.MessageType
import com.example.sparks.ui.theme.SignalBlue
import com.example.sparks.util.formatMessageTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isFromMe: Boolean,
    senderName: String? = null,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    onLongClick: () -> Unit = {},
    onImageClick: (String) -> Unit = {}
) {
    val bubbleColor = if (isFromMe) SignalBlue else MaterialTheme.colorScheme.tertiary
    val textColor = if (isFromMe) Color.White else MaterialTheme.colorScheme.onSurface
    val alignment = if (isFromMe) Alignment.End else Alignment.Start
    val finalShape = shape ?: RoundedCornerShape(18.dp)

    // Timestamp & Status Logic
    val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
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
                            if (message.type == MessageType.IMAGE && message.imageUrl != null) {
                                onImageClick(message.imageUrl)
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
                                Box {
                                    AsyncImage(
                                        model = message.imageUrl,
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
                            }
                        }
                        MessageType.AUDIO -> {
                            if (message.imageUrl != null) {
                                Column {
                                    AudioPlayerBubble(audioUrl = message.imageUrl, tint = textColor)
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
    var emojiCount = 0
    var i = 0
    val length = trimmed.length

    while (i < length) {
        val codePoint = trimmed.codePointAt(i)
        val charCount = Character.charCount(codePoint)
        val type = Character.getType(codePoint)

        val isSymbol = type == Character.SURROGATE.toInt() ||
                type == Character.OTHER_SYMBOL.toInt() ||
                type == Character.MATH_SYMBOL.toInt() ||
                type == Character.MODIFIER_SYMBOL.toInt()

        if (!isSymbol) return false // Found a letter/digit

        emojiCount++
        i += charCount
    }
    // Limit to short strings (approx 1-3 emojis)
    return length < 12
}