package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ProfessorEntity
import com.example.data.local.entity.StudentProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM student_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<StudentProfileEntity?>

    @Query("SELECT * FROM student_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileSync(): StudentProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: StudentProfileEntity)

    @Query("SELECT * FROM professors ORDER BY name ASC")
    fun getAllProfessors(): Flow<List<ProfessorEntity>>

    @Query("SELECT * FROM professors ORDER BY name ASC")
    suspend fun getAllProfessorsSync(): List<ProfessorEntity>

    @Query("SELECT * FROM professors WHERE id = :id LIMIT 1")
    suspend fun getProfessorById(id: String): ProfessorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfessor(professor: ProfessorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfessors(professors: List<ProfessorEntity>)
}
