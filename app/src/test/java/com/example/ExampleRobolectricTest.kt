package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

  private lateinit var db: AppDatabase

  @Before
  fun createDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun closeDb() {
    db.close()
  }

  @Test
  fun readStringFromContext() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Student OS", appName)
  }

  @Test
  fun testRoomCourseAndAttendanceOperations() = runBlocking {
    val dao = db.studentDao()

    // Insert course
    val course = CourseEntity(
      id = "c1",
      name = "ترمودینامیک ۱",
      colorHex = "#6366F1",
      units = 3
    )
    dao.insertCourse(course)

    val courses = dao.getAllCourses().first()
    assertEquals(1, courses.size)
    assertEquals("ترمودینامیک ۱", courses[0].name)

    // Insert attendance
    val attendance = AttendanceEntity(
      courseId = "c1",
      courseName = "ترمودینامیک ۱",
      absentCount = 2,
      maxAllowed = 3
    )
    dao.insertAttendance(attendance)
    val attendanceList = dao.getAllAttendance().first()
    assertEquals(1, attendanceList.size)
    assertEquals(2, attendanceList[0].absentCount)

    // Increase attendance (reaching 3/16 limit)
    dao.updateAttendance(attendanceList[0].copy(absentCount = 3))
    val updated = dao.getAllAttendance().first()
    assertEquals(3, updated[0].absentCount)
    assertTrue(updated[0].absentCount >= updated[0].maxAllowed)
  }

  @Test
  fun testGpaAndGradesCalculation() = runBlocking {
    val dao = db.studentDao()

    dao.insertAllGrades(
      listOf(
        GradeEntity(
          courseName = "ترمودینامیک ۱",
          units = 3,
          midtermGrade = 5.5,
          finalGrade = 12.5
        ),
        GradeEntity(
          courseName = "مکانیک سیالات ۱",
          units = 3,
          midtermGrade = 5.0,
          finalGrade = 13.0
        )
      )
    )

    val grades = dao.getAllGrades().first()
    val totalUnits = grades.sumOf { it.units }
    val weightedScore = grades.sumOf { (it.midtermGrade + it.finalGrade) * it.units }
    val gpa = weightedScore / totalUnits

    assertEquals(6, totalUnits)
    assertEquals(18.0, gpa, 0.01)
    // GPA >= 17 => Honors (allowed 24 units in term 4)
    assertTrue(gpa >= 17.0)
  }

  @Test
  fun testTaskStatusToggle() = runBlocking {
    val dao = db.studentDao()

    val task = TaskEntity(
      courseName = "ترمودینامیک ۱",
      title = "پروژه شبیه‌سازی سیکل رانکین",
      dueDate = "1405/09/28",
      isCompleted = false
    )
    val id = dao.insertTask(task)

    val tasks = dao.getAllTasks().first()
    assertEquals(1, tasks.size)
    assertEquals(false, tasks[0].isCompleted)

    dao.updateTask(tasks[0].copy(isCompleted = true))
    val updatedTasks = dao.getAllTasks().first()
    assertEquals(true, updatedTasks[0].isCompleted)
  }
}
