package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.CurriculumCourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurriculumDao {
    @Query("SELECT * FROM curriculum_courses ORDER BY recommendedSemester ASC, name ASC")
    fun getAllCurriculumCourses(): Flow<List<CurriculumCourseEntity>>

    @Query("SELECT * FROM curriculum_courses")
    suspend fun getAllCurriculumCoursesSync(): List<CurriculumCourseEntity>

    @Query("SELECT * FROM curriculum_courses WHERE recommendedSemester = :semester ORDER BY name ASC")
    fun getCoursesBySemester(semester: Int): Flow<List<CurriculumCourseEntity>>

    @Query("SELECT * FROM curriculum_courses WHERE majorId = :majorId ORDER BY recommendedSemester ASC")
    fun getCoursesByMajor(majorId: String): Flow<List<CurriculumCourseEntity>>

    @Query("SELECT * FROM curriculum_courses WHERE name LIKE '%' || :query || '%' LIMIT 10")
    suspend fun searchCourses(query: String): List<CurriculumCourseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurriculumCourses(courses: List<CurriculumCourseEntity>)

    @Query("SELECT COUNT(*) FROM curriculum_courses")
    suspend fun getCurriculumCount(): Int

    // Reference tables access
    @Query("SELECT * FROM reference_universities")
    fun getAllUniversities(): Flow<List<com.example.data.local.entity.UniversityEntity>>

    @Query("SELECT * FROM reference_universities")
    suspend fun getAllUniversitiesSync(): List<com.example.data.local.entity.UniversityEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUniversities(universities: List<com.example.data.local.entity.UniversityEntity>)

    @Query("SELECT * FROM reference_majors")
    fun getAllMajors(): Flow<List<com.example.data.local.entity.MajorEntity>>

    @Query("SELECT * FROM reference_majors")
    suspend fun getAllMajorsSync(): List<com.example.data.local.entity.MajorEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMajors(majors: List<com.example.data.local.entity.MajorEntity>)

    @Query("SELECT * FROM curriculum_versions")
    fun getAllCurriculumVersions(): Flow<List<com.example.data.local.entity.CurriculumVersionEntity>>

    @Query("SELECT * FROM curriculum_versions")
    suspend fun getAllCurriculumVersionsSync(): List<com.example.data.local.entity.CurriculumVersionEntity>

    @Query("SELECT * FROM curriculum_versions WHERE majorId = :majorId")
    suspend fun getCurriculumVersionsByMajor(majorId: String): List<com.example.data.local.entity.CurriculumVersionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCurriculumVersions(versions: List<com.example.data.local.entity.CurriculumVersionEntity>)

    @Query("SELECT * FROM course_aliases")
    suspend fun getAllAliasesSync(): List<com.example.data.local.entity.CourseAliasEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAliases(aliases: List<com.example.data.local.entity.CourseAliasEntity>)
}
