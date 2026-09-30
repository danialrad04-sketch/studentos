package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.ui.components.ActionableEmptyState
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

class AccessibilitySmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun guestAccessActionIsExposedAndInvokesCallback() {
        var guestClicked = false

        composeRule.setContent {
            MyApplicationTheme {
                com.example.ui.components.AccountGateV2(
                    onSignIn = { _, _, result -> result(false, "test") },
                    onSignUp = { _, _, _, result -> result(false, "test") },
                    onForgotPassword = {},
                    onGoogleSignIn = {},
                    onContinueAsGuest = { guestClicked = true }
                )
            }
        }

        composeRule.onNodeWithTag("continue_as_guest_button")
            .assertHasClickAction()
            .performClick()

        assertTrue(guestClicked)
    }

    @Test
    fun actionableEmptyStateExposesAccessiblePrimaryAction() {
        var clicked = false

        composeRule.setContent {
            MyApplicationTheme {
                ActionableEmptyState(
                    title = "هنوز درسی ثبت نشده",
                    description = "برای شروع یک درس اضافه کنید.",
                    icon = Icons.Default.Add,
                    primaryActionTitle = "افزودن درس",
                    onPrimaryAction = { clicked = true }
                )
            }
        }

        composeRule.onNodeWithText("افزودن درس")
            .assertHasClickAction()
            .performClick()

        assertTrue(clicked)
    }
}
