package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class OfflineStartupTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun assertLocalEntry() {
        rule.waitUntil(15_000) {
            rule.onAllNodesWithTag("local_onboarding").fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithTag("primary_navigation").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("primary_guest_entry").assertDoesNotExist()
        rule.onNodeWithText("ساخت حساب").assertDoesNotExist()
    }

    @Test fun launchAndActivityRecreationDoNotRequireAccountEntry() {
        assertLocalEntry()
        rule.activityRule.scenario.recreate()
        assertLocalEntry()
    }

    @Test fun packagedPermissionsMatchThePublicRelease() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val info = context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
        val permissions = info.requestedPermissions.orEmpty().toSet()
        org.junit.Assert.assertFalse(permissions.contains("com.farsitel.bazaar.permission.PAY_THROUGH_BAZAAR"))
        org.junit.Assert.assertFalse(permissions.contains("android.permission.USE_EXACT_ALARM"))
        org.junit.Assert.assertFalse(permissions.contains("android.permission.SCHEDULE_EXACT_ALARM"))
        org.junit.Assert.assertFalse(permissions.contains("android.permission.VIBRATE"))
        org.junit.Assert.assertFalse(permissions.contains("android.permission.MANAGE_EXTERNAL_STORAGE"))
    }
}
