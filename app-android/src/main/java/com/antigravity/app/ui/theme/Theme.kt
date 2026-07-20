package com.antigravity.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val OrbitColorScheme = darkColorScheme(
    primary = Blurple,
    onPrimary = SolidWhite,
    primaryContainer = RaisedIndigo,
    onPrimaryContainer = SolidWhite,
    secondary = ElectricGreen,
    onSecondary = SolidBlack,
    secondaryContainer = VibrantMagenta,
    onSecondaryContainer = SolidWhite,
    background = IndigoCanvas,
    onBackground = SolidWhite,
    surface = Onyx,
    onSurface = SolidWhite,
    surfaceVariant = RaisedIndigo,
    onSurfaceVariant = TextMuted,
    error = VibrantMagenta,
    onError = SolidWhite
)

@Composable
fun AntigravityTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = OrbitColorScheme.background.toArgb()
            window.navigationBarColor = OrbitColorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = OrbitColorScheme,
        typography = OrbitTypography,
        content = content
    )
}
