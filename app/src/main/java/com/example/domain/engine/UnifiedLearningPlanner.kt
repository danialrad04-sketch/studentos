package com.example.domain.engine

import com.example.data.local.entity.*
import com.example.data.local.relation.CourseWithSessions
import com.example.domain.util.JalaliCalendarUtil
import com.example.domain.util.JalaliDate

data class PlannedCourseWork(
    val id: String,
    val courseId: String,
    val title: String,
    val dueDate: String,
    val remainingMinutes: Int?,
    val shortfallMinutes: Int,
    val overdue: Boolean,
)

/** Shares one time budget with study goals. Missing effort is never guessed. */
object UnifiedLearningPlanner {
    fun build(
        today: JalaliDate,
        goals: List<StudyGoalEntity>,
        availability: List<StudyAvailabilityEntity>,
        completed: List<StudyCompletionEntity>,
        classes: List<CourseWithSessions>,
        examDates: List<String>,
        tasks: List<TaskEntity>,
        projects: List<ProjectEntity>,
        nowMinute: Int = 0,
        reservedBlocks: List<LearningBlock> = emptyList(),
    ): AdaptiveLearningPlan {
        val courseIds = classes.filterNot { it.isArchived }.map { it.course.id }.toSet()
        data class Work(
            val id: String,
            val courseId: String,
            val title: String,
            val due: String,
            val minutes: Int,
        )
        val openProjects =
            projects.filter { !it.isDeleted && it.completedAt == null && it.courseId in courseIds }
        val work =
            openProjects.map {
                Work("project:${it.id}", it.courseId, it.title, it.dueDate, it.estimatedMinutes)
            } +
                tasks
                    .filter { t ->
                        !t.isCompleted &&
                            t.courseId in courseIds &&
                            openProjects.none {
                                it.id == t.projectId &&
                                    it.estimatedMinutes > 0 &&
                                    JalaliCalendarUtil.parse(it.dueDate) != null
                            }
                    }
                    .map {
                        Work(
                            "task:${it.id}",
                            it.courseId,
                            it.title,
                            it.dueDate,
                            it.estimatedMinutes,
                        )
                    }
        val planned =
            work.filter { it.minutes in 5..6000 && JalaliCalendarUtil.parse(it.due) != null }
        val keys =
            planned.associate { w ->
                var key = "work:${w.id}"
                while (key in courseIds) key += "#"
                w.id to key
            }
        val synthetic = planned.map { w -> StudyGoalEntity(keys.getValue(w.id), w.due, w.minutes) }
        val confirmed =
            completed.mapNotNull { c ->
                if (c.workId == null) c else keys[c.workId]?.let { c.copy(courseId = it) }
            }
        val reserved =
            reservedBlocks.map { b ->
                if (b.workId == null) b
                else b.copy(courseId = keys[b.workId] ?: "reserved:${b.workId}")
            }
        val base =
            AdaptiveLearningEngine.build(
                today,
                goals + synthetic,
                availability,
                confirmed,
                classes,
                examDates,
                nowMinute,
                reserved,
            )
        val byKey = planned.associateBy { keys.getValue(it.id) }
        return base.copy(
            blocks =
                base.blocks.map { b ->
                    byKey[b.courseId]?.let {
                        b.copy(courseId = it.courseId, workId = it.id, workTitle = it.title)
                    } ?: b
                },
            remainingMinutes = base.remainingMinutes.filterKeys { it !in byKey },
            overdueGoals = base.overdueGoals.filter { it !in byKey }.toSet(),
            capacityShortfall = base.capacityShortfall.filterKeys { it !in byKey },
            workItems =
                work.map { w ->
                    val key = keys[w.id]
                    PlannedCourseWork(
                        w.id,
                        w.courseId,
                        w.title,
                        w.due,
                        key?.let { base.remainingMinutes[it] },
                        key?.let { base.capacityShortfall[it] } ?: 0,
                        key in base.overdueGoals,
                    )
                },
        )
    }
}
