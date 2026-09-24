package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance")
    suspend fun getAllAttendanceSync(): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE courseId = :courseId LIMIT 1")
    suspend fun getAttendanceByCourseId(courseId: String): AttendanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Update
    suspend fun updateAttendance(attendance: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAttendance(items: List<AttendanceEntity>)

    @Query("DELETE FROM attendance WHERE courseId = :courseId")
    suspend fun deleteAttendanceByCourseId(courseId: String)

    @Query("DELETE FROM attendance WHERE courseName = :courseName OR courseId = :courseId")
    suspend fun deleteAttendanceForCourse(courseName: String, courseId: String = "")

    @Query("DELETE FROM attendance")
    suspend fun clearAttendance()
}
