package dev.barakah.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.barakah.app.R

/** Preference value: keep the system Arabic face (the app's historical behaviour). */
const val QURAN_FONT_SYSTEM = "system"

/** Preference value: use [AmiriQuranFamily] for Quran text. */
const val QURAN_FONT_AMIRI = "amiri"

/**
 * The Uthmani-script face, used for Quran text only: the verse text, the Bismillah,
 * and the Arabic verse shown in the tafsir dialog. Everything else in the app keeps
 * the system font, so this never changes the look of the surrounding UI.
 */
val AmiriQuranFamily = FontFamily(Font(R.font.amiri_quran))

/**
 * Amiri Quran declares a 2.45em line box (hhea ascent 1.815 + descent 0.634) because
 * its Quranic marks stack far above the letters. Any lineHeight tighter than that
 * clips the marks, so Quran text drawn in this face must use this multiplier.
 */
const val AMIRI_QURAN_LINE_HEIGHT = 2.5f

/** The system Arabic face fits comfortably in 1.5x, which is what this app has always used. */
const val DEFAULT_ARABIC_LINE_HEIGHT = 1.5f

/** Resolves the Quran text face from the stored preference. */
fun quranFontFamilyFor(preference: String): FontFamily =
    if (preference == QURAN_FONT_AMIRI) AmiriQuranFamily else FontFamily.Default

/** Resolves the lineHeight multiplier that goes with [quranFontFamilyFor]. */
fun quranLineHeightFor(preference: String): Float =
    if (preference == QURAN_FONT_AMIRI) AMIRI_QURAN_LINE_HEIGHT else DEFAULT_ARABIC_LINE_HEIGHT

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 54.sp,
        lineHeight = 62.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        lineHeight = 50.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

