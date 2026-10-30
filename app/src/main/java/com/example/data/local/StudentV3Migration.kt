package com.example.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val STUDENT_V3_MIGRATION =
    object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `library_files` (`id` TEXT NOT NULL PRIMARY KEY, `courseId` TEXT NOT NULL, `projectId` TEXT, `title` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `blobKey` TEXT NOT NULL, `byteCount` INTEGER NOT NULL, `pageCount` INTEGER NOT NULL, `lastPage` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `isPurged` INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_library_files_courseId` ON `library_files` (`courseId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_library_files_projectId` ON `library_files` (`projectId`)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `page_bookmarks` (`id` TEXT NOT NULL PRIMARY KEY, `attachmentId` TEXT NOT NULL, `page` INTEGER NOT NULL, `note` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_page_bookmarks_attachmentId` ON `page_bookmarks` (`attachmentId`)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `study_projects` (`id` TEXT NOT NULL PRIMARY KEY, `courseId` TEXT NOT NULL, `title` TEXT NOT NULL, `dueDate` TEXT NOT NULL, `description` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `estimatedMinutes` INTEGER NOT NULL, `completedAt` INTEGER)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_study_projects_courseId` ON `study_projects` (`courseId`)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `project_steps` (`id` TEXT NOT NULL PRIMARY KEY, `projectId` TEXT NOT NULL, `title` TEXT NOT NULL, `dueDate` TEXT NOT NULL, `completedAt` INTEGER, `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_project_steps_projectId` ON `project_steps` (`projectId`)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `practice_questions` (`id` TEXT NOT NULL PRIMARY KEY, `courseId` TEXT NOT NULL, `prompt` TEXT NOT NULL, `choicesJson` TEXT NOT NULL, `answer` TEXT NOT NULL, `kind` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_practice_questions_courseId` ON `practice_questions` (`courseId`)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `practice_attempts` (`id` TEXT NOT NULL PRIMARY KEY, `courseId` TEXT NOT NULL, `startedAt` INTEGER NOT NULL, `endsAt` INTEGER NOT NULL, `finishedAt` INTEGER, `questionsJson` TEXT NOT NULL, `answersJson` TEXT NOT NULL, `ratingsJson` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `questionCount` INTEGER NOT NULL, `correctCount` INTEGER NOT NULL, `incorrectCount` INTEGER NOT NULL, `ungradedCount` INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_practice_attempts_courseId` ON `practice_attempts` (`courseId`)"
            )
            db.execSQL("ALTER TABLE tasks ADD COLUMN estimatedMinutes INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE study_completions ADD COLUMN workId TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE tasks ADD COLUMN projectId TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE tasks ADD COLUMN completedAt INTEGER DEFAULT NULL")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_projectId ON tasks(projectId)")
        }
    }
