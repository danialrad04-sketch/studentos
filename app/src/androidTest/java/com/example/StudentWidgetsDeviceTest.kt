package com.example

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.repository.AppPreferencesRepository
import com.example.widget.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

/** Real system-bound RemoteViews and their broadcast PendingIntent, hosted inside a test window. */
class StudentWidgetsDeviceTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun allThreeWidgetsRenderAndFocusActionUsesTheSharedDeadline() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val ui = InstrumentationRegistry.getInstrumentation().uiAutomation
        val manager = AppWidgetManager.getInstance(context)
        val host = AppWidgetHost(context, 73020)
        val prefs = AppPreferencesRepository.getInstance(context)
        val original = prefs.focusSession.value
        val ids = mutableListOf<Int>()
        try {
            prefs.saveFocusSession(com.example.domain.model.FocusSession(1500, 1500))
            ui.adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET")
            val providers =
                listOf(
                    StudentTodayWidget::class.java,
                    StudentTasksWidget::class.java,
                    StudentFocusWidget::class.java,
                )
            val views = mutableListOf<AppWidgetHostView>()
            rule.runOnIdle {
                providers.forEach { provider ->
                    val id = host.allocateAppWidgetId()
                    ids += id
                    assertTrue(
                        "Platform refused ${provider.simpleName}",
                        manager.bindAppWidgetIdIfAllowed(id, ComponentName(context, provider)),
                    )
                    views +=
                        host.createView(context, id, checkNotNull(manager.getAppWidgetInfo(id)))
                }
                host.startListening()
            }
            rule.setContent {
                Column(Modifier.fillMaxSize()) {
                    views.forEach { hostView ->
                        AndroidView(
                            factory = { hostView },
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                        )
                    }
                }
            }
            runBlocking(Dispatchers.IO) { StudentWidgets.refresh(context) }
            rule.waitUntil(15000) {
                views.map { it.findViewById<TextView>(R.id.widget_title)?.text?.toString() } ==
                    listOf("کلاس بعدی", "کارهای امروز", "تمرکز")
            }
            assertFalse(prefs.focusSession.value.isRunning)
            rule.runOnIdle {
                assertTrue(
                    views.last().findViewById<android.view.View>(R.id.widget_action).performClick()
                )
            }
            rule.waitUntil(15000) { prefs.focusSession.value.isRunning }
            val deadline = prefs.focusSession.value.endsAtMillis
            assertTrue(deadline > System.currentTimeMillis())
            runBlocking(Dispatchers.IO) { StudentWidgets.refresh(context) }
            rule.waitUntil(15000) {
                views.last().findViewById<TextView>(R.id.widget_action)?.text == "مکث"
            }
            val evidence =
                java.io.File("/sdcard/Download/studentos-navigation-diagnostics").apply { mkdirs() }
            ui.takeScreenshot()?.let { bitmap ->
                java.io.File(evidence, "v3-widgets.png").outputStream().use {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
                bitmap.recycle()
            }
            assertEquals(deadline, prefs.focusSession.value.endsAtMillis)
            rule.runOnIdle {
                views.last().findViewById<android.view.View>(R.id.widget_action).performClick()
            }
            rule.waitUntil(15000) { !prefs.focusSession.value.isRunning }
            assertTrue(prefs.focusSession.value.pausedSeconds in 1490..1500)
        } finally {
            rule.runOnIdle {
                host.stopListening()
                ids.forEach(host::deleteAppWidgetId)
                host.deleteHost()
            }
            ui.dropShellPermissionIdentity()
            prefs.saveFocusSession(original)
            runBlocking(Dispatchers.IO) { StudentWidgets.refresh(context) }
        }
    }
}
