package com.example.kropimagecropper.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Modern color palette
val ModernBlue = Color(0xFF667EEA)
val ModernPurple = Color(0xFF764BA2)
val ModernOrange = Color(0xFFFF6B6B)
val ModernTeal = Color(0xFF4ECDC4)
val ModernGreen = Color(0xFF45B7D1)

// Light theme colors
private val ModernLightColorScheme = lightColorScheme(
    primary = ModernBlue,
    onPrimary = Color.White,
    primaryContainer = ModernBlue.copy(alpha = 0.1f),
    onPrimaryContainer = ModernBlue,

    secondary = ModernOrange,
    onSecondary = Color.White,
    secondaryContainer = ModernOrange.copy(alpha = 0.1f),
    onSecondaryContainer = ModernOrange,

    tertiary = ModernTeal,
    onTertiary = Color.White,
    tertiaryContainer = ModernTeal.copy(alpha = 0.1f),
    onTertiaryContainer = ModernTeal,

    background = Color(0xFFFAFBFF),
    onBackground = Color(0xFF1A1C1E),

    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFF3F4F9),
    onSurfaceVariant = Color(0xFF44474F),

    outline = Color(0xFFCAC4D0),
    outlineVariant = Color(0xFFE7E0EC),

    error = Color(0xFFE53E3E),
    onError = Color.White,
    errorContainer = Color(0xFFFFEDED),
    onErrorContainer = Color(0xFF8B0000),

    surfaceTint = ModernBlue
)

// Dark theme colors
private val ModernDarkColorScheme = darkColorScheme(
    primary = ModernBlue.copy(alpha = 0.9f),
    onPrimary = Color.White,
    primaryContainer = ModernBlue.copy(alpha = 0.2f),
    onPrimaryContainer = ModernBlue.copy(alpha = 0.9f),

    secondary = ModernOrange.copy(alpha = 0.9f),
    onSecondary = Color.White,
    secondaryContainer = ModernOrange.copy(alpha = 0.2f),
    onSecondaryContainer = ModernOrange.copy(alpha = 0.9f),

    tertiary = ModernTeal.copy(alpha = 0.9f),
    onTertiary = Color.White,
    tertiaryContainer = ModernTeal.copy(alpha = 0.2f),
    onTertiaryContainer = ModernTeal.copy(alpha = 0.9f),

    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E1E5),

    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2D2D30),
    onSurfaceVariant = Color(0xFFCAC4D0),

    outline = Color(0xFF938F96),
    outlineVariant = Color(0xFF44474F),

    error = Color(0xFFFF6B6B),
    onError = Color.White,
    errorContainer = Color(0xFF3E1414),
    onErrorContainer = Color(0xFFFF6B6B),

    surfaceTint = ModernBlue
)

@Composable
fun KropImageCropperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled to use our modern colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ModernDarkColorScheme
        else -> ModernLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.copy(alpha = 0.95f).toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ModernTypography,
        shapes = ModernShapes,
        content = content
    )
}