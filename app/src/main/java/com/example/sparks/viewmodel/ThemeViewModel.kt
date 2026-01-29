package com.example.sparks.viewmodel

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.example.sparks.ui.theme.SignalBlue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

// 1. Define the Wallpaper Types
sealed class Wallpaper {
    data class SolidColor(val color: Color) : Wallpaper()
    data class Image(val uri: Uri) : Wallpaper()
    object None : Wallpaper() // Default white/black
}

class ThemeViewModel : ViewModel() {
    // Theme Mode
    private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    val themeMode = _themeMode.asStateFlow()

    // Chat Bubble Color (Accents)
    private val _chatColor = MutableStateFlow(SignalBlue)
    val chatColor = _chatColor.asStateFlow()

    // 2. Wallpaper State
    private val _currentWallpaper = MutableStateFlow<Wallpaper>(Wallpaper.None)
    val currentWallpaper = _currentWallpaper.asStateFlow()

    // 3. Dimming State
    private val _dimWallpaperInDarkMode = MutableStateFlow(false)
    val dimWallpaperInDarkMode = _dimWallpaperInDarkMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
    }

    fun setChatColor(color: Color) {
        _chatColor.value = color
    }

    fun setWallpaper(wallpaper: Wallpaper) {
        _currentWallpaper.value = wallpaper
    }

    fun setDimWallpaper(dim: Boolean) {
        _dimWallpaperInDarkMode.value = dim
    }

    // Preset Colors (Reusing your previous list, but also for wallpapers)
    val presetColors = listOf(
        Color(0xFFE91E63), Color(0xFFE67C73), Color(0xFF795548),
        Color(0xFF4CAF50), Color(0xFF2E7D32), Color(0xFF00BCD4),
        Color(0xFF607D8B), Color(0xFF3F51B5), Color(0xFF9C27B0),
        Color(0xFFFF4081), SignalBlue, Color(0xFF9E9E9E),
        Color(0xFFFFC107), Color(0xFF212121)
    )

    // Softer Pastel colors specifically for Wallpapers (optional, matches Signal style)
    val wallpaperPresets = listOf(
        Color(0xFFEFEBE9), Color(0xFFFCE4EC), Color(0xFFE0F2F1),
        Color(0xFFFFF3E0), Color(0xFFE8EAF6), Color(0xFFF3E5F5),
        Color(0xFFE1F5FE), Color(0xFFF1F8E9)
    ) + presetColors // Combine them
}