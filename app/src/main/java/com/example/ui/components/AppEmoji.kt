package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AppEmojiType {
    HOME,
    BOOK,
    CALENDAR,
    CHECK,
    MORE,
    COPILOT,
    GRAD_CAP,
    BOLT,
    PLUS,
    POMODORO,
    SPARKLE,
    FIRE,
    TARGET,
    TROPHY,
    MEDAL,
    WARNING,
    BELL,
    CHART
}

@Composable
fun AppEmoji(
    type: AppEmojiType,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    shapeRadiusRatio: Float = 0.28f,
    elevation: Dp = 0.dp
) {
    val radius = size * shapeRadiusRatio
    val shape = RoundedCornerShape(radius)

    val (brush, iconContent) = when (type) {
        AppEmojiType.HOME -> Pair(
            Brush.verticalGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))),
            @Composable {
                Box(contentAlignment = Alignment.Center) {
                    Text("🏠", fontSize = (size.value * 0.55f).sp)
                }
            }
        )
        AppEmojiType.BOOK -> Pair(
            Brush.verticalGradient(listOf(Color(0xFF818CF8), Color(0xFF4F46E5))),
            @Composable {
                Text("📖", fontSize = (size.value * 0.55f).sp)
            }
        )
        AppEmojiType.CALENDAR -> Pair(
            Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))),
            @Composable {
                Text("📅", fontSize = (size.value * 0.55f).sp)
            }
        )
        AppEmojiType.CHECK -> Pair(
            Brush.verticalGradient(listOf(Color(0xFF34D399), Color(0xFF059669))),
            @Composable {
                Text("✅", fontSize = (size.value * 0.55f).sp)
            }
        )
        AppEmojiType.MORE -> Pair(
            Brush.verticalGradient(listOf(Color(0xFF94A3B8), Color(0xFF64748B))),
            @Composable {
                Box(contentAlignment = Alignment.Center) {
                    Text("•••", color = Color.White, fontSize = (size.value * 0.42f).sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
                }
            }
        )
        AppEmojiType.COPILOT -> Pair(
            Brush.verticalGradient(listOf(Color(0xFFA78BFA), Color(0xFF06B6D4))),
            @Composable {
                Text("🤖", fontSize = (size.value * 0.55f).sp)
            }
        )
        AppEmojiType.GRAD_CAP -> Pair(
            Brush.verticalGradient(listOf(Color(0xFF0EA5E9), Color(0xFF1E40AF))),
            @Composable {
                Text("🎓", fontSize = (size.value * 0.55f).sp)
            }
        )
        AppEmojiType.BOLT -> Pair(
            Brush.verticalGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))),
            @Composable {
                Text("⚡", fontSize = (size.value * 0.55f).sp)
            }
        )
        AppEmojiType.PLUS -> Pair(
            Brush.verticalGradient(listOf(Color(0xFF10B981), Color(0xFF047857))),
            @Composable {
                Text("➕", fontSize = (size.value * 0.52f).sp)
            }
        )
        AppEmojiType.POMODORO -> Pair(
            Brush.verticalGradient(listOf(Color(0xFFA78BFA), Color(0xFF7C3AED))),
            @Composable {
                Text("⏱️", fontSize = (size.value * 0.55f).sp)
            }
        )
        AppEmojiType.SPARKLE -> Pair(
            Brush.verticalGradient(listOf(Color(0xFFC084FC), Color(0xFF9333EA))),
            @Composable {
                Text("✨", fontSize = (size.value * 0.55f).sp)
            }
        )
        AppEmojiType.FIRE -> Pair(
            Brush.verticalGradient(listOf(Color(0xFFFBBF24), Color(0xFFEA580C))),
            @Composable {
                Text("🔥", fontSize = (size.value * 0.55f).sp)
            }
        )
        AppEmojiType.TARGET -> Pair(
            Brush.verticalGradient(listOf(Color(0xFF34D399), Color(0xFF0D9488))),
            @Composable {
                Text("🎯", fontSize = (size.value * 0.55f).sp)
            }
        )
        AppEmojiType.TROPHY -> Pair(
            Brush.verticalGradient(listOf(Color(0xFFFCD34D), Color(0xFFD97706))),
            @Composable {
                Text("🏆", fontSize = (size.value * 0.55f).sp)
            }
        )
        AppEmojiType.MEDAL -> Pair(
            Brush.verticalGradient(listOf(Color(0xFFFDE047), Color(0xFFCA8A04))),
            @Composable {
                Text("🎖️", fontSize = (size.value * 0.55f).sp)
            }
        )
        AppEmojiType.WARNING -> Pair(
            Brush.verticalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
            @Composable {
                Text("⚠️", fontSize = (size.value * 0.55f).sp)
            }
        )
        AppEmojiType.BELL -> Pair(
            Brush.verticalGradient(listOf(Color(0xFFFB7185), Color(0xFFE11D48))),
            @Composable {
                Text("🔔", fontSize = (size.value * 0.55f).sp)
            }
        )
        AppEmojiType.CHART -> Pair(
            Brush.verticalGradient(listOf(Color(0xFF22D3EE), Color(0xFF0284C7))),
            @Composable {
                Text("📈", fontSize = (size.value * 0.55f).sp)
            }
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape) else Modifier)
            .clip(shape)
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        iconContent()
    }
}
