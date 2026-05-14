package com.m2h.m2haichatbot.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CoralPrimary,
    secondary = CoralActive,
    tertiary = Success,
    background = SurfaceDark,
    surface = SurfaceDark,
    onPrimary = Canvas,
    onSecondary = Canvas,
    onTertiary = Canvas,
    onBackground = Canvas,
    onSurface = Canvas,
)

private val LightColorScheme = lightColorScheme(
    primary = CoralPrimary,
    secondary = CoralActive,
    tertiary = Success,
    background = Canvas,
    surface = SurfaceCard,
    onPrimary = Canvas,
    onSecondary = Canvas,
    onTertiary = Canvas,
    onBackground = Ink,
    onSurface = Ink,
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
        content = content
    )
}
