package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseSessionEntity
import com.example.data.local.entity.SemesterEntity
import com.example.data.local.entity.StudentCourseAttemptEntity
import com.example.data.local.relation.CourseWithSessions
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    // ==========================================
    // Semesters
    // ==========================================
    @Query("SELECT * FROM semesters ORDER BY academicYear DESC, termNumber DESC")
    fun getAllSemesters(): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM semesters ORDER BY academicYear DESC, termNumber DESC")
    suspend fun getAllSemestersSync(): List<SemesterEntity>

    @Query("SELECT * FROM semesters WHERE isArchived = 1 ORDER BY academicYear DESC, termNumber DESC")
    fun getArchivedSemesters(): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM semesters WHERE isCurrent = 1 LIMIT 1")
    fun getCurrentSemester(): Flow<SemesterEntity?>

    @Query("SELECT * FROM semesters WHERE isCurrent = 1 LIMIT 1")
    suspend fun getCurrentSemesterSync(): SemesterEntity?

    @Query("SELECT * FROM semesters WHERE id = :semesterId LIMIT 1")
    suspend fun getSemesterById(semesterId: String): SemesterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemester(semester: SemesterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemesters(semesters: List<SemesterEntity>)

    @Update
    suspend fun updateSemester(semester: SemesterEntity)

    @Query("UPDATE semesters SET isCurrent = 0")
    suspend fun clearCurrentSemesterFlag()

    @Query("DELETE FROM semesters WHERE id = :semesterId")
    suspend fun deleteSemester(semesterId: String)

    // ==========================================
    // Courses (Logical)
    // ==========================================
    @Query("""
        SELECT * FROM courses 
        WHERE (
            semesterId IN (SELECT id FROM semesters WHERE isCurrent = 1) 
            OR semesterId = 'current'
            OR (SELECT COUNT(*) FROM semesters WHERE isCurrent = 1) = 0
            OR semesterId IN (SELECT 'sem_' || currentSemester FROM student_profile LIMIT 1)
        ) 
          AND isArchived = 0 
        ORDER BY name ASC
    """)
    fun getCurrentSemesterCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses ORDER BY name ASC")
    fun getAllCoursesIncludingArchived(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses ORDER BY name ASC")
    suspend fun getAllCoursesIncludingArchivedSync(): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY name ASC")
    fun getCoursesBySemester(semesterId: String): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY name ASC")
    suspend fun getCoursesBySemesterSync(semesterId: String): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
    suspend fun getCourseById(courseId: String): CourseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>)

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Query("DELETE FROM courses WHERE id = :courseId")
    suspend fun deleteCourseById(courseId: String)

    @Query("DELETE FROM courses")
    suspend fun clearCourses()

    // ==========================================
    // Course Sessions (1-to-N Weekly Slots)
    // ==========================================
    @Transaction
    @Query("""
        SELECT * FROM courses 
        WHERE (
            semesterId IN (SELECT id FROM semesters WHERE isCurrent = 1) 
            OR semesterId = 'current'
            OR (SELECT COUNT(*) FROM semesters WHERE isCurrent = 1) = 0
            OR semesterId IN (SELECT 'sem_' || currentSemester FROM student_profile LIMIT 1)
        ) 
          AND isArchived = 0 
        ORDER BY name ASC
    """)
    fun getCurrentSemesterCoursesWithSessions(): Flow<List<CourseWithSessions>>

    @Transaction
    @Query("SELECT * FROM courses WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllCoursesWithSessions(): Flow<List<CourseWithSessions>>

    @Transaction
    @Query("SELECT * FROM courses ORDER BY name ASC")
    fun getAllCoursesWithSessionsIncludingArchived(): Flow<List<CourseWithSessions>>

    @Transaction
    @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
    suspend fun getCourseWithSessionsById(courseId: String): CourseWithSessions?

    @Query("SELECT * FROM course_sessions WHERE courseId = :courseId ORDER BY day ASC, start ASC")
    fun getSessionsForCourse(courseId: String): Flow<List<CourseSessionEntity>>

    @Query("SELECT * FROM course_sessions WHERE courseId = :courseId ORDER BY day ASC, start ASC")
    suspend fun getSessionsForCourseSync(courseId: String): List<CourseSessionEntity>

    @Query("SELECT * FROM course_sessions ORDER BY day ASC, start ASC")
    fun getAllSessions(): Flow<List<CourseSessionEntity>>

    @Query("SELECT * FROM course_sessions ORDER BY day ASC, start ASC")
    suspend fun getAllSessionsSync(): List<CourseSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseSession(session: CourseSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseSessions(sessions: List<CourseSessionEntity>)

    suspend fun insertSessions(sessions: List<CourseSessionEntity>) = insertCourseSessions(sessions)

    @Update
    suspend fun updateCourseSession(session: CourseSessionEntity)

    @Query("DELETE FROM course_sessions WHERE id = :sessionId")
    suspend fun deleteCourseSessionById(sessionId: String)

    @Query("DELETE FROM course_sessions WHERE courseId = :courseId")
    suspend fun deleteSessionsByCourseId(courseId: String)

    @Query("DELETE FROM course_sessions")
    suspend fun clearCourseSessions()

    // ==========================================
    // Student Course Attempts
    // ==========================================
    @Query("SELECT * FROM student_course_attempts WHERE profileId = :profileId ORDER BY semesterIndex ASC, attemptNumber ASC")
    fun getStudentAttempts(profileId: Int = 1): Flow<List<StudentCourseAttemptEntity>>

    @Query("SELECT * FROM student_course_attempts WHERE profileId = :profileId ORDER BY semesterIndex ASC, attemptNumber ASC")
    suspend fun getStudentAttemptsSync(profileId: Int = 1): List<StudentCourseAttemptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentAttempt(attempt: StudentCourseAttemptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllStudentAttempts(attempts: List<StudentCourseAttemptEntity>)

    @Query("DELETE FROM student_course_attempts WHERE id = :attemptId")
    suspend fun deleteStudentAttempt(attemptId: String)

    @Query("DELETE FROM student_course_attempts WHERE profileId = :profileId")
    suspend fun clearStudentAttempts(profileId: Int = 1)
}
