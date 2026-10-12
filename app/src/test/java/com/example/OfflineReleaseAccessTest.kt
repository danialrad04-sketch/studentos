package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.domain.model.UserAccount
import com.example.ui.components.AccountGateV2
import com.example.ui.components.AuthAccountDialog
import com.example.ui.theme.StudentOsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OfflineReleaseAccessTest {
    @get:Rule val rule = createComposeRule()

    @Test fun localEntryCannotInvokeAuthentication() {
        var authCalls = 0
        var continued = 0
        rule.setContent {
            StudentOsTheme {
                AccountGateV2(
                    onSignIn = { _, _, _ -> authCalls++ },
                    onSignUp = { _, _, _, _ -> authCalls++ },
                    onGoogleSignIn = { authCalls++ },
                    onForgotPassword = { authCalls++ },
                    onContinueAsGuest = { continued++ }
                )
            }
        }
        rule.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        rule.onNodeWithText("ساخت حساب").assertDoesNotExist()
        rule.onNodeWithTag("continue_as_guest_button").performClick()
        assertEquals(1, continued)
        assertEquals(0, authCalls)
    }

    @Test fun guestAccountRetainsLocalBackupWithoutOfferingRegistration() {
        var backups = 0
        rule.setContent {
            StudentOsTheme {
                AuthAccountDialog(UserAccount(), { _, _, _ -> }, { _, _, _, _ -> }, {}, {}, {}, {}, {}, {}, {},
                    onOpenBackupRestore = { backups++ })
            }
        }
        rule.onNodeWithText("ورود یا ساخت حساب").assertDoesNotExist()
        rule.onNodeWithText("همگام‌سازی اکنون").assertDoesNotExist()
        rule.onNodeWithText("پشتیبان و بازیابی").performScrollTo().performClick()
        assertEquals(1, backups)
    }

    @Test fun existingAccountCannotExposeCheckoutFromServerCatalog() {
        var purchases = 0
        var restores = 0
        rule.setContent {
            StudentOsTheme {
                com.example.ui.components.SubscriptionUpgradeDialog(
                    UserAccount(uid = "existing-member", isGuest = false),
                    com.example.domain.model.PremiumStatus(salesEnabled = true),
                    com.example.domain.model.BazaarBillingState(connected = true),
                    { purchases++ }, { restores++ }, {}, {}, {})
            }
        }
        rule.onNodeWithTag("billing_unavailable").assertIsDisplayed()
        rule.onNodeWithTag("purchase_subscription").assertDoesNotExist()
        rule.onNodeWithTag("restore_purchase").assertDoesNotExist()
        assertEquals(0, purchases)
        assertEquals(0, restores)
    }

    @Test fun subscriptionRouteDoesNotContactBillingForGuestOrRestoredMember() {
        rule.setContent {
            StudentOsTheme {
                com.example.ui.components.SubscriptionUpgradeRoute(
                    UserAccount(uid = "existing-member", isGuest = false), {}, {}, {})
            }
        }
        rule.onNodeWithTag("billing_unavailable").assertIsDisplayed()
        rule.onNodeWithTag("premium_sign_in").assertDoesNotExist()
    }
}
