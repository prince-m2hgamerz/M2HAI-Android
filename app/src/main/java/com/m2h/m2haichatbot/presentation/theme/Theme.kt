package com.m2h.m2haichatbot.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = CoralPrimary,
    onPrimary = OnPrimary,
    primaryContainer = CoralDisabled,
    onPrimaryContainer = Ink,
    secondary = AccentTeal,
    onSecondary = OnPrimary,
    tertiary = AccentAmber,
    background = Canvas,
    onBackground = Ink,
    surface = Canvas,
    onSurface = Ink,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = Body,
    error = Error,
    onError = OnPrimary,
    outline = Hairline,
    outlineVariant = HairlineSoft,
    secondaryContainer = HairlineSoft,
    onSecondaryContainer = Ink,
    tertiaryContainer = Color(0xFFFDEFD9),
    onTertiaryContainer = Color(0xFF8B5E3C)
)

private val DarkColorScheme = darkColorScheme(
    primary = CoralPrimary,
    onPrimary = OnPrimary,
    primaryContainer = CoralActive,
    onPrimaryContainer = OnDark,
    secondary = AccentTeal,
    onSecondary = Ink,
    tertiary = AccentAmber,
    background = SurfaceDark,
    onBackground = OnDark,
    surface = SurfaceDark,
    onSurface = OnDark,
    surfaceVariant = SurfaceDarkElevated,
    onSurfaceVariant = OnDarkSoft,
    error = Error,
    onError = OnPrimary,
    outline = Color(0xFF3D3D3A),
    outlineVariant = Color(0xFF252523),
    secondaryContainer = Color(0xFF252523),
    onSecondaryContainer = OnDark,
    tertiaryContainer = Color(0xFF332A1E),
    onTertiaryContainer = AccentAmber
)

@Composable
fun M2HAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
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
        shapes = Shapes,
        content = content
    )
}
