package com.example.ui.v3

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.withTransaction
import com.example.StudentApplication
import com.example.data.backup.LocalDataBackupManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.v3.*
import com.example.ui.StudentViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class StudentV3ViewModel(
    app: Application,
    private val canWrite: () -> Boolean,
    private val ownerValid: () -> Boolean = canWrite,
    private val bulkChange: suspend (suspend () -> Unit) -> Unit = { it() },
    private val restored: (suspend (String) -> Unit)? = null,
    database: AppDatabase? = null,
    val ownerKey: String = java.util.UUID.randomUUID().toString(),
) : AndroidViewModel(app) {
    private val container = (app as? StudentApplication)?.container
    private val db = database ?: container?.database ?: AppDatabase.getDatabase(app)
    val files = container?.v3Files ?: StudentV3Files(app)
    val repository = StudentV3Repository(db, canWrite)
    private val preferences =
        container?.preferencesRepository
            ?: com.example.data.repository.AppPreferencesRepository.getInstance(app)
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    private val reload = MutableStateFlow(0)
    @OptIn(ExperimentalCoroutinesApi::class)
    val state =
        reload
            .flatMapLatest {
                combine(
                        combine(
                            repository.dao.observeAttachments(),
                            repository.dao.observePageBookmarks(),
                            repository.dao.observeProjects(),
                            repository.dao.observeProjectSteps(),
                            repository.dao.observePracticeQuestions(),
                        ) { f, b, p, s, q ->
                            StudentV3Snapshot(f, b, p, s, q)
                        },
                        repository.dao.observePracticeSummaries(),
                        repository.dao.getAllCoursesIncludingArchived(),
                        repository.dao.getAllTasks(),
                    ) { snapshot, attempts, courses, tasks ->
                        V3UiState(snapshot, courses, tasks, true, attempts = attempts)
                    }
                    .catch { e ->
                        if (e is CancellationException) throw e
                        emit(V3UiState(loaded = true, error = "اطلاعات باز نشد؛ دوباره تلاش کنید."))
                    }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), V3UiState())

    fun retry() {
        reload.value++
    }

    fun perform(action: suspend StudentV3Repository.() -> Unit, onDone: () -> Unit = {}) {
        runCommand({ repository.action() }, onDone)
    }

    private fun runCommand(action: suspend () -> Unit, onDone: () -> Unit = {}) {
        if (busy.value) return
        busy.value = true
        message.value = null
        viewModelScope.launch {
            try {
                check(canWrite()) { "نشست تغییر کرده؛ صفحه را دوباره باز کنید." }
                action()
                if (ownerValid()) withContext(Dispatchers.IO) { preferences.recordSuccessfulSave() }
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message.value =
                    e.localizedMessage?.takeIf { it.any { c -> c in '\u0600'..'\u06ff' } }
                        ?: "عملیات انجام نشد؛ ورودی‌ها را بررسی کن و دوباره تلاش کن."
            } finally {
                busy.value = false
            }
        }
    }

    fun purgeFile(row: AttachmentEntity) =
        runCommand({
            files.mutex.lock()
            try {
                repository.purgeFile(row)
            } finally {
                files.mutex.unlock()
                withContext(NonCancellable) {
                    files.pruneUnreferenced {
                        repository.dao
                            .attachmentsSnapshot()
                            .filter { !it.isPurged }
                            .map { it.blobKey }
                            .toSet()
                    }
                }
            }
        })

    fun importFile(uri: Uri, courseId: String, projectId: String? = null) =
        runCommand({
            files.import(uri, courseId, projectId, canWrite) { repository.saveFile(it) }
            message.value = "فایل به کتابخانه اضافه شد."
        })

    private suspend fun metadata(): String =
        db.withTransaction {
            check(canWrite())
            LocalDataBackupManager.withStudySettings(
                LocalDataBackupManager.exportCompleteDatabaseJson(
                    db.studentDao(),
                    db.curriculumDao(),
                ),
                preferences.exportStudySettings(),
            )
        }

    fun exportBackup(uri: Uri) =
        runCommand({
            StudentPortableBackup(files).export(uri, ::metadata, canWrite)
            message.value = "پشتیبان کامل همراه فایل‌ها ذخیره شد."
        })

    fun restoreBackup(uri: Uri) =
        runCommand({
            bulkChange {
                StudentPortableBackup(files) { repository.dao.attachmentsSnapshot() }
                    .import(uri, ownerValid) { json ->
                        val settings = LocalDataBackupManager.readStudySettings(json)
                        db.withTransaction {
                            check(ownerValid())
                            LocalDataBackupManager.importCompleteDatabaseJson(
                                    json,
                                    db.studentDao(),
                                    db.curriculumDao(),
                                )
                                .getOrThrow()
                            check(ownerValid())
                        }
                        if (restored != null) restored.invoke(json)
                        else
                            withContext(NonCancellable) {
                                settings?.let(preferences::restoreStudySettings)
                                LocalDataBackupManager.saveLocalSnapshot(getApplication(), json)
                            }
                    }
            }
            message.value = "اطلاعات و فایل‌های پشتیبان بازیابی شدند."
        })
}

data class V3UiState(
    val data: StudentV3Snapshot = StudentV3Snapshot(),
    val courses: List<CourseEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val loaded: Boolean = false,
    val error: String? = null,
    val attempts: List<PracticeAttemptSummary> = emptyList(),
)

@Composable
fun studentV3ViewModel(student: StudentViewModel): StudentV3ViewModel {
    val owner by student.workspaceGeneration.collectAsStateWithLifecycle()
    val app = LocalContext.current.applicationContext as Application
    return viewModel(
        key = "student-v3:$owner",
        factory =
            remember(owner) {
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        StudentV3ViewModel(
                            app,
                            canWrite = {
                                student.workspaceGeneration.value == owner &&
                                    !student.workspaceBusy.value
                            },
                            ownerValid = { student.workspaceGeneration.value == owner },
                            bulkChange = { action -> student.withWorkspaceChange(owner, action) },
                            restored = student::finishPortableRestore,
                            ownerKey = owner,
                        )
                            as T
                }
            },
    )
}
