package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Compatibility token layer for Student OS.
 *
 * The product source of truth is the Academic Premium palette:
 * Petrol/Navy + Olive with separate semantic status colors.
 *
 * Existing public token names are retained so feature screens can migrate
 * incrementally without a wholesale rewrite.
 */
object StudentOsColors {
    // Core Academic Premium surfaces
    val LightCanvas = Color(0xFFF8FAFC)
    val LightPaper = Color(0xFFF1F5F9)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurface2 = Color(0xFFF1F5F9)
    val LightInk = Color(0xFF0F172A)
    val LightInkSoft = Color(0xFF475569)
    val LightInkFaint = Slate400
    val LightBrand = AcademicNavy
    val LightIndigoStrong = Color(0xFF0A2D3A)
    val LightIndigoSoft = Color(0xFFE8EFF2)

    // Semantic status colors — deliberately separate from brand colors
    val LightAmber = Color(0xFFD97706)
    val LightMint = AcademicOlive
    val LightCoral = LightError
    val LightSky = Sky600

    // Dark academic surfaces
    val DarkCanvas = DarkVelvetCanvas
    val DarkNavyBlack = DeepNavyBlack
    val DarkPaper = DeepSpacePaper
    val DarkSurface = OledCardSurface
    val DarkSurface2 = OledSurfaceElevated
    val DarkInk = Color(0xFFEEF0F6)
    val DarkInkSoft = Color(0xFF9FA8BD)
    val DarkInkFaint = Color(0xFF6D7690)
    val DarkLine = Color(0xFF28324D)

    // Compatibility accent names mapped to the restrained Academic Premium system
    val CyanAccent = AcademicNavyDark
    val CyanGlow = Color(0xFF6FA7B8)
    val EmeraldAccent = AcademicOlive
    val EmeraldGlow = AcademicOliveLight
    val PurpleAccent = Color(0xFF7B6D5A)
    val PurpleGlow = Color(0xFF9A8A73)
    val ElectricBlue = AcademicNavy
    val AmberAccent = AmberGlow
    val CrimsonAccent = CrimsonRose

    // Compatibility aliases
    val CyberCyan = CyanAccent
    val SkyCyan = CyanGlow
    val EmeraldNeon = EmeraldAccent
    val MintGlow = EmeraldGlow
    val CyberViolet = PurpleAccent
    val AmberGlow = AmberAccent
    val CrimsonRose = CrimsonAccent
    val DarkVelvetCanvas = DarkCanvas
    val VioletAccent = PurpleAccent

    // Legacy aliases retained for source compatibility; no neon values are introduced here.
    val DarkIndigo = AcademicNavyDark
    val DarkAmber = Color(0xFFF0B054)
    val DarkMint = AcademicOliveLight
    val DarkCoral = Color(0xFFF0897A)
    val DarkSky = Color(0xFF79A8F2)
}

/**
 * Surface/border compatibility tokens.
 *
 * These remain available to older components, but use opaque/low-noise
 * Academic Premium surfaces rather than prominent glass effects.
 */
object StudentOsGlassTokens {
    val glassPrimary = OledCardSurface
    val glassElevated = OledSurfaceElevated
    val glassSubtle = DeepSpacePaper
    val glassHighlight = Color(0x14FFFFFF)

    val GlassSurfaceDark = glassPrimary
    val GlassSurfaceElevated = glassElevated
    val glassDark = glassPrimary
    val GlassSurfaceLight = LightSurface
    val GlassSurfaceElevatedLight = Color(0xFFFFFFFF)

    val GlassBorderGradient = Brush.linearGradient(
        listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))
    )
    val GlassBorderLightGradient = GlassBorderGradient

    val borderLight = LightOutline
    val borderGradient = GlassBorderGradient
    val borderHighlightGradient = Brush.linearGradient(
        listOf(AcademicNavy.copy(alpha = 0.16f), AcademicNavy.copy(alpha = 0.04f))
    )

    // Compatibility border names, now using brand/semantic tones.
    val borderCyanGradient = Brush.linearGradient(
        listOf(AcademicNavy.copy(alpha = 0.22f), AcademicNavy.copy(alpha = 0.05f))
    )
    val borderEmeraldGradient = Brush.linearGradient(
        listOf(AcademicOlive.copy(alpha = 0.24f), AcademicOlive.copy(alpha = 0.05f))
    )
    val borderPurpleGradient = Brush.linearGradient(
        listOf(Color(0xFF7B6D5A).copy(alpha = 0.20f), Color(0xFF7B6D5A).copy(alpha = 0.05f))
    )
    val borderVioletGradient = borderPurpleGradient

    // Subtle branded gradients retained only for legacy callers.
    val heroPassport = Brush.linearGradient(
        listOf(AcademicNavyDark, AcademicNavy, AcademicOlive)
    )
    val copilotAi = Brush.horizontalGradient(
        listOf(AcademicNavy, AcademicOlive)
    )
    val emeraldProgress = Brush.horizontalGradient(
        listOf(AcademicOlive, AcademicOliveLight)
    )
}

/** Centralized spacing tokens. */
object StudentOsSpacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val smPlus: Dp = 10.dp
    val md: Dp = 12.dp
    val mdPlus: Dp = 14.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val hero: Dp = 32.dp
}

/** Centralized shape compatibility tokens. */
object StudentOsShapes {
    val largeContainer: Dp = 24.dp
    val heroCard: Dp = 20.dp
    val mediumCard: Dp = 16.dp
    val button: Dp = 10.dp
    val pill: Dp = 999.dp
}
