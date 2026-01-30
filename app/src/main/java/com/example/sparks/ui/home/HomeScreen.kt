package com.example.sparks.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.sparks.model.StoryType
import com.example.sparks.viewmodel.AuthViewModel
import com.example.sparks.viewmodel.ChatListItem
import com.example.sparks.viewmodel.HomeViewModel
import com.example.sparks.viewmodel.StoriesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController, // Added NavController
    authViewModel: AuthViewModel,
    onChatClick: (String) -> Unit,
    onFabClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onArchivedClick: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
    storiesViewModel: StoriesViewModel = viewModel() // Added StoriesViewModel
) {
    val chatList by homeViewModel.activeChats.collectAsState()
    val searchQuery by homeViewModel.searchQuery.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var showThreadDeleteDialog by remember { mutableStateOf<String?>(null) }
    var isSearchMode by remember { mutableStateOf(false) }

    // --- QUICK STORY UPLOAD (Camera FAB) ---
    val context = LocalContext.current
    val quickStoryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            // DETECT TYPE
            val type = context.contentResolver.getType(uri)
            val storyType = if (type?.startsWith("video/") == true) StoryType.VIDEO else StoryType.IMAGE

            storiesViewModel.uploadStory(uri, storyType)
            selectedTab = 2
        }
    }

    Scaffold(
        topBar = {
            if (isSearchMode) {
                // --- SEARCH BAR MODE ---
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { homeViewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Search...") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearchMode = false
                            homeViewModel.onSearchQueryChanged("")
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Search")
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { homeViewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear text")
                            }
                        }
                    }
                )
            } else {
                // --- NORMAL TITLE MODE ---
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary)
                                    .clickable { onProfileClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                val emailInitial = authViewModel.currentUser.value?.email?.firstOrNull()?.toString()?.uppercase() ?: "U"
                                Text(text = emailInitial, color = Color.White, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Sparks", fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchMode = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }

                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(text = { Text("New group") }, onClick = { showMenu = false; onFabClick() })
                                DropdownMenuItem(text = { Text("Mark all read") }, onClick = { showMenu = false })
                                DropdownMenuItem(text = { Text("Invite friends") }, onClick = { showMenu = false })
                                DropdownMenuItem(text = { Text("Archived") }, onClick = { showMenu = false; onArchivedClick() })
                                DropdownMenuItem(text = { Text("Settings") }, onClick = { showMenu = false; onSettingsClick() })
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Chats") },
                    label = { Text("Chats") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Phone, contentDescription = "Calls") },
                    label = { Text("Calls") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Outlined.PhotoCamera, contentDescription = "Sparkies") },
                    label = { Text("Sparkies") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Quick Story Upload Button
                    SmallFloatingActionButton(
                        onClick = {
                            // CHANGE THIS LINE: Allow Image AND Video
                            quickStoryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Post Story")
                    }
                    FloatingActionButton(
                        onClick = { onFabClick() },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "New Message")
                    }
                }
            }
        }
    ) { paddingValues ->

        if (showThreadDeleteDialog != null) {
            val chatId = showThreadDeleteDialog!!
            AlertDialog(
                onDismissRequest = { showThreadDeleteDialog = null },
                title = { Text("Delete conversation?") },
                text = { Text("This will remove the chat from your list.") },
                confirmButton = {
                    TextButton(onClick = {
                        homeViewModel.deleteChat(chatId, forEveryone = false)
                        showThreadDeleteDialog = null
                    }) { Text("Delete for Me", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { showThreadDeleteDialog = null }) { Text("Cancel") }
                }
            )
        }

        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> {
                    // --- CHATS LIST ---
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = chatList,
                            key = { it.id }
                        ) { chat ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    when (value) {
                                        SwipeToDismissBoxValue.EndToStart -> {
                                            showThreadDeleteDialog = chat.id
                                            false
                                        }
                                        SwipeToDismissBoxValue.StartToEnd -> {
                                            homeViewModel.toggleArchive(chat.id, archive = true)
                                            true
                                        }
                                        else -> false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val direction = dismissState.dismissDirection
                                    val color = if (direction == SwipeToDismissBoxValue.StartToEnd) Color(0xFF4CAF50) else Color.Red
                                    val icon = if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Archive else Icons.Default.Delete
                                    val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(color)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = alignment
                                    ) {
                                        Icon(icon, contentDescription = null, tint = Color.White)
                                    }
                                },
                                content = {
                                    Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                                        ChatListItem(
                                            username = chat.username,
                                            lastMessage = chat.lastMessage,
                                            timestamp = chat.timestamp,
                                            profileUrl = chat.profilePictureUrl,
                                            onClick = { onChatClick(chat.id) }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
                1 -> {
                    // --- CALLS TAB ---
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Calls Coming Soon")
                    }
                }
                2 -> {
                    // --- STORIES TAB ---
                    // Now calling the actual Stories Screen!
                    StoriesScreen(navController = navController, viewModel = storiesViewModel)
                }
            }
        }
    }
}