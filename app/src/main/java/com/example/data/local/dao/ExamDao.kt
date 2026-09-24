package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ExamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY date ASC, time ASC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams ORDER BY date ASC, time ASC")
    suspend fun getAllExamsSync(): List<ExamEntity>

    @Query("SELECT * FROM exams WHERE id = :id LIMIT 1")
    suspend fun getExamById(id: String): ExamEntity?

    @Query("SELECT * FROM exams WHERE courseId = :courseId LIMIT 1")
    suspend fun getExamByCourseId(courseId: String): ExamEntity?

    @Query("""
        SELECT e.* FROM exams e
        INNER JOIN courses c ON e.courseId = c.id
        WHERE c.semesterId = :semesterId
        ORDER BY e.date ASC, e.time ASC
    """)
    fun getExamsForSemester(semesterId: String): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExams(exams: List<ExamEntity>)

    @Query("DELETE FROM exams WHERE courseId = :courseId")
    suspend fun deleteExamByCourseId(courseId: String)

    @Query("DELETE FROM exams")
    suspend fun clearExams()
}
