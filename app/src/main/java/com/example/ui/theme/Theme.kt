package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Modern 'Student OS' 2026 Material 3 Color Schemes.
 * Dark Mode features:
 * - Deep Blue-Black Canvas: Dark Velvet (#07090E) & Navy Deep Space (#0E1322)
 * - Glassmorphic Surface: rgba(22, 28, 45, 0.70) with 1px soft highlight border
 * - Accents: Electric Sky/Cyan (#38BDF8 / #06B6D4), Emerald Neon (#10B981), Cyber Purple (#8B5CF6)
 */
private val DarkColorScheme = darkColorScheme(
    primary = StudentPrimaryDark,                // #38BDF8 Sky Cyan
    onPrimary = Color(0xFF0C192C),
    primaryContainer = StudentPrimaryContainerDark, // #1D4ED8 Electric Blue
    onPrimaryContainer = StudentOnPrimaryContainerDark,
    secondary = StudentSecondaryDark,            // #10B981 Emerald Neon
    onSecondary = Color(0xFF022C22),
    secondaryContainer = StudentSecondaryContainerDark,
    onSecondaryContainer = StudentOnSecondaryContainerDark,
    tertiary = StudentTertiaryDark,              // #8B5CF6 Cyber Violet
    onTertiary = Color(0xFF1E1035),
    tertiaryContainer = StudentTertiaryContainerDark,
    onTertiaryContainer = StudentOnTertiaryContainerDark,
    error = StudentErrorDark,                    // #F43F5E Crimson Rose
    onError = Color(0xFF4C0519),
    errorContainer = StudentErrorContainerDark,
    onErrorContainer = StudentOnErrorContainerDark,
    background = StudentCanvasDark,              // #07090E Dark Velvet
    onBackground = StudentOnCanvasDark,
    surface = StudentSurfaceDark,                // #161C2D Glass Slate
    onSurface = StudentOnSurfaceDark,
    surfaceVariant = StudentSurfaceVariantDark,  // #1C2438 Elevated Glass
    onSurfaceVariant = StudentOnSurfaceVariantDark,
    outline = StudentOutlineDark,
    outlineVariant = StudentOutlineVariantDark,
    surfaceTint = StudentPrimaryDark,
    inverseSurface = Color(0xFFE2E8F0),
    inverseOnSurface = Color(0xFF0F172A),
    inversePrimary = Color(0xFF2563EB),
    scrim = Color(0xCC000000)
)

private val LightColorScheme = lightColorScheme(
    primary = StudentPrimaryLight,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = StudentPrimaryContainerLight,
    onPrimaryContainer = StudentOnPrimaryContainerLight,
    secondary = StudentSecondaryLight,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = StudentSecondaryContainerLight,
    onSecondaryContainer = StudentOnSecondaryContainerLight,
    tertiary = StudentTertiaryLight,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = StudentTertiaryContainerLight,
    onTertiaryContainer = StudentOnTertiaryContainerLight,
    error = StudentErrorLight,
    onError = Color(0xFFFFFFFF),
    errorContainer = StudentErrorContainerLight,
    onErrorContainer = StudentOnErrorContainerLight,
    background = StudentCanvasLight,
    onBackground = StudentOnCanvasLight,
    surface = StudentSurfaceLight,
    onSurface = StudentOnSurfaceLight,
    surfaceVariant = StudentSurfaceVariantLight,
    onSurfaceVariant = StudentOnSurfaceVariantLight,
    outline = StudentOutlineLight,
    outlineVariant = StudentOutlineVariantLight,
    surfaceTint = StudentPrimaryLight,
    inverseSurface = Color(0xFF0F172A),
    inverseOnSurface = Color(0xFFF8FAFC),
    inversePrimary = Color(0xFF818CF8),
    scrim = Color(0x99000000)
)

/**
 * Extended semantic tokens specifically crafted for Student OS workflows.
 */
@Immutable
data class StudentSemanticColors(
    val studyFocus: Color,
    val streakFire: Color,
    val passedUnitBadge: Color,
    val attendanceSafe: Color,
    val attendanceWarning: Color,
    val attendanceCritical: Color,
    val gpaAlpha: Color,
    val gpaProbation: Color,
    val brandGradient: Brush,
    val copilotGradient: Brush,
    val heroPassportGradient: Brush,
    val cardBorderGlow: Color,
    val cyanAccent: Color,
    val emeraldAccent: Color,
    val purpleAccent: Color,
    val deepSpaceCanvas: Color,
    val glassSurface: Color,
    val glassSurfaceElevated: Color,
    val glassBorder: Color,
    val glassBorderGradient: Brush
)

private val LightStudentSemanticColors = StudentSemanticColors(
    studyFocus = Color(0xFF667052),
    streakFire = Color(0xFFD97706),
    passedUnitBadge = Color(0xFF667052),
    attendanceSafe = Color(0xFF16A34A),
    attendanceWarning = Color(0xFFD97706),
    attendanceCritical = Color(0xFFEF4444),
    gpaAlpha = Color(0xFF667052),
    gpaProbation = Color(0xFFEF4444),
    brandGradient = Brush.linearGradient(listOf(Color(0xFF667052), Color(0xFF556B2F), Color(0xFF244953))),
    copilotGradient = Brush.horizontalGradient(listOf(Color(0xFF667052), Color(0xFF244953))),
    heroPassportGradient = Brush.linearGradient(listOf(Color(0xFF244953), Color(0xFF1B363E), Color(0xFF2E4230))),
    cardBorderGlow = Color(0x1A667052),
    cyanAccent = Color(0xFF244953),
    emeraldAccent = Color(0xFF667052),
    purpleAccent = Color(0xFF6B7280),
    deepSpaceCanvas = Color(0xFFF7F6F1),
    glassSurface = Color(0xFFFFFFFF),
    glassSurfaceElevated = Color(0xFFF0EFE9),
    glassBorder = Color(0xFFDCDDD6),
    glassBorderGradient = Brush.linearGradient(listOf(Color(0xFFDCDDD6), Color(0xFFE8E9E3)))
)

private val DarkStudentSemanticColors = StudentSemanticColors(
    studyFocus = Color(0xFF7E936A),
    streakFire = Color(0xFFF59E0B),
    passedUnitBadge = Color(0xFF7E936A),
    attendanceSafe = Color(0xFF22C55E),
    attendanceWarning = Color(0xFFF59E0B),
    attendanceCritical = Color(0xFFEF4444),
    gpaAlpha = Color(0xFF7E936A),
    gpaProbation = Color(0xFFEF4444),
    brandGradient = Brush.linearGradient(listOf(Color(0xFF163944), Color(0xFF244953), Color(0xFF336675))),
    copilotGradient = Brush.horizontalGradient(listOf(Color(0xFF7E936A), Color(0xFF336675))),
    heroPassportGradient = Brush.linearGradient(listOf(Color(0xFF0F2A33), Color(0xFF163944), Color(0xFF1C3E38))),
    cardBorderGlow = Color(0x267E936A),
    cyanAccent = Color(0xFF336675),
    emeraldAccent = Color(0xFF7E936A),
    purpleAccent = Color(0xFF94A3B8),
    deepSpaceCanvas = Color(0xFF0F2A33),
    glassSurface = Color(0xF2163944),
    glassSurfaceElevated = Color(0xFF1A4350),
    glassBorder = Color(0xFF224F5D),
    glassBorderGradient = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f)))
)

