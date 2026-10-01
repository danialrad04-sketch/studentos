package com.example.domain.engine

import com.example.data.local.entity.TaskEntity
import com.example.domain.model.AcademicPlanningCollision
import com.example.domain.model.AcademicPlanningCollisionSeverity
import com.example.domain.model.AcademicPlanningCollisionType
import com.example.domain.model.ExamItem

object AcademicPlanningCollisionEngine {

    fun detect(
        tasks: List<TaskEntity>,
        exams: List<ExamItem>
    ): List<AcademicPlanningCollision> {
        val openTasks = tasks
            .filter { !it.isCompleted && it.title.isNotBlank() && it.dueDate.isNotBlank() }
            .distinctBy { it.id }

        val result = mutableListOf<AcademicPlanningCollision>()

        openTasks
            .groupBy { normalizeDate(it.dueDate) }
            .filterKeys { it.isNotBlank() }
            .filterValues { it.size >= 2 }
            .forEach { (date, sameDayTasks) ->
                result += AcademicPlanningCollision(
                    id = "task-task:" + date,
                    type = AcademicPlanningCollisionType.TASK_TASK,
                    severity = if (sameDayTasks.size >= 3) {
                        AcademicPlanningCollisionSeverity.HIGH
                    } else {
                        AcademicPlanningCollisionSeverity.WARNING
                    },
                    date = date,
                    title = sameDayTasks.size.toString() + " کار در یک روز",
                    description = "برای این روز " + sameDayTasks.size + " کار باز ثبت شده است؛ بهتر است بخشی از آن‌ها را زودتر انجام بدهی."
                )
            }

        val examDatesByCourse = exams
            .filter { it.courseName.isNotBlank() && it.solarDate.isNotBlank() }
            .groupBy { normalizeCourse(it.courseName) }

        openTasks.forEach { task ->
            val exam = examDatesByCourse[normalizeCourse(task.courseName)]
                ?.firstOrNull()
                ?: return@forEach

            val taskDate = normalizeDate(task.dueDate)
            val examDate = normalizeDate(exam.solarDate)

            if (taskDate.isNotBlank() && taskDate == examDate) {
                result += AcademicPlanningCollision(
                    id = "task-exam:" + task.id + ":" + exam.id,
                    type = AcademicPlanningCollisionType.TASK_EXAM,
                    severity = AcademicPlanningCollisionSeverity.HIGH,
                    date = taskDate,
                    title = "تکلیف و امتحان در یک روز",
                    description = "موعد «" + task.title.trim() + "» با امتحان " + exam.courseName.trim() + " در یک روز قرار دارد."
                )
            }
        }

        return result.sortedWith(
            compareByDescending<AcademicPlanningCollision> {
                if (it.severity == AcademicPlanningCollisionSeverity.HIGH) 2 else 1
            }.thenBy { it.date }.thenBy { it.title }
        )
    }

    private fun normalizeCourse(value: String): String = value.trim().lowercase()

    private fun normalizeDate(value: String): String {
        val raw = value.trim()
            .replace('-', '/')
            .replace('۰','0').replace('۱','1').replace('۲','2').replace('۳','3')
            .replace('۴','4').replace('۵','5').replace('۶','6').replace('۷','7')
            .replace('۸','8').replace('۹','9')
        val parts = raw.split('/')
        if (parts.size != 3) return ""
        return parts[0].padStart(4, '0') +
            parts[1].padStart(2, '0') +
            parts[2].padStart(2, '0')
    }
}
