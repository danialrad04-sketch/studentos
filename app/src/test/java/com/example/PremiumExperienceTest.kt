package com.example

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.example.domain.model.*
import com.example.ui.components.SubscriptionUpgradeDialog
import com.example.ui.theme.StudentOsTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xxhdpi", sdk = [35])
class PremiumExperienceTest {
    @get:Rule val rule = createComposeRule()
    private val monthly = PremiumPlan("studentos_pro_monthly", "یک‌ماهه", 1, 194500)
    private val quarterly = PremiumPlan("studentos_pro_quarterly", "سه‌ماهه", 3, 583500)
    private fun screen(guest: Boolean = false, dark: Boolean = false, scale: Float = 1f,
        status: PremiumStatus = PremiumStatus(listOf(monthly, quarterly), true, true),
        billing: BazaarBillingState = BazaarBillingState(connected = true, prices = mapOf(monthly.productId to "۱٬۹۴۵٬۰۰۰ ریال", quarterly.productId to "۵٬۸۳۵٬۰۰۰ ریال")),
        purchase: (PremiumPlan) -> Unit = {}, signIn: () -> Unit = {}, restore: () -> Unit = {}) {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                StudentOsTheme(darkTheme = dark) {
                    SubscriptionUpgradeDialog(UserAccount(uid = "test-member", isGuest = guest), status, billing, purchase, restore, {}, signIn, {})
                }
            }
        }
        rule.waitForIdle()
    }
    @Test fun verifiedCatalogAndMatchingMarketPriceAllowCheckout() {
        var purchased: PremiumPlan? = null
        screen(purchase = { purchased = it })
        rule.onNodeWithTag("purchase_subscription").assertIsEnabled().assertIsDisplayed().performClick()
        assertEquals(monthly, purchased)
        rule.onNodeWithTag("premium_page").captureRoboImage("build/outputs/visual-review/premium-light.png")
    }
    @Test fun selectingQuarterlyChangesTheRealCheckoutProduct() {
        var purchased: PremiumPlan? = null
        screen(purchase = { purchased = it })
        rule.onNodeWithTag("plan_${quarterly.productId}").performScrollTo().performClick().assertIsSelected()
        rule.onNodeWithTag("purchase_subscription").performClick()
        assertEquals(quarterly, purchased)
    }
    @Test fun priceMismatchNeverStartsPayment() {
        screen(billing = BazaarBillingState(connected = true, prices = mapOf(monthly.productId to "۲۵۰٬۰۰۰ تومان")))
        rule.onNodeWithTag("purchase_subscription").assertIsNotEnabled()
    }
    @Test fun anUnconfiguredServerDoesNotOfferFakeActivation() {
        screen(status = PremiumStatus(listOf(monthly), salesEnabled = false), billing = BazaarBillingState())
        rule.onNodeWithTag("purchase_subscription").assertIsNotEnabled()
        rule.onNodeWithTag("restore_purchase").performScrollTo().assertIsNotEnabled()
    }
    @Test fun guestUsesRealSignInAndCannotRestoreAnotherAccount() {
        var clicked = false
        screen(guest = true, signIn = { clicked = true })
        rule.onNodeWithTag("premium_sign_in").performClick()
        assertTrue(clicked)
        rule.onNodeWithTag("restore_purchase").performScrollTo().assertIsNotEnabled()
    }
    @Test fun pendingVerificationPreventsRepeatedPurchasesAndShowsRecovery() {
        screen(billing = BazaarBillingState(busy = true, connected = true, message = "پرداخت دریافت شد؛ در حال تأیید اشتراک…"))
        rule.onNodeWithTag("purchase_subscription").assertIsNotEnabled()
        rule.onNodeWithTag("billing_message").performScrollTo().assertIsDisplayed()
    }
    @Test fun darkLargeFontKeepsCheckoutReachableAndRestoreWorks() {
        var restored = false
        screen(dark = true, scale = 1.5f, restore = { restored = true })
        rule.onNodeWithTag("purchase_subscription").assertIsEnabled().assertIsDisplayed()
        rule.onNodeWithTag("restore_purchase").performScrollTo().performClick()
        assertTrue(restored)
        rule.onNodeWithTag("premium_page").captureRoboImage("build/outputs/visual-review/premium-dark-large-font.png")
    }
    @Test fun marketPriceParsesBothDigitsAndCurrenciesButRejectsAmbiguousAmounts() {
        assertTrue(BazaarPrice.matches("۱۹۴٬۵۰۰ تومان", 194500))
        assertTrue(BazaarPrice.matches("١٬٩٤٥٬٠٠٠ ریال", 194500))
        assertTrue(BazaarPrice.matches("1,945,000 IRR", 194500))
        assertFalse(BazaarPrice.matches("194500", 194500))
        assertFalse(BazaarPrice.matches("۱۹۴۵۰۰ و ۲۵۰۰۰۰ تومان", 194500))
        assertFalse(BazaarPrice.matches("۱٬۹۴۵٬۰۰۰ تومان", 194500))
    }
    @Test fun expiredSubscriptionCannotUnlockPremium() {
        assertFalse(SubscriptionDetails(tier = SubscriptionTier.PRO, expiresAt = System.currentTimeMillis() - 1000).isProOrHigher)
    }
    @Test fun cloudSnapshotPreservesExpiryAndUsesTheActualPaidQuotas() {
        val now = System.currentTimeMillis()
        val fields = mapOf<String, Any>("subscriptionTier" to "PRO", "subscriptionExpiresAt" to now + 60000,
            "bazaarAutoRenewing" to false, "maxDailyAiQuota" to 999)
        val active = com.example.data.cloud.FirestoreSyncManager.subscriptionFromCloud(fields, now)
        assertEquals(now + 60000, active.expiresAt)
        assertEquals(50, active.maxDailyAiQuota)
        assertEquals(10, active.maxDailyScanQuota)
        assertTrue(active.isProOrHigher)
        assertFalse(active.autoRenewing)
        val expired = com.example.data.cloud.FirestoreSyncManager.subscriptionFromCloud(fields, now + 60001)
        assertEquals(SubscriptionTier.FREE, expired.tier)
        assertEquals(5, expired.maxDailyAiQuota)
        assertFalse(expired.isCloudSyncEnabled)
        val timestampFields = fields + ("subscriptionExpiresAt" to com.google.firebase.Timestamp(java.util.Date(now + 60000)))
        assertEquals(now + 60000, com.example.data.cloud.FirestoreSyncManager.subscriptionFromCloud(timestampFields, now).expiresAt)
    }
}
