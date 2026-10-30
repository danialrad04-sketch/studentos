package com.example

import com.example.data.local.entity.*
import com.example.data.local.relation.CourseWithSessions
import com.example.domain.engine.*
import com.example.domain.util.JalaliDate
import org.junit.Assert.*
import org.junit.Test

class UnifiedLearningPlannerTest {
    private val today = JalaliDate(1405, 8, 1)
    private val due = StudentAgendaEngine.addDays(today, 2).format()
    private val classes =
        listOf(CourseWithSessions(CourseEntity(id = "math", name = "ریاضی"), emptyList()))
    private val free = (0..6).map { StudyAvailabilityEntity(it, 540, 720, 60) }

    @Test
    fun courseworkAndOrdinaryStudyShareOneBudgetWithoutDoubleCountingLinkedTasks() {
        val project = ProjectEntity("p", "math", "پروژه", due, estimatedMinutes = 45)
        val task =
            TaskEntity(
                id = 1,
                title = "مرحله پروژه",
                courseId = "math",
                dueDate = due,
                projectId = "p",
                estimatedMinutes = 45,
            )
        val plan =
            UnifiedLearningPlanner.build(
                today,
                listOf(StudyGoalEntity("math", due, 30)),
                free,
                emptyList(),
                classes,
                emptyList(),
                listOf(task),
                listOf(project),
            )
        assertEquals(listOf("project:p"), plan.workItems.map { it.id })
        assertEquals(75, plan.blocks.sumOf { it.minutes })
        assertEquals(45, plan.blocks.filter { it.workId == "project:p" }.sumOf { it.minutes })
        assertTrue(plan.blocks.all { it.courseId == "math" })
        assertTrue(plan.blocks.groupBy { it.date }.values.all { it.sumOf { b -> b.minutes } <= 60 })
    }

    @Test
    fun unestimatedWorkIsVisibleButNoDurationIsInvented() {
        val task = TaskEntity(id = 1, title = "بدون برآورد", courseId = "math", dueDate = due)
        val plan =
            UnifiedLearningPlanner.build(
                today,
                emptyList(),
                free,
                emptyList(),
                classes,
                emptyList(),
                listOf(task),
                emptyList(),
            )
        assertTrue(plan.blocks.isEmpty())
        assertNull(plan.workItems.single().remainingMinutes)
    }

    @Test
    fun confirmedProjectMinutesDoNotConsumeOrdinaryStudyGoal() {
        val project = ProjectEntity("p", "math", "پروژه", due, estimatedMinutes = 45)
        val completion =
            StudyCompletionEntity("done", "math", today.format(), 450, 25, 25, workId = "project:p")
        val plan =
            UnifiedLearningPlanner.build(
                today,
                listOf(StudyGoalEntity("math", due, 30)),
                free,
                listOf(completion),
                classes,
                emptyList(),
                emptyList(),
                listOf(project),
            )
        assertEquals(30, plan.remainingMinutes["math"])
        assertEquals(20, plan.workItems.single().remainingMinutes)
    }

    @Test
    fun expiredWorkNeverSilentlyGetsAnExtendedDeadline() {
        val past = StudentAgendaEngine.addDays(today, -1).format()
        val plan =
            UnifiedLearningPlanner.build(
                today,
                emptyList(),
                free,
                emptyList(),
                classes,
                emptyList(),
                emptyList(),
                listOf(ProjectEntity("p", "math", "گذشته", past, estimatedMinutes = 45)),
            )
        assertTrue(plan.blocks.isEmpty())
        assertTrue(plan.workItems.single().overdue)
    }

    @Test
    fun closedProjectDoesNotHideItsStillOpenIndependentlyEstimatedTask() {
        val project =
            ProjectEntity("p", "math", "تمام", due, estimatedMinutes = 45, completedAt = 1000)
        val task =
            TaskEntity(
                id = 1,
                title = "باز",
                courseId = "math",
                dueDate = due,
                projectId = "p",
                estimatedMinutes = 20,
            )
        val plan =
            UnifiedLearningPlanner.build(
                today,
                emptyList(),
                free,
                emptyList(),
                classes,
                emptyList(),
                listOf(task),
                listOf(project),
            )
        assertEquals("task:1", plan.workItems.single().id)
        assertEquals(20, plan.blocks.sumOf { it.minutes })
    }
}
