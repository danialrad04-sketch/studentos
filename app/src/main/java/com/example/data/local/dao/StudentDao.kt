package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/**
 * Composite DAO for Student OS.
 * Follows the Interface Segregation Principle (ISP) by decomposing entity access into focused DAOs
 * (CourseDao, TaskDao, AttendanceDao, ProfileDao, ExamDao, GradeDao, NoteDao) while serving as
 * a single unified contract for Room Database binding and legacy callers.
 */
@Dao
interface StudentDao : CourseDao, TaskDao, AttendanceDao, ProfileDao, ExamDao, GradeDao, NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncMetadata(metadata: com.example.data.local.entity.SyncMetadataEntity)

    @Query("SELECT updatedAt FROM sync_metadata WHERE dataType = :dataType LIMIT 1")
    suspend fun getSyncUpdatedAt(dataType: String): Long?

    @Query("SELECT * FROM sync_metadata")
    suspend fun getAllSyncMetadata(): List<com.example.data.local.entity.SyncMetadataEntity>

    @Query("DELETE FROM sync_metadata")
    suspend fun clearSyncMetadata()

    @Query("DELETE FROM student_profile")
    suspend fun clearStudentProfile()

    @Query("DELETE FROM semesters")
    suspend fun clearSemesters()

    @Query("DELETE FROM course_sessions")
    suspend fun clearAllCourseSessions()

    @Query("DELETE FROM notes")
    suspend fun clearAllNotes()

    @Transaction
    suspend fun clearAllUserData() {
        clearAttendance()
        clearGrades()
        clearTasks()
        clearExams()
        clearAllCourseSessions()
        clearAllNotes()
        clearCourses()
        clearStudentAttempts(1)
        clearSemesters()
        clearStudentProfile()
        clearSyncMetadata()
    }

    // ==========================================
    // Orphan Cleanup Across Multi-Entity Relations
    // ==========================================
    @Query("DELETE FROM attendance WHERE courseId != '' AND courseId NOT IN (SELECT id FROM courses)")
    suspend fun deleteOrphanAttendance()

    @Query("DELETE FROM grades WHERE courseId != '' AND courseId NOT IN (SELECT id FROM courses)")
    suspend fun deleteOrphanGrades()

    @Query("DELETE FROM tasks WHERE courseId IS NOT NULL AND courseId != '' AND courseId NOT IN (SELECT id FROM courses)")
    suspend fun deleteOrphanTasks()

    @Query("DELETE FROM exams WHERE courseId NOT IN (SELECT id FROM courses)")
    suspend fun deleteOrphanExams()

    @Query("DELETE FROM course_sessions WHERE courseId NOT IN (SELECT id FROM courses)")
    suspend fun deleteOrphanCourseSessions()

    @Transaction
    suspend fun cleanupOrphans() {
        deleteOrphanAttendance()
        deleteOrphanGrades()
        deleteOrphanTasks()
        deleteOrphanExams()
        deleteOrphanCourseSessions()
    }
}
