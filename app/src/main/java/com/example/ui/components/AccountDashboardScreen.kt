package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.local.entity.StudentProfileEntity
import com.example.domain.model.UserAccount
import com.example.ui.models.ThemeMode

/** Personal dashboard, backed by the same persisted profile and preferences as Settings. */
@Composable
fun AccountDashboardScreen(
    profile: StudentProfileEntity,
    account: UserAccount,
    themeMode: ThemeMode,
    notificationsEnabled: Boolean,
    courseCount: Int,
    pendingTaskCount: Int,
    dailyGoalMinutes: Int,
    focusMinutes: Int,
    onEditProfile: () -> Unit,
    onSelectTheme: (ThemeMode) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onOpenSettings: (String) -> Unit,
    onOpenAccount: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenCourses: () -> Unit,
    onOpenTasks: () -> Unit,
) {
    val name = profile.name.ifBlank { account.displayName.ifBlank { "دانشجو" } }
    Column(
        Modifier.fillMaxWidth().testTag("account_dashboard"),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        StudentGlassSurface(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Icon(
                            Icons.Rounded.PersonOutline,
                            null,
                            Modifier.padding(16.dp).size(30.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            if (account.isGuest) "فضای شخصی روی این دستگاه"
                            else account.email.orEmpty().ifBlank { "حساب متصل" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                val academicIdentity =
                    listOf(profile.university, profile.major).filter { it.isNotBlank() }
                Text(
                    academicIdentity.joinToString(" · ").ifBlank {
                        "با تکمیل پروفایل، اطلاعات دانشگاهت را اینجا ببین."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onEditProfile,
                    modifier = Modifier.testTag("account_edit_profile"),
                ) {
                    Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ویرایش پروفایل")
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccountMetric("درس این ترم", courseCount, onOpenCourses, Modifier.weight(1f))
                    AccountMetric("کار باز", pendingTaskCount, onOpenTasks, Modifier.weight(1f))
                }
            }
        }
        StudentSettingsGroup("ظاهر دلخواه تو") {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "حالت نمایش",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                AccountThemePicker(themeMode, onSelectTheme)
            }
        }
        StudentSettingsGroup("مطالعه و یادآورها") {
            com.example.ui.v3.StudentWidgetSettings()
            StudentSettingsRow(
                "مطالعه و تمرکز",
                "هدف روزانه ${dailyGoalMinutes.toString().toPersianDigits()} دقیقه · تمرکز ${focusMinutes.toString().toPersianDigits()} دقیقه",
                Icons.Rounded.Timer,
                { onOpenSettings("study") },
            )
            StudentSettingsRow(
                "اعلان‌های درسی",
                if (notificationsEnabled) "یادآورهای برنامه فعال‌اند"
                else "یادآورهای برنامه خاموش‌اند",
                Icons.Rounded.NotificationsNone,
                { onToggleNotifications(!notificationsEnabled) },
                modifier =
                    Modifier.semantics {
                        role = Role.Switch
                        toggleableState =
                            if (notificationsEnabled) ToggleableState.On else ToggleableState.Off
                    },
                trailing = { Switch(checked = notificationsEnabled, onCheckedChange = null) },
            )
            StudentSettingsRow(
                "تنظیمات یادآورها",
                "مجوز گوشی و آزمایش اعلان",
                Icons.Rounded.Tune,
                { onOpenSettings("notifications") },
            )
        }
        StudentSettingsGroup("اطلاعات و حساب") {
            StudentSettingsRow(
                "پشتیبان و بازیابی",
                "نگهداری فایل پشتیبان و انتقال به گوشی دیگر",
                Icons.Rounded.Backup,
                onOpenBackup,
            )
            StudentSettingsRow(
                "مدیریت حساب",
                if (account.isGuest) "اطلاعات محلی و وضعیت حساب" else "وضعیت اتصال و مدیریت حساب",
                Icons.Rounded.ManageAccounts,
                onOpenAccount,
            )
            StudentSettingsRow(
                "همهٔ تنظیمات",
                "مدیریت داده‌ها، راه‌اندازی و تنظیمات بیشتر",
                Icons.Rounded.Settings,
                { onOpenSettings("home") },
            )
        }
        StudentSettingsGroup("راهنما و حریم خصوصی") {
            StudentSettingsRow(
                "پشتیبانی",
                "درخواست‌های تو و پاسخ‌ها",
                Icons.AutoMirrored.Rounded.HelpOutline,
                onOpenSupport,
            )
            StudentSettingsRow(
                "حریم خصوصی",
                "اطلاعاتت چطور نگهداری می‌شود؟",
                Icons.Rounded.PrivacyTip,
                onOpenPrivacy,
            )
        }
        Text(
            "Student OS · نسخهٔ ${BuildConfig.VERSION_NAME.toPersianDigits()}",
            Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AccountMetric(title: String, count: Int, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                count.toString().toPersianDigits(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(title, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun AccountThemePicker(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val largeText = LocalDensity.current.fontScale > 1.3f
    BoxWithConstraints(Modifier.fillMaxWidth().selectableGroup()) {
        val stacked = largeText || maxWidth < 280.dp
        val option: @Composable (ThemeMode, Modifier) -> Unit = { mode, modifier ->
            val active = selected == mode
            Surface(
                selected = active,
                onClick = { onSelect(mode) },
                modifier =
                    modifier.testTag("account_theme_${mode.name}").semantics {
                        role = Role.RadioButton
                    },
                shape = RoundedCornerShape(18.dp),
                color =
                    if (active) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                border =
                    if (active)
                        androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary,
                        )
                    else null,
            ) {
                Column(
                    Modifier.padding(12.dp).heightIn(min = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        when (mode) {
                            ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                            ThemeMode.LIGHT -> Icons.Rounded.LightMode
                            ThemeMode.DARK -> Icons.Rounded.DarkMode
                        },
                        null,
                        Modifier.size(24.dp),
                    )
                    Text(
                        when (mode) {
                            ThemeMode.SYSTEM -> "خودکار"
                            ThemeMode.LIGHT -> "روشن"
                            ThemeMode.DARK -> "تاریک"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    )
                    if (active) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp))
                    else Spacer(Modifier.size(16.dp))
                }
            }
        }
        if (stacked)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { option(it, Modifier.fillMaxWidth()) }
            }
        else
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { option(it, Modifier.weight(1f)) }
            }
    }
}

private fun String.toPersianDigits(): String =
    map { if (it in '0'..'9') '۰' + (it - '0') else it }.joinToString("")
