package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "library_files", indices = [Index("courseId"), Index("projectId")])
data class AttachmentEntity(
    @PrimaryKey val id: String = "",
    val courseId: String = "",
    val projectId: String? = null,
    val title: String = "",
    val mimeType: String = "",
    val blobKey: String = "",
    val byteCount: Long = 0,
    val pageCount: Int = 1,
    val lastPage: Int = 0,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
    val isPurged: Boolean = false,
)

@Entity(tableName = "page_bookmarks", indices = [Index("attachmentId")])
data class PageBookmarkEntity(
    @PrimaryKey val id: String = "",
    val attachmentId: String = "",
    val page: Int = 0,
    val note: String = "",
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
)

@Entity(tableName = "study_projects", indices = [Index("courseId")])
data class ProjectEntity(
    @PrimaryKey val id: String = "",
    val courseId: String = "",
    val title: String = "",
    val dueDate: String = "",
    val description: String = "",
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
    val estimatedMinutes: Int = 0,
    val completedAt: Long? = null,
)

@Entity(tableName = "project_steps", indices = [Index("projectId")])
data class ProjectStepEntity(
    @PrimaryKey val id: String = "",
    val projectId: String = "",
    val title: String = "",
    val dueDate: String = "",
    val completedAt: Long? = null,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
)

@Entity(tableName = "practice_questions", indices = [Index("courseId")])
data class PracticeQuestionEntity(
    @PrimaryKey val id: String = "",
    val courseId: String = "",
    val prompt: String = "",
    val choicesJson: String = "[]",
    val answer: String = "",
    val kind: String = "SHORT",
    val updatedAt: Long = 0,
    val isArchived: Boolean = false,
)

@Entity(tableName = "practice_attempts", indices = [Index("courseId")])
data class PracticeAttemptEntity(
    @PrimaryKey val id: String = "",
    val courseId: String = "",
    val startedAt: Long = 0,
    val endsAt: Long = 0,
    val finishedAt: Long? = null,
    val questionsJson: String = "[]",
    val answersJson: String = "{}",
    val ratingsJson: String = "{}",
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
    val questionCount: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val ungradedCount: Int = 0,
)

/** List projection intentionally excludes the potentially large immutable question snapshot. */
data class PracticeAttemptSummary(
    val id: String,
    val courseId: String,
    val startedAt: Long,
    val endsAt: Long,
    val finishedAt: Long?,
    val isDeleted: Boolean,
    val questionCount: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val ungradedCount: Int,
)
