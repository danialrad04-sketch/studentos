package com.example.ui.components

import com.example.BuildConfig
import com.example.data.local.entity.StudentProfileEntity
import com.example.domain.model.UserAccount
import com.example.ui.models.ThemeMode
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Settings contain real controls; roadmap promises belong in repository docs. */
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
    userAccount: UserAccount? = null,
    onOpenAuth: () -> Unit = {},
    onOpenUpgrade: () -> Unit = {},
    onOpenBackupRestore: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onOpenSupportTickets: () -> Unit = {},
    initialSection: String = "home",
    onSectionChanged: (String) -> Unit = {}
) {
    var section by rememberSaveable { mutableStateOf(initialSection) }
    var confirmation by rememberSaveable { mutableStateOf<String?>(null) }
    fun navigate(value: String) { section = value; onSectionChanged(value) }
    val back = { if (section == "home") onDismiss() else navigate("home") }
    val title = when (section) { "appearance" -> "ظاهر برنامه"; "notifications" -> "اعلان‌ها و یادآورها"; "data" -> "داده‌ها و پشتیبان"; "about" -> "راهنما و دربارهٔ برنامه"; else -> "تنظیمات" }
    StudentSettingsPage(title, back) {
        StudentSectionTransition(section) { page ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                when (page) {
                    "home" -> {
                        StudentSettingsGroup("حساب و هویت تحصیلی") {
                            StudentSettingsRow("حساب کاربری", if (userAccount?.isGuest != false) "حالت مهمان · اطلاعات روی این دستگاه" else userAccount.email ?: userAccount.displayName, Icons.Rounded.AccountCircle, onOpenAuth)
                            StudentSettingsRow("پروفایل دانشجویی", listOf(profile.name, profile.university).filter { it.isNotBlank() }.joinToString(" · "), Icons.Rounded.School, onOpenEditProfile)
                        }
                        StudentSettingsGroup("شخصی‌سازی") {
                            StudentSettingsRow("ظاهر برنامه", themeMode.titleFa, Icons.Rounded.Palette, { navigate("appearance") })
                            StudentSettingsRow("اعلان‌ها و یادآورها", if (notificationsEnabled) "یادآورهای درسی فعال‌اند" else "یادآورهای درسی خاموش‌اند", Icons.Rounded.Notifications, { navigate("notifications") })
                        }
                        StudentSettingsGroup("اطلاعات و راهنما") {
                            StudentSettingsRow("داده‌ها و پشتیبان", "خروجی، بازیابی و مدیریت اطلاعات", Icons.Rounded.Backup, { navigate("data") })
                            StudentSettingsRow("راهنما و دربارهٔ برنامه", "پشتیبانی، حریم خصوصی و نسخهٔ برنامه", Icons.AutoMirrored.Rounded.HelpOutline, { navigate("about") })
                        }
                    }
                    "appearance" -> {
                        Text("ظاهر مناسب محیط مطالعه‌ات را انتخاب کن.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        StudentSettingsGroup("حالت نمایش") {
                            Column(Modifier.selectableGroup()) {
                                ThemeMode.entries.forEach { mode ->
                                    Row(Modifier.fillMaxWidth().heightIn(min = 80.dp).selectable(selected = themeMode == mode, onClick = { onSelectThemeMode(mode) }, role = Role.RadioButton).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        RadioButton(selected = themeMode == mode, onClick = null)
                                        Column(Modifier.weight(1f)) {
                                            Text(mode.titleFa, style = MaterialTheme.typography.titleMedium)
                                            Text(when (mode) { ThemeMode.SYSTEM -> "هماهنگ با تنظیمات گوشی"; ThemeMode.LIGHT -> "سطح روشن خنثی با تأکید زیتونی"; ThemeMode.DARK -> "سرمه‌ای آرام برای محیط کم‌نور" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                        Text("انیمیشن‌ها از تنظیمات حرکت سیستم پیروی می‌کنند.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    "notifications" -> {
                        StudentSettingsGroup("یادآورهای تحصیلی") {
                            StudentSettingsRow("اعلان‌های برنامه", "یادآوری کلاس، امتحان و تکلیف", Icons.Rounded.NotificationsActive, { onToggleNotifications(!notificationsEnabled) }, trailing = { Switch(checked = notificationsEnabled, onCheckedChange = null) })
                            StudentSettingsRow("آزمایش یادآور", "ارسال اعلان آزمایشی و بررسی مجوز گوشی", Icons.Rounded.Notifications, onTestNotification)
                        }
                        Text("نمایش اعلان به مجوز و تنظیمات باتری گوشی هم بستگی دارد.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    "data" -> {
                        StudentSettingsGroup("نگهداری اطلاعات") {
                            StudentSettingsRow("پشتیبان و بازیابی", "گرفتن فایل پشتیبان یا بازگردانی اطلاعات", Icons.Rounded.Backup, onOpenBackupRestore)
                            StudentSettingsRow("سوابق نیم‌سال‌ها", "مرور درس‌ها و نتایج ترم‌های قبلی", Icons.Rounded.History, onOpenPastSemesters)
                        }
                        StudentSettingsGroup("شروع دوباره") {
                            StudentSettingsRow("راهنمای راه‌اندازی", "بازبینی مشخصات و تنظیمات اولیه", Icons.Rounded.AutoAwesome, onReopenOnboarding)
                            StudentSettingsRow("اطلاعات نمونه", "افزودن درس‌ها و داده‌های نمونه برای آشنایی", Icons.Rounded.Science, { confirmation = "demo" })
                            StudentSettingsRow("پاکسازی اطلاعات محلی", "حذف اطلاعات تحصیلی ذخیره‌شده روی دستگاه", Icons.Rounded.DeleteSweep, { confirmation = "reset" }, destructive = true)
                        }
                    }
                    "about" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Student OS", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("همراه مطالعه و زندگی دانشجویی", style = MaterialTheme.typography.bodyLarge)
                            Text("نسخهٔ ${BuildConfig.VERSION_NAME} · ساخت ${BuildConfig.VERSION_CODE}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StudentSettingsGroup("راهنما و شفافیت") {
                            StudentSettingsRow("پشتیبانی", "ثبت درخواست و پیگیری پاسخ‌ها", Icons.AutoMirrored.Rounded.HelpOutline, onOpenSupportTickets)
                            StudentSettingsRow("حریم خصوصی", "نحوهٔ نگهداری و استفاده از اطلاعات", Icons.Rounded.PrivacyTip, onOpenPrivacyPolicy)
                            StudentSettingsRow("اطلاعات نسخه", "جزئیات برنامه و فایل نصب", Icons.Rounded.Info, onOpenApkInfo)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
    confirmation?.let { action ->
        StudentConfirmDialog(
            title = if (action == "reset") "پاکسازی اطلاعات محلی؟" else "افزودن اطلاعات نمونه؟",
            message = if (action == "reset") "درس‌ها، نمرات و اطلاعات تحصیلی این دستگاه پاک می‌شوند. ابتدا از بخش پشتیبان یک نسخهٔ پشتیبان بگیر. این کار قابل برگشت نیست." else "داده‌های نمونه وارد برنامه می‌شوند و ممکن است اطلاعات فعلی را تغییر دهند. ابتدا نسخهٔ پشتیبان بگیر.",
            confirmLabel = if (action == "reset") "پاکسازی اطلاعات" else "افزودن نمونه",
            destructive = true,
            onConfirm = { confirmation = null; if (action == "reset") onClearToFreshSlate() else onLoadDemoData() },
            onDismiss = { confirmation = null }
        )
    }
}
