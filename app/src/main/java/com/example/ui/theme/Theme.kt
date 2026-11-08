package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlueLight,
    onPrimary = Color.White,
    primaryContainer = ElectricBlue,
    onPrimaryContainer = Color.White,
    secondary = NeonCyan,
    onSecondary = ObsidianBackground,
    secondaryContainer = ObsidianSurfaceHighlight,
    onSecondaryContainer = NeonCyanGlow,
    tertiary = NeonCyanGlow,
    onTertiary = ObsidianBackground,
    background = ObsidianBackground,
    onBackground = SlateTextPrimary,
    surface = ObsidianSurface,
    onSurface = SlateTextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = SlateTextSecondary,
    outline = ObsidianBorder,
    error = ErrorColor,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = LightBlueAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = LightBlueAccent,
    secondary = ElectricBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFF6FF),
    onSecondaryContainer = LightBlueAccent,
    tertiary = NeonCyan,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = ErrorColor,
    onError = Color.White
)

@Composable
fun MyAiTheme(
    darkTheme: Boolean = true, // Default to dark theme per Meta AI design requirements
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
