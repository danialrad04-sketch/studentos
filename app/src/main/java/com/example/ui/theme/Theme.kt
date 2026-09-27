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
 * Student OS Academic Premium Material 3 color system.
 * Dark Mode features:
 * - Deep Blue-Black Canvas: Dark Velvet (#07090E) & Navy Deep Space (#0E1322)
 * - Glassmorphic Surface: rgba(22, 28, 45, 0.70) with 1px soft highlight border
 * - Accents: Electric Sky/Cyan (#38BDF8 / #06B6D4), Emerald Neon (#10B981), Cyber Purple (#8B5CF6)
 */
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7BAEBB),                // #38BDF8 Sky Cyan
    onPrimary = Color(0xFF0C192C),
    primaryContainer = Color(0xFF163F4D), // #1D4ED8 Electric Blue
    onPrimaryContainer = StudentOnPrimaryContainerDark,
    secondary = Color(0xFFA7AD78),            // #10B981 Emerald Neon
    onSecondary = Color(0xFF022C22),
    secondaryContainer = Color(0xFF39402D),
    onSecondaryContainer = StudentOnSecondaryContainerDark,
    tertiary = Color(0xFF6FA7B8),              // #8B5CF6 Cyber Violet
    onTertiary = Color(0xFF1E1035),
    tertiaryContainer = Color(0xFF17353F),
    onTertiaryContainer = StudentOnTertiaryContainerDark,
    error = StudentErrorDark,                    // #F43F5E Crimson Rose
    onError = Color(0xFF4C0519),
    errorContainer = StudentErrorContainerDark,
    onErrorContainer = StudentOnErrorContainerDark,
    background = Color(0xFF08151B),              // #07090E Dark Velvet
    onBackground = StudentOnCanvasDark,
    surface = Color(0xFF10242C),                // #161C2D Glass Slate
    onSurface = StudentOnSurfaceDark,
    surfaceVariant = Color(0xFF16313A),  // #1C2438 Elevated Glass
    onSurfaceVariant = StudentOnSurfaceVariantDark,
    outline = Color(0xFF29434C),
    outlineVariant = Color(0xFF1F3942),
    surfaceTint = StudentPrimaryDark,
    inverseSurface = Color(0xFFE2E8F0),
    inverseOnSurface = Color(0xFF0F172A),
    inversePrimary = Color(0xFF7BAEBB),
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
    background = Color(0xFFF7F6F1),
    onBackground = StudentOnCanvasLight,
    surface = StudentSurfaceLight,
    onSurface = StudentOnSurfaceLight,
    surfaceVariant = Color(0xFFF0EFE9),
    onSurfaceVariant = StudentOnSurfaceVariantLight,
    outline = Color(0xFFDCDDD6),
    outlineVariant = Color(0xFFE5E4DD),
    surfaceTint = StudentPrimaryLight,
    inverseSurface = Color(0xFF0F172A),
    inverseOnSurface = Color(0xFFF8FAFC),
    inversePrimary = Color(0xFF7D8A5C),
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
    studyFocus = Color(0xFF0F3B4D),
    streakFire = Color(0xFFF59E0B),
    passedUnitBadge = Color(0xFF10B981),
    attendanceSafe = Color(0xFF059669),
    attendanceWarning = Color(0xFFD97706),
    attendanceCritical = Color(0xFFE11D48),
    gpaAlpha = Color(0xFF059669),
    gpaProbation = Color(0xFFE11D48),
    brandGradient = Brush.linearGradient(listOf(Color(0xFF0F3B4D), Color(0xFF1E5367), Color(0xFF6B705C))),
    copilotGradient = Brush.horizontalGradient(listOf(Color(0xFF0F3B4D), Color(0xFF6B705C))),
    heroPassportGradient = Brush.linearGradient(listOf(Color(0xFF0F3B4D), Color(0xFF285A6C), Color(0xFF6B705C))),
    cardBorderGlow = Color(0x1A0F3B4D),
    cyanAccent = AcademicNavy,
    emeraldAccent = AcademicOliveLight,
    purpleAccent = AcademicOlive,
    deepSpaceCanvas = Color(0xFFF7F6F1),
    glassSurface = Color(0xFFFFFFFF),
    glassSurfaceElevated = Color(0xFFF0EFE9),
    glassBorder = StudentOutlineLight,
    glassBorderGradient = Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))
)

private val DarkStudentSemanticColors = StudentSemanticColors(
    studyFocus = Color(0xFF6FA7B8),
    streakFire = Color(0xFFD97757),
    passedUnitBadge = Color(0xFF10B981),
    attendanceSafe = Color(0xFF10B981),
    attendanceWarning = Color(0xFFF59E0B),
    attendanceCritical = Color(0xFFF43F5E),
    gpaAlpha = Color(0xFF0D9488),
    gpaProbation = Color(0xFFF43F5E),
    brandGradient = Brush.linearGradient(listOf(Color(0xFF102D37), Color(0xFF3C7180), Color(0xFF7F8B5E))),
    copilotGradient = Brush.horizontalGradient(listOf(Color(0xFF285968), Color(0xFF7F8B5E))),
    heroPassportGradient = Brush.linearGradient(listOf(Color(0xFF0B222A), Color(0xFF163F4D), Color(0xFF6B705C))),
    cardBorderGlow = Color(0x330D9488),
    cyanAccent = Color(0xFF6FA7B8),
    emeraldAccent = Color(0xFF10B981),
    purpleAccent = Color(0xFFA7AD78),
    deepSpaceCanvas = Color(0xFF08151B),
    glassSurface = Color(0xFF10242C),
    glassSurfaceElevated = Color(0xFF16313A),
    glassBorder = Color(0x33FFFFFF),
    glassBorderGradient = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.05f)))
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