val LocalStudentSemanticColors = staticCompositionLocalOf { LightStudentSemanticColors }

/**
 * Accessor for student-specific design tokens from any Composable.
 */
val MaterialTheme.studentColors: StudentSemanticColors
    @Composable
    @ReadOnlyComposable
    get() = LocalStudentSemanticColors.current

/**
 * Primary Material 3 Theme for Student OS.
 */
@Composable
fun StudentOsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Intentional brand identity preferred over generic Android wallpaper tint
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val semanticColors = if (darkTheme) DarkStudentSemanticColors else LightStudentSemanticColors

    CompositionLocalProvider(
        LocalStudentSemanticColors provides semanticColors,
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}

/**
 * Backward compatibility wrapper for MyApplicationTheme.
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    StudentOsTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}

/**
 * Crisp high-contrast card border stroke that guarantees separation in both Light & Dark modes.
 */
val MaterialTheme.cardBorderStroke: BorderStroke
    @Composable
    @ReadOnlyComposable
    get() {
        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        return BorderStroke(
            1.dp,
            if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)
        )
    }

/**
 * Subtle elevation token for cards in Light mode, with 0dp in Dark mode (using glass highlight borders instead).
 */
val MaterialTheme.cardElevation: CardElevation
    @Composable
    get() {
        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        return CardDefaults.cardElevation(
            defaultElevation = if (isDark) 0.dp else 1.5.dp,
            pressedElevation = if (isDark) 2.dp else 3.dp,
            hoveredElevation = if (isDark) 1.dp else 2.dp
        )
    }


