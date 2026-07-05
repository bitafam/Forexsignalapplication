package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyberPrimary,
    secondary = CyberSecondary,
    tertiary = CyberTertiary,
    background = CyberObsidian,
    surface = CyberSurface,
    onPrimary = CyberObsidian,
    onSecondary = CyberObsidian,
    onTertiary = CyberObsidian,
    onBackground = CyberTextPrimary,
    onSurface = CyberTextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = CyberTextSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force premium dark mode by default for trading visual comfort
    dynamicColor: Boolean = false, // Disable dynamic colors to keep our unique brand identity
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

