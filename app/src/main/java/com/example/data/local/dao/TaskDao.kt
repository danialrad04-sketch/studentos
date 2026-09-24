package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, id DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, id DESC")
    suspend fun getAllTasksSync(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE courseId = :courseId ORDER BY isCompleted ASC, id DESC")
    fun getTasksByCourseId(courseId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE semesterId = :semesterId ORDER BY isCompleted ASC, id DESC")
    fun getTasksBySemester(semesterId: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE courseId = :courseId")
    suspend fun deleteTasksByCourseId(courseId: String)

    @Query("DELETE FROM tasks WHERE courseId = :courseId OR courseName = :courseName")
    suspend fun deleteTasksForCourse(courseId: String, courseName: String = "")

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()
}
