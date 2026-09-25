package com.example.ui.components

import com.example.BuildConfig

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.StudentProfileEntity
import com.example.ui.models.ThemeMode
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Rose600
import com.example.ui.theme.StudentOsColors
import com.example.ui.theme.AcademicNavy
import com.example.ui.theme.AcademicOlive
import com.example.ui.theme.StudentShapeTokens

@Composable
fun SettingsAndRoadmapDialog(
    profile: StudentProfileEntity,
    themeMode: ThemeMode,
    onSelectThemeMode: (ThemeMode) -> Unit,
    notificationsEnabled: Boolean,
    onToggleNotifications: (Boolean) -> Unit,
    onOpenEditProfile: () -> Unit,
    onDismiss: () -> Unit,
    onLoadDemoData: () -> Unit,
    onClearToFreshSlate: () -> Unit,
    onReopenOnboarding: () -> Unit,
    onOpenPastSemesters: () -> Unit,
    onOpenApkInfo: () -> Unit,
    onTestNotification: () -> Unit,
    userAccount: com.example.domain.model.UserAccount? = null,
    onOpenAuth: () -> Unit = {},
    onOpenUpgrade: () -> Unit = {},
    onOpenBackupRestore: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onOpenSupportTickets: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    var selectedSection by remember { mutableIntStateOf(0) } // 0: ظاهر و حساب, 1: اعلان و داده‌ها, 2: درباره و نقشه راه
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showDemoConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteAccountConfirmDialog by remember { mutableStateOf(false) }

    StudentGlassModalSheet(
        onDismiss = onDismiss,
        maxWidth = 620.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "تنظیمات و نقشه راه سیستم‌عامل",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Student OS · نسخه ${BuildConfig.VERSION_NAME}",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section Tabs
                TabRow(
                    selectedTabIndex = selectedSection,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(StudentShapeTokens.Compact)
                ) {
                    Tab(
                        selected = selectedSection == 0,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedSection = 0
                        },
                        text = { Text("ظاهر و حساب", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedSection == 1,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedSection = 1
                        },
                        text = { Text("اعلان و داده‌ها", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedSection == 2,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedSection = 2
                        },
                        text = { Text("درباره و نقشه راه", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedSection) {
                        0 -> {
                            // ════════════════════════════════════════════════
                            // Section 1: Appearance & Theme
                            // ════════════════════════════════════════════════
                            Text(
                                text = "🎨 ظاهر و حالت نمایش (Theme Engine)",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = "انتخاب وضعیت رنگ‌بندی رابط کاربری به صورت دائمی در حافظه ذخیره می‌شود:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Theme Selector Cards (System / Light / Dark)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val themeOptions = listOf(
                                    Triple(ThemeMode.SYSTEM, Icons.Default.SettingsBrightness, "مطابق سیستم (پیش‌فرض)"),
                                    Triple(ThemeMode.LIGHT, Icons.Default.LightMode, "تم روشن"),
                                    Triple(ThemeMode.DARK, Icons.Default.DarkMode, "تم تاریک")
                                )

                                themeOptions.forEach { (mode, icon, title) ->
                                    val isSelected = themeMode == mode
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .tactileClickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onSelectThemeMode(mode)
                                            },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            }
                                        ),
                                        border = if (isSelected) {
                                            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                        } else {
                                            androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = null,
                                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = title,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = mode.descriptionFa,
                                                    fontSize = 9.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "انتخاب شده",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // ════════════════════════════════════════════════
                            // Section 2: Account & Profile Summary
                            // ════════════════════════════════════════════════
                            Text(
                                text = "👤 حساب و مشخصات دانشجو",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = StudentShapeTokens.Card,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            modifier = Modifier.size(46.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = profile.name.trim().firstOrNull()?.toString() ?: "د",
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = profile.name.ifEmpty { "دانشجوی گرامی" },
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "شماره دانشجویی: ${profile.studentId.ifEmpty { "ثبت نشده" }}",
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${profile.university} · ${profile.major} · ترم ${profile.currentSemester}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = StudentShapeTokens.Compact,
                                            color = Emerald600.copy(alpha = 0.12f),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(text = "واحدهای پاس‌شده", fontSize = 9.sp, color = Emerald600)
                                                Text(text = "${profile.passedUnits} واحد", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Emerald600)
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(text = "واحدهای ترم جاری", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                                                Text(text = "${profile.activeUnits} واحد", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onDismiss()
                                            onOpenEditProfile()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(42.dp),
                                        shape = StudentShapeTokens.Compact,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("ویرایش کامل مشخصات دانشجویی", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onDismiss()
                                                onOpenAuth()
                                            },
                                            modifier = Modifier.weight(1f).height(40.dp),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (userAccount?.isGuest == true) "ورود / همگام‌سازی ابری" else "مدیریت حساب", fontSize = 10.5.sp)
                                        }

                                        Button(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onDismiss()
                                                onOpenUpgrade()
                                            },
                                            modifier = Modifier.weight(1f).height(40.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.Amber500)
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("طرح ویژه (Pro)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onDismiss()
                                            onOpenSupportTickets()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(40.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("پشتیبانی و ثبت تیکت (ارتباط با تیم توسعه)", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        1 -> {
                            // ════════════════════════════════════════════════
                            // Section 2: Notifications & Data Management
                            // ════════════════════════════════════════════════
                            Text(
                                text = "🔔 اعلان‌ها و هشدارهای تحصیلی",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (notificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                                contentDescription = null,
                                                tint = if (notificationsEnabled) Emerald600 else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = if (notificationsEnabled) "اعلان‌ها فعال است" else "اعلان‌ها غیرفعال است",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "ارسال هشدار غیبت ۳/۱۶، سررسید تکالیف و امتحانات",
                                                    fontSize = 9.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Switch(
                                            checked = notificationsEnabled,
                                            onCheckedChange = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onToggleNotifications(it)
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Emerald600)
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = onTestNotification,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("ارسال تست هشدار به نوار وضعیت گوشی (Android Notification)", fontSize = 10.5.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "🚀 مدیریت داده‌ها و شروع نو",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Option A: Clean Slate (For Real Users)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tactileClickable {
                                        showResetConfirmDialog = true
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Rose600.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Rose600.copy(alpha = 0.15f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteSweep,
                                                contentDescription = null,
                                                tint = Rose600,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "پاکسازی کامل و شروع نو (Clean Slate)",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "حذف دروس نمونه برای ثبت اختصاصی مشخصات و برنامه خود کاربر.",
                                            fontSize = 9.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Option Backup & Restore (JSON / Cloud)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tactileClickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onDismiss()
                                        onOpenBackupRestore()
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "پشتیبان‌گیری و بازیابی داده‌ها (Backup & Restore)",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "خروجی گرفتن و بازگردانی نسخه کامل JSON دیتابیس بدون از دست رفتن اطلاعات.",
                                            fontSize = 9.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Option B: Rich Demo Mode
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tactileClickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showDemoConfirmDialog = true
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Emerald600.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Emerald600.copy(alpha = 0.15f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Dataset,
                                                contentDescription = null,
                                                tint = Emerald600,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "بارگذاری نمونه دمو و تور کامل (Rich Demo)",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "بارگذاری ۱۲ درس، نمرات، نمودار غیبت و تکالیف جهت تست امکانات.",
                                            fontSize = 9.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Navigation Shortcuts
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onReopenOnboarding()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("⚡ اجرای مجدد راه‌اندازی سریع (Zero-Setup Wizard)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onOpenPastSemesters()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.HistoryEdu, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("📜 مدیریت سوابق ترم‌های گذشته (۱ تا ۸)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }

                            // Option: Privacy Policy
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tactileClickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onDismiss()
                                        onOpenPrivacyPolicy()
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.Policy,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "سیاست حفظ حریم خصوصی (Privacy Policy)",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "شفافیت در نگهداری داده‌ها، حفاظت ابری Firebase و حقوق کاربران.",
                                            fontSize = 9.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Option: Delete Account and All Data (Google Play Requirement)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tactileClickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showDeleteAccountConfirmDialog = true
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = StudentOsColors.CrimsonRose.copy(alpha = 0.08f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StudentOsColors.CrimsonRose.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = StudentOsColors.CrimsonRose.copy(alpha = 0.15f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteForever,
                                                contentDescription = null,
                                                tint = StudentOsColors.CrimsonRose,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "حذف حساب کاربری و تمامی داده‌ها (Delete Account & Data)",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StudentOsColors.CrimsonRose
                                        )
                                        Text(
                                            text = "حذف دائم اسناد ابری Firestore، حساب کاربری و پایگاه داده محلی.",
                                            fontSize = 9.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        2 -> {
                            // ════════════════════════════════════════════════
                            // Section 3: About Student OS & Roadmap
                            // ════════════════════════════════════════════════
                            Text(
                                text = "📱 درباره Student OS",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.School,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Student OS",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "دستیار هوشمند و سیستم‌عامل جامع تحصیلی",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = "سامانه یکپارچه برنامه‌ریزی هفتگی، رادار هوشمند غیبت‌های ۳/۱۶، پیش‌بینی معدل الف و مشروطی، تایمر پومودورو، چارت پیش‌نیازهای تحصیلی و شناسنامه دیجیتال دانشجو.",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "نسخه رسمی: v2.4.0 (Build 2026.09.19)", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(text = "معماری: Room DB + Compose M3", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "🗺️ وضعیت ۹ ماژول استاندارد انتشار",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            val roadmapPhases = listOf(
                                Triple("فاز ۱: داشبورد بنتو و اکتیویتی زنده", "داشبورد ۳بعدی با کپسول جزیره پویا و تایمر هوشمند", true),
                                Triple("فاز ۲: برنامه هفتگی و رادار غیبت ۳/۱۶", "جدول زمانی تعاملی روزها، اخطار حذف درس و آلارم", true),
                                Triple("فاز ۳: نمرات وزنی و پیش‌بینی معدل", "محاسبه دقیق معدل الف/مشروطی و تحلیل توزیع نمرات", true),
                                Triple("فاز ۴: امتحانات و تقویم معکوس", "شمارش معکوس زنده روزهای مانده، ساعت و صندلی", true),
                                Triple("فاز ۵: ماتریس تسک‌ها و تکالیف", "مدیریت سررسید ددلاین‌ها و پیوند مستقیم با دروس", true),
                                Triple("فاز ۶: چارت سرفصل و گراف پیش‌نیازها", "چارت مصوب، شبیه‌ساز ترم و تطبیق دوره‌ها", true),
                                Triple("فاز ۷: موتور کوپایلوت هوشمند تحصیلی", "پیشنهاد خودکار ساعات مطالعه و تحلیل ریسک", true),
                                Triple("فاز ۸: استخراج برگه گلستان (OCR/Paste)", "پارسر هوشمند متن و تصویر برگه انتخاب واحد", true),
                                Triple("فاز ۹: پاسپورت، گیمیفیکیشن و صادرات", "کارت ویزیت دانشجویی NFC، مدال‌ها و تراز تحصیلی", true)
                            )

                            roadmapPhases.forEach { phase ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Emerald600,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = phase.first,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = phase.second,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Emerald600.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "فعال ✓",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Emerald600,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("بستن تنظیمات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
    }

    // Confirmation dialog for Clean Slate
    if (showDemoConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDemoConfirmDialog = false },
            title = { Text("بارگذاری داده‌های نمونه؟") },
            text = {
                Text("این عملیات داده‌های تحصیلی فعلی را با داده‌های نمونه جایگزین می‌کند. قبل از تأیید، برای بازیابی داده‌های فعلی از Backup استفاده کنید.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDemoConfirmDialog = false
                        onLoadDemoData()
                        onDismiss()
                    }
                ) { Text("بارگذاری نمونه") }
            },
            dismissButton = {
                TextButton(onClick = { showDemoConfirmDialog = false }) { Text("انصراف") }
            }
        )
    }

    if (showResetConfirmDialog) {
        Dialog(onDismissRequest = { showResetConfirmDialog = false }) {
            Card(
                shape = StudentShapeTokens.Card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "⚠️ تأیید پاکسازی و شروع نو",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Rose600
                    )
                    Text(
                        text = "آیا مطمئن هستید که می‌خواهید کلیه داده‌های نمونه را پاک کرده و با یک بوم خالی برای ورود اطلاعات خود شروع کنید؟",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { showResetConfirmDialog = false },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("انصراف", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showResetConfirmDialog = false
                                onClearToFreshSlate()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Rose600)
                        ) {
                            Text("بله، پاکسازی کن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showDeleteAccountConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAccountConfirmDialog = false },
                title = {
                    Text(
                        "حذف دائمی حساب و تمامی داده‌ها",
                        fontWeight = FontWeight.Bold,
                        color = StudentOsColors.CrimsonRose,
                        fontSize = 15.sp
                    )
                },
                text = {
                    Text(
                        "با این اقدام تمامی اسناد ابری Firestore، حساب کاربری و اطلاعات ذخیره شده در دستگاه به طور کامل و غیرقابل بازگشت حذف می‌شوند. آیا ادامه می‌دهید؟",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteAccountConfirmDialog = false
                            onDismiss()
                            onDeleteAccount()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StudentOsColors.CrimsonRose),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حذف دائم حساب و داده‌ها", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAccountConfirmDialog = false }) {
                        Text("انصراف")
                    }
                },
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}
