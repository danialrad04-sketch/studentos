package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningDao {
    @Query("SELECT * FROM study_goals") fun observeStudyGoals(): Flow<List<StudyGoalEntity>>
    @Query("SELECT * FROM study_availability ORDER BY weekday") fun observeStudyAvailability(): Flow<List<StudyAvailabilityEntity>>
    @Query("SELECT * FROM study_completions") fun observeStudyCompletions(): Flow<List<StudyCompletionEntity>>
    @Query("SELECT * FROM review_cards ORDER BY dueDate, id") fun observeReviewCards(): Flow<List<ReviewCardEntity>>
    @Query("SELECT * FROM study_goals") suspend fun studyGoalsSnapshot(): List<StudyGoalEntity>
    @Query("SELECT * FROM study_availability") suspend fun studyAvailabilitySnapshot(): List<StudyAvailabilityEntity>
    @Query("SELECT * FROM study_completions") suspend fun studyCompletionsSnapshot(): List<StudyCompletionEntity>
    @Query("SELECT * FROM review_cards") suspend fun reviewCardsSnapshot(): List<ReviewCardEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertStudyGoal(row: StudyGoalEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertStudyAvailability(row: StudyAvailabilityEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertStudyCompletion(row: StudyCompletionEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertReviewCard(row: ReviewCardEntity)
    @Query("DELETE FROM study_goals") suspend fun clearStudyGoals()
    @Query("DELETE FROM study_availability") suspend fun clearStudyAvailability()
    @Query("DELETE FROM study_completions") suspend fun clearStudyCompletions()
    @Query("DELETE FROM review_cards") suspend fun clearReviewCards()
    @Query("DELETE FROM study_goals WHERE courseId NOT IN (SELECT id FROM courses)") suspend fun cleanupStudyGoals()
    @Query("DELETE FROM study_completions WHERE courseId NOT IN (SELECT id FROM courses)") suspend fun cleanupStudyCompletions()
    @Query("DELETE FROM review_cards WHERE courseId NOT IN (SELECT id FROM courses)") suspend fun cleanupReviewCards()
}
