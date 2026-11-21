package com.smarteq.pro.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/*
 * "Peach Pulse" palette, approved on the design canvas (2026-11-21).
 * Structure follows a neutral-surfaces + one-accent + harmonising-accents system.
 * Keep res/values/colors.xml (window/splash background) in step with BgDark / BgLight.
 */

// Brand accent and its harmonising colours (peach -> blush -> lilac, plus butter).
val Peach = Color(0xFFFFB599)
val Blush = Color(0xFFF7B7C6)
val Lilac = Color(0xFFCDB9F2)
val Butter = Color(0xFFFFD8A8)

/** Deeper peach for fills (sliders, switches) on light surfaces: 3:1 or better on white. */
val PeachDeep = Color(0xFFD9714A)

/** Light-mode primary (button fills): white text on it passes 4.5:1. */
val PeachInk = Color(0xFFB5532C)

// Dark mode
val BgDark = Color(0xFF141113)
val CardDark = Color(0xFF1E191C)
val SoftDark = Color(0xFF2A2327)
val LineDark = Color(0xFF3A3136)
val InkDark = Color(0xFFFBF4F1)
val Ink2Dark = Color(0xFFD2C4C0)
val MutedDark = Color(0xFFBFB2AE)
val TintDark = Color(0xFF3D2820)
val AccentTextDark = Color(0xFFFFC4AC)
val OnPeach = Color(0xFF2A140C)

// Light mode
val BgLight = Color(0xFFFBF5F2)
val CardLight = Color(0xFFFFFFFF)
val SoftLight = Color(0xFFF3E8E3)
val LineLight = Color(0xFFE9DBD5)
val InkLight = Color(0xFF1E1517)
val Ink2Light = Color(0xFF54474A)
val MutedLight = Color(0xFF75666A)
val TintLight = Color(0xFFFFE6DB)
val AccentTextLight = Color(0xFF9C4527)

// Status colours
val ErrorDark = Color(0xFFFF8A80)
val ErrorLight = Color(0xFFB3261E)
val SpotifyGreen = Color(0xFF1DB954)

/**
 * Fader colour for band [index] of [total]: a blend across peach -> blush -> lilac
 * in dark mode; the single deeper peach in light mode (pastels vanish on white).
 */
fun bandColor(index: Int, total: Int, dark: Boolean = true): Color {
    if (!dark) return PeachDeep
    val x = if (total <= 1) 0f else index.toFloat() / (total - 1)
    return if (x < 0.5f) lerp(Peach, Blush, x * 2f) else lerp(Blush, Lilac, (x - 0.5f) * 2f)
}
