package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.room.InvalidationTracker
import androidx.work.*
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.FocusSessionController
import com.example.data.repository.AppPreferencesRepository
import com.example.data.repository.learningToday
import com.example.domain.engine.StudentAgendaEngine
import com.example.domain.util.JalaliCalendarUtil
import com.example.ui.components.AppEmojiType
import com.example.ui.components.studentWidgetSticker
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

open class StudentWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeout(9000) {
                    if (intent.action == StudentWidgets.TOGGLE)
                        intent.getStringExtra("owner_generation")?.let {
                            FocusSessionController.toggle(context, it)
                        }
                    StudentWidgets.refresh(context)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                com.example.util.CrashLogger.recordException(e)
            } finally {
                pending.finish()
            }
        }
    }
}

class StudentTodayWidget : StudentWidgetProvider()

class StudentTasksWidget : StudentWidgetProvider()

class StudentFocusWidget : StudentWidgetProvider()

class StudentWidgetWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result =
        try {
            StudentWidgets.refresh(applicationContext)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
}

/** No widget installed means no observers, polling job or widget-driven database reads. */
object StudentWidgets {
    const val TOGGLE = "com.aistudio.studentos.TOGGLE_FOCUS"
    private const val NEXT = "student_widgets_next_update"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var database: AppDatabase? = null
    private var observer: InvalidationTracker.Observer? = null
    private var settings: Job? = null
    private val requests =
        MutableSharedFlow<Context>(
            extraBufferCapacity = 1,
            onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
        )
    private val types =
        listOf(
            StudentTodayWidget::class.java,
            StudentTasksWidget::class.java,
            StudentFocusWidget::class.java,
        )

    init {
        scope.launch {
            @OptIn(FlowPreview::class)
            requests.debounce(350).collect { context ->
                try {
                    refresh(context)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    com.example.util.CrashLogger.recordException(e)
                }
            }
        }
    }

    fun request(context: Context) {
        requests.tryEmit(context.applicationContext)
    }

