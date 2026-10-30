package com.example.ui.v3

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.components.StudentGlassModalSheet
import com.example.ui.components.StudentSettingsRow
import com.example.widget.*

@Composable
fun StudentWidgetSettings() {
    var open by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    StudentSettingsRow(
        "ویجت‌های صفحهٔ اصلی",
        "کلاس بعدی، کارهای امروز و تمرکز",
        Icons.Rounded.Widgets,
        { open = true },
    )
    if (open)
        StudentGlassModalSheet(
            title = "ویجت‌های گوشی",
            subtitle = "اطلاعات مهم، بیرون از برنامه",
            onDismiss = { open = false },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "یک ویجت انتخاب کن. اگر گوشی افزودن مستقیم را پشتیبانی نکند، روی فضای خالی صفحهٔ اصلی نگه دار و از فهرست ویجت‌ها Student OS را انتخاب کن."
                )
                listOf(
                        "کلاس بعدی" to StudentTodayWidget::class.java,
                        "کارهای امروز" to StudentTasksWidget::class.java,
                        "تمرکز" to StudentFocusWidget::class.java,
                    )
                    .forEach { (title, type) ->
                        OutlinedButton(
                            {
                                val manager = AppWidgetManager.getInstance(context)
                                val requested =
                                    runCatching {
                                            Build.VERSION.SDK_INT >= 26 &&
                                                manager.isRequestPinAppWidgetSupported &&
                                                manager.requestPinAppWidget(
                                                    ComponentName(context, type),
                                                    null,
                                                    null,
                                                )
                                        }
                                        .getOrDefault(false)
                                message =
                                    if (requested)
                                        "درخواست افزودن «$title» به صفحهٔ اصلی فرستاده شد."
                                    else
                                        "این گوشی افزودن مستقیم را پشتیبانی نمی‌کند؛ از فهرست ویجت‌های صفحهٔ اصلی استفاده کن."
                            },
                            Modifier.fillMaxWidth(),
                        ) {
                            Text("افزودن $title")
                        }
                    }
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                Text(
                    "رنگ ویجت‌ها از تم روشن یا تیرهٔ گوشی پیروی می‌کند.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
}
