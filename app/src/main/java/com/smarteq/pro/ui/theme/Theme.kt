package com.smarteq.pro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Light/dark choice from Settings › Appearance. */
enum class ThemeMode(val id: String, val label: String) {
    SYSTEM("system", "Match phone"),
    DARK("dark", "Dark"),
    LIGHT("light", "Light");

    companion object {
        fun fromId(id: String?): ThemeMode = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

/** Tokens Material's colour scheme has no slot for: liquid-card colours and the accent family. */
@Immutable
data class PeachTokens(
    val isDark: Boolean,
    val card: Color,
    val soft: Color,
    val line: Color,
    val tint: Color,
    val accentText: Color,
    /** Colour used for fills (sliders, active switches) on this mode's surfaces. */
    val fill: Color,
    /** Harmonising colours pooled inside liquid cards. */
    val harmony: List<Color>,
    /** Strength of those pools (0..1). */
    val poolAlpha: Float,
)

val LocalPeach = staticCompositionLocalOf {
    PeachTokens(true, CardDark, SoftDark, LineDark, TintDark, AccentTextDark, Peach, listOf(Peach, Blush, Lilac, Butter), 0.20f)
}

private val DarkColors = darkColorScheme(
    primary = Peach,
    onPrimary = OnPeach,
    primaryContainer = TintDark,
    onPrimaryContainer = AccentTextDark,
    secondary = Blush,
    onSecondary = OnPeach,
    secondaryContainer = SoftDark,
    onSecondaryContainer = InkDark,
    tertiary = Lilac,
    onTertiary = Color(0xFF1C1430),
    tertiaryContainer = Color(0xFF2C2440),
    onTertiaryContainer = Color(0xFFE6DCFA),
    background = BgDark,
    onBackground = InkDark,
    surface = CardDark,
    onSurface = InkDark,
    surfaceVariant = SoftDark,
    onSurfaceVariant = MutedDark,
    surfaceContainerLowest = BgDark,
    surfaceContainerLow = Color(0xFF191517),
    surfaceContainer = CardDark,
    surfaceContainerHigh = SoftDark,
    surfaceContainerHighest = Color(0xFF332B30),
    inverseSurface = InkDark,
    inverseOnSurface = BgDark,
    inversePrimary = PeachDeep,
    outline = Color(0xFF7A6D70),
    outlineVariant = LineDark,
    error = ErrorDark,
    onError = Color(0xFF3B0A06),
    errorContainer = Color(0xFF4A1A15),
    onErrorContainer = Color(0xFFFFDAD5),
    scrim = Color.Black,
)

private val LightColors = lightColorScheme(
    primary = PeachInk,
    onPrimary = Color.White,
    primaryContainer = TintLight,
    onPrimaryContainer = AccentTextLight,
    secondary = Color(0xFFB4576F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE4EA),
    onSecondaryContainer = Color(0xFF5A2333),
    tertiary = Color(0xFF6B55A8),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE6FB),
    onTertiaryContainer = Color(0xFF2E1F5C),
    background = BgLight,
    onBackground = InkLight,
    surface = CardLight,
    onSurface = InkLight,
    surfaceVariant = SoftLight,
    onSurfaceVariant = MutedLight,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFDF9F7),
    surfaceContainer = CardLight,
    surfaceContainerHigh = SoftLight,
    surfaceContainerHighest = Color(0xFFECDFD9),
    inverseSurface = InkLight,
    inverseOnSurface = BgLight,
    inversePrimary = Peach,
    outline = Color(0xFF8E7F83),
    outlineVariant = LineLight,
    error = ErrorLight,
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    scrim = Color.Black,
)

@Composable
fun SmartEqTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    font: AppFont = AppFont.DEFAULT,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val colorScheme = if (dark) DarkColors else LightColors
    val tokens = if (dark) {
        PeachTokens(true, CardDark, SoftDark, LineDark, TintDark, AccentTextDark, Peach, listOf(Peach, Blush, Lilac, Butter), 0.20f)
    } else {
        PeachTokens(false, CardLight, SoftLight, LineLight, TintLight, AccentTextLight, PeachDeep, listOf(Peach, Blush, Lilac, Butter), 0.30f)
    }
    val typography = remember(font) { typographyFor(font) }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // The app draws edge to edge; only the status/navigation icon colours need setting.
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    CompositionLocalProvider(LocalPeach provides tokens) {
        MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
    }
}
