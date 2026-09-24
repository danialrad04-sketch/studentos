package com.example.domain.usecase

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.dao.StudentDao
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.SemesterEntity
import com.example.data.local.entity.StudentProfileEntity
import java.util.UUID

data class NewSemesterRequest(
    val title: String,
    val academicYear: Int,
    val termNumber: Int,
    val startDate: String = "",
    val endDate: String = "",
    val newCourses: List<CourseEntity> = emptyList(),
    val finalRecordedGpa: Double? = null
)

data class SemesterTransitionResult(
    val previousSemester: SemesterEntity?,
    val createdSemester: SemesterEntity,
    val archivedCoursesCount: Int,
    val newCoursesCount: Int
)

/**
 * Transaction-safe UseCase orchestrating the lifecycle transition between semesters:
 * 1. Read current active semester
 * 2. Calculate summary (GPA, total units, courses)
 * 3. Archive current semester and its courses
 * 4. Create new semester and set as current
 * 5. Link new courses and initialize attendance/grades/exams
 * 6. Update student profile
 * All steps run inside a single atomic database transaction.
 */
class SemesterTransitionUseCase(
    private val database: AppDatabase,
    private val dao: StudentDao = database.studentDao()
) {
    suspend fun startNewSemester(request: NewSemesterRequest): SemesterTransitionResult {
        return database.withTransaction {
            val current = dao.getCurrentSemesterSync()
            var archivedCourseCount = 0

            // 1 & 2. Summarize & Archive current semester
            if (current != null) {
                val currentCourses = dao.getCoursesBySemesterSync(current.id)
                archivedCourseCount = currentCourses.size
                val currentUnits = currentCourses.sumOf { it.units }

                var calculatedGpa = request.finalRecordedGpa
                if (calculatedGpa == null && currentCourses.isNotEmpty()) {
                    var totalPoints = 0.0
                    var gradedUnits = 0
                    for (c in currentCourses) {
                        val g = dao.getGradeByCourseId(c.id)
                        if (g != null && g.finalGrade > 0) {
                            totalPoints += g.finalGrade * c.units
                            gradedUnits += c.units
                        }
                    }
                    if (gradedUnits > 0) {
                        calculatedGpa = totalPoints / gradedUnits
                    }
                }

                val archivedSem = current.copy(
                    isCurrent = false,
                    isArchived = true,
                    status = "ARCHIVED",
                    totalUnits = currentUnits,
                    gpa = calculatedGpa ?: current.gpa,
                    updatedAt = System.currentTimeMillis()
                )
                dao.updateSemester(archivedSem)

                for (c in currentCourses) {
                    dao.updateCourse(c.copy(isArchived = true))
                }
            }

            // 3. Clear all current flags
            dao.clearCurrentSemesterFlag()

            // 4. Create new semester
            val newSemId = "sem_${request.academicYear}_${request.termNumber}_${UUID.randomUUID().toString().take(6)}"
            val newSemester = SemesterEntity(
                id = newSemId,
                title = request.title,
                year = request.academicYear,
                academicYear = request.academicYear,
                semesterNumber = request.termNumber,
                termNumber = request.termNumber,
                startDate = request.startDate,
                endDate = request.endDate,
                status = "ACTIVE",
                isCurrent = true,
                isArchived = false,
                totalUnits = request.newCourses.sumOf { it.units },
                gpa = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            dao.insertSemester(newSemester)

            // 5. Add new courses linked to new semester
            val preparedCourses = request.newCourses.map { c ->
                val courseId = if (c.id.isNotBlank()) c.id else "c_${UUID.randomUUID().toString().take(8)}"
                c.copy(
                    id = courseId,
                    semesterId = newSemId,
                    isArchived = false
                )
            }
            dao.insertCourses(preparedCourses)

            // 6. Generate initial attendance, grade, and exam entries for new courses
            for (c in preparedCourses) {
                dao.insertAttendance(
                    AttendanceEntity(
                        courseId = c.id,
                        courseName = c.name,
                        absentCount = 0,
                        maxAllowed = if (c.name.contains("آزمایشگاه") || c.name.contains("کارگاه")) 2 else 3
                    )
                )

                dao.insertGrade(
                    GradeEntity(
                        courseId = c.id,
                        courseName = c.name,
                        units = c.units,
                        midtermGrade = 0.0,
                        finalGrade = 0.0
                    )
                )

                if (c.examDate.isNotBlank() || c.examTime.isNotBlank()) {
                    dao.insertExam(
                        ExamEntity(
                            id = "exam_${c.id}",
                            courseId = c.id,
                            courseName = c.name,
                            date = c.examDate,
                            time = c.examTime,
                            location = c.examLocation
                        )
                    )
                }
            }

            // 7. Update StudentProfile
            val profile = dao.getProfileSync() ?: StudentProfileEntity()
            val totalNewUnits = preparedCourses.sumOf { it.units }
            val updatedProfile = profile.copy(
                currentSemester = request.termNumber,
                term = request.title,
                activeUnits = totalNewUnits,
                faculty = "دانشکده ${profile.major} · $totalNewUnits واحد فعال",
                updatedAt = System.currentTimeMillis()
            )
            dao.insertProfile(updatedProfile)

            SemesterTransitionResult(
                previousSemester = current,
                createdSemester = newSemester,
                archivedCoursesCount = archivedCourseCount,
                newCoursesCount = preparedCourses.size
            )
        }
    }
}
