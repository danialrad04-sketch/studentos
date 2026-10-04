package com.example.domain.engine

import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.StudentProfileEntity
import com.example.data.local.entity.TaskEntity
import com.example.domain.model.CopilotPayload
import org.junit.Assert.assertTrue
import org.junit.Test

class AcademicCopilotPriorityTest {

    @Test
    fun answersPriorityQueryDeterministicallyFromStudentData() {
        val message = AcademicCopilotEngine.processUserQuery(
            query = "مهم‌ترین کارم چیه؟",
            profile = StudentProfileEntity(
                name = "دانشجو",
                major = "مهندسی",
                currentSemester = 4
            ),
            courses = listOf(
                CourseEntity(
                    id = "c1",
                    name = "ریاضی مهندسی",
                    units = 3
                )
            ),
            attendanceList = emptyList<AttendanceEntity>(),
            grades = emptyList<GradeEntity>(),
            tasks = listOf(
                TaskEntity(
                    id = 10L,
                    title = "تحویل پروژه",
                    courseName = "ریاضی مهندسی",
                    dueDate = "1405/06/20",
                    courseId = "c1",
                    isCompleted = false
                )
            ),
            exams = emptyList(),
            coursesWithSessions = emptyList(),
            curriculumCourses = emptyList()
        )

        assertTrue(message.text.contains("اولویت اصلی الان"))
        assertTrue(message.text.contains("تحویل پروژه"))
        assertTrue(message.proposedAction?.payload is CopilotPayload.NavigateToTab)
    }
}
