package com.smarteq.pro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * SmartEQ Pro is intentionally always-dark by default (matching the
 * original web app and the visual identity of the brand), but still
 * exposes a light scheme so the app respects system-level forced-light
 * accessibility settings rather than fighting them — current Material
 * guidance recommends never fully locking out a user's accessibility
 * preferences.
 */
private val DarkColors = darkColorScheme(
    primary = BrandPurple,
    onPrimary = SurfaceDeepest,
    secondary = BrandTeal,
    onSecondary = SurfaceDeepest,
    tertiary = BrandPink,
    onTertiary = SurfaceDeepest,
    background = SurfaceDeepest,
    onBackground = OnSurfacePrimary,
    surface = SurfacePanel,
    onSurface = OnSurfacePrimary,
    surfaceVariant = SurfacePanelAlt,
    onSurfaceVariant = OnSurfaceSecondary,
    surfaceContainerHigh = SurfaceContainerHigh,
    error = ErrorRed,
    outline = OnSurfaceSecondary.copy(alpha = 0.3f),
)

private val LightColors = lightColorScheme(
    primary = BrandPurple,
    secondary = BrandTeal,
    tertiary = BrandPink,
    error = ErrorRed,
)

@Composable
fun SmartEqTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SmartEqTypography,
        content = content,
    )
}
