package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.BuildConfig

/** Release implementation details stay in repository docs, not in the student experience. */
@Composable
fun AndroidApkDialog(onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    StudentGlassModalSheet(onDismiss, title = "دربارهٔ Student OS", subtitle = "همراه مطالعه و زندگی دانشجویی") {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("نسخهٔ ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.headlineMedium)
            Text("شمارهٔ ساخت ${BuildConfig.VERSION_CODE}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("برنامه‌ریزی کلاس‌ها، پیگیری تکلیف و امتحان، حضور و غیاب و مرور کارنامه، در یک فضای تحصیلی.", style = MaterialTheme.typography.bodyLarge)
            Text("برای گزارش مشکل، اطلاعات نسخه را همراه با توضیح مشکل در بخش پشتیبانی بفرست.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString("Student OS ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n${BuildConfig.APPLICATION_ID}"))
                    Toast.makeText(context, "اطلاعات نسخه کپی شد", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Icon(Icons.Rounded.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text("کپی اطلاعات نسخه") }
        }
    }
}
