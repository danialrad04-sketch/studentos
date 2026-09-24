package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.StudentApplication
import com.example.data.local.entity.CourseEntity
import com.example.data.repository.StudentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Specialized ViewModel handling Weekly Class Schedules, Exams, and Workload calculations.
 */
class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StudentRepository =
        (application as? StudentApplication)?.container?.studentRepository
            ?: run {
                val db = com.example.data.local.AppDatabase.getDatabase(application, viewModelScope)
                StudentRepository(db.studentDao(), db.curriculumDao(), db)
            }

    val currentSemesterCourses: StateFlow<List<CourseEntity>> =
        repository.courses
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCourse(course: CourseEntity) {
        viewModelScope.launch {
            repository.saveCourse(course)
        }
    }

    fun updateCourse(course: CourseEntity) {
        viewModelScope.launch {
            repository.saveCourse(course)
        }
    }

    fun deleteCourse(courseId: String) {
        viewModelScope.launch {
            repository.deleteCourse(courseId)
        }
    }
}

