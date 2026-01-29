package com.example.sparks.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun SparksTheme(
    // 1. Add parameters for dynamic control
    themeMode: com.example.sparks.viewmodel.AppThemeMode,
    primaryColor: Color,
    content: @Composable () -> Unit
) {
    // 2. Determine if we should be dark based on selection
    val darkTheme = when (themeMode) {
        com.example.sparks.viewmodel.AppThemeMode.LIGHT -> false
        com.example.sparks.viewmodel.AppThemeMode.DARK -> true
        com.example.sparks.viewmodel.AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // 3. Apply the selected Primary Color to the palettes
    val DarkColorScheme = darkColorScheme(
        primary = primaryColor, // Dynamic!
        onPrimary = Color.White,
        secondary = primaryColor,
        tertiary = DarkGrayBubble,
        background = DarkBackground,
        surface = DarkBackground,
        onBackground = DarkText,
        onSurface = DarkText
    )

    val LightColorScheme = lightColorScheme(
        primary = primaryColor, // Dynamic!
        onPrimary = Color.White,
        secondary = primaryColor,
        tertiary = LightGrayBubble,
        background = LightBackground,
        surface = LightBackground,
        onBackground = LightText,
        onSurface = LightText,
        surfaceVariant = LightSurface
    )

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}