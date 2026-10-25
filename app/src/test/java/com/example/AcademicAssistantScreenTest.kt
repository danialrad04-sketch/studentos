package com.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.local.entity.StudentProfileEntity
import com.example.domain.model.UserAccount
import com.example.ui.components.AcademicCopilotScreen
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
class AcademicAssistantScreenTest {
    @get:Rule val rule = createComposeRule()
    @Test fun freeGuestCanUseEveryModeAndLocalAnalysisWithoutProvider() {
        var premiumOpened = false
        var modelCalls = 0
        rule.setContent { StudentOsTheme { Surface(Modifier.fillMaxSize()) {
            AcademicCopilotScreen(StudentProfileEntity(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
                onNavigateTab = {}, onExecuteAction = { _, done -> done(false) }, onOpenPastSemestersDialog = {},
                onOpenPremium = { premiumOpened = true }, statusProvider = { null },
                adviceProvider = { _, _, _, _ -> modelCalls++; Result.success("پاسخ آزمایشی") })
        } } }
        rule.onNodeWithTag("ai_local_only").performClick()
        for (mode in listOf("coach", "exam", "review", "chat")) {
            rule.onNodeWithTag("ai_mode_$mode").performScrollTo().performClick()
            rule.onNodeWithTag("ai_mode_$mode").assertIsSelected()
        }
        rule.onNodeWithTag("copilot_input_field").performTextReplacement("تکالیف من را بگو")
        rule.onNodeWithTag("ai_send").performClick()
        rule.waitForIdle()
        rule.runOnIdle { assertFalse(premiumOpened); assertEquals(0, modelCalls) }
        rule.onRoot().captureRoboImage("build/outputs/visual-review/assistant-light-local.png")
    }
    @Test fun anonymousTransportUidChangePreservesGuestDraft() {
        var account by mutableStateOf(UserAccount())
        rule.setContent { StudentOsTheme { Surface(Modifier.fillMaxSize()) {
            AcademicCopilotScreen(StudentProfileEntity(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
                onNavigateTab = {}, onExecuteAction = { _, _ -> }, onOpenPastSemestersDialog = {}, userAccount = account, statusProvider = { null })
        } } }
        rule.onNodeWithTag("copilot_input_field").performTextInput("پرسش نیمه‌تمام")
        rule.runOnIdle { account = UserAccount(uid = "firebase-anonymous", isGuest = true) }
        rule.onNodeWithTag("copilot_input_field").assertTextContains("پرسش نیمه‌تمام")
    }
}
