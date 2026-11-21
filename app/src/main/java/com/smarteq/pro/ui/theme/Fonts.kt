package com.smarteq.pro.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.smarteq.pro.R

/**
 * The fonts the user can pick in Settings › Appearance. All are bundled in the app
 * (res/font, SIL Open Font License, texts in assets/licenses/fonts-OFL.txt), so choosing
 * one never downloads anything.
 *
 * Each file is a variable font; one file covers every weight.
 */
enum class AppFont(
    val id: String,
    val label: String,
    private val headingRes: Int,
    private val bodyRes: Int,
    /** Weight used for headlines and large numbers in this font. */
    val headingWeight: FontWeight,
) {
    SORA("sora", "Sora", R.font.font_sora, R.font.font_sora, FontWeight.SemiBold),
    URBANIST("urbanist", "Urbanist", R.font.font_urbanist, R.font.font_urbanist, FontWeight.ExtraBold),
    OUTFIT("outfit", "Outfit", R.font.font_outfit, R.font.font_outfit, FontWeight.Bold),
    LEXEND("lexend", "Lexend", R.font.font_lexend, R.font.font_lexend, FontWeight.SemiBold),
    RED_HAT("red_hat", "Red Hat", R.font.font_redhatdisplay, R.font.font_redhattext, FontWeight.ExtraBold),
    ALBERT_SANS("albert_sans", "Albert Sans", R.font.font_albertsans, R.font.font_albertsans, FontWeight.Bold),
    READEX_PRO("readex_pro", "Readex Pro", R.font.font_readexpro, R.font.font_readexpro, FontWeight.SemiBold),
    NUNITO("nunito", "Nunito", R.font.font_nunito, R.font.font_nunito, FontWeight.ExtraBold);

    val heading: FontFamily by lazy { variableFamily(headingRes) }
    val body: FontFamily by lazy { variableFamily(bodyRes) }

    companion object {
        val DEFAULT = SORA
        fun fromId(id: String?): AppFont = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

private val WEIGHTS = listOf(400, 500, 600, 700, 800)

@OptIn(ExperimentalTextApi::class)
private fun variableFamily(res: Int): FontFamily = FontFamily(
    WEIGHTS.map { w ->
        Font(
            resId = res,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w)),
        )
    }
)
