package com.example.ui.components

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.PremiumApiClient
import com.example.data.billing.BazaarBillingClient
import com.example.domain.model.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SubscriptionUpgradeRoute(userAccount: UserAccount, onVerified: (SubscriptionDetails) -> Unit, onSignIn: () -> Unit, onDismiss: () -> Unit) {
    if (userAccount.isGuest && !com.example.ui.ReleaseAccessPolicy.onlineAccountEntryEnabled) {
        StudentSettingsPage("امکانات آنلاین", onDismiss) { OfflineAccountNotice(onContinue = onDismiss) }
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(PremiumStatus()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val verifiedCallback by rememberUpdatedState(onVerified)
    val billing = remember(context, userAccount.uid) { BazaarBillingClient(context, scope) { details ->
        status = status.copy(subscription = details)
        verifiedCallback(details)
    } }
    val state by billing.state.collectAsStateWithLifecycle()
    fun refresh() {
        if (loading || state.busy || userAccount.isGuest) return
        scope.launch {
            loading = true; error = null
            val result = PremiumApiClient.result { PremiumApiClient.catalog() }
            result.onSuccess { status = it; onVerified(it.subscription); billing.connect(it) }
                .onFailure { error = it.message }
            loading = false
        }
    }
    LaunchedEffect(userAccount.uid) { refresh() }
    DisposableEffect(billing) { onDispose { billing.disconnect() } }
    SubscriptionUpgradeDialog(userAccount, status, state.copy(busy = state.busy || loading, message = error ?: state.message),
        onPurchase = { plan -> context.activity()?.let { billing.purchase(it, plan) } },
        onRestore = billing::restore, onRefresh = { refresh() }, onSignIn = onSignIn, onDismiss = onDismiss)
}
private tailrec fun Context.activity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}
private fun amount(value: Long) = NumberFormat.getIntegerInstance(Locale("fa", "IR")).format(value)

@Composable
fun SubscriptionUpgradeDialog(
    userAccount: UserAccount,
    status: PremiumStatus,
    billing: BazaarBillingState,
    onPurchase: (PremiumPlan) -> Unit,
    onRestore: () -> Unit,
    onRefresh: () -> Unit,
    onSignIn: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedId by rememberSaveable { mutableStateOf("studentos_pro_monthly") }
    val selected = status.plans.firstOrNull { it.productId == selectedId } ?: status.plans.firstOrNull()
    val active = status.subscription.isProOrHigher || userAccount.isProOrHigher
    val actualPrice = selected?.let { billing.prices[it.productId] }
    val canBuy = !userAccount.isGuest && !active && status.salesEnabled && billing.connected && !billing.busy &&
        selected != null && actualPrice != null && BazaarPrice.matches(actualPrice, selected.priceToman)
    StudentSettingsPage("اشتراک Student Pro", onDismiss) {
        Column(Modifier.fillMaxSize().testTag("premium_page")) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("مطالعهٔ منظم‌تر با Student Pro", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("مربی مطالعه، مرور امتحان و تحلیل پیشرفت متناسب با برنامهٔ شما.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                if (active) {
                    StudentSettingsGroup("اشتراک شما") {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Student Pro فعال است", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            val expiry = status.subscription.expiresAt ?: userAccount.subscription.expiresAt
                            expiry?.let { Text("پایان دسترسی: ${expiryDate(it)}", style = MaterialTheme.typography.bodyLarge) }
                            Text(if (status.subscription.autoRenewing) "تمدید خودکار فعال است؛ مدیریت آن در بخش اشتراک‌های بازار انجام می‌شود." else "وضعیت تمدید و خرید در بخش اشتراک‌های بازار قابل بررسی است.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (!active) {
                    Text("انتخاب مدت اشتراک", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    status.plans.forEach { plan ->
                        val isSelected = selected?.productId == plan.productId
                        Surface(shape = RoundedCornerShape(20.dp), color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth().selectable(isSelected, role = Role.RadioButton, onClick = { selectedId = plan.productId }).testTag("plan_${plan.productId}")) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                RadioButton(isSelected, onClick = null)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(plan.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${amount(plan.priceToman)} تومان", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text("مبلغ کل این دوره · پرداخت از طریق بازار", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    if (!status.salesEnabled && !billing.busy) Text(if (userAccount.isGuest) "برای مشاهدهٔ قیمت و خرید، وارد حساب خود شوید." else "خرید اشتراک فعلاً در دسترس نیست. پس از فعال‌شدن فروش، قیمت و طرح‌ها از بازار دریافت می‌شوند.", style = MaterialTheme.typography.bodyLarge)
                    if (status.salesEnabled) Text("قیمت نهایی در صفحهٔ پرداخت بازار نمایش داده می‌شود. قبل از تأیید پرداخت، مبلغ و شرایط تمدید را بررسی کنید.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StudentSettingsGroup("امکانات پرو") {
                    PremiumFeature("مربی مطالعهٔ هفت‌روزه", "تنظیم اولویت‌ها و زمان مطالعه با توجه به برنامهٔ واقعی شما", Icons.Rounded.EventNote)
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    PremiumFeature("۵۰ پاسخ آنلاین در روز", "مشاورهٔ درسی، مرور امتحان و تحلیل پیشرفت با اطلاعات تحصیلی شما", Icons.Rounded.ChatBubbleOutline)
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    PremiumFeature("۱۰ اسکن برنامه در روز", "تبدیل عکس برنامهٔ کلاسی به جدول؛ پیش از ثبت، نتیجه را بررسی و تأیید کنید", Icons.Rounded.DocumentScanner)
                }
                Text("طرح رایگان: ۵ پاسخ آنلاین و یک اسکن روزانه پس از ورود. برنامهٔ کلاسی، تکالیف و اطلاعات ذخیره‌شدهٔ شما همچنان در دسترس‌اند.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                billing.message?.let { Text(it, Modifier.testTag("billing_message"), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface) }
                OutlinedButton(onClick = onRestore, enabled = billing.connected && !billing.busy && !userAccount.isGuest, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("restore_purchase")) {
                    Icon(Icons.Rounded.Restore, null); Spacer(Modifier.width(8.dp)); Text("بازیابی خرید قبلی")
                }
                Text("با همان حساب Student OS زمان خرید وارد شوید. حذف و نصب برنامه خرید را حذف نمی‌کند. برای وضعیت پرداخت و تمدید، بخش اشتراک‌های حساب بازار را بررسی کنید.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (billing.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (userAccount.isGuest) Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("premium_sign_in")) { Text("ورود برای خرید اشتراک") }
                else if (active) Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("ادامه با Student Pro") }
                else Button(onClick = { selected?.let(onPurchase) }, enabled = canBuy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("purchase_subscription")) { Text(if (canBuy) "خرید ${selected?.title} · ${amount(selected!!.priceToman)} تومان" else "خرید از بازار") }
                if (!userAccount.isGuest && !billing.busy) TextButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("بررسی دوبارهٔ اتصال و قیمت") }
            }
        }
    }
}
@Composable
private fun PremiumFeature(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun expiryDate(millis: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    return com.example.domain.util.JalaliCalendarUtil.gregorianToJalali(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH)).toPersianDigits()
}
