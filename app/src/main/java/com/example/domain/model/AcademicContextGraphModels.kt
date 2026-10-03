package com.example.domain.model

import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseSessionEntity
import com.example.data.local.entity.TaskEntity
import com.example.domain.model.ExamItem

data class AcademicCourseContext(
    val course: CourseEntity,
    val sessions: List<CourseSessionEntity>,
    val attendance: AttendanceEntity?,
    val tasks: List<TaskEntity>,
    val exams: List<ExamItem>
)

data class AcademicContextGraph(
    val courses: List<AcademicCourseContext>,
    val orphanTasks: List<TaskEntity>,
    val orphanExams: List<ExamItem>
)
