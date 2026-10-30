package com.example.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.STUDENT_V3_MIGRATION
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class StudentV3MigrationTest {
    @Test
    fun actualPublishedSchema13MigratesWithoutInventingTaskCompletionTimes() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "v3-migration-${java.util.UUID.randomUUID()}"
        val schema =
            JSONObject(
                    javaClass.classLoader!!
                        .getResourceAsStream("com.example.data.local.AppDatabase/13.json")!!
                        .bufferedReader()
                        .use { it.readText() }
                )
                .getJSONObject("database")
        val helper =
            FrameworkSQLiteOpenHelperFactory()
                .create(
                    SupportSQLiteOpenHelper.Configuration.builder(context)
                        .name(name)
                        .callback(
                            object : SupportSQLiteOpenHelper.Callback(13) {
                                override fun onCreate(db: SupportSQLiteDatabase) {
                                    val entities = schema.getJSONArray("entities")
                                    for (i in 0 until entities.length()) {
                                        val entity = entities.getJSONObject(i)
                                        db.execSQL(
                                            entity
                                                .getString("createSql")
                                                .replace(
                                                    "\${TABLE_NAME}",
                                                    entity.getString("tableName"),
                                                )
                                        )
                                        val indices =
                                            entity.optJSONArray("indices") ?: org.json.JSONArray()
                                        for (j in 0 until indices.length()) db.execSQL(
                                            indices
                                                .getJSONObject(j)
                                                .getString("createSql")
                                                .replace(
                                                    "\${TABLE_NAME}",
                                                    entity.getString("tableName"),
                                                )
                                        )
                                    }
                                    db.execSQL(
                                        "INSERT INTO courses(id,name,colorHex,units,semesterId,courseCode,professor,examDate,examTime,examLocation,notes,isArchived) VALUES('math','ریاضی قدیمی','#59652F',3,'current','','','','','','',0)"
                                    )
                                    db.execSQL(
                                        "INSERT INTO tasks(id,title,courseName,dueDate,isCompleted,courseId,semesterId) VALUES(41,'قدیمی','ریاضی','1405/08/01',1,'math','current')"
                                    )
                                    db.execSQL(
                                        "INSERT INTO study_goals(courseId,targetDate,totalMinutes,difficulty,enabled,updatedAt) VALUES('math','1405/09/01',120,2,1,100)"
                                    )
                                    db.execSQL(
                                        "INSERT INTO review_cards(id,courseId,question,answer,dueDate,intervalDays,repetitions,lapses,lastReviewedDate,isArchived,updatedAt) VALUES('review','math','پرسش قدیمی','پاسخ','1405/08/01',1,2,0,'1405/07/30',0,100)"
                                    )
                                    db.execSQL(
                                        "INSERT INTO study_completions(id,courseId,date,startMinute,plannedMinutes,actualMinutes,updatedAt,isDeleted) VALUES('old','math','1405/08/01',540,25,20,100,0)"
                                    )
                                }

                                override fun onUpgrade(
                                    db: SupportSQLiteDatabase,
                                    oldVersion: Int,
                                    newVersion: Int,
                                ) = Unit
                            }
                        )
                        .build()
                )
        helper.writableDatabase
        helper.close()
        val upgraded =
            Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(STUDENT_V3_MIGRATION)
                .allowMainThreadQueries()
                .build()
        try {
            val task = upgraded.studentDao().getTaskById(41)!!
            assertTrue(task.isCompleted)
            assertNull(task.completedAt)
            assertNull(task.projectId)
            assertEquals(0, task.estimatedMinutes)
            assertEquals(
                "ریاضی قدیمی",
                upgraded.studentDao().getAllCoursesIncludingArchivedSync().single().name,
            )
            assertNull(upgraded.studentDao().studyCompletionsSnapshot().single().workId)
            assertEquals(120, upgraded.studentDao().studyGoalsSnapshot().single().totalMinutes)
            assertEquals(
                "پرسش قدیمی",
                upgraded.studentDao().reviewCardsSnapshot().single().question,
            )
            assertTrue(upgraded.studentDao().attachmentsSnapshot().isEmpty())
            assertTrue(upgraded.studentDao().practiceAttemptsSnapshot().isEmpty())
        } finally {
            upgraded.close()
            context.deleteDatabase(name)
        }
    }
}
