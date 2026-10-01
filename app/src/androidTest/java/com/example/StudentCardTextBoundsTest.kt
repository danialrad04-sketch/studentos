package com.example

import androidx.compose.ui.test.assertHeightIsAtMost
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.ui.components.StudentCardBody
import org.junit.Rule
import org.junit.Test

class StudentCardTextBoundsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun longCardBodyCannotCreateUnboundedHeight() {
        val longText = "این یک متن بسیار طولانی برای تست محدودیت ارتفاع کارت است. ".repeat(40)

        composeRule.setContent {
            MaterialTheme {
                StudentCardBody(
                    text = longText,
                    maxLines = 3,
                    modifier = androidx.compose.ui.Modifier
                )
            }
        }

        composeRule
            .onNodeWithText(longText)
            .assertHeightIsAtMost(90.dp)
    }
}
