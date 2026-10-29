package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.GradeEntity
import com.example.ui.components.*
import com.example.ui.models.AppTab
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
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xxhdpi")
class StudentColorfulToolsTest {
    @get:Rule val rule = createComposeRule()

    @Test fun persianNumbersProduceRealExamRequirementAndImpossibleGoal() {
        rule.setContent { StudentOsTheme { Surface {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { ExamScorePlannerCard() }
        } } }
        rule.onNodeWithTag("exam_score_expand").performClick()
        rule.onNodeWithTag("exam_earned").performScrollTo().performTextInput("۶")
        rule.onNodeWithTag("exam_maximum").performScrollTo().performTextInput("۱۲")
        rule.onNodeWithTag("exam_score_result").performScrollTo().assertTextContains("4.00", substring = true)
        rule.onNodeWithTag("exam_target").performScrollTo().performTextReplacement("۱۹")
        rule.onNodeWithTag("exam_score_result").performScrollTo().assertTextContains("ممکن نیست", substring = true)
        rule.onRoot().captureRoboImage("build/outputs/visual-review/exam-score-light.png")
    }

    @Test fun goalPlannerDoesNotOfferToWriteOrInventUnreachableGrades() {
        var writes = 0
        rule.setContent { StudentOsTheme(darkTheme = true) { Surface {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                GradeWhatIfSimulatorCard(listOf(
                    GradeEntity(courseName = "ریاضی", units = 3, midtermGrade = 6.0, finalGrade = 6.0, isRecorded = true),
                    GradeEntity(courseName = "فیزیک", units = 1)
                ), 4, { writes++ })
            }
        } } }
        rule.onNodeWithTag("grade_goal_result").assertTextContains("دست‌یافتنی نیست", substring = true)
        rule.onNodeWithText("جزئیات محاسبه").performScrollTo().performClick()
        rule.onNodeWithText("ریاضی · 3 واحد · مجموع ثبت‌شده: 12.00").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("کپی خلاصهٔ هدف").performScrollTo().performClick()
        rule.onNodeWithText("خلاصه کپی شد").assertExists()
        rule.runOnIdle { assertEquals(0, writes) }
        rule.onRoot().captureRoboImage("build/outputs/visual-review/grade-goal-dark.png")
    }

    @Test fun colorfulHomeNavigationStillOpensEachRealDestination() {
        var selected: AppTab? = null
        rule.setContent { StudentOsTheme { Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                    StudentWorkspaceShortcuts({ selected = it })
                    Spacer(Modifier.height(12.dp))
                    StudentHomeToolGrid { selected = it }
                }
                FloatingIslandNavigationBar(AppTab.DASHBOARD, { selected = it })
            }
        } } }
        rule.onRoot().captureRoboImage("build/outputs/visual-review/colorful-home-light.png")
        rule.onNodeWithText("کارنامه").performClick()
        rule.runOnIdle { assertEquals(AppTab.GRADES, selected) }
        rule.onNodeWithText("حساب").performClick()
        rule.runOnIdle { assertEquals(AppTab.ACCOUNT, selected) }
        rule.onNodeWithTag("home_NOTES").performClick()
        rule.runOnIdle { assertEquals(AppTab.NOTES, selected) }
    }
}
