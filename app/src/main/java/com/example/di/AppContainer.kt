package com.example.di

import android.content.Context
import com.example.data.auth.StudentAuthManager
import com.example.data.local.AppDatabase
import com.example.data.local.dao.CurriculumDao
import com.example.data.local.dao.StudentDao
import com.example.data.repository.AppPreferencesRepository
import com.example.data.repository.StudentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Service Locator / Dependency Injection Container for Student OS.
 * Provides clean, decoupled singletons across the application lifecycle.
 */
class AppContainer(private val context: Context) {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context, applicationScope)
    }

    val studentDao: StudentDao by lazy {
        database.studentDao()
    }

    val curriculumDao: CurriculumDao by lazy {
        database.curriculumDao()
    }

    val studentRepository: StudentRepository by lazy {
        StudentRepository(studentDao, curriculumDao, database)
    }

    val preferencesRepository: AppPreferencesRepository by lazy {
        AppPreferencesRepository.getInstance(context)
    }

    val authManager: StudentAuthManager by lazy {
        StudentAuthManager.getInstance(context)
    }
}
