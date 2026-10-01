package com.example.domain.engine

import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.relation.CourseWithSessions
import com.example.domain.model.AcademicContextGraph
import com.example.domain.model.AcademicCourseContext
import com.example.domain.model.ExamItem
import java.util.Locale

object AcademicContextGraphBuilder {

    fun build(
        courses: List<CourseEntity>,
        coursesWithSessions: List<CourseWithSessions>,
        attendance: List<AttendanceEntity>,
        tasks: List<TaskEntity>,
        exams: List<ExamItem>
    ): AcademicContextGraph {
        val activeCourses = courses
            .filter { !it.isArchived }
            .distinctBy { it.id }

        val sessionsByCourseId = coursesWithSessions
            .groupBy { it.course.id }
            .mapValues { (_, values) -> values.flatMap { it.sessions } }

        val attendanceByCourseId = attendance.associateBy { it.courseId }

        val tasksByCourseId = tasks
            .filter { it.courseId.isNotBlank() }
            .groupBy { it.courseId }

        val coursesByName = activeCourses.associateBy { normalize(it.name) }
        val examsByCourseId = exams.groupBy { exam ->
            coursesByName[normalize(exam.courseName)]?.id ?: ""
        }

        val graphCourses = activeCourses.map { course ->
            AcademicCourseContext(
                course = course,
                sessions = sessionsByCourseId[course.id].orEmpty(),
                attendance = attendanceByCourseId[course.id],
                tasks = tasksByCourseId[course.id]
                    ?: tasks.filter { normalize(it.courseName) == normalize(course.name) },
                exams = examsByCourseId[course.id].orEmpty()
            )
        }

        val knownCourseIds = activeCourses.map { it.id }.toSet()
        val knownCourseNames = activeCourses.map { normalize(it.name) }.toSet()

        val orphanTasks = tasks.filter { task ->
            task.courseId.isBlank() && normalize(task.courseName) !in knownCourseNames ||
                task.courseId.isNotBlank() && task.courseId !in knownCourseIds
        }

        val orphanExams = exams.filter {
            normalize(it.courseName) !in knownCourseNames
        }

        return AcademicContextGraph(
            courses = graphCourses,
            orphanTasks = orphanTasks,
            orphanExams = orphanExams
        )
    }

    private fun normalize(value: String): String = value.trim().lowercase(Locale.ROOT)
}
