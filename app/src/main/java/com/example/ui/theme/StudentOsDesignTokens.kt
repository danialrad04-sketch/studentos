package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Master Student OS Design Tokens (2026 Architectural Specification).
 * 
 * Aesthetic:
 * - Deep blue-black canvas: Dark Velvet (#07090E), Deep Space Paper (#0E1322)
 * - Glassmorphic surface tokens: GlassSurface (#161C2D with alpha), White/Gradients highlight borders
 * - Vibrant Accents: Cyber Cyan (#06B6D4), Emerald Neon (#10B981), Cyber Purple (#8B5CF6), Electric Blue (#1D4ED8), Amber Glow (#F59E0B)
 */
object StudentOsColors {
    // Light Theme Tokens (Crisp Academic Frost White 2026)
    val LightCanvas = Color(0xFFF8FAFC)
    val LightPaper = Color(0xFFF1F5F9)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurface2 = Color(0xFFF1F5F9)
    val LightInk = Color(0xFF0F172A)
    val LightInkSoft = Color(0xFF475569)
    val LightInkFaint = Color(0xFF94A3B8)
    val LightBrand = Color(0xFF4F46E5)
    val LightIndigoStrong = Color(0xFF312E81)
    val LightIndigoSoft = Color(0xFFEEF2FF)
    val LightAmber = Color(0xFFD97706)
    val LightMint = Color(0xFF059669)
    val LightCoral = Color(0xFFE11D48)
    val LightSky = Color(0xFF0284C7)

    // Dark Velvet & Deep Blue-Black Canvas Tokens
    val DarkCanvas = Color(0xFF07090E)        // Master deep blue-black / Dark Velvet (#07090E)
    val DarkNavyBlack = Color(0xFF0A0E1A)     // Deep Navy Space
    val DarkPaper = Color(0xFF0E1322)        // Container Layer Paper
    val DarkSurface = Color(0xFF161C2D)      // Primary Glass/Card Surface
    val DarkSurface2 = Color(0xFF1C2438)     // Secondary Elevated Glass Surface
    val DarkInk = Color(0xFFEEF0F6)          // Primary High-Contrast Ink
    val DarkInkSoft = Color(0xFF9FA8BD)      // Secondary Slate Ink
    val DarkInkFaint = Color(0xFF6D7690)     // Faint Caption Ink
    val DarkLine = Color(0xFF28324D)         // Divider Line

    // Signature Accents
    val CyanAccent = Color(0xFF06B6D4)       // Cyber Cyan #06B6D4
    val CyanGlow = Color(0xFF38BDF8)         // Sky Cyan Glow #38BDF8
    val EmeraldAccent = Color(0xFF10B981)    // Emerald Neon #10B981
    val EmeraldGlow = Color(0xFF34D399)      // Mint Glow #34D399
    val PurpleAccent = Color(0xFF8B5CF6)     // Cyber Violet / Purple #8B5CF6
    val PurpleGlow = Color(0xFFA78BFA)       // Light Purple Glow #A78BFA
    val ElectricBlue = Color(0xFF1D4ED8)     // Electric Blue #1D4ED8
    val AmberAccent = Color(0xFFF59E0B)      // Amber Glow #F59E0B
    val CrimsonAccent = Color(0xFFF43F5E)    // Crimson Rose #F43F5E

    // 2026 Semantic Tokens Aliases
    val CyberCyan = CyanAccent
    val SkyCyan = CyanGlow
    val EmeraldNeon = EmeraldAccent
    val MintGlow = EmeraldGlow
    val CyberViolet = PurpleAccent
    val AmberGlow = AmberAccent
    val CrimsonRose = CrimsonAccent
    val DarkVelvetCanvas = DarkCanvas
    val VioletAccent = PurpleAccent

    // Legacy Aliases
    val DarkIndigo = Color(0xFF8579FF)
    val DarkAmber = Color(0xFFF0B054)
    val DarkMint = Color(0xFF4FD5A6)
    val DarkCoral = Color(0xFFF0897A)
    val DarkSky = Color(0xFF79A8F2)
}

/**
 * Glassmorphic 2.0 specific tokens for translucent layers and ambient glow
 */
object StudentOsGlassTokens {
    val glassPrimary = Color(0xB3161C2D)          // rgba(22, 28, 45, 0.70)
    val glassElevated = Color(0xCC1A233A)         // rgba(26, 35, 58, 0.80)
    val glassSubtle = Color(0x66161C2D)           // rgba(22, 28, 45, 0.40)
    val glassHighlight = Color(0x1FFFFFFF)        // Ultra-subtle white wash

    // 2026 Semantic Tokens
    val GlassSurfaceDark = glassPrimary
    val GlassSurfaceElevated = glassElevated
    val glassDark = glassPrimary
    val GlassSurfaceLight = Color(0xF2FFFFFF)      // Frost white glass for light mode (95% opaque with light sheen)
    val GlassSurfaceElevatedLight = Color(0xFFFFFFFF)
    val GlassBorderGradient = Brush.linearGradient(
        listOf(Color.White.copy(alpha = 0.24f), Color.White.copy(alpha = 0.04f))
    )
    val GlassBorderLightGradient = Brush.linearGradient(
        listOf(Color(0xFFCBD5E1).copy(alpha = 0.70f), Color(0xFFE2E8F0).copy(alpha = 0.35f))
    )

    val borderLight = Color(0x2EFFFFFF)           // 1px subtle white border
    val borderGradient = Brush.linearGradient(
        listOf(Color.White.copy(alpha = 0.24f), Color.White.copy(alpha = 0.04f))
    )
    val borderHighlightGradient = Brush.linearGradient(
        listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.08f))
    )

    // Colored Border Highlights
    val borderCyanGradient = Brush.linearGradient(
        listOf(Color(0xFF06B6D4).copy(alpha = 0.55f), Color(0xFF06B6D4).copy(alpha = 0.08f))
    )
    val borderEmeraldGradient = Brush.linearGradient(
        listOf(Color(0xFF10B981).copy(alpha = 0.55f), Color(0xFF10B981).copy(alpha = 0.08f))
    )
    val borderPurpleGradient = Brush.linearGradient(
        listOf(Color(0xFF8B5CF6).copy(alpha = 0.55f), Color(0xFF8B5CF6).copy(alpha = 0.08f))
    )
    val borderVioletGradient = borderPurpleGradient

    // Master Hero Gradients
    val heroPassport = Brush.linearGradient(
        listOf(Color(0xFF1D4ED8), Color(0xFF0284C7), Color(0xFF0D9488))
    )
    val copilotAi = Brush.horizontalGradient(
        listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4))
    )
    val emeraldProgress = Brush.horizontalGradient(
        listOf(Color(0xFF059669), Color(0xFF10B981), Color(0xFF34D399))
    )
}

/**
 * Centralized Spacing tokens according to the Student OS System specification:
 * 4, 8, 10, 12, 14, 16, 20, 24, 32
 */
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

/**
 * Centralized Shape tokens:
 * Large containers: 28dp - 32dp
 * Medium cards: 20dp - 24dp
 * Small chips/buttons: 14dp - 16dp
 * Pills: 999dp
 */
// StudentOsShapes reference tokens
object StudentOsShapes {
    val largeContainer: Dp = 26.dp
    val heroCard: Dp = 38.dp
    val mediumCard: Dp = 26.dp
    val button: Dp = 16.dp
    val pill: Dp = 999.dp
}

