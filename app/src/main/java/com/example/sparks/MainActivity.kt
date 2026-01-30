package com.example.sparks

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sparks.ui.auth.AuthScreen
import com.example.sparks.ui.auth.ProfileSetupScreen
import com.example.sparks.ui.chat.ChatScreen
import com.example.sparks.ui.chat.GroupDetailsScreen
import com.example.sparks.ui.chat.ImageViewerScreen
import com.example.sparks.ui.chat.NewChatScreen
import com.example.sparks.ui.chat.NewGroupScreen
import com.example.sparks.ui.common.VideoPlayer
import com.example.sparks.ui.home.ArchivedChatsScreen
import com.example.sparks.ui.home.HomeScreen
import com.example.sparks.ui.home.StoryViewerScreen
import com.example.sparks.ui.profile.ContactProfileScreen
import com.example.sparks.ui.profile.UserProfileScreen
import com.example.sparks.ui.settings.AccountSettingsScreen
import com.example.sparks.ui.settings.AppearanceSettingsScreen
import com.example.sparks.ui.settings.ChatColorWallpaperScreen
import com.example.sparks.ui.settings.ChatSettingsScreen
import com.example.sparks.ui.settings.SettingsScreen
import com.example.sparks.ui.settings.WallpaperPickerScreen
import com.example.sparks.ui.theme.SparksTheme
import com.example.sparks.util.PresenceManager
import com.example.sparks.viewmodel.AuthViewModel
import com.example.sparks.viewmodel.HomeViewModel
import com.example.sparks.viewmodel.ThemeViewModel
import com.example.sparks.viewmodel.StoriesViewModel
import java.nio.charset.StandardCharsets
import kotlin.getValue

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()
    // 1. ADD THIS LINE
    private val storiesViewModel: StoriesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 1. ASK FOR NOTIFICATION PERMISSION (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // 2. OBSERVE APP LIFECYCLE FOR PRESENCE (Consolidated)
        try {
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    PresenceManager.setOnlineStatus(true)
                }
                override fun onStop(owner: LifecycleOwner) {
                    PresenceManager.setOnlineStatus(false)
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            val themeMode by themeViewModel.themeMode.collectAsState()
            val chatColor by themeViewModel.chatColor.collectAsState()

            SparksTheme(themeMode = themeMode, primaryColor = chatColor) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // Pass the Intent to handle deep links
                    SparksApp(authViewModel, themeViewModel, storiesViewModel, intent)
                }
            }
        }
    }
}

@Composable
fun SparksApp(
    authViewModel: AuthViewModel,
    themeViewModel: ThemeViewModel,
    storiesViewModel: StoriesViewModel,
    intent: android.content.Intent? // Add this parameter
) {
    val navController = rememberNavController()

    // CHECK FOR NOTIFICATION CLICK
    LaunchedEffect(intent) {
        val chatId = intent?.getStringExtra("chatId")
        if (!chatId.isNullOrEmpty()) {
            // Small delay to ensure NavGraph is ready
            kotlinx.coroutines.delay(100)
            navController.navigate("chat/$chatId")
        }
    }

    NavHost(navController = navController, startDestination = "auth") {

        // --- AUTH ---
        composable("auth") {
            AuthScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                },
                onSignUpSuccess = { navController.navigate("profile_setup") }
            )
        }

        composable("profile_setup") {
            ProfileSetupScreen(
                viewModel = authViewModel,
                onProfileCompleted = {
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        // --- MAIN APP ---
        composable("home") {
            HomeScreen(
                navController = navController,
                authViewModel = authViewModel,
                storiesViewModel = storiesViewModel, // 4. Pass shared instance here
                onChatClick = { chatId -> navController.navigate("chat/$chatId") },
                onFabClick = { navController.navigate("new_chat") },
                onProfileClick = { navController.navigate("settings") },
                onSettingsClick = { navController.navigate("settings") },
                onArchivedClick = { navController.navigate("archived") }
            )
        }

        // --- SETTINGS ---
        composable("settings") { SettingsScreen(navController, authViewModel) }
        composable("settings_account") { AccountSettingsScreen(navController) }
        composable("settings_chats") { ChatSettingsScreen(navController) }
        composable("settings_appearance") { AppearanceSettingsScreen(navController, themeViewModel) }
        composable("settings_appearance_chat_color") { ChatColorWallpaperScreen(navController, themeViewModel) }
        composable("settings_wallpaper_picker") { WallpaperPickerScreen(navController, themeViewModel) }

        // --- CHAT CREATION ---
        composable("new_chat") {
            NewChatScreen(navController)
        }

        composable("new_group_select") {
            NewGroupScreen(navController) { selectedIds ->
                Log.d("DEBUG", "Selected IDs: $selectedIds")
                val idsString = selectedIds.joinToString(",")
                navController.navigate("new_group_details/$idsString")
            }
        }

        composable("new_group_details/{ids}") { backStackEntry ->
            val ids = backStackEntry.arguments?.getString("ids") ?: ""
            GroupDetailsScreen(navController, ids)
        }

        // --- CHAT SCREEN ---
        composable(
            route = "chat/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatScreen(
                navController = navController,
                chatId = chatId,
                chatName = "Chat",
                onTitleClick = { userId -> navController.navigate("contact_profile/$userId") },
                themeViewModel = themeViewModel
            )
        }

        // --- ARCHIVED ---
        composable("archived") {
            val homeViewModel: HomeViewModel = viewModel()
            ArchivedChatsScreen(navController, homeViewModel)
        }

        // --- MEDIA VIEWER ---
        composable("image_viewer/{encodedUrl}") { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("encodedUrl") ?: ""
            val url = try {
                String(Base64.decode(encodedUrl, Base64.URL_SAFE or Base64.NO_WRAP), StandardCharsets.UTF_8)
            } catch (e: Exception) { "" }
            ImageViewerScreen(navController, url)
        }

        // --- PROFILES ---
        composable("profile") {
            val profileAuthViewModel: AuthViewModel = viewModel()
            val photoPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia(),
                onResult = { uri -> if (uri != null) profileAuthViewModel.uploadProfilePicture(uri) }
            )
            UserProfileScreen(
                navController = navController,
                onPhotoClicked = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            )
        }

        composable(
            route = "contact_profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ContactProfileScreen(navController, userId)
        }

        // --- STORY VIEWER ---
        composable(
            route = "story_viewer/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            StoryViewerScreen(
                navController = navController,
                userId = userId,
                viewModel = storiesViewModel // 5. Pass shared instance here
            )
        }

        // --- CHAT VIDEO PLAYER ---
        composable(
            route = "video_player/{encodedUri}",
            arguments = listOf(navArgument("encodedUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("encodedUri") ?: ""
            val uriString = try {
                String(Base64.decode(encodedUri, Base64.URL_SAFE or Base64.NO_WRAP), StandardCharsets.UTF_8)
            } catch (e: Exception) { "" }

            // Re-use our VideoPlayer helper
            com.example.sparks.ui.common.VideoPlayer(
                uri = Uri.parse(uriString),
                autoPlay = true,
                useController = true
            )
        }
    }
}