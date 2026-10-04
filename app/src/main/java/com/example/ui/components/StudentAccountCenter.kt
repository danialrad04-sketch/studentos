package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.UserAccount
import com.example.ui.models.SyncUiState
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Uses the startup sign-in form and reports real asynchronous sync results. */
@Composable
fun AuthAccountDialog(
    userAccount: UserAccount,
    onSignInEmail: (String, String, (Boolean, String) -> Unit) -> Unit,
    onSignUpEmail: (String, String, String, (Boolean, String) -> Unit) -> Unit,
    onGoogleSignIn: ((Boolean, String) -> Unit) -> Unit,
    onForgotPassword: (String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onOpenUpgrade: () -> Unit,
    onSyncNow: ((Boolean, String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onOpenProfile: () -> Unit = {},
    onOpenBackupRestore: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    onOpenSupport: () -> Unit = {}
) {
    var showLogin by rememberSaveable { mutableStateOf(false) }
    var confirmation by rememberSaveable { mutableStateOf<String?>(null) }
    var syncState by remember { mutableStateOf<SyncUiState>(SyncUiState.Idle) }
    val back = { if (showLogin) showLogin = false else onDismiss() }
    StudentSettingsPage(if (showLogin) "ورود یا ساخت حساب" else "حساب کاربری", back) {
        if (showLogin && userAccount.isGuest) {
            AccountGateV2(
                onSignIn = onSignInEmail,
                onSignUp = onSignUpEmail,
                onGoogleSignIn = onGoogleSignIn,
                onForgotPassword = onForgotPassword,
                onContinueAsGuest = { showLogin = false }
            )
        } else {
            StudentAccountContent(
                userAccount = userAccount, syncState = syncState,
                onSignIn = { showLogin = true }, onOpenProfile = onOpenProfile,
                onOpenBackupRestore = onOpenBackupRestore, onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                onOpenSupport = onOpenSupport, onOpenUpgrade = onOpenUpgrade,
                onResetPassword = { userAccount.email?.let(onForgotPassword) },
                onSignOut = { confirmation = "logout" }, onDeleteAccount = { confirmation = "delete" },
                onSyncNow = {
                    if (syncState != SyncUiState.Syncing) {
                        syncState = SyncUiState.Syncing
                        onSyncNow { ok, message -> syncState = if (ok) SyncUiState.Success(message, System.currentTimeMillis()) else SyncUiState.Error(message, System.currentTimeMillis()) }
                    }
                }
            )
        }
    }
    confirmation?.let { action ->
        StudentConfirmDialog(
            title = if (action == "delete") "حذف دائمی حساب؟" else "خروج از حساب؟",
            message = if (action == "delete") "حساب و اطلاعات وابسته به آن حذف می‌شوند. پیش از ادامه فایل پشتیبان بگیر. این کار قابل برگشت نیست و ممکن است نیاز به ورود دوباره داشته باشد." else "نشست حساب بسته و اطلاعات محلی این حساب از دستگاه پاک می‌شود. پیش از خروج، همگام‌سازی یا پشتیبان‌گیری را انجام بده.",
            confirmLabel = if (action == "delete") "حذف دائمی حساب" else "خروج از حساب",
            destructive = true,
            onDismiss = { confirmation = null },
            onConfirm = { confirmation = null; if (action == "delete") onDeleteAccount() else onSignOut() }
        )
    }
}

@Composable
fun StudentAccountContent(
    userAccount: UserAccount,
    syncState: SyncUiState,
    onSignIn: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenUpgrade: () -> Unit,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onResetPassword: () -> Unit = {}
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp).testTag("account_center"), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.AccountCircle, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(userAccount.displayName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(if (userAccount.isGuest) "حالت مهمان · اطلاعات روی این دستگاه ذخیره می‌شود" else userAccount.email ?: "حساب متصل", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                if (userAccount.isGuest) Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) { Text("ورود یا ساخت حساب") }
            }
        }
        StudentSettingsGroup("پروفایل و دسترسی‌ها") {
            StudentSettingsRow("پروفایل دانشجویی", "نام، دانشگاه، رشته و مشخصات ترم", Icons.Rounded.School, onOpenProfile)
            StudentSettingsRow("طرح حساب", userAccount.subscription.tier.titleFa, Icons.Rounded.Verified, onOpenUpgrade)
        }
        StudentSettingsGroup("اطلاعات و همگام‌سازی") {
            if (!userAccount.isGuest) {
                val lastSyncAt = (syncState as? SyncUiState.Success)?.completedAt ?: userAccount.lastSyncAt
                val lastSync = if (lastSyncAt > 0) DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale("fa")).format(Date(lastSyncAt)) else "هنوز همگام‌سازی ثبت نشده"
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("آخرین همگام‌سازی: $lastSync", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onSyncNow, enabled = syncState != SyncUiState.Syncing, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        if (syncState == SyncUiState.Syncing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Icon(Icons.Rounded.CloudSync, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (syncState == SyncUiState.Syncing) "در حال همگام‌سازی…" else "همگام‌سازی اکنون")
                    }
                    val message = when (syncState) { is SyncUiState.Success -> syncState.message; is SyncUiState.Error -> syncState.message; else -> null }
                    message?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.bodyMedium, color = if (syncState is SyncUiState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
                }
            }
            StudentSettingsRow("پشتیبان و بازیابی", "ذخیره یا بازگردانی فایل اطلاعات تحصیلی", Icons.Rounded.Backup, onOpenBackupRestore)
        }
        StudentSettingsGroup("حریم خصوصی و پشتیبانی") {
            StudentSettingsRow("حریم خصوصی", "اطلاعاتی که نگهداری می‌شود و نحوهٔ استفاده", Icons.Rounded.PrivacyTip, onOpenPrivacyPolicy)
            StudentSettingsRow("پشتیبانی", "ثبت و پیگیری درخواست", Icons.AutoMirrored.Rounded.HelpOutline, onOpenSupport)
        }
        if (!userAccount.isGuest) StudentSettingsGroup("مدیریت حساب") {
            if (!userAccount.email.isNullOrBlank()) StudentSettingsRow("بازیابی رمز عبور", "ارسال لینک بازیابی به ایمیل حساب", Icons.Rounded.LockReset, onResetPassword)
            StudentSettingsRow("خروج از حساب", "بستن نشست و پاکسازی اطلاعات محلی حساب", Icons.AutoMirrored.Rounded.ExitToApp, onSignOut)
            StudentSettingsRow("حذف دائمی حساب", "حذف حساب و اطلاعات وابسته؛ غیرقابل برگشت", Icons.Rounded.DeleteForever, onDeleteAccount, destructive = true)
        }
        Spacer(Modifier.height(12.dp))
    }
}
