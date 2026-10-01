package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import com.example.ui.components.AcademicInfoText
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UiOverflowGuardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun longInformationTextIsRenderedThroughBoundedComponent() {
        val longText = "این یک متن طولانی آزمایشی برای بررسی رفتار کارت‌های اطلاعاتی است. ".repeat(20)

        composeRule.setContent {
            MyApplicationTheme {
                AcademicInfoText(
                    text = longText,
                    maxLines = 2
                )
            }
        }

        assertTrue(
            composeRule.onAllNodesWithText(
                "این یک متن طولانی آزمایشی برای بررسی رفتار کارت‌های اطلاعاتی است. ".trim()
            ).fetchSemanticsNodes().isNotEmpty()
        )
    }
}