package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.GradeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeDao {
    @Query("SELECT * FROM grades ORDER BY id ASC")
    fun getAllGrades(): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades ORDER BY id ASC")
    suspend fun getAllGradesSync(): List<GradeEntity>

    @Query("SELECT * FROM grades WHERE courseId = :courseId LIMIT 1")
    suspend fun getGradeByCourseId(courseId: String): GradeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: GradeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllGrades(grades: List<GradeEntity>)

    @Update
    suspend fun updateGrade(grade: GradeEntity)

    @Query("DELETE FROM grades WHERE courseId = :courseId")
    suspend fun deleteGradeByCourseId(courseId: String)

    @Query("DELETE FROM grades WHERE courseName = :courseName OR courseId = :courseId")
    suspend fun deleteGradeForCourse(courseName: String, courseId: String = "")

    @Query("DELETE FROM grades")
    suspend fun clearGrades()
}
