package com.example.sparks.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.sparks.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactProfileScreen(
    navController: NavController,
    userId: String
) {
    var user by remember { mutableStateOf<User?>(null) }
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    val firestore = FirebaseFirestore.getInstance()

    // --- STATES ---
    var showBlockDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showDisappearingDialog by remember { mutableStateOf(false) }

    var nicknameText by remember { mutableStateOf("") }

    // Feature States
    var isMuted by remember { mutableStateOf(false) }
    var isBlocked by remember { mutableStateOf(false) }
    var isFollowing by remember { mutableStateOf(false) } // NEW: Follow State
    var selectedDisappearingLabel by remember { mutableStateOf("Off") }

    // --- MAPPING LOGIC (Disappearing Messages) ---
    val durationMap = mapOf(
        "Off" to 0L, "4 weeks" to 2419200L, "1 week" to 604800L, "96 hours" to 345600L,
        "24 hours" to 86400L, "1 hour" to 3600L, "30 minutes" to 1800L, "10 minutes" to 600L, "10 seconds" to 10L
    )
    fun getLabelFromSeconds(seconds: Long): String = durationMap.entries.find { it.value == seconds }?.key ?: "Off"
    val disappearingOptions = durationMap.keys.toList()

    // --- HELPERS ---
    fun getChatId(user1: String, user2: String): String = if (user1 < user2) "${user1}_${user2}" else "${user2}_${user1}"

    fun formatLastSeen(timestamp: Long): String {
        if (timestamp == 0L) return "Offline"
        val diff = System.currentTimeMillis() - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            diff < 60_000 -> "Active just now"
            minutes < 60 -> "Last seen $minutes min ago"
            hours < 24 -> "Last seen $hours hours ago"
            else -> "Last seen $days days ago"
        }
    }

    // --- ACTIONS ---

    // 1. Toggle Mute
    fun toggleMute() {
        if (currentUser == null) return
        val chatId = getChatId(currentUser.uid, userId)
        val newMuteState = !isMuted

        val update = if (newMuteState) FieldValue.arrayUnion(currentUser.uid) else FieldValue.arrayRemove(currentUser.uid)

        firestore.collection("conversations").document(chatId)
            .update("mutedBy", update)
            .addOnSuccessListener {
                isMuted = newMuteState
                val msg = if (newMuteState) "Notifications muted" else "Notifications unmuted"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
    }

    // 2. Toggle Block
    fun toggleBlock() {
        if (currentUser == null) return
        val newBlockState = !isBlocked
        val update = if (newBlockState) FieldValue.arrayUnion(userId) else FieldValue.arrayRemove(userId)

        firestore.collection("users").document(currentUser.uid)
            .update("blockedIds", update)
            .addOnSuccessListener {
                isBlocked = newBlockState
                val msg = if (newBlockState) "${user?.firstName} blocked" else "${user?.firstName} unblocked"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
    }

    // 3. NEW: Toggle Follow
    fun toggleFollow() {
        if (currentUser == null) return
        val newFollowState = !isFollowing
        // We store "followingIds" on the current user's document
        val update = if (newFollowState) FieldValue.arrayUnion(userId) else FieldValue.arrayRemove(userId)

        firestore.collection("users").document(currentUser.uid)
            .update("followingIds", update)
            .addOnSuccessListener {
                isFollowing = newFollowState
                val msg = if (newFollowState) "Following ${user?.firstName}" else "Unfollowed ${user?.firstName}"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                // If the field doesn't exist, create it (merge)
                if (newFollowState) {
                    firestore.collection("users").document(currentUser.uid)
                        .set(mapOf("followingIds" to listOf(userId)), SetOptions.merge())
                        .addOnSuccessListener { isFollowing = true }
                }
            }
    }

    // 4. Report Spam
    fun reportSpam() {
        if (currentUser == null) return
        val report = hashMapOf(
            "reporterId" to currentUser.uid,
            "reportedUserId" to userId,
            "timestamp" to System.currentTimeMillis(),
            "reason" to "spam"
        )
        firestore.collection("reports").add(report)
            .addOnSuccessListener {
                Toast.makeText(context, "Report sent. Thank you.", Toast.LENGTH_SHORT).show()
            }
    }

    // --- REAL-TIME DATA LISTENER ---
    DisposableEffect(userId) {
        val listener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    user = snapshot.toObject(User::class.java)
                    if (nicknameText.isEmpty()) { nicknameText = user?.firstName ?: "" }
                }
            }
        onDispose { listener.remove() }
    }

    LaunchedEffect(userId) {
        if (currentUser == null) return@LaunchedEffect

        // Check Chat Settings (Mute / Disappearing)
        try {
            val chatId = getChatId(currentUser.uid, userId)
            firestore.collection("conversations").document(chatId).addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    val mutedBy = doc.get("mutedBy") as? List<String> ?: emptyList()
                    isMuted = mutedBy.contains(currentUser.uid)

                    val savedSeconds = doc.getLong("disappearingDuration") ?: 0L
                    selectedDisappearingLabel = getLabelFromSeconds(savedSeconds)
                }
            }
        } catch (e: Exception) { }
    }

    // Check Block & Follow Status (on Current User Doc)
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid).addSnapshotListener { doc, _ ->
                if (doc != null) {
                    // Check Blocked
                    val blocked = doc.get("blockedIds") as? List<String> ?: emptyList()
                    isBlocked = blocked.contains(userId)

                    // Check Following
                    val following = doc.get("followingIds") as? List<String> ?: emptyList()
                    isFollowing = following.contains(userId)
                }
            }
        }
    }

    // --- DIALOGS ---

    // 1. Block Dialog
    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text(if (isBlocked) "Unblock ${user?.firstName}?" else "Block ${user?.firstName}?") },
            text = { Text(if (isBlocked) "They will be able to message you again." else "Blocked contacts will not be able to call you or send you messages.") },
            confirmButton = {
                TextButton(onClick = {
                    showBlockDialog = false
                    toggleBlock()
                }) { Text(if (isBlocked) "Unblock" else "Block", color = if(isBlocked) MaterialTheme.colorScheme.primary else Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") }
            }
        )
    }

    // 2. Report Dialog
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Spam") },
            text = { Text("Are you sure you want to report ${user?.firstName} for spam? This conversation will be forwarded to our safety team.") },
            confirmButton = {
                TextButton(onClick = {
                    showReportDialog = false
                    reportSpam()
                }) { Text("Report", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showReportDialog = false }) { Text("Cancel") } }
        )
    }

    // 3. Nickname Dialog
    if (showNicknameDialog) {
        AlertDialog(
            onDismissRequest = { showNicknameDialog = false },
            title = { Text("Set nickname") },
            text = {
                OutlinedTextField(value = nicknameText, onValueChange = { nicknameText = it }, label = { Text("First Name") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = { showNicknameDialog = false; Toast.makeText(context, "Nickname saved (Local only in demo)", Toast.LENGTH_SHORT).show() }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showNicknameDialog = false }) { Text("Cancel") } }
        )
    }

    // 4. Disappearing Messages Dialog
    if (showDisappearingDialog) {
        AlertDialog(
            onDismissRequest = { showDisappearingDialog = false },
            title = { Text("Disappearing messages") },
            text = {
                Column {
                    disappearingOptions.forEach { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    selectedDisappearingLabel = option
                                    showDisappearingDialog = false
                                    if (currentUser != null) {
                                        val chatId = getChatId(currentUser.uid, userId)
                                        val seconds = durationMap[option] ?: 0L
                                        firestore.collection("conversations").document(chatId)
                                            .set(hashMapOf("disappearingDuration" to seconds), SetOptions.merge())
                                            .addOnSuccessListener { Toast.makeText(context, "Timer set to $option", Toast.LENGTH_SHORT).show() }
                                    }
                                }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (selectedDisappearingLabel == option) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (selectedDisappearingLabel == option) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(text = option, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDisappearingDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Avatar
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                if (user?.profileImageUrl != null) {
                    AsyncImage(
                        model = user!!.profileImageUrl,
                        contentDescription = "Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(text = user?.firstName?.firstOrNull()?.toString() ?: "?", fontSize = 40.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Name
            Text(
                text = "${user?.firstName} ${user?.lastName}".trim(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Normal
            )

            // Presence Indicator
            if (user != null) {
                val now = System.currentTimeMillis()
                val diff = now - user!!.lastActive
                val isRealTimeOnline = user!!.isOnline
                val isRecentlyActive = diff < 2 * 60_000
                val showGreen = isRealTimeOnline || isRecentlyActive
                val statusColor = if (showGreen) Color(0xFF4CAF50) else Color.Gray
                val statusText = when {
                    isRealTimeOnline -> "Online"
                    diff < 60_000 -> "Active just now"
                    else -> formatLastSeen(user!!.lastActive)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(statusColor))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = statusText, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Action Buttons (UPDATED SEARCH -> FOLLOW)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ContactActionButton(
                    icon = Icons.Default.Videocam,
                    label = "Video",
                    onClick = { Toast.makeText(context, "Starting Video Call...", Toast.LENGTH_SHORT).show() }
                )
                ContactActionButton(
                    icon = Icons.Default.Call,
                    label = "Audio",
                    onClick = { Toast.makeText(context, "Starting Audio Call...", Toast.LENGTH_SHORT).show() }
                )
                ContactActionButton(
                    icon = if (isMuted) Icons.Filled.NotificationsOff else Icons.Outlined.Notifications,
                    label = if (isMuted) "Unmute" else "Mute",
                    onClick = { toggleMute() }
                )
                // NEW: FOLLOW BUTTON
                ContactActionButton(
                    icon = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                    label = if (isFollowing) "Following" else "Follow",
                    onClick = { toggleFollow() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))

            // 4. Settings List
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                ContactOptionItem(
                    icon = Icons.Default.Timer,
                    title = "Disappearing messages",
                    subtitle = selectedDisappearingLabel,
                    onClick = { showDisappearingDialog = true }
                )
                ContactOptionItem(
                    icon = Icons.Default.Edit,
                    title = "Nickname",
                    subtitle = null,
                    onClick = { showNicknameDialog = true }
                )
                ContactOptionItem(
                    icon = Icons.Default.Palette,
                    title = "Chat color & wallpaper",
                    subtitle = null,
                    onClick = { navController.navigate("settings_appearance_chat_color") }
                )
                ContactOptionItem(
                    icon = Icons.Default.Notifications,
                    title = "Sounds & notifications",
                    subtitle = null,
                    onClick = {}
                )
                ContactOptionItem(
                    icon = Icons.Default.Person,
                    title = "Phone contact info",
                    subtitle = null,
                    onClick = {}
                )
                ContactOptionItem(
                    icon = Icons.Default.Person,
                    title = "View safety number",
                    subtitle = null,
                    onClick = {}
                )
            }

            HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))

            // 5. Destructive Actions
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = if (isBlocked) "Unblock user" else "Block user",
                    color = if (isBlocked) MaterialTheme.colorScheme.primary else Color.Red,
                    fontSize = 17.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBlockDialog = true }
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                )

                Text(
                    "Report spam",
                    color = Color.Red,
                    fontSize = 17.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showReportDialog = true }
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ContactActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            modifier = Modifier
                .size(64.dp)
                .clickable { onClick() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ContactOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.width(24.dp))
        Column {
            Text(text = title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}