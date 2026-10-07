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
}
