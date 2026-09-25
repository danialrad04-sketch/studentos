package com.example

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.components.ActionableEmptyState
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

class AccessibilitySmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

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

        assert(clicked)
    }
}