    fun installed(context: Context): Boolean =
        types.any {
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, it))
                .isNotEmpty()
        }

    suspend fun refresh(context: Context) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val manager = AppWidgetManager.getInstance(context)
                val ids =
                    types.associateWith { manager.getAppWidgetIds(ComponentName(context, it)) }
                if (ids.values.all { it.isEmpty() }) {
                    observer?.let { database?.invalidationTracker?.removeObserver(it) }
                    observer = null
                    database = null
                    settings?.cancel()
                    settings = null
                    WorkManager.getInstance(context).cancelUniqueWork(NEXT)
                    return@withLock
                }
                val db = AppDatabase.getDatabase(context)
                val dao = db.studentDao()
                val prefs = AppPreferencesRepository.getInstance(context)
                if (database !== db) {
                    database = db
                    observer =
                        object :
                                InvalidationTracker.Observer(
                                    arrayOf(
                                        "courses",
                                        "course_sessions",
                                        "tasks",
                                        "student_profile",
                                        "semesters",
                                    )
                                ) {
                                override fun onInvalidated(tables: Set<String>) {
                                    request(context)
                                }
                            }
                            .also { db.invalidationTracker.addObserver(it) }
                    settings =
                        scope.launch {
                            combine(prefs.focusSession, prefs.taskReminderGeneration) { _, _ ->
                                    Unit
                                }
                                .collect { request(context) }
                        }
                }
                FocusSessionController.complete(context)
                val now = System.currentTimeMillis()
                val today = learningToday()
                val courses = dao.getCurrentSemesterCourses().first()
                val courseIds = courses.map { it.id }.toSet()
                val sessions = dao.getAllSessionsSync().filter { it.courseId in courseIds }
                val open =
                    dao.getAllTasksSync().filter {
                        !it.isCompleted && (it.courseId.isBlank() || it.courseId in courseIds)
                    }
                val due =
                    open
                        .filter { task ->
                            JalaliCalendarUtil.parse(task.dueDate)?.let { it <= today } == true
                        }
                        .sortedBy { it.dueDate }
                data class Occurrence(
                    val name: String,
                    val place: String,
                    val start: Long,
                    val end: Long,
                )
                val next =
                    sessions
                        .flatMap { session ->
                            (0..7).mapNotNull { offset ->
                                val date = StudentAgendaEngine.addDays(today, offset)
                                if (StudentAgendaEngine.weekday(date) != session.day)
                                    return@mapNotNull null
                                val start =
                                    com.example.domain.util.AcademicInputValidator.examTimestamp(
                                        date.format(),
                                        session.start,
                                    )
                                val end =
                                    com.example.domain.util.AcademicInputValidator.examTimestamp(
                                        date.format(),
                                        session.end,
                                    )
                                if (start == null || end == null || end <= now || end <= start) null
                                else
                                    Occurrence(
                                        courses.first { it.id == session.courseId }.name,
                                        session.location,
                                        start,
                                        end,
                                    )
                            }
                        }
                        .minByOrNull { it.start }
                val session = prefs.focusSession.value
                val stamp =
                    java.text
                        .SimpleDateFormat("HH:mm", java.util.Locale("fa"))
                        .apply { timeZone = TimeZone.getTimeZone("Asia/Tehran") }
                        .format(java.util.Date(now))
                val todayBody =
                    next?.let {
                        "${it.name}\n${if(it.start<=now) "در حال برگزاری" else java.text.SimpleDateFormat("EEE HH:mm",java.util.Locale("fa")).apply { timeZone=TimeZone.getTimeZone("Asia/Tehran") }.format(java.util.Date(it.start))}\n${it.place.ifBlank { "مکان ثبت نشده" }}"
                    } ?: "کلاسی در برنامهٔ هفته ثبت نشده است."
                for ((type, widgets) in ids) {
                    val focus = type == StudentFocusWidget::class.java
                    val tasks = type == StudentTasksWidget::class.java
                    for (id in widgets) {
                        val views = RemoteViews(context.packageName, R.layout.student_widget)
                        views.setTextViewText(
                            R.id.widget_title,
                            if (focus) "تمرکز" else if (tasks) "کارهای امروز" else "کلاس بعدی",
                        )
                        views.setImageViewBitmap(
                            R.id.widget_icon,
                            studentWidgetSticker(
                                context,
                                if (focus) AppEmojiType.POMODORO
                                else if (tasks) AppEmojiType.CHECK else AppEmojiType.CALENDAR,
                            ),
                        )
                        val compact = context.resources.configuration.fontScale > 1.3f
                        if (compact) views.setViewVisibility(R.id.widget_icon, View.GONE)
                        val body =
                            if (focus) session.label
                            else if (tasks) {
                                if (due.isEmpty()) "کاری با موعد امروز یا گذشته باقی نمانده است."
                                else
                                    due.take(3).joinToString("\n") { it.title } +
                                        if (due.size > 3) "\nو ${due.size-3} کار دیگر" else ""
                            } else todayBody
                        views.setTextViewText(
                            R.id.widget_body,
                            body + if (!focus) "\nبه‌روز: $stamp" else "",
                        )
                        views.setInt(R.id.widget_body, "setMaxLines", if (compact) 2 else 5)
                        views.setOnClickPendingIntent(
                            R.id.widget_open,
                            PendingIntent.getActivity(
                                context,
                                id,
                                Intent(context, MainActivity::class.java)
                                    .putExtra(
                                        "destination_tab",
                                        if (focus) "POMODORO"
                                        else if (tasks) "TASKS" else "SCHEDULE",
                                    )
                                    .addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    ),
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                            ),
                        )
                        views.setTextViewText(
                            R.id.widget_action,
                            if (focus) if (session.isRunning) "مکث" else "شروع" else "به‌روزرسانی",
                        )
                        views.setOnClickPendingIntent(
                            R.id.widget_action,
                            PendingIntent.getBroadcast(
                                context,
                                id,
                                Intent(context, type)
                                    .setAction(
                                        if (focus) TOGGLE
                                        else AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                    )
                                    .putExtra(
                                        "owner_generation",
                                        prefs.taskReminderGeneration.value,
                                    ),
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                            ),
                        )
                        if (focus) {
                            views.setViewVisibility(R.id.widget_timer, View.VISIBLE)
                            if (compact) views.setViewVisibility(R.id.widget_body, View.GONE)
                            views.setChronometerCountDown(R.id.widget_timer, true)
                            views.setChronometer(
                                R.id.widget_timer,
                                SystemClock.elapsedRealtime() +
                                    session.remainingSeconds(now) * 1000L,
                                "%s",
                                session.isRunning,
                            )
                            if (!session.isRunning)
                                views.setTextViewText(
                                    R.id.widget_timer,
                                    "${session.remainingSeconds(now)/60}:${(session.remainingSeconds(now)%60).toString().padStart(2,'0')}",
                                )
                        }
                        manager.updateAppWidget(id, views)
                    }
                }
                val tomorrow =
                    Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
                        .apply {
                            timeInMillis = now
                            add(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 2)
                            set(Calendar.MILLISECOND, 0)
                        }
                        .timeInMillis
                val change =
                    listOfNotNull(
                            next?.let { if (it.start > now) it.start else it.end },
                            session.endsAtMillis.takeIf { it > now },
                            tomorrow,
                        )
                        .min()
                WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        NEXT,
                        ExistingWorkPolicy.REPLACE,
                        OneTimeWorkRequestBuilder<StudentWidgetWorker>()
                            .setInitialDelay(
                                (change - now).coerceAtLeast(1000),
                                TimeUnit.MILLISECONDS,
                            )
                            .build(),
                    )
            }
        }
}
