package com.bmtp.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BmtpDarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = Secondary,
    tertiary = MeshAccent,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onPrimary = OnPrimary,
    onSecondary = OnSecondary,
    onBackground = OnBackground,
    onSurface = OnSurface,
    error = ErrorColor,
    onError = OnError
)

/**
 * Application theme composable providing Material3 styling.
 * BMTP uses a dark theme exclusively due to its aesthetic.
 *
 * @param content The composable content to be themed.
 */
@Composable
fun BmtpTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = BmtpDarkColorScheme,
        typography = Typography,
        content = content
    )
}
