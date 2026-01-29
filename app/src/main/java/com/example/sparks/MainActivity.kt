package com.example.sparks

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
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
import com.example.sparks.ui.home.ArchivedChatsScreen
import com.example.sparks.ui.home.HomeScreen
import com.example.sparks.ui.profile.ContactProfileScreen
import com.example.sparks.ui.profile.UserProfileScreen
import com.example.sparks.ui.settings.AccountSettingsScreen
import com.example.sparks.ui.settings.AppearanceSettingsScreen
import com.example.sparks.ui.settings.ChatColorWallpaperScreen
import com.example.sparks.ui.settings.ChatSettingsScreen
import com.example.sparks.ui.settings.SettingsScreen
import com.example.sparks.ui.settings.WallpaperPickerScreen
import com.example.sparks.ui.theme.SparksTheme
import com.example.sparks.viewmodel.AuthViewModel
import com.example.sparks.viewmodel.HomeViewModel
import com.example.sparks.viewmodel.ThemeViewModel
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            // Observe Theme State
            val themeMode by themeViewModel.themeMode.collectAsState()
            val chatColor by themeViewModel.chatColor.collectAsState()

            // Pass dynamic values to Theme
            SparksTheme(
                themeMode = themeMode,
                primaryColor = chatColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SparksApp(authViewModel, themeViewModel)
                }
            }
        }
    }
}

@Composable
fun SparksApp(
    authViewModel: AuthViewModel,
    themeViewModel: ThemeViewModel
) {
    val navController = rememberNavController()

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
                onSignUpSuccess = {
                    navController.navigate("profile_setup")
                }
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

        // --- HOME ---
        composable("home") {
            HomeScreen(
                authViewModel = viewModel(), // Or pass authViewModel passed to SparksApp
                onChatClick = { chatId -> navController.navigate("chat/$chatId") },
                onFabClick = { navController.navigate("new_chat") }, // Fixed: Navigates to New Chat
                onProfileClick = { navController.navigate("profile") },
                onSettingsClick = { navController.navigate("settings") }, // <--- FIXED: Points to settings
                onArchivedClick = { navController.navigate("archived") }
            )
        }

        // --- SETTINGS (Main) ---
        composable("settings") {
            SettingsScreen(navController, authViewModel)
        }

        // --- SUB SETTINGS ---
        composable("settings_account") {
            AccountSettingsScreen(navController)
        }

        composable("settings_chats") {
            ChatSettingsScreen(navController)
        }

        composable("settings_appearance") {
            AppearanceSettingsScreen(navController, themeViewModel)
        }

        composable("settings_appearance_chat_color") {
            ChatColorWallpaperScreen(navController, themeViewModel)
        }

        composable("settings_wallpaper_picker") {
            WallpaperPickerScreen(navController, themeViewModel)
        }

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
                chatName = "Chat", // Ideally pass chatName in route for better UX
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
                String(
                    Base64.decode(encodedUrl, Base64.URL_SAFE or Base64.NO_WRAP),
                    StandardCharsets.UTF_8
                )
            } catch (e: Exception) {
                ""
            }
            ImageViewerScreen(navController, url)
        }

        // --- PROFILES ---

        // 1. My Profile
        composable("profile") {
            val profileAuthViewModel: AuthViewModel = viewModel()
            val photoPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia(),
                onResult = { uri ->
                    if (uri != null) {
                        profileAuthViewModel.uploadProfilePicture(uri)
                    }
                }
            )
            UserProfileScreen(
                navController = navController,
                onPhotoClicked = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }

        // 2. Contact Profile
        composable(
            route = "contact_profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ContactProfileScreen(navController, userId)
        }
    }
}