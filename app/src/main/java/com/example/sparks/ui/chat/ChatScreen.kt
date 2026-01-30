package com.example.sparks.ui.chat

import android.Manifest
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.sparks.model.Message
import com.example.sparks.model.MessageType
import com.example.sparks.util.AudioRecorder
import com.example.sparks.util.PresenceManager
import com.example.sparks.util.getChatHeaderDate
import com.example.sparks.util.isSameDay
import com.example.sparks.viewmodel.ChatViewModel
import com.example.sparks.viewmodel.ThemeViewModel
import com.example.sparks.viewmodel.Wallpaper
import java.io.File
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    chatId: String,
    chatName: String,
    onTitleClick: (String) -> Unit,
    viewModel: ChatViewModel = viewModel(),
    themeViewModel: ThemeViewModel = viewModel()
) {
    // 1. TRACK CURRENT CHAT (For Notifications)
    DisposableEffect(chatId) {
        PresenceManager.currentChatId = chatId
        onDispose {
            PresenceManager.currentChatId = null
        }
    }

    // 1. OBSERVE DURATION & DIALOG STATE
    val disappearingDuration by viewModel.disappearingDuration.collectAsState()
    var showDisappearingDialog by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }

    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    val currentUserId = viewModel.getCurrentUserId()

    // Theme & Wallpaper State
    val currentWallpaper by themeViewModel.currentWallpaper.collectAsState()
    val dimInDark by themeViewModel.dimWallpaperInDarkMode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()

    // Metadata & Members
    val chatMetadata by viewModel.chatMetadata.collectAsState()
    val finalChatName = chatMetadata.first
    val finalChatImage = chatMetadata.second
    val remoteTypingUsers by viewModel.remoteTypingUsers.collectAsState()
    val groupMembers by viewModel.groupMembers.collectAsState()

    val replyingTo by viewModel.replyingTo.collectAsState()

    // UI State
    var showDeleteDialog by remember { mutableStateOf<Message?>(null) }
    var showStickerPicker by remember { mutableStateOf(false) }
    var selectedMessageForReaction by remember { mutableStateOf<Message?>(null) }

    // Filter messages
    val visibleMessages = messages.filter { !it.deletedFor.contains(currentUserId) }
    val clipboardManager = LocalClipboardManager.current
    val isGroupChat = groupMembers.size > 2

    // Media & Audio
    val context = LocalContext.current
    // Note: decryptedCache is used inside MessageBubble now, not here.

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                // FIX 1: Pass 'context' here, not 'chatName'
                viewModel.sendImageMessage(chatId, uri, context)
            }
        }
    )

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                viewModel.sendVideoMessage(chatId, uri, context)
            }
        }
    )

    var isRecording by remember { mutableStateOf(false) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    val recorder = remember { AudioRecorder(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isRecording = true
            recordedFile = recorder.startRecording()
        }
    }

    // Effects
    LaunchedEffect(chatId) {
        viewModel.listenToMessages(chatId)
        viewModel.fetchChatDetails(chatId)
        viewModel.listenToTyping(chatId)
        viewModel.fetchGroupMembers(chatId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(0)
    }

    BackHandler(enabled = showStickerPicker) {
        showStickerPicker = false
    }

    // --- ROOT CONTAINER ---
    Box(modifier = Modifier.fillMaxSize()) {

        // 1. WALLPAPER
        when (val wp = currentWallpaper) {
            is Wallpaper.SolidColor -> Box(modifier = Modifier.fillMaxSize().background(wp.color))
            is Wallpaper.Image -> AsyncImage(model = wp.uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Wallpaper.None -> Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }
        if (dimInDark && isSystemDark) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
        }

        // 2. SCAFFOLD
        Scaffold(
            modifier = Modifier.statusBarsPadding(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val ids = chatId.split("_")
                                    val otherId = ids.firstOrNull { it != currentUserId } ?: ""
                                    if (otherId.isNotEmpty()) onTitleClick(otherId)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary),
                                contentAlignment = Alignment.Center
                            ) {
                                if (finalChatImage != null) {
                                    AsyncImage(model = finalChatImage, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else {
                                    Text(text = finalChatName.take(1).uppercase(), color = Color.White, fontSize = 16.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = finalChatName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (remoteTypingUsers.isNotEmpty()) {
                                    Text("Typing...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { }) { Icon(Icons.Default.Videocam, contentDescription = "Video") }
                        IconButton(onClick = { }) { Icon(Icons.Default.Call, contentDescription = "Call") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.30f),
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
                    )
                )
            }
        ) { paddingValues ->

            // --- MAIN CONTENT ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
            ) {
                // A. Messages
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    reverseLayout = true,
                    contentPadding = PaddingValues(bottom = 8.dp, top = 8.dp)
                ) {
                    val reversedMessages = visibleMessages.reversed()
                    items(reversedMessages.size) { index ->
                        val message = reversedMessages[index]
                        val isMe = message.senderId == currentUserId

                        val nextMsg = reversedMessages.getOrNull(index + 1)
                        val prevMsg = reversedMessages.getOrNull(index - 1)
                        val showDateHeader = nextMsg == null || !isSameDay(message.timestamp, nextMsg.timestamp)
                        val isGroupStart = nextMsg == null || nextMsg.senderId != message.senderId || showDateHeader
                        val isGroupEnd = prevMsg == null || prevMsg.senderId != message.senderId || !isSameDay(message.timestamp, prevMsg.timestamp)

                        val rFull = 18.dp
                        val rSmall = 2.dp
                        val bubbleShape = if (isMe) {
                            RoundedCornerShape(
                                topStart = rFull, bottomStart = rFull,
                                topEnd = if (isGroupStart) rFull else rSmall,
                                bottomEnd = if (isGroupEnd) rFull else rSmall
                            )
                        } else {
                            RoundedCornerShape(
                                topEnd = rFull, bottomEnd = rFull,
                                topStart = if (isGroupStart) rFull else rSmall,
                                bottomStart = if (isGroupEnd) rFull else rSmall
                            )
                        }
                        val bottomSpacing = if (isGroupEnd) 8.dp else 1.dp

                        MessageBubble(
                            message = message,
                            isFromMe = isMe,
                            senderName = if (!isMe && isGroupChat) groupMembers[message.senderId] else null,
                            shape = bubbleShape,
                            modifier = Modifier.padding(bottom = bottomSpacing),
                            onLongClick = { selectedMessageForReaction = message },
                            onImageClick = { urlString ->
                                if (message.type == MessageType.VIDEO) {
                                    // Open Video Player
                                    val encoded = Base64.encodeToString(urlString.toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
                                    navController.navigate("video_player/$encoded")
                                } else {
                                    // Open Image Viewer
                                    val encoded = Base64.encodeToString(urlString.toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
                                    navController.navigate("image_viewer/$encoded")
                                }
                            },
                            viewModel = viewModel // Pass VM for E2EE decryption
                        )

                        if (showDateHeader) {
                            DateHeader(text = getChatHeaderDate(message.timestamp))
                        }
                    }
                }

                // FIX 2: REMOVED THE FLOATING CODE BLOCK THAT CAUSED 'Unresolved reference message'
                // The decryption logic is now safely inside MessageBubble.kt

                // B. Reply Preview
                if (replyingTo != null) {
                    ReplyPreviewBar(message = replyingTo!!, onClose = { viewModel.setReplyingTo(null) })
                }

                // C. PILL INPUT CONTAINER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            IconButton(onClick = { showStickerPicker = !showStickerPicker }) {
                                Icon(
                                    if (showStickerPicker) Icons.Default.KeyboardArrowDown else Icons.Outlined.EmojiEmotions,
                                    contentDescription = "Emoji",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            TextField(
                                value = messageText,
                                onValueChange = { newText ->
                                    messageText = newText
                                    viewModel.onUserInputChanged(chatId, newText)
                                },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Spark message", fontSize = 15.sp) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                maxLines = 4
                            )

                            IconButton(
                                onClick = {
                                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            IconButton(
                                onClick = {
                                    videoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = "Video", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            IconButton(
                                onClick = {
                                    if (isRecording) {
                                        recorder.stopRecording()
                                        isRecording = false
                                        if (recordedFile != null) viewModel.sendAudioMessage(chatId, recordedFile!!, chatName)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Record",
                                    tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    FloatingActionButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(chatId, messageText, chatName)
                                messageText = ""
                                showStickerPicker = false
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier.size(45.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = if (messageText.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.Add,
                            contentDescription = "Send",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (showStickerPicker) {
                    StickerPicker(onStickerSelected = { url ->
                        viewModel.sendSticker(chatId, url)
                        showStickerPicker = false
                    })
                }
            }
        }

        // 3. OVERLAYS
        if (selectedMessageForReaction != null) {
            ReactionOverlay(
                selectedMessage = selectedMessageForReaction,
                isGroupChat = isGroupChat,
                onDismiss = { selectedMessageForReaction = null },
                onReactionSelect = { emoji ->
                    viewModel.toggleReaction(chatId, selectedMessageForReaction!!.id, emoji)
                    selectedMessageForReaction = null
                },
                onOptionSelect = { option ->
                    when (option) {
                        "Reply" -> { viewModel.setReplyingTo(selectedMessageForReaction); selectedMessageForReaction = null }
                        "Copy" -> { clipboardManager.setText(AnnotatedString(selectedMessageForReaction?.text ?: "")); selectedMessageForReaction = null }
                        "Delete" -> { showDeleteDialog = selectedMessageForReaction; selectedMessageForReaction = null }
                        else -> selectedMessageForReaction = null
                    }
                }
            )
        }

        if (showDeleteDialog != null) {
            val msg = showDeleteDialog!!
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Delete message?") },
                text = { Text("You can delete this message for yourself or for everyone in the chat.") },
                confirmButton = { TextButton(onClick = { viewModel.deleteMessage(chatId, msg, true); showDeleteDialog = null }) { Text("Delete for Everyone", color = Color.Red) } },
                dismissButton = { TextButton(onClick = { viewModel.deleteMessage(chatId, msg, false); showDeleteDialog = null }) { Text("Delete for Me") } }
            )
        }

        if (showDisappearingDialog) {
            DisappearingMessagesDialog(
                currentDuration = disappearingDuration,
                onDismiss = { showDisappearingDialog = false },
                onDurationSelected = { newDuration ->
                    viewModel.updateDisappearingDuration(chatId, newDuration / 1000L)
                }
            )
        }
    }
}

@Composable
fun ReplyPreviewBar(message: Message, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(4.dp).height(36.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Replying to ${message.replyToName ?: "User"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(text = if (message.type == MessageType.IMAGE) "Photo" else message.text, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
        }
        if (message.type == MessageType.IMAGE && message.imageUrl != null) {
            AsyncImage(model = message.imageUrl, contentDescription = null, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.width(8.dp))
        }
        IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Cancel reply") }
    }
}