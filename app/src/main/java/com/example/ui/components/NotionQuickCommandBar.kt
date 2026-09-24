package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.example.ui.theme.BrandIndigo50
import com.example.ui.theme.BrandIndigo500
import com.example.ui.theme.BrandIndigo600
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Rose50
import com.example.ui.theme.Rose600
import com.example.ui.theme.Sky50
import com.example.ui.theme.Sky600
import com.example.ui.theme.VioletNeon
import com.example.ui.theme.studentColors
import com.example.ui.theme.Amber500

/**
 * 2026 Student Operations & Action Hub (مرکز ابزارها و عملیات سریع هوشمند)
 * Inspired by high-end Figma Analytics & SaaS Dashboards.
 * Replaces squeezed button bars with high-impact, beautifully-styled operational micro-cards.
 */
@Composable
fun StudentOperationsHubCard(
    onOpenCommandCenter: () -> Unit,
    onQuickAddTask: () -> Unit,
    onQuickAttendance: () -> Unit,
    onQuickPomodoro: () -> Unit,
    onOpenCopilot: () -> Unit,
    onOpenOcrImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 5 Quick Action Buttons Hub from screenshot with squircle emoji pack
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickEmojiButton(
            title = "سنتر",
            emojiType = AppEmojiType.BOLT,
            onClick = onOpenCommandCenter
        )
        QuickEmojiButton(
            title = "تسک",
            emojiType = AppEmojiType.PLUS,
            onClick = onQuickAddTask
        )
        QuickEmojiButton(
            title = "حضور",
            emojiType = AppEmojiType.TARGET,
            onClick = onQuickAttendance
        )
        QuickEmojiButton(
            title = "پومودورو",
            emojiType = AppEmojiType.POMODORO,
            onClick = onQuickPomodoro
        )
        QuickEmojiButton(
            title = "کوپایلوت",
            emojiType = AppEmojiType.COPILOT,
            onClick = onOpenCopilot
        )
    }
}

@Composable
private fun QuickEmojiButton(
    title: String,
    emojiType: AppEmojiType,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.tactileClickable { onClick() }
    ) {
        AppEmoji(
            type = emojiType,
            size = 52.dp,
            shapeRadiusRatio = 0.28f,
            elevation = 3.dp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ModernActionCard(
    icon: ImageVector,
    title: String,
    caption: String,
    accentColor: Color,
    badgeText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = accentColor.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = accentColor.copy(alpha = 0.18f)
        ),
        modifier = modifier
            .height(82.dp)
            .tactileClickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = title,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = caption,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Bento 2026 Quick Actions Dock (Compact compatibility pill bar)
 */
@Composable
fun BentoQuickActionDock(
    onQuickAddTask: () -> Unit,
    onQuickAttendance: () -> Unit,
    onQuickPomodoro: () -> Unit,
    onQuickNotes: () -> Unit,
    onOpenCommandCenter: (() -> Unit)? = null,
    onOpenCopilot: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (onOpenCommandCenter != null) {
            QuickActionPill(
                icon = Icons.Default.FlashOn,
                label = "فرمان",
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                onClick = onOpenCommandCenter
            )
        }
        QuickActionPill(
            icon = Icons.Default.Add,
            label = "تکلیف",
            accentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
            onClick = onQuickAddTask
        )
        QuickActionPill(
            icon = Icons.Default.WarningAmber,
            label = "غیبت",
            accentColor = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
            onClick = onQuickAttendance
        )
        QuickActionPill(
            icon = Icons.Default.HourglassTop,
            label = "فوکوس",
            accentColor = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
            onClick = onQuickPomodoro
        )
        if (onOpenCopilot != null) {
            QuickActionPill(
                icon = Icons.Default.AutoAwesome,
                label = "کوپایلوت",
                accentColor = Color(0xFF10B981),
                modifier = Modifier.weight(1f),
                onClick = onOpenCopilot
            )
        }
    }
}

@Composable
private fun QuickActionPill(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
        ),
        modifier = modifier
            .height(42.dp)
            .tactileClickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
