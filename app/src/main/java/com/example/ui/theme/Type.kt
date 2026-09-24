package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Master Persian & Numeric Typography System for Student OS.
 * 
 * Design Principles:
 * 1. Persian RTL Optimization: Uses native Vazirmatn font with generous line-height
 *    (1.4x - 1.6x) to prevent ascender/descender clipping on complex Persian ligatures.
 * 2. Tabular Numeric Clarity: Uses Plus Jakarta Sans for high-legibility English numerals
 *    (GPA, dates, timers, badges, and counters).
 * 3. Consistent Material 3 Hierarchy: From large display headers down to micro captions.
 */
val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn, FontWeight.Normal)
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal)
)

val PlusJakartaSansFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal)
)

private val DefaultLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    displayMedium = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    displaySmall = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    headlineLarge = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    headlineMedium = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    headlineSmall = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    titleLarge = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    titleMedium = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.5.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    titleSmall = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.5.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    bodyLarge = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    bodyMedium = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    bodySmall = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    labelLarge = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.5.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    labelMedium = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    labelSmall = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = DefaultLineHeightStyle
    )
)

/**
 * Bold, high-clarity typography specifically tuned for prominent English digits
 * (GPA numbers, counters, time codes, unit badges, and stats).
 */
val NumericDisplayHero = TextStyle(
    fontFamily = PlusJakartaSansFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 32.sp,
    lineHeight = 38.sp,
    letterSpacing = (-0.5).sp
)

val NumericDisplayStat = TextStyle(
    fontFamily = PlusJakartaSansFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    letterSpacing = 0.sp
)

val NumericBadgeText = TextStyle(
    fontFamily = PlusJakartaSansFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    lineHeight = 17.sp,
    letterSpacing = 0.2.sp
)

/**
 * Additional semantic typography helpers for clean, expressive layouts.
 */
object StudentTextTokens {
    val SectionHeader = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )

    val CardTitle = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )

    val MetaText = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    )

    val BadgeText = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )

    val MonoCodeText = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp
    )

    val MonoNumericText = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    )
}


