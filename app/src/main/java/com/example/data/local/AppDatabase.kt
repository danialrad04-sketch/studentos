package com.example.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.CurriculumDao
import com.example.data.local.dao.StudentDao
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseAliasEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseSessionEntity
import com.example.data.local.entity.CurriculumCourseEntity
import com.example.data.local.entity.CurriculumVersionEntity
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.MajorEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ProfessorEntity
import com.example.data.local.entity.SemesterEntity
import com.example.data.local.entity.StudentCourseAttemptEntity
import com.example.data.local.entity.StudentProfileEntity
import com.example.data.local.entity.SyncMetadataEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.UniversityEntity
import com.example.data.seed.CurriculumSeedData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SemesterEntity::class,
        CourseEntity::class,
        CourseSessionEntity::class,
        AttendanceEntity::class,
        GradeEntity::class,
        TaskEntity::class,
        StudentProfileEntity::class,
        CurriculumCourseEntity::class,
        UniversityEntity::class,
        MajorEntity::class,
        CurriculumVersionEntity::class,
        CourseAliasEntity::class,
        StudentCourseAttemptEntity::class,
        ProfessorEntity::class,
        ExamEntity::class,
        NoteEntity::class,
        SyncMetadataEntity::class
    ],
    version = 9,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun curriculumDao(): CurriculumDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `curriculum_courses` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `majorId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `nameEn` TEXT NOT NULL,
                        `courseCode` TEXT NOT NULL,
                        `units` INTEGER NOT NULL,
                        `courseType` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `prerequisites` TEXT NOT NULL,
                        `coRequisites` TEXT NOT NULL,
                        `recommendedSemester` INTEGER NOT NULL,
                        `syllabus` TEXT NOT NULL,
                        `description` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reference_universities` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `displayNameFa` TEXT NOT NULL,
                        `shortName` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reference_majors` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `universityId` TEXT NOT NULL,
                        `facultyId` TEXT NOT NULL,
                        `facultyDisplayNameFa` TEXT NOT NULL,
                        `majorDisplayNameFa` TEXT NOT NULL,
                        FOREIGN KEY(`universityId`) REFERENCES `reference_universities`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `curriculum_versions` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `majorId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `entryYearMin` INTEGER NOT NULL,
                        `entryYearMax` INTEGER NOT NULL,
                        `totalCreditsRequired` INTEGER NOT NULL,
                        FOREIGN KEY(`majorId`) REFERENCES `reference_majors`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `course_aliases` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `courseId` TEXT NOT NULL,
                        `aliasRaw` TEXT NOT NULL,
                        `aliasNormalized` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `student_course_attempts` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `profileId` INTEGER NOT NULL,
                        `courseId` TEXT NOT NULL,
                        `semesterIndex` INTEGER NOT NULL,
                        `attemptNumber` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `grade` REAL,
                        `isPassed` INTEGER NOT NULL,
                        FOREIGN KEY(`profileId`) REFERENCES `student_profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                // Add missing columns to student_profile safely
                val cursor = db.query("PRAGMA table_info(`student_profile`)")
                val existingCols = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    val nameIdx = cursor.getColumnIndex("name")
                    if (nameIdx != -1) existingCols.add(cursor.getString(nameIdx))
                }
                cursor.close()

                if (!existingCols.contains("universityId")) {
                    db.execSQL("ALTER TABLE `student_profile` ADD COLUMN `universityId` TEXT DEFAULT NULL")
                }
                if (!existingCols.contains("facultyId")) {
                    db.execSQL("ALTER TABLE `student_profile` ADD COLUMN `facultyId` TEXT DEFAULT NULL")
                }
                if (!existingCols.contains("majorId")) {
                    db.execSQL("ALTER TABLE `student_profile` ADD COLUMN `majorId` TEXT DEFAULT NULL")
                }
                if (!existingCols.contains("declaredPassedCredits")) {
                    db.execSQL("ALTER TABLE `student_profile` ADD COLUMN `declaredPassedCredits` INTEGER DEFAULT NULL")
                }
                if (!existingCols.contains("declaredGpa")) {
                    db.execSQL("ALTER TABLE `student_profile` ADD COLUMN `declaredGpa` REAL DEFAULT NULL")
                }
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create semesters table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `semesters` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `year` INTEGER NOT NULL DEFAULT 1403,
                        `semesterNumber` INTEGER NOT NULL DEFAULT 1,
                        `startDate` TEXT NOT NULL DEFAULT '',
                        `endDate` TEXT NOT NULL DEFAULT '',
                        `isCurrent` INTEGER NOT NULL DEFAULT 0,
                        `isArchived` INTEGER NOT NULL DEFAULT 0,
                        `totalUnits` INTEGER NOT NULL DEFAULT 0,
                        `gpa` REAL DEFAULT NULL
                    )
                    """.trimIndent()
                )

                // 2. Add semesterId and metadata to courses
                val cursorCourses = db.query("PRAGMA table_info(`courses`)")
                val courseCols = mutableSetOf<String>()
                while (cursorCourses.moveToNext()) {
                    val nameIdx = cursorCourses.getColumnIndex("name")
                    if (nameIdx != -1) courseCols.add(cursorCourses.getString(nameIdx))
                }
                cursorCourses.close()

                if (!courseCols.contains("semesterId")) {
                    db.execSQL("ALTER TABLE `courses` ADD COLUMN `semesterId` TEXT NOT NULL DEFAULT 'current'")
                }
                if (!courseCols.contains("courseCode")) {
                    db.execSQL("ALTER TABLE `courses` ADD COLUMN `courseCode` TEXT NOT NULL DEFAULT ''")
                }
                if (!courseCols.contains("professor")) {
                    db.execSQL("ALTER TABLE `courses` ADD COLUMN `professor` TEXT NOT NULL DEFAULT ''")
                }
                if (!courseCols.contains("examDate")) {
                    db.execSQL("ALTER TABLE `courses` ADD COLUMN `examDate` TEXT NOT NULL DEFAULT ''")
                }
                if (!courseCols.contains("examTime")) {
                    db.execSQL("ALTER TABLE `courses` ADD COLUMN `examTime` TEXT NOT NULL DEFAULT ''")
                }
                if (!courseCols.contains("examLocation")) {
                    db.execSQL("ALTER TABLE `courses` ADD COLUMN `examLocation` TEXT NOT NULL DEFAULT ''")
                }
                if (!courseCols.contains("notes")) {
                    db.execSQL("ALTER TABLE `courses` ADD COLUMN `notes` TEXT NOT NULL DEFAULT ''")
                }
                if (!courseCols.contains("isArchived")) {
                    db.execSQL("ALTER TABLE `courses` ADD COLUMN `isArchived` INTEGER NOT NULL DEFAULT 0")
                }
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_courses_semesterId` ON `courses` (`semesterId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_courses_courseCode` ON `courses` (`courseCode`)")

                // 3. Add courseId to attendance, grades, tasks
                val cursorAtt = db.query("PRAGMA table_info(`attendance`)")
                val attCols = mutableSetOf<String>()
                while (cursorAtt.moveToNext()) {
                    val nameIdx = cursorAtt.getColumnIndex("name")
                    if (nameIdx != -1) attCols.add(cursorAtt.getString(nameIdx))
                }
                cursorAtt.close()
                if (!attCols.contains("courseId")) {
                    db.execSQL("ALTER TABLE `attendance` ADD COLUMN `courseId` TEXT NOT NULL DEFAULT ''")
                }
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_courseId` ON `attendance` (`courseId`)")

                val cursorGrades = db.query("PRAGMA table_info(`grades`)")
                val gradeCols = mutableSetOf<String>()
                while (cursorGrades.moveToNext()) {
                    val nameIdx = cursorGrades.getColumnIndex("name")
                    if (nameIdx != -1) gradeCols.add(cursorGrades.getString(nameIdx))
                }
                cursorGrades.close()
                if (!gradeCols.contains("courseId")) {
                    db.execSQL("ALTER TABLE `grades` ADD COLUMN `courseId` TEXT NOT NULL DEFAULT ''")
                }
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_grades_courseId` ON `grades` (`courseId`)")

                val cursorTasks = db.query("PRAGMA table_info(`tasks`)")
                val taskCols = mutableSetOf<String>()
                while (cursorTasks.moveToNext()) {
                    val nameIdx = cursorTasks.getColumnIndex("name")
                    if (nameIdx != -1) taskCols.add(cursorTasks.getString(nameIdx))
                }
                cursorTasks.close()
                if (!taskCols.contains("courseId")) {
                    db.execSQL("ALTER TABLE `tasks` ADD COLUMN `courseId` TEXT NOT NULL DEFAULT ''")
                }
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_courseId` ON `tasks` (`courseId`)")

                // 4. Populate default current semester if none exists
                db.execSQL("""
                    INSERT OR IGNORE INTO `semesters` (`id`, `title`, `year`, `semesterNumber`, `isCurrent`, `isArchived`, `totalUnits`)
                    VALUES ('current', 'ترم جاری', 1403, 1, 1, 0, 0)
                """.trimIndent())
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Upgrade semesters table with academicYear, termNumber, status, createdAt, updatedAt
                val cursorSem = db.query("PRAGMA table_info(`semesters`)")
                val semCols = mutableSetOf<String>()
                while (cursorSem.moveToNext()) {
                    val nameIdx = cursorSem.getColumnIndex("name")
                    if (nameIdx != -1) semCols.add(cursorSem.getString(nameIdx))
                }
                cursorSem.close()

                if (!semCols.contains("academicYear")) {
                    db.execSQL("ALTER TABLE `semesters` ADD COLUMN `academicYear` INTEGER NOT NULL DEFAULT 1403")
                    db.execSQL("UPDATE `semesters` SET `academicYear` = `year`")
                }
                if (!semCols.contains("termNumber")) {
                    db.execSQL("ALTER TABLE `semesters` ADD COLUMN `termNumber` INTEGER NOT NULL DEFAULT 1")
                    db.execSQL("UPDATE `semesters` SET `termNumber` = `semesterNumber`")
                }
                if (!semCols.contains("status")) {
                    db.execSQL("ALTER TABLE `semesters` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'ACTIVE'")
                    db.execSQL("UPDATE `semesters` SET `status` = CASE WHEN `isArchived` = 1 THEN 'ARCHIVED' ELSE 'ACTIVE' END")
                }
                if (!semCols.contains("createdAt")) {
                    db.execSQL("ALTER TABLE `semesters` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0")
                }
                if (!semCols.contains("updatedAt")) {
                    db.execSQL("ALTER TABLE `semesters` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                }
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_semesters_isCurrent` ON `semesters` (`isCurrent`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_semesters_academicYear_termNumber` ON `semesters` (`academicYear`, `termNumber`)")

                // 2. Add professorId to courses
                val cursorCourses = db.query("PRAGMA table_info(`courses`)")
                val courseCols = mutableSetOf<String>()
                while (cursorCourses.moveToNext()) {
                    val nameIdx = cursorCourses.getColumnIndex("name")
                    if (nameIdx != -1) courseCols.add(cursorCourses.getString(nameIdx))
                }
                cursorCourses.close()

                if (!courseCols.contains("professorId")) {
                    db.execSQL("ALTER TABLE `courses` ADD COLUMN `professorId` TEXT DEFAULT NULL")
                }
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_courses_professorId` ON `courses` (`professorId`)")

                // 3. Create professors table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `professors` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `email` TEXT NOT NULL DEFAULT '',
                        `office` TEXT NOT NULL DEFAULT '',
                        `department` TEXT NOT NULL DEFAULT '',
                        `notes` TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())

                // 4. Create exams table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `exams` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `courseId` TEXT NOT NULL,
                        `courseName` TEXT NOT NULL DEFAULT '',
                        `date` TEXT NOT NULL DEFAULT '',
                        `time` TEXT NOT NULL DEFAULT '',
                        `location` TEXT NOT NULL DEFAULT '',
                        `notes` TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(`courseId`) REFERENCES `courses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exams_courseId` ON `exams` (`courseId`)")

                // Populate exams from existing courses with exam info
                db.execSQL("""
                    INSERT OR IGNORE INTO `exams` (`id`, `courseId`, `courseName`, `date`, `time`, `location`)
                    SELECT 'exam_' || id, id, name, examDate, examTime, examLocation
                    FROM `courses`
                    WHERE examDate != '' OR examTime != ''
                """.trimIndent())

                // 5. Create notes table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notes` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `courseId` TEXT DEFAULT NULL,
                        `title` TEXT NOT NULL DEFAULT '',
                        `content` TEXT NOT NULL DEFAULT '',
                        `updatedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_courseId` ON `notes` (`courseId`)")

                // 6. Migrate attendance table to have courseId as primary key
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `attendance_new` (
                        `courseId` TEXT NOT NULL PRIMARY KEY,
                        `courseName` TEXT NOT NULL DEFAULT '',
                        `absentCount` INTEGER NOT NULL DEFAULT 0,
                        `maxAllowed` INTEGER NOT NULL DEFAULT 3,
                        FOREIGN KEY(`courseId`) REFERENCES `courses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT OR REPLACE INTO `attendance_new` (`courseId`, `courseName`, `absentCount`, `maxAllowed`)
                    SELECT 
                        CASE 
                            WHEN a.courseId != '' THEN a.courseId 
                            WHEN c.id IS NOT NULL THEN c.id 
                            ELSE 'att_' || abs(random()) 
                        END,
                        a.courseName,
                        a.absentCount,
                        a.maxAllowed
                    FROM `attendance` a
                    LEFT JOIN `courses` c ON a.courseName = c.name
                """.trimIndent())

                db.execSQL("DROP TABLE `attendance`")
                db.execSQL("ALTER TABLE `attendance_new` RENAME TO `attendance`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_attendance_courseId` ON `attendance` (`courseId`)")

                // 7. Add semesterId to tasks if missing
                val cursorTasks = db.query("PRAGMA table_info(`tasks`)")
                val taskCols = mutableSetOf<String>()
                while (cursorTasks.moveToNext()) {
                    val nameIdx = cursorTasks.getColumnIndex("name")
                    if (nameIdx != -1) taskCols.add(cursorTasks.getString(nameIdx))
                }
                cursorTasks.close()

                if (!taskCols.contains("semesterId")) {
                    db.execSQL("ALTER TABLE `tasks` ADD COLUMN `semesterId` TEXT DEFAULT NULL")
                }
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_semesterId` ON `tasks` (`semesterId`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("PRAGMA table_info(`student_profile`)")
                val cols = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    val nameIdx = cursor.getColumnIndex("name")
                    if (nameIdx != -1) cols.add(cursor.getString(nameIdx))
                }
                cursor.close()

                if (!cols.contains("updatedAt")) {
                    db.execSQL("ALTER TABLE `student_profile` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create course_sessions table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `course_sessions` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `courseId` TEXT NOT NULL,
                        `day` INTEGER NOT NULL DEFAULT 0,
                        `start` TEXT NOT NULL DEFAULT '08:00',
                        `end` TEXT NOT NULL DEFAULT '10:00',
                        `location` TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(`courseId`) REFERENCES `courses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_course_sessions_courseId` ON `course_sessions` (`courseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_course_sessions_day` ON `course_sessions` (`day`)")

                // 2. Query existing courses and migrate sessions while merging duplicate course rows
                data class TempCourseRow(
                    val id: String,
                    val name: String,
                    val day: Int,
                    val start: String,
                    val end: String,
                    val location: String,
                    val colorHex: String,
                    val units: Int,
                    val semesterId: String,
                    val isArchived: Boolean,
                    val courseCode: String,
                    val professor: String,
                    val examDate: String,
                    val examTime: String,
                    val examLocation: String,
                    val notes: String
                )

                val rows = mutableListOf<TempCourseRow>()
                val cursor = db.query(
                    "SELECT id, name, day, start, end, location, colorHex, units, semesterId, isArchived, courseCode, professor, examDate, examTime, examLocation, notes FROM `courses`"
                )
                while (cursor.moveToNext()) {
                    rows.add(
                        TempCourseRow(
                            id = cursor.getString(0) ?: "",
                            name = cursor.getString(1) ?: "",
                            day = cursor.getInt(2),
                            start = cursor.getString(3) ?: "08:00",
                            end = cursor.getString(4) ?: "10:00",
                            location = cursor.getString(5) ?: "",
                            colorHex = cursor.getString(6) ?: "#10B981",
                            units = cursor.getInt(7),
                            semesterId = cursor.getString(8) ?: "current",
                            isArchived = cursor.getInt(9) != 0,
                            courseCode = cursor.getString(10) ?: "",
                            professor = cursor.getString(11) ?: "",
                            examDate = cursor.getString(12) ?: "",
                            examTime = cursor.getString(13) ?: "",
                            examLocation = cursor.getString(14) ?: "",
                            notes = cursor.getString(15) ?: ""
                        )
                    )
                }
                cursor.close()

                // Group courses by logical identity (courseCode + semesterId, or normalized name + semesterId)
                val groups = rows.groupBy { row ->
                    val cleanCode = row.courseCode.trim()
                    val cleanName = row.name.trim().lowercase()
                    val sem = row.semesterId
                    if (cleanCode.isNotBlank()) "code_${cleanCode}_sem_${sem}"
                    else "name_${cleanName}_sem_${sem}"
                }

                for ((_, groupRows) in groups) {
                    if (groupRows.isEmpty()) continue

                    // Canonical course is chosen as the one with highest units, or first with non-blank exam/prof
                    val canonical = groupRows.maxWithOrNull(
                        compareBy<TempCourseRow> { it.units }
                            .thenBy { it.examDate.isNotBlank() || it.examTime.isNotBlank() }
                            .thenBy { it.professor.isNotBlank() }
                    ) ?: groupRows.first()

                    val canonicalId = canonical.id

                    // Consolidate best metadata across duplicate rows into canonical
                    val bestProf = groupRows.firstOrNull { it.professor.isNotBlank() }?.professor ?: canonical.professor
                    val bestExamDate = groupRows.firstOrNull { it.examDate.isNotBlank() }?.examDate ?: canonical.examDate
                    val bestExamTime = groupRows.firstOrNull { it.examTime.isNotBlank() }?.examTime ?: canonical.examTime
                    val bestExamLoc = groupRows.firstOrNull { it.examLocation.isNotBlank() }?.examLocation ?: canonical.examLocation
                    val bestNotes = groupRows.firstOrNull { it.notes.isNotBlank() }?.notes ?: canonical.notes
                    val maxUnits = groupRows.maxOf { it.units }

                    // Update canonical row with merged metadata
                    db.execSQL(
                        "UPDATE `courses` SET `units` = ?, `professor` = ?, `examDate` = ?, `examTime` = ?, `examLocation` = ?, `notes` = ? WHERE `id` = ?",
                        arrayOf(maxUnits, bestProf, bestExamDate, bestExamTime, bestExamLoc, bestNotes, canonicalId)
                    )

                    // Insert course_sessions for each row in the group
                    groupRows.forEachIndexed { idx, row ->
                        val sessId = "sess_${canonicalId.take(8)}_$idx"
                        db.execSQL(
                            "INSERT OR REPLACE INTO `course_sessions` (`id`, `courseId`, `day`, `start`, `end`, `location`) VALUES (?, ?, ?, ?, ?, ?)",
                            arrayOf(sessId, canonicalId, row.day, row.start, row.end, row.location)
                        )
                    }

                    // For secondary rows, re-link tasks/notes/grades, merge attendance & exams to canonicalId, and delete secondary row
                    for (row in groupRows) {
                        if (row.id != canonicalId) {
                            val secId = row.id
                            // 1. Tasks
                            db.execSQL("UPDATE `tasks` SET `courseId` = ? WHERE `courseId` = ?", arrayOf(canonicalId, secId))

                            // 2. Notes
                            db.execSQL("UPDATE `notes` SET `courseId` = ? WHERE `courseId` = ?", arrayOf(canonicalId, secId))

                            // 3. Attendance: merge absentCount and maxAllowed into canonical, or re-point if canonical has none
                            val secAttCursor = db.query(
                                "SELECT absentCount, maxAllowed, courseName FROM `attendance` WHERE `courseId` = ?",
                                arrayOf(secId)
                            )
                            if (secAttCursor.moveToFirst()) {
                                val secAbsent = secAttCursor.getInt(0)
                                val secMaxAllowed = secAttCursor.getInt(1)
                                val secCourseName = secAttCursor.getString(2) ?: ""
                                secAttCursor.close()

                                val canAttCursor = db.query(
                                    "SELECT absentCount, maxAllowed FROM `attendance` WHERE `courseId` = ?",
                                    arrayOf(canonicalId)
                                )
                                if (canAttCursor.moveToFirst()) {
                                    val canAbsent = canAttCursor.getInt(0)
                                    val canMaxAllowed = canAttCursor.getInt(1)
                                    canAttCursor.close()

                                    val mergedAbsent = canAbsent + secAbsent
                                    val mergedMaxAllowed = maxOf(canMaxAllowed, secMaxAllowed)

                                    db.execSQL(
                                        "UPDATE `attendance` SET `absentCount` = ?, `maxAllowed` = ? WHERE `courseId` = ?",
                                        arrayOf(mergedAbsent, mergedMaxAllowed, canonicalId)
                                    )
                                    db.execSQL("DELETE FROM `attendance` WHERE `courseId` = ?", arrayOf(secId))
                                } else {
                                    canAttCursor.close()
                                    db.execSQL(
                                        "UPDATE `attendance` SET `courseId` = ? WHERE `courseId` = ?",
                                        arrayOf(canonicalId, secId)
                                    )
                                }
                            } else {
                                secAttCursor.close()
                            }

                            // 4. Grades: re-point all grades from secId to canonicalId
                            db.execSQL("UPDATE `grades` SET `courseId` = ? WHERE `courseId` = ?", arrayOf(canonicalId, secId))

                            // 5. Exams: re-point and merge into canonical exam row
                            val cursorExamsInfo = db.query("PRAGMA table_info(`exams`)")
                            val examCols = mutableSetOf<String>()
                            while (cursorExamsInfo.moveToNext()) {
                                val nameIdx = cursorExamsInfo.getColumnIndex("name")
                                if (nameIdx != -1) examCols.add(cursorExamsInfo.getString(nameIdx))
                            }
                            cursorExamsInfo.close()
                            val hasExamNotes = examCols.contains("notes")

                            val secExamsQuery = if (hasExamNotes) {
                                "SELECT id, date, time, location, notes FROM `exams` WHERE `courseId` = ?"
                            } else {
                                "SELECT id, date, time, location, '' FROM `exams` WHERE `courseId` = ?"
                            }

                            val secExamsCursor = db.query(secExamsQuery, arrayOf(secId))
                            val secExamList = mutableListOf<Array<String>>()
                            while (secExamsCursor.moveToNext()) {
                                secExamList.add(arrayOf(
                                    secExamsCursor.getString(0) ?: "",
                                    secExamsCursor.getString(1) ?: "",
                                    secExamsCursor.getString(2) ?: "",
                                    secExamsCursor.getString(3) ?: "",
                                    secExamsCursor.getString(4) ?: ""
                                ))
                            }
                            secExamsCursor.close()

                            for (secExam in secExamList) {
                                val sExamId = secExam[0]
                                val sDate = secExam[1]
                                val sTime = secExam[2]
                                val sLoc = secExam[3]
                                val sNotes = secExam[4]

                                val canExamsCursor = db.query(secExamsQuery, arrayOf(canonicalId))
                                if (canExamsCursor.moveToFirst()) {
                                    val cExamId = canExamsCursor.getString(0) ?: ""
                                    val cDate = canExamsCursor.getString(1) ?: ""
                                    val cTime = canExamsCursor.getString(2) ?: ""
                                    val cLoc = canExamsCursor.getString(3) ?: ""
                                    val cNotes = canExamsCursor.getString(4) ?: ""
                                    canExamsCursor.close()

                                    val mergedDate = if (cDate.isNotBlank()) cDate else sDate
                                    val mergedTime = if (cTime.isNotBlank()) cTime else sTime
                                    val mergedLoc = if (cLoc.isNotBlank()) cLoc else sLoc
                                    val mergedNotes = if (cNotes.isNotBlank()) cNotes else sNotes

                                    if (hasExamNotes) {
                                        db.execSQL(
                                            "UPDATE `exams` SET `date` = ?, `time` = ?, `location` = ?, `notes` = ? WHERE `id` = ?",
                                            arrayOf(mergedDate, mergedTime, mergedLoc, mergedNotes, cExamId)
                                        )
                                    } else {
                                        db.execSQL(
                                            "UPDATE `exams` SET `date` = ?, `time` = ?, `location` = ? WHERE `id` = ?",
                                            arrayOf(mergedDate, mergedTime, mergedLoc, cExamId)
                                        )
                                    }
                                    db.execSQL("DELETE FROM `exams` WHERE `id` = ?", arrayOf(sExamId))
                                } else {
                                    canExamsCursor.close()
                                    db.execSQL("UPDATE `exams` SET `courseId` = ? WHERE `id` = ?", arrayOf(canonicalId, sExamId))
                                }
                            }

                            // 6. Delete secondary course row
                            db.execSQL("DELETE FROM `courses` WHERE `id` = ?", arrayOf(secId))
                        }
                    }
                }
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop legacy day, start, end, location columns from courses table using safe table recreation pattern
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `courses_new` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `colorHex` TEXT NOT NULL DEFAULT '#0D9488',
                        `units` INTEGER NOT NULL DEFAULT 3,
                        `semesterId` TEXT NOT NULL DEFAULT 'current',
                        `courseCode` TEXT NOT NULL DEFAULT '',
                        `professorId` TEXT DEFAULT NULL,
                        `professor` TEXT NOT NULL DEFAULT '',
                        `examDate` TEXT NOT NULL DEFAULT '',
                        `examTime` TEXT NOT NULL DEFAULT '',
                        `examLocation` TEXT NOT NULL DEFAULT '',
                        `notes` TEXT NOT NULL DEFAULT '',
                        `isArchived` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO `courses_new` (`id`, `name`, `colorHex`, `units`, `semesterId`, `courseCode`, `professorId`, `professor`, `examDate`, `examTime`, `examLocation`, `notes`, `isArchived`)
                    SELECT `id`, `name`, `colorHex`, `units`, `semesterId`, `courseCode`, `professorId`, `professor`, `examDate`, `examTime`, `examLocation`, `notes`, `isArchived`
                    FROM `courses`
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE `courses`")
                db.execSQL("ALTER TABLE `courses_new` RENAME TO `courses`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_courses_semesterId` ON `courses` (`semesterId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_courses_courseCode` ON `courses` (`courseCode`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_courses_professorId` ON `courses` (`professorId`)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `sync_metadata` (`dataType` TEXT NOT NULL PRIMARY KEY, `updatedAt` INTEGER NOT NULL)")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "student_os_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .addCallback(DatabaseCallback(scope))
                    .enableMultiInstanceInvalidation()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.studentDao(), database.curriculumDao())
                    }
                }
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                Log.e("AppDatabase", "CRITICAL WARNING: Destructive migration occurred on database! User data may have been affected.")
            }
        }

        suspend fun populateInitialData(dao: StudentDao, curriculumDao: CurriculumDao? = null) {
            // 1. Seed Reference Data first (Strict foreign key hierarchy: University -> Major -> CurriculumVersion)
            curriculumDao?.let { cDao ->
                val universities = listOf(
                    UniversityEntity(
                        id = "UNI_AUT",
                        displayNameFa = "دانشگاه صنعتی امیرکبیر",
                        shortName = "پلی‌تکنیک تهران"
                    ),
                    UniversityEntity(
                        id = "UNI_UT",
                        displayNameFa = "دانشگاه تهران",
                        shortName = "دانشگاه تهران"
                    ),
                    UniversityEntity(
                        id = "UNI_SUT",
                        displayNameFa = "دانشگاه صنعتی شریف",
                        shortName = "شریف"
                    )
                )
                cDao.insertUniversities(universities)

                val majors = listOf(
                    MajorEntity(
                        id = "MAJ_AUT_CHEM_ENG",
                        universityId = "UNI_AUT",
                        facultyId = "FAC_AUT_CHEM_OIL",
                        facultyDisplayNameFa = "دانشکده مهندسی شیمی و نفت",
                        majorDisplayNameFa = "مهندسی شیمی"
                    ),
                    MajorEntity(
                        id = "MAJ_AUT_COMP_ENG",
                        universityId = "UNI_AUT",
                        facultyId = "FAC_AUT_COMP",
                        facultyDisplayNameFa = "دانشکده مهندسی کامپیوتر",
                        majorDisplayNameFa = "مهندسی کامپیوتر"
                    )
                )
                cDao.insertMajors(majors)

                val versions = listOf(
                    CurriculumVersionEntity(
                        id = "CURR_AUT_CE_1401",
                        majorId = "MAJ_AUT_CHEM_ENG",
                        title = "چارت مهندسی شیمی امیرکبیر ورودی‌های ۱۴۰۱ به بعد",
                        entryYearMin = 1401,
                        entryYearMax = 1405,
                        totalCreditsRequired = 140
                    )
                )
                cDao.insertCurriculumVersions(versions)

                // Populate accredited curriculum courses
                cDao.insertCurriculumCourses(CurriculumSeedData.chemicalEngineeringCourses)

                // Aliases for accurate matching
                val aliases = listOf(
                    CourseAliasEntity(courseId = "CE_THERMO_1", aliasRaw = "ترمو ۱", aliasNormalized = "ترمو 1"),
                    CourseAliasEntity(courseId = "CE_THERMO_1", aliasRaw = "ترمودینامیک ۱", aliasNormalized = "ترمودینامیک 1"),
                    CourseAliasEntity(courseId = "CE_FLUID_1", aliasRaw = "سیالات ۱", aliasNormalized = "سیالات 1"),
                    CourseAliasEntity(courseId = "CE_HEAT_1", aliasRaw = "حرارت ۱", aliasNormalized = "حرارت 1"),
                    CourseAliasEntity(courseId = "CE_MATH_1", aliasRaw = "ریاضی ۱", aliasNormalized = "ریاضی 1"),
                    CourseAliasEntity(courseId = "CE_MATH_2", aliasRaw = "ریاضی ۲", aliasNormalized = "ریاضی 2")
                )
                cDao.insertAliases(aliases)
            }

            // 2. Clean Zero Setup Initial Profile (Zero personal/demo developer data)
            // Only seed initial blank profile if NO profile currently exists in the database
            val existingProfile = dao.getProfileSync()
            if (existingProfile == null) {
                dao.insertProfile(
                    StudentProfileEntity(
                        id = 1,
                        name = "دانشجو",
                        studentId = "",
                        faculty = "دانشکده مهندسی",
                        term = "ترم ۱",
                        activeUnits = 0,
                        passedUnits = 0,
                        notes = "",
                        isOnboardingCompleted = false,
                        universityId = "UNI_AUT",
                        facultyId = "FAC_AUT_CHEM_OIL",
                        majorId = "MAJ_AUT_CHEM_ENG",
                        declaredPassedCredits = 0,
                        declaredGpa = null
                    )
                )
            }

            // 3. Default clean initial semester (only if no active semester exists)
            val existingSemesters = dao.getAllSemestersSync()
            if (existingSemesters.isEmpty()) {
                dao.insertSemester(
                    SemesterEntity(
                        id = "sem_1",
                        title = "ترم ۱ (پاییز ۱۴۰۳)",
                        year = 1403,
                        academicYear = 1403,
                        semesterNumber = 1,
                        termNumber = 1,
                        isCurrent = true,
                        isArchived = false,
                        totalUnits = 0,
                        status = "ACTIVE"
                    )
                )
            }
        }
    }
}
