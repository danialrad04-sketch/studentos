package com.example.data.billing

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.api.PremiumApiClient
import com.example.domain.model.*
import ir.cafebazaar.poolakey.Connection
import ir.cafebazaar.poolakey.Payment
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.entity.PurchaseInfo
import ir.cafebazaar.poolakey.entity.PurchaseState
import ir.cafebazaar.poolakey.request.PurchaseRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Bazaar owns checkout; only a server-verified receipt can activate premium. */
class BazaarBillingClient(context: Context, private val scope: CoroutineScope, private val onVerified: (SubscriptionDetails) -> Unit) {
    private val appContext = context.applicationContext
    private var payment: Payment? = null
    private var connection: Connection? = null
    private var catalog = PremiumStatus()
    private val uid = PremiumApiClient.memberId()
    private val mutableState = MutableStateFlow(BazaarBillingState())
    val state = mutableState.asStateFlow()
    private val pending by lazy {
        val masterKey = MasterKey.Builder(appContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(appContext, "bazaar_pending_purchases", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    }
    fun connect(status: PremiumStatus) {
        disconnect()
        catalog = status
        if (uid == null || uid != PremiumApiClient.memberId()) { message("ابتدا با حساب زمان خرید وارد شوید."); return }
        if (status.rsaPublicKey.isBlank()) { message("پرداخت بازار هنوز فعال نشده است."); return }
        try {
            pending.contains(uid) // Check encrypted storage before allowing checkout.
            payment = Payment(appContext, PaymentConfiguration(SecurityCheck.Enable(status.rsaPublicKey)))
            mutableState.value = BazaarBillingState(busy = true)
            connection = payment!!.connect {
                connectionSucceed {
                    mutableState.value = mutableState.value.copy(connected = true, busy = false)
                    payment?.getSubscriptionSkuDetails(status.plans.map { it.productId }) {
                        getSkuDetailsSucceed { products -> mutableState.value = mutableState.value.copy(prices = products.associate { it.sku to it.price }) }
                        getSkuDetailsFailed { message("قیمت از بازار دریافت نشد؛ اتصال را دوباره بررسی کنید.") }
                    }
                    if (pending.contains(uid)) restore()
                }
                connectionFailed { mutableState.value = BazaarBillingState(message = "به بازار متصل نشد؛ بازار را نصب یا به‌روز کنید و دوباره تلاش کنید.") }
                disconnected { mutableState.value = mutableState.value.copy(connected = false, busy = false) }
            }
        } catch (_: Exception) { message("سرویس خرید آماده نشد؛ دوباره تلاش کنید.") }
    }
    fun purchase(activity: ComponentActivity, plan: PremiumPlan) {
        val current = mutableState.value
        if (current.busy) return
        if (uid == null || uid != PremiumApiClient.memberId()) { message("حساب شما تغییر کرده؛ صفحه را دوباره باز کنید."); return }
        if (!catalog.salesEnabled || !current.connected || catalog.plans.none { it == plan }) { message("خرید هنوز در دسترس نیست؛ دوباره اتصال را بررسی کنید."); return }
        if (!BazaarPrice.matches(current.prices[plan.productId].orEmpty(), plan.priceToman)) { message("قیمت بازار با طرح هماهنگ نیست؛ خرید انجام نشد."); return }
        mutableState.value = current.copy(busy = true, message = "در حال بازکردن پرداخت امن بازار…")
        payment?.subscribeProduct(activity.activityResultRegistry, PurchaseRequest(plan.productId, "studentos:$uid")) {
            purchaseSucceed { verify(it) }
            purchaseCanceled { message("خرید لغو شد؛ تغییری در اشتراک انجام نشد.") }
            purchaseFailed { message("خرید تکمیل نشد؛ وضعیت پرداخت را در بازار بررسی کنید.") }
            failedToBeginFlow { message("صفحهٔ خرید باز نشد؛ بازار را به‌روز کنید و دوباره تلاش کنید.") }
        }
    }
    fun restore() {
        if (!mutableState.value.connected || mutableState.value.busy || uid != PremiumApiClient.memberId()) return
        mutableState.value = mutableState.value.copy(busy = true, message = "در حال بازیابی و تأیید خرید…")
        payment?.getSubscribedProducts {
            querySucceed { purchases ->
                val eligible = purchases.filter { it.purchaseState == PurchaseState.PURCHASED && catalog.plans.any { p -> p.productId == it.productId } }
                val saved = uid?.let { pending.getString(it, null) }
                scope.launch {
                    try {
                        var latest: SubscriptionDetails? = null
                        val receipts = eligible.map { it.originalJson to it.dataSignature }.toMutableList()
                        saved?.let { val json = JSONObject(it); receipts.add(json.getString("json") to json.getString("signature")) }
                        for ((json, signature) in receipts.distinct()) {
                            val result = PremiumApiClient.result { PremiumApiClient.verify(json, signature) }
                            if (result.isSuccess) latest = result.getOrThrow()
                        }
                        val restored = latest
                        if (restored != null && uid == PremiumApiClient.memberId()) {
                            pending.edit().remove(uid).commit()
                            onVerified(restored)
                            message("خرید بازیابی شد و اشتراک پرو فعال است.")
                        } else message(if (receipts.isEmpty()) "اشتراکی برای این حساب بازار پیدا نشد." else "خرید تأیید نشد؛ با حساب زمان خرید وارد شوید و دوباره بازیابی کنید.")
                    } catch (_: Exception) { message("بازیابی انجام نشد؛ دوباره تلاش کنید.") }
                }
            }
            queryFailed { message("خریدها از بازار دریافت نشد؛ دوباره تلاش کنید.") }
        }
    }
    private fun verify(info: PurchaseInfo) {
        mutableState.value = mutableState.value.copy(busy = true, message = "پرداخت دریافت شد؛ در حال تأیید اشتراک…")
        try {
            check(uid != null)
            check(pending.edit().putString(uid, JSONObject().put("json", info.originalJson).put("signature", info.dataSignature).toString()).commit())
        } catch (_: Exception) { message("خرید ثبت شد؛ برای تکمیل فعال‌سازی دکمهٔ بازیابی خرید را بزنید."); return }
        scope.launch {
            val result = PremiumApiClient.result { PremiumApiClient.verify(info.originalJson, info.dataSignature) }
            if (result.isSuccess && uid == PremiumApiClient.memberId()) {
                pending.edit().remove(uid).commit()
                onVerified(result.getOrThrow())
                message("اشتراک پرو فعال شد؛ به فضای هوشمند خوش آمدید.")
            } else message(result.exceptionOrNull()?.message ?: "حساب تغییر کرده؛ با حساب زمان خرید وارد شوید.")
        }
    }
    private fun message(value: String) { mutableState.value = mutableState.value.copy(busy = false, message = value) }
    fun disconnect() { connection?.disconnect(); connection = null; payment = null; mutableState.value = mutableState.value.copy(connected = false, busy = false) }
}
