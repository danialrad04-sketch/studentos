package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.models.AcademicProgressUiState
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
@Config(sdk=[35],qualifiers="w360dp-h800dp-xxhdpi")
class StudentVisualRefinementTest {
    @get:Rule val rule=createComposeRule()
    private fun gallery(dark:Boolean) {
        rule.setContent { StudentOsTheme(darkTheme=dark) { Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("استیکرهای Student OS",style=MaterialTheme.typography.titleMedium)
                AppEmojiType.entries.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        row.forEach { type ->
                            Surface(Modifier.weight(1f),shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surfaceContainerLow) {
                                Column(Modifier.padding(6.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                    AppEmoji(type,size=48.dp)
                                    Text(type.name,style=MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        } } }
        rule.onRoot().captureRoboImage("build/outputs/visual-review/sticker-atlas-${if(dark)"dark"else"light"}.png")
    }
    @Test fun generatedStickersRenderInLightTheme()=gallery(false)
    @Test fun generatedStickersRenderInDarkTheme()=gallery(true)

    @Test fun largeTextToolRowsKeepEveryDestinationReadableAndClickable() {
        var selected:AppTab?=null
        rule.setContent { val d=LocalDensity.current;CompositionLocalProvider(LocalDensity provides Density(d.density,1.5f)) {
            StudentOsTheme(darkTheme=true) { Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
                Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) { StudentHomeToolGrid { selected=it } }
            } }
        } }
        for(tab in listOf(AppTab.COPILOT,AppTab.EXAMS,AppTab.POMODORO,AppTab.ATTENDANCE,AppTab.CURRICULUM,AppTab.HISTORY)) {
            rule.onNodeWithTag("home_tool_${tab.name}").performScrollTo().assertIsDisplayed().performClick()
            rule.runOnIdle { assertEquals(tab,selected) }
        }
        rule.onRoot().captureRoboImage("build/outputs/visual-review/home-tools-dark-large.png")
    }
    @Test fun compactProgressNeverInventsPercentageAndKeepsTheRecordsRouteAvailable() {
        var recordsOpened=false
        rule.setContent { StudentOsTheme { Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                AcademicJourneyOverview(state=AcademicProgressUiState.Loading,targetGpa=null,recommendations=emptyList(),
                    dailyStudyGoalMinutes=120,studiedTodayMinutes=0,onOpenPassport={},onOpenGrades={},onOpenFocus={},
                    compact=true,declaredPassedUnits=34,declaredGpa="17.25",onCompleteRecords={recordsOpened=true})
            }
        } } }
        rule.onNodeWithText("۰٪").assertDoesNotExist()
        rule.onNodeWithText("0٪").assertDoesNotExist()
        rule.onNodeWithText("اطلاعات اعلامی: 34 واحد · معدل 17.25").assertExists()
        rule.onNodeWithText("تکمیل سوابق").performScrollTo().performClick()
        rule.runOnIdle { assertTrue(recordsOpened) }
        rule.onRoot().captureRoboImage("build/outputs/visual-review/progress-compact-partial-light.png")
    }
    @Test fun calendarHelpExplainsTheManualSnapshotAndDismissesNormally() {
        rule.setContent { StudentOsTheme { Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                StudentAgendaScreen(emptyList(),emptyList(),emptyList(),{},{},{},{},{})
            }
        } } }
        rule.onNodeWithTag("agenda_calendar_help").performScrollTo().performClick()
        rule.onNodeWithText("ساعت‌ها بر مبنای تهران‌اند.",substring=true).assertIsDisplayed()
        rule.onNodeWithText("خودکار به‌روز نمی‌شود",substring=true).assertIsDisplayed()
        rule.onNodeWithText("متوجه شدم").performClick()
        rule.onNodeWithText("خودکار به‌روز نمی‌شود",substring=true).assertDoesNotExist()
    }
    @Test fun populatedHomeKeepsPriorityAndWorkspaceActionsReachableAtLargeText() {
        var selected:AppTab?=null
        var focusStarts=0
        val today=com.example.domain.util.JalaliCalendarUtil.today()
        val course=com.example.data.local.entity.CourseEntity(id="math",name="ریاضی مهندسی و کاربردهای معادلات دیفرانسیل",units=3)
        rule.setContent { val d=LocalDensity.current;CompositionLocalProvider(LocalDensity provides Density(d.density,1.5f)) {
            StudentOsTheme(darkTheme=true) { Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
                ModernBentoDashboard(courses=listOf(course),
                    coursesWithSessions=listOf(com.example.data.local.relation.CourseWithSessions(course,listOf(
                        com.example.data.local.entity.CourseSessionEntity(id="s",courseId=course.id,day=com.example.domain.engine.StudentAgendaEngine.weekday(today),start="09:00",end="11:00")))),
                    attendanceList=emptyList(),tasks=listOf(com.example.data.local.entity.TaskEntity(id=1,courseId=course.id,
                        title="تمرین فصل سوم و تحویل گزارش گروهی",dueDate=today.format())),
                    exams=listOf(com.example.ui.models.ExamItem(id="e",courseName=course.name,
                        solarDate=com.example.domain.engine.StudentAgendaEngine.addDays(today,1).format(),time="09:00",location="کلاس ۱۰۸",units=3)),
                    grades=emptyList(),pomodoroSeconds=1500,isPomodoroRunning=false,
                    onTogglePomodoro={focusStarts++},onNavigateTab={selected=it},
                    modifier=Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp))
            } }
        } }
        rule.onRoot().captureRoboImage("build/outputs/visual-review/home-populated-dark-large-top.png")
        val layouts=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        rule.onNodeWithText("تمرین فصل سوم و تحویل گزارش گروهی",substring=true).performScrollTo()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        rule.runOnIdle { assertFalse("The priority title must not be ellipsized at large font scale",layouts.single().hasVisualOverflow) }
        rule.onRoot().captureRoboImage("build/outputs/visual-review/home-priority-dark-large.png")
        rule.onNodeWithTag("home_NOTES").performScrollTo().assertIsDisplayed()
        rule.onRoot().captureRoboImage("build/outputs/visual-review/home-populated-dark-large-workspace.png")
        rule.onNodeWithTag("home_NOTES").performClick()
        rule.runOnIdle { assertEquals(AppTab.NOTES,selected) }
        rule.onNodeWithText("شروع تمرکز").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1,focusStarts);assertEquals(AppTab.POMODORO,selected) }
    }

}
