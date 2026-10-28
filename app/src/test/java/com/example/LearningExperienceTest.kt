package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.*
import com.example.data.repository.learningToday
import com.example.domain.engine.*
import com.example.domain.model.StudyDay
import com.example.ui.components.*
import com.example.ui.models.LearningWorkspaceState
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
class LearningExperienceTest {
    @get:Rule val rule = createComposeRule()
    private val course = CourseEntity(id = "math", name = "ریاضی مهندسی و کاربرد معادلات دیفرانسیل")
    private val emptyPlan =
        AdaptiveLearningPlan(emptyList(), emptyMap(), emptySet(), emptySet(), emptyMap())

    @Test
    fun persianGoalInputIsValidatedAndWritesOnlyAfterExplicitSave() {
        var saved: StudyGoalEntity? = null
        rule.setContent {
            StudentOsTheme {
                Surface {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
                    ) {
                        LearningPlannerScreen(
                            LearningWorkspaceState(loaded = true),
                            emptyPlan,
                            listOf(course),
                            {},
                            { row, done ->
                                saved = row
                                done(true)
                            },
                            { _, done -> done(true) },
                            { _, _, done -> done(true) },
                            { _, done -> done(true) },
                            {},
                        )
                    }
                }
            }
        }
        rule.onNodeWithTag("learning_add_goal").performClick()
        rule.onNodeWithTag("learning_goal_save").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("learning_goal_minutes").performTextInput("۱۲۰")
        rule
            .onNodeWithTag("learning_goal_date")
            .performTextInput(StudentAgendaEngine.addDays(learningToday(), 10).toPersianDigits())
        rule.runOnIdle { assertNull(saved) }
        rule.onNodeWithTag("learning_goal_save").performScrollTo().assertIsEnabled().performClick()
        rule.runOnIdle {
            assertEquals(120, saved!!.totalMinutes)
            assertEquals(course.id, saved!!.courseId)
        }
    }

    @Test
    fun weeklyAvailabilityDoesNotAcceptOverbookedMinutes() {
        var saved: StudyAvailabilityEntity? = null
        rule.setContent {
            StudentOsTheme {
                Surface {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
                    ) {
                        LearningPlannerScreen(
                            LearningWorkspaceState(loaded = true),
                            emptyPlan,
                            listOf(course),
                            {},
                            { _, done -> done(true) },
                            { row, done ->
                                saved = row
                                done(true)
                            },
                            { _, _, done -> done(true) },
                            { _, done -> done(true) },
                            {},
                        )
                    }
                }
            }
        }
        rule.onNodeWithTag("learning_availability").performClick()
        rule.onNodeWithTag("learning_free_budget").performTextReplacement("۴۸۱")
        rule.onNodeWithTag("learning_free_save").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("learning_free_budget").performTextReplacement("۶۰")
        rule.onNodeWithTag("learning_free_save").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(60, saved!!.budgetMinutes)
            assertEquals(1020, saved!!.startMinute)
        }
    }

    @Test
    fun largeTextTodayPlanKeepsFocusAndConfirmedWorkSeparateAndReachable() {
        val block = LearningBlock("slot", course.id, learningToday().format(), 600, 25)
        var started: LearningBlock? = null
        var actual = 0
        rule.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, 1.5f)) {
                StudentOsTheme(darkTheme = true) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        Column(
                            Modifier.fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            LearningPlannerScreen(
                                LearningWorkspaceState(
                                    goals =
                                        listOf(
                                            StudyGoalEntity(
                                                course.id,
                                                learningToday().format(),
                                                120,
                                            )
                                        ),
                                    loaded = true,
                                ),
                                emptyPlan.copy(blocks = listOf(block)),
                                listOf(course),
                                {},
                                { _, done -> done(true) },
                                { _, done -> done(true) },
                                { _, minutes, done ->
                                    actual = minutes
                                    done(true)
                                },
                                { _, done -> done(true) },
                                { started = it },
                            )
                        }
                    }
                }
            }
        }
        rule.onNodeWithText("ویرایش هدف").assertDoesNotExist()
        rule.onNodeWithTag("learning_focus_slot").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(block, started)
            assertEquals(0, actual)
        }
        rule.onRoot().captureRoboImage("build/outputs/visual-review/learning-plan-dark-large.png")
        rule.onNodeWithText("ثبت انجام جلسه").performScrollTo().performClick()
        rule.onNodeWithTag("learning_actual_minutes").performTextReplacement("۲۰")
        rule.onNodeWithText("ثبت جلسه").performClick()
        rule.runOnIdle { assertEquals(20, actual) }
    }

    @Test
    fun reviewRevealsAnswerBeforeRatingAndAgainRequiresANewRound() {
        var cards by
            mutableStateOf(
                listOf(
                    ReviewCardEntity(
                        "c",
                        course.id,
                        "مشتق سینوس چیست؟",
                        "کسینوس",
                        learningToday().format(),
                    )
                )
            )
        var ratings = 0
        rule.setContent {
            StudentOsTheme(darkTheme = true) {
                CourseReviewDeck(
                    course,
                    cards,
                    {},
                    { _, done -> done(true) },
                    { card, rating, done ->
                        cards =
                            listOf(
                                SpacedReviewEngine.rate(
                                    card,
                                    rating,
                                    learningToday(),
                                    System.currentTimeMillis(),
                                )
                            )
                        ratings++
                        done(true)
                    },
                )
            }
        }
        rule.onNodeWithTag("review_answer").assertDoesNotExist()
        rule.onNodeWithTag("review_rate_GOOD").assertDoesNotExist()
        rule.onNodeWithTag("review_reveal").performScrollTo().performClick()
        rule.onNodeWithTag("review_answer").assertTextEquals("کسینوس")
        rule.onRoot().captureRoboImage("build/outputs/visual-review/review-deck-dark.png")
        rule.onNodeWithTag("review_rate_AGAIN").performScrollTo().performClick()
        rule.onNodeWithText("شروع نوبت تازه").performScrollTo().performClick()
        rule.onNodeWithTag("review_reveal").performScrollTo().performClick()
        rule.onNodeWithTag("review_rate_GOOD").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(2, ratings)
            assertEquals(
                StudentAgendaEngine.addDays(learningToday(), 1).format(),
                cards.single().dueDate,
            )
        }
    }

    @Test
    fun cardEditorPreservesPersianDraftThroughRecreationAndFailedSave() {
        val restoration = StateRestorationTester(rule)
        var saved: ReviewCardEntity? = null
        var callback: ((Boolean) -> Unit)? = null
        restoration.setContent {
            StudentOsTheme {
                CourseReviewDeck(
                    course,
                    emptyList(),
                    {},
                    { card, done ->
                        saved = card
                        callback = done
                    },
                    { _, _, done -> done(true) },
                )
            }
        }
        rule.onNodeWithTag("review_add").performScrollTo().performClick()
        rule.onNodeWithTag("review_edit_question").performTextInput("پرسش محفوظ")
        rule.onNodeWithTag("review_edit_answer").performTextInput("پاسخ محفوظ")
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("review_edit_question").assertTextContains("پرسش محفوظ")
        rule.onNodeWithTag("review_save").performScrollTo().performClick()
        rule.onNodeWithTag("review_save").assertIsNotEnabled()
        rule.runOnIdle {
            assertEquals("پاسخ محفوظ", saved!!.answer)
            callback!!(false)
        }
        rule.onNodeWithTag("review_edit_answer").assertTextContains("پاسخ محفوظ")
        rule.onNodeWithTag("review_save").assertIsEnabled()
    }

    @Test
    fun dayStripScrollsWithoutExpandingSettingsOverToday() {
        val block = LearningBlock("light-slot", course.id, learningToday().format(), 600, 25)
        rule.setContent {
            StudentOsTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
                    ) {
                        LearningPlannerScreen(
                            LearningWorkspaceState(
                                goals =
                                    listOf(
                                        StudyGoalEntity(
                                            course.id,
                                            StudentAgendaEngine.addDays(learningToday(), 6)
                                                .format(),
                                            60,
                                        )
                                    ),
                                availability = listOf(StudyAvailabilityEntity(0, 540, 720, 60)),
                                loaded = true,
                            ),
                            emptyPlan.copy(blocks = listOf(block)),
                            listOf(course),
                            {},
                            { _, done -> done(true) },
                            { _, done -> done(true) },
                            { _, _, done -> done(true) },
                            { _, done -> done(true) },
                            {},
                        )
                    }
                }
            }
        }
        rule.onNodeWithText("ویرایش هدف").assertDoesNotExist()
        rule.onNodeWithTag("learning_day_0").assertIsSelected()
        rule.onRoot().captureRoboImage("build/outputs/visual-review/learning-plan-light.png")
        rule.onNodeWithTag("learning_day_6").performScrollTo().performClick().assertIsSelected()
        rule.onNodeWithTag("learning_focus_light-slot").assertDoesNotExist()
    }

    @Test
    fun reportDoesNotAddTimerMinutesToSelfReportedMinutes() {
        val day =
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val state =
            LearningWorkspaceState(
                completions =
                    listOf(
                        StudyCompletionEntity("x", course.id, learningToday().format(), 600, 25, 20)
                    ),
                loaded = true,
            )
        rule.setContent {
            StudentOsTheme {
                Surface {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
                    ) {
                        WeeklyLearningReport(
                            state,
                            listOf(StudyDay(day, 15, 1)),
                            day,
                            setOf(course.id),
                            emptyList(),
                            {},
                        )
                    }
                }
            }
        }
        rule.onNodeWithText("15 دقیقه با تایمر", substring = true).assertExists()
        rule.onNodeWithText("20 دقیقه اعلامی", substring = true).assertExists()
        rule.onNodeWithText("35 دقیقه", substring = true).assertDoesNotExist()
        rule
            .onRoot()
            .captureRoboImage("build/outputs/visual-review/weekly-learning-report-light.png")
    }
}
