package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.StudentProfileEntity
import com.example.ui.models.ThemeMode

@Composable
fun HeaderSection(
    profile: StudentProfileEntity,
    courseCount: Int,
    gpa: String,
    passedUnits: Int,
    totalRequiredCredits: Int = 140,
    notifCount: Int,
    isDarkTheme: Boolean,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onToggleTheme: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAndroidInfo: () -> Unit,
    onResetDefaults: () -> Unit,
    onOpenCommandCenter: () -> Unit = {},
    onOpenCopilot: () -> Unit = {},
    onOpenOcrImport: () -> Unit = {},
    onOpenSettingsAndRoadmap: () -> Unit = {},
    onLoadDemoData: () -> Unit = {},
    onClearToFreshSlate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val studentDisplayName = if (profile.name.isNotBlank()) profile.name else "امیر"
    val termDisplayText = if (profile.term.isNotBlank()) "${profile.term} - هفته ۶" else "ترم پاییز ۱۴۰۴ - هفته ۶"
    val initialLetter = studentDisplayName.trim().firstOrNull()?.toString() ?: "ع"

    // Minimalist Top Bar matching the exact design in the user screenshot
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Streak Badge (Flame Pill on left in RTL)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
            ),
            modifier = Modifier
                .semantics {
                    contentDescription = "زنجیره ۱۲ روز مطالعه مستمر، باز کردن پروفایل"
                    role = Role.Button
                }
                .tactileClickable { onOpenProfile() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AppEmoji(
                    type = AppEmojiType.FIRE,
                    size = 20.dp,
                    shapeRadiusRatio = 0.28f
                )
                Text(
                    text = "۱۲",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        // Student Profile & Greeting (Right in RTL)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "سلام، $studentDisplayName",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "✍️",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = termDisplayText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Avatar Circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .semantics {
                        contentDescription = "پروفایل کاربری $studentDisplayName، باز کردن منوی تنظیمات"
                        role = Role.Button
                    }
                    .tactileClickable { showMenu = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initialLetter,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onPrimary
                )

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    shape = RoundedCornerShape(18.dp)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "✏️ ویرایش مشخصات دانشجو",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        onClick = {
                            showMenu = false
                            onOpenProfile()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "⚙️ تنظیمات و وضعیت تم",
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        onClick = {
                            showMenu = false
                            onOpenSettingsAndRoadmap()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "🌗 تغییر سریع تم",
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        onClick = {
                            showMenu = false
                            onToggleTheme()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.SettingsBrightness, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "🎓 بارگذاری نمونه دمو (Rich Demo)",
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        onClick = {
                            showMenu = false
                            onLoadDemoData()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "🧹 پاکسازی کامل داده‌ها (Clean Slate)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showMenu = false
                            onClearToFreshSlate()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }
        }
    }
}

