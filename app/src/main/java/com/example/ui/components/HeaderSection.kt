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
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.StudentProfileEntity
import com.example.ui.models.ThemeMode

@Composable
fun HeaderSection(
    profile: StudentProfileEntity,
    courseCount: Int,
    gpa: String,
    passedUnits: Int,
    totalRequiredCredits: Int = 0,
    notifCount: Int,
    isDarkTheme: Boolean,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onToggleTheme: () -> Unit,
    onOpenProfile: () -> Unit,
    studyStreakDays: Int = 0,
    accountEmail: String? = null,
    isAccountConnected: Boolean = false,
    onOpenAccount: () -> Unit = {},
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
    var showDemoConfirmation by remember { mutableStateOf(false) }
    var showCleanSlateConfirmation by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val studentDisplayName = profile.name.ifBlank { "دانشجو" }
    val termDisplayText = profile.term.ifBlank { "اطلاعات ترم ثبت نشده" }
    val initialLetter = studentDisplayName.trim().firstOrNull()?.toString() ?: "د"

    // Top Bar matching exact Student OS reference design
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Actions: Bell + Leaf (Theme) buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .size(42.dp)
                    .tactileClickable { onOpenNotifications() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "اعلان‌ها",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    if (notifCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .size(42.dp)
                    .tactileClickable { onToggleTheme() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "تغییر تم",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Student Profile & Brand (Right in RTL)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Student OS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "همراه هوشمند دانشجو",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Avatar Circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .semantics {
                        contentDescription = "پروفایل کاربری $studentDisplayName، باز کردن منوی تنظیمات"
                        role = Role.Button
                    }
                    .tactileClickable { showMenu = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initialLetter,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary
                )

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    shape = RoundedCornerShape(18.dp)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "☁️ حساب کاربری و همگام‌سازی",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        onClick = {
                            showMenu = false
                            onOpenAccount()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    )
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
                                text = "🎨 انتخاب تم و ظاهر برنامه",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
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
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Nature & Calm Greeting Banner Card
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
        ),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "سلام، $studentDisplayName",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "هر قدم کوچک، تو را به هدفت نزدیک‌تر می‌کند...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDemoConfirmation) {
        AlertDialog(
            onDismissRequest = { showDemoConfirmation = false },
            title = { Text("بارگذاری داده‌های نمونه؟") },
            text = {
                Text("این عملیات داده‌های تحصیلی فعلی را با داده‌های نمونه جایگزین می‌کند و فقط برای مشاهده محیط آزمایشی است.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDemoConfirmation = false
                        onLoadDemoData()
                    }
                ) { Text("بارگذاری نمونه") }
            },
            dismissButton = {
                TextButton(onClick = { showDemoConfirmation = false }) { Text("انصراف") }
            }
        )
    }

    if (showCleanSlateConfirmation) {
        AlertDialog(
            onDismissRequest = { showCleanSlateConfirmation = false },
            title = { Text("پاکسازی کامل داده‌ها؟") },
            text = {
                Text("دروس، نمرات، حضور و غیاب، تکالیف و امتحانات این دستگاه پاک می‌شوند. این عملیات را فقط وقتی انجام دهید که از حذف اطلاعات مطمئن هستید.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCleanSlateConfirmation = false
                        onClearToFreshSlate()
                    }
                ) { Text("پاکسازی") }
            },
            dismissButton = {
                TextButton(onClick = { showCleanSlateConfirmation = false }) { Text("انصراف") }
            }
        )
    }


}
