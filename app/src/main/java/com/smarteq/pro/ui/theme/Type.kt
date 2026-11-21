package com.smarteq.pro.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The full Material 3 type scale for the chosen [AppFont]. Every role is defined, so no
 * text falls back to the system font. Headings use the font's heading face and weight;
 * everything else uses its text face. Labels use tabular figures so dB values line up.
 */
fun typographyFor(font: AppFont): Typography {
    val h = font.heading
    val b = font.body
    val hw = font.headingWeight
    val tnum = "tnum"
    return Typography(
        displayLarge = TextStyle(fontFamily = h, fontWeight = hw, fontSize = 48.sp, lineHeight = 54.sp, letterSpacing = (-1).sp),
        displayMedium = TextStyle(fontFamily = h, fontWeight = hw, fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.8).sp),
        displaySmall = TextStyle(fontFamily = h, fontWeight = hw, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.6).sp, fontFeatureSettings = tnum),
        headlineLarge = TextStyle(fontFamily = h, fontWeight = hw, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
        headlineMedium = TextStyle(fontFamily = h, fontWeight = hw, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.4).sp),
        headlineSmall = TextStyle(fontFamily = h, fontWeight = hw, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp),
        titleLarge = TextStyle(fontFamily = h, fontWeight = hw, fontSize = 20.sp, lineHeight = 26.sp),
        titleMedium = TextStyle(fontFamily = b, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
        titleSmall = TextStyle(fontFamily = b, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = b, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = b, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = b, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
        labelLarge = TextStyle(fontFamily = b, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp, fontFeatureSettings = tnum),
        labelMedium = TextStyle(fontFamily = b, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, fontFeatureSettings = tnum),
        labelSmall = TextStyle(fontFamily = b, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.4.sp, fontFeatureSettings = tnum),
    )
}
