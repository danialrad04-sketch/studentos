package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.AppPreferencesRepository
import com.example.ui.StudentViewModel
import com.example.ui.models.AppTab
import org.junit.Rule
import org.junit.Test

/** Exercises the production viewport, including the chat's finite list and actual IME. */
class MainWorkspaceDeviceTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    @Test fun chatComposerAndNewWorkspacePagesRemainReachableInTheActualApp() {
        rule.waitUntil(30000) { rule.onAllNodesWithTag("local_onboarding").fetchSemanticsNodes().isNotEmpty() || rule.onAllNodesWithTag("primary_navigation").fetchSemanticsNodes().isNotEmpty() }
        AppPreferencesRepository.getInstance(rule.activity).setNotificationsEnabled(false)
        val vm=ViewModelProvider(rule.activity)[StudentViewModel::class.java]
        val database = com.example.data.local.AppDatabase.getDatabase(rule.activity)
        kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
            database.studentDao().insertProfile(
                com.example.data.local.entity.StudentProfileEntity(name="دانشجوی آزمون",university="دانشگاه آزمون",major="ریاضی",entryYear=1405,currentSemester=1,isOnboardingCompleted=true))
        }
        rule.waitUntil(20000){rule.onAllNodesWithTag("primary_navigation").fetchSemanticsNodes().isNotEmpty()}
        rule.runOnIdle{vm.selectTab(AppTab.COPILOT)}
        rule.waitUntil(15000){rule.onAllNodesWithTag("copilot_input_field").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("copilot_input_field").performClick().performTextInput("پرسش آزمون")
        rule.onNodeWithTag("ai_send").assertIsDisplayed()
        rule.onNodeWithTag("copilot_input_field").assertIsDisplayed()
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        rule.runOnIdle{vm.selectTab(AppTab.AGENDA)}
        rule.waitUntil(15000){rule.onAllNodesWithTag("student_agenda_screen").fetchSemanticsNodes().isNotEmpty()}
        rule.runOnIdle{vm.selectTab(AppTab.NOTES)}
        rule.waitUntil(15000){rule.onAllNodesWithTag("study_notes_screen").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("note_add").performScrollTo().performClick()
        rule.onNodeWithTag("note_title").performTextInput("یادداشت روی دستگاه")
        rule.onNodeWithTag("note_content").performTextInput("ذخیرهٔ واقعی و آفلاین")
        rule.onNodeWithTag("note_save").performScrollTo().performClick()
        rule.waitUntil(15000){rule.onAllNodesWithTag("note_save").fetchSemanticsNodes().isEmpty()}
        rule.waitUntil(15000){rule.onAllNodesWithText("یادداشت روی دستگاه").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText("یادداشت روی دستگاه").assertExists()
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        AppTab.entries.forEach { tab ->
            rule.runOnIdle { vm.selectTab(tab) }
            rule.waitUntil(15000) { rule.onAllNodesWithText(tab.title).fetchSemanticsNodes().isNotEmpty() }
            rule.waitForIdle()
            rule.onNodeWithTag("primary_navigation").assertExists()
        }

    }
}
