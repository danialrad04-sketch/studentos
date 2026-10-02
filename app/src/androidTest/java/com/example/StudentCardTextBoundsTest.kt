package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.example.ui.components.StudentCardText
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StudentCardTextBoundsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun long_academic_text_never_creates_an_unbounded_card() {
        val longText = "اطلاعات واردشده توسط دانشجو ".repeat(80).trim()

        composeRule.setContent {
            MyApplicationTheme {
                StudentCardText(
                    text = longText,
                    maxLines = 3
                )
            }
        }

        val node = composeRule.onNodeWithText(longText, substring = false, useUnmergedTree = true)
        node.assertIsDisplayed()

        val maxHeightPx = with(composeRule.density) { 96.dp.toPx() }
        assertTrue(
            "Bounded card text exceeded the expected compact height",
            node.getUnclippedBoundsInRoot().height <= maxHeightPx
        )
    }
}
