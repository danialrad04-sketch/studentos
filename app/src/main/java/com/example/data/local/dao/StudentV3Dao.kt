package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentV3Dao {
    @Query("SELECT * FROM library_files ORDER BY updatedAt DESC, id")
    fun observeAttachments(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM library_files") suspend fun attachmentsSnapshot(): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttachment(row: AttachmentEntity)

    @Query("DELETE FROM library_files") suspend fun clearAttachments()

    @Query("SELECT * FROM page_bookmarks ORDER BY updatedAt DESC, id")
    fun observePageBookmarks(): Flow<List<PageBookmarkEntity>>

    @Query("SELECT * FROM page_bookmarks")
    suspend fun pageBookmarksSnapshot(): List<PageBookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPageBookmark(row: PageBookmarkEntity)

    @Query("DELETE FROM page_bookmarks") suspend fun clearPageBookmarks()

    @Query("SELECT * FROM study_projects ORDER BY updatedAt DESC, id")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM study_projects") suspend fun projectsSnapshot(): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertProject(row: ProjectEntity)

    @Query("DELETE FROM study_projects") suspend fun clearProjects()

    @Query("SELECT * FROM project_steps ORDER BY updatedAt DESC, id")
    fun observeProjectSteps(): Flow<List<ProjectStepEntity>>

    @Query("SELECT * FROM project_steps")
    suspend fun projectStepsSnapshot(): List<ProjectStepEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProjectStep(row: ProjectStepEntity)

    @Query("DELETE FROM project_steps") suspend fun clearProjectSteps()

    @Query("SELECT * FROM practice_questions ORDER BY updatedAt DESC, id")
    fun observePracticeQuestions(): Flow<List<PracticeQuestionEntity>>

    @Query("SELECT * FROM practice_questions")
    suspend fun practiceQuestionsSnapshot(): List<PracticeQuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPracticeQuestion(row: PracticeQuestionEntity)

    @Query("DELETE FROM practice_questions") suspend fun clearPracticeQuestions()

    @Query("SELECT * FROM practice_attempts ORDER BY updatedAt DESC, id")
    fun observePracticeAttempts(): Flow<List<PracticeAttemptEntity>>

    @Query("SELECT * FROM practice_attempts")
    suspend fun practiceAttemptsSnapshot(): List<PracticeAttemptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPracticeAttempt(row: PracticeAttemptEntity)

    @Query("DELETE FROM practice_attempts") suspend fun clearPracticeAttempts()

    @Query(
        "SELECT id,courseId,startedAt,endsAt,finishedAt,isDeleted,questionCount,correctCount,incorrectCount,ungradedCount FROM practice_attempts ORDER BY startedAt DESC,id"
    )
    fun observePracticeSummaries(): Flow<List<PracticeAttemptSummary>>

    @Query("SELECT * FROM practice_attempts WHERE id=:id LIMIT 1")
    fun observePracticeAttempt(id: String): Flow<PracticeAttemptEntity?>

    @Transaction
    suspend fun clearV3Data() {
        clearPageBookmarks()
        clearAttachments()
        clearProjectSteps()
        clearProjects()
        clearPracticeQuestions()
        clearPracticeAttempts()
    }

    @Query(
        "UPDATE library_files SET isDeleted=1, isPurged=1, updatedAt=MAX(updatedAt+1,:now) WHERE courseId NOT IN (SELECT id FROM courses) AND isPurged=0"
    )
    suspend fun cleanupV3Files(now: Long)

    @Query(
        "UPDATE study_projects SET isDeleted=1, updatedAt=MAX(updatedAt+1,:now) WHERE courseId NOT IN (SELECT id FROM courses) AND isDeleted=0"
    )
    suspend fun cleanupV3Projects(now: Long)

    @Query(
        "UPDATE practice_questions SET isArchived=1, updatedAt=MAX(updatedAt+1,:now) WHERE courseId NOT IN (SELECT id FROM courses) AND isArchived=0"
    )
    suspend fun cleanupV3Questions(now: Long)

    @Query(
        "UPDATE practice_attempts SET isDeleted=1, updatedAt=MAX(updatedAt+1,:now) WHERE courseId NOT IN (SELECT id FROM courses) AND isDeleted=0"
    )
    suspend fun cleanupV3Attempts(now: Long)
}
