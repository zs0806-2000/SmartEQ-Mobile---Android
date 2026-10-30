package com.smarteq.pro.ui.theme

import androidx.compose.ui.graphics.Color

// Brand anchors, carried over from the web app's gradient identity
val BrandPink = Color(0xFFEC4899)
val BrandPurple = Color(0xFFA855F7)
val BrandTeal = Color(0xFF06B6D4)

val SurfaceDeepest = Color(0xFF0B0914)
val SurfacePanel = Color(0xFF1A1525)
val SurfacePanelAlt = Color(0xFF13101E)
val SurfaceContainerHigh = Color(0xFF221C33)

val OnSurfacePrimary = Color(0xFFE5E5E5)
val OnSurfaceSecondary = Color(0xFF9CA3AF)

val SpotifyGreen = Color(0xFF1DB954)
val WarningAmber = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF4444)
val SuccessGreen = Color(0xFF22C55E)

/** Per-band gradient across pink -> purple -> teal, matching getBandColor() in App.tsx. */
fun bandColor(index: Int, total: Int): Color {
    val percent = if (total <= 1) 0f else index.toFloat() / (total - 1)
    val (c1, c2, p) = if (percent < 0.5f) {
        Triple(BrandPink, BrandPurple, percent * 2f)
    } else {
        Triple(BrandPurple, BrandTeal, (percent - 0.5f) * 2f)
    }
    return lerpColor(c1, c2, p)
}

private fun lerpColor(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f,
)
