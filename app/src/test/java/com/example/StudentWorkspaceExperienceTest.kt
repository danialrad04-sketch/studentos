package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.Density
import com.example.data.local.entity.*
import com.example.data.local.relation.CourseWithSessions
import com.example.domain.engine.StudentAgendaEngine
import com.example.domain.model.ExamItem
import com.example.domain.util.JalaliCalendarUtil
import com.example.ui.components.*
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
@Config(qualifiers="w360dp-h800dp-xxhdpi",sdk=[35])
class StudentWorkspaceExperienceTest {
    @get:Rule val rule=createComposeRule()
    private val course=CourseEntity(id="math",name="ریاضی مهندسی",units=3,colorHex="#FFFF00")
    @Test fun failedNoteSaveKeepsTheDraftAndOnlySuccessfulAcknowledgementClosesIt() {
        var complete:((Boolean)->Unit)?=null
        rule.setContent { StudentOsTheme { Surface(Modifier.fillMaxSize(),color=androidx.compose.material3.MaterialTheme.colorScheme.background) { Column(Modifier.verticalScroll(rememberScrollState())) {
            StudyNotesScreen(emptyList(),listOf(course),"owner",{_,_,done->complete=done},{_,_,_->})
        } } } }
        rule.onNodeWithTag("note_add").performClick()
        rule.onNodeWithTag("note_title").performTextInput("فرمول انتگرال")
        rule.onNodeWithTag("note_content").performTextInput("متن کامل یادداشت")
        rule.onNodeWithTag("note_save").performScrollTo().performClick()
        rule.runOnIdle { assertNotNull(complete) }
        rule.onNodeWithTag("note_content").assertExists()
        rule.runOnIdle { complete!!(false) }
        rule.onNodeWithTag("note_content").assertTextContains("متن کامل یادداشت")
        rule.onNodeWithTag("note_save").performScrollTo().performClick()
        rule.runOnIdle { complete!!(true) }
        rule.onNodeWithTag("note_content").assertDoesNotExist()
    }
    @Test fun accountChangeClearsSearchAndDoesNotReplayAnotherAccountsDraft() {
        var owner by mutableStateOf("first")
        rule.setContent { StudentOsTheme { Surface(Modifier.fillMaxSize(),color=androidx.compose.material3.MaterialTheme.colorScheme.background) { Column(Modifier.verticalScroll(rememberScrollState())) {
            StudyNotesScreen(emptyList(),emptyList(),owner,{_,_,_->},{_,_,_->})
        } } } }
        rule.onNodeWithTag("notes_search").performTextInput("عبارت خصوصی")
        rule.onNodeWithTag("note_add").performClick()
        rule.onNodeWithTag("note_title").performTextInput("عنوان حساب اول")
        rule.runOnIdle { owner="second" }
        rule.onNodeWithTag("note_title").assertDoesNotExist()
        assertEquals("",rule.onNodeWithTag("notes_search").fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        rule.onNodeWithTag("note_add").performClick()
        assertEquals("",rule.onNodeWithTag("note_title").fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
    }
    @Test fun archiveAndRestoreAreConfirmedAndLightNotebookHasReadableSurfaces() {
        val rows=mutableStateListOf(NoteEntity(id="n",courseId="math",title="خلاصهٔ فصل اول",content="نکتهٔ مهم\n".repeat(8)))
        rule.setContent { StudentOsTheme { Surface(Modifier.fillMaxSize(),color=androidx.compose.material3.MaterialTheme.colorScheme.background) { Column(Modifier.verticalScroll(rememberScrollState())) {
            StudyNotesScreen(rows,listOf(course),"owner",{_,_,_->},{note,archived,done->rows[0]=note.copy(isArchived=archived);done(true)})
        } } } }
        rule.onRoot().captureRoboImage("build/outputs/visual-review/notebook-light-rtl.png")
        rule.onNodeWithTag("note_archive_n").performScrollTo().performClick()
        rule.onNodeWithText("تأیید").performClick()
        rule.onNodeWithTag("note_n").assertDoesNotExist()
        rule.onNodeWithTag("notes_archived").performScrollTo().performClick()
        rule.onNodeWithTag("note_archive_n").performScrollTo().performClick()
        rule.onNodeWithText("تأیید").performClick()
        rule.onNodeWithTag("notes_active").performScrollTo().performClick()
        rule.onNodeWithTag("note_n").assertExists()
    }
    @Test fun darkLargeTextNotebookKeepsSaveReachableAndDoesNotSilentlyTruncateLongText() {
        rule.setContent { val d=LocalDensity.current;CompositionLocalProvider(LocalDensity provides Density(d.density,1.5f)) {
            StudentOsTheme(darkTheme=true) { Surface(Modifier.fillMaxSize(),color=androidx.compose.material3.MaterialTheme.colorScheme.background) { Column(Modifier.verticalScroll(rememberScrollState())) {
                StudyNotesScreen(listOf(NoteEntity(id="n",title="یادداشت درسی با عنوان بلند و فرمول",content="متن درسی خوانا")),listOf(course),"owner",{_,_,_->},{_,_,_->})
            } } }
        } }
        rule.onRoot().captureRoboImage("build/outputs/visual-review/notebook-dark-large-rtl.png")
        rule.onNodeWithTag("note_add").performClick()
        rule.onNodeWithTag("note_title").performTextInput("عنوان")
        val longText="الف ".repeat(8000)
        rule.onNodeWithTag("note_content").performTextReplacement(longText)
        rule.onNodeWithTag("note_save").performScrollTo().assertIsNotEnabled()
        assertEquals(longText,rule.onNodeWithTag("note_content").fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        rule.onNodeWithTag("note_content").performTextReplacement("متن درست")
        rule.onNodeWithTag("note_save").performScrollTo().assertIsEnabled()
    }
    @Test fun agendaFiltersAndWeekNavigationUseRealRecordsAndExportTheFullSelectedWeek() {
        val today=JalaliCalendarUtil.today();val session=CourseSessionEntity(id="s",courseId="math",day=StudentAgendaEngine.weekday(today),start="10:00",end="12:00")
        var snapshot:StudentAgendaEngine.Week?=null
        rule.setContent { StudentOsTheme { Surface(Modifier.fillMaxSize(),color=androidx.compose.material3.MaterialTheme.colorScheme.background) { Column(Modifier.verticalScroll(rememberScrollState())) {
            StudentAgendaScreen(listOf(CourseWithSessions(course,listOf(session))),listOf(TaskEntity(id=1,title="تمرین",dueDate=today.format())),listOf(ExamItem("e","ریاضی",today.format(),"","",3)),{},{},{},{},{snapshot=it})
        } } } }
        rule.onRoot().captureRoboImage("build/outputs/visual-review/agenda-light-rtl.png")
        rule.onNodeWithTag("agenda_day_${today.format()}").assertExists()
        rule.onNodeWithTag("agenda_show_all").performScrollTo().performClick()
        StudentAgendaEngine.build(StudentAgendaEngine.startOfWeek(today),emptyList(),emptyList(),emptyList(),today).days.forEach { day -> rule.onNodeWithTag("agenda_day_${day.format()}").assertExists() }
        rule.onNodeWithTag("agenda_filter_TASK").performScrollTo().performClick()
        rule.onNodeWithTag("agenda_export").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(3,snapshot!!.events.size) }
        rule.onNodeWithTag("agenda_next").performScrollTo().performClick()
        rule.onNodeWithTag("agenda_export").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1,snapshot!!.events.size);assertTrue(snapshot!!.start>today) }
        rule.onNodeWithTag("agenda_current").performScrollTo().performClick()
        rule.onNodeWithTag("agenda_export").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(StudentAgendaEngine.startOfWeek(today),snapshot!!.start) }
    }
}
