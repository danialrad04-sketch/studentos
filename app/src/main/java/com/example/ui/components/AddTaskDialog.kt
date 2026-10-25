package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.datepicker.JalaliCalendarUtil
import com.example.ui.components.datepicker.JalaliDatePickerField

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTaskDialog(courseNames: List<String>, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit, initialTask: com.example.data.local.entity.TaskEntity? = null, isSaving: Boolean = false, onSavePlanned: ((String, String, String, Int, String) -> Unit)? = null) {
    val courses = courseNames.filter { it.isNotBlank() }.distinct().ifEmpty { listOf("عمومی", "پروژه", "امتحان") }
    var title by rememberSaveable { mutableStateOf(initialTask?.title.orEmpty()) }
    var course by rememberSaveable { mutableStateOf(initialTask?.courseName ?: courses.first()) }
    var dueDate by rememberSaveable { mutableStateOf(initialTask?.dueDate ?: JalaliCalendarUtil.today().format()) }
    var repeatDays by rememberSaveable { mutableIntStateOf(initialTask?.repeatEveryDays ?: 0) }
    var reminderEnabled by rememberSaveable { mutableStateOf(!initialTask?.reminderTime.isNullOrBlank()) }
    var reminderTime by rememberSaveable { mutableStateOf(initialTask?.reminderTime?.ifBlank { "09:00" } ?: "09:00") }
    val normalizedTime = com.example.domain.util.AcademicInputValidator.time(reminderTime)
    val valid = title.isNotBlank() && JalaliCalendarUtil.parse(dueDate) != null && (!reminderEnabled || normalizedTime != null)
    StudentGlassModalSheet(onDismiss = onDismiss, title = if (initialTask == null) "افزودن تکلیف" else "ویرایش تکلیف", subtitle = "یک کار روشن با موعد مشخص", maxWidth = 560.dp) {
        val dismiss = LocalStudentModalDismiss.current ?: onDismiss
        Column(Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("عنوان تکلیف") }, placeholder = { Text("مثلاً حل تمرین فصل سوم") },
                modifier = Modifier.fillMaxWidth().testTag("task_title"),
                shape = MaterialTheme.shapes.large, minLines = 1, maxLines = 3
            )
            Text("درس مرتبط", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(courses) { name -> FilterChip(selected = course == name, onClick = { course = name }, label = { Text(name) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer, selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer)) }
            }
            JalaliDatePickerField(value = dueDate, onValueChange = { dueDate = it }, label = "موعد تحویل", placeholder = "تاریخ را انتخاب کن", modifier = Modifier.fillMaxWidth())
            if (onSavePlanned != null) {
                Text("تکرار تکلیف", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "بدون تکرار", 1 to "هر روز", 7 to "هر هفته", 14 to "هر دو هفته", 30 to "هر ۳۰ روز").forEach { (days, label) ->
                        FilterChip(selected = repeatDays == days, onClick = { repeatDays = days }, label = { Text(label) }, modifier = Modifier.testTag("task_repeat_$days"))
                    }
                }
                if (repeatDays > 0) Text("با انجام این تکلیف، موعد بعدی ساخته می‌شود. موعدهای گذشته رد می‌شوند؛ لغو انجام، نسخهٔ بعدی را حذف نمی‌کند.", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("یادآور روز تحویل", modifier = Modifier.weight(1f))
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it }, modifier = Modifier.testTag("task_reminder_switch"))
                }
                if (reminderEnabled) OutlinedTextField(value = reminderTime, onValueChange = { reminderTime = it }, label = { Text("ساعت یادآور، مثلاً 09:00") },
                    isError = normalizedTime == null, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("task_reminder_time"),
                    supportingText = { Text(if (normalizedTime == null) "ساعت معتبر وارد کن؛ از 00:00 تا 23:59." else "به وقت تهران؛ اعلان‌ها باید فعال باشند. زمان گذشته دوباره ارسال نمی‌شود و اندروید ممکن است اعلان را با تأخیر نمایش دهد.") })
            }
            Spacer(Modifier.height(4.dp))
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = dismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("انصراف") }
            Button(onClick = { if (onSavePlanned != null) onSavePlanned(title.trim(), course, dueDate.trim(), repeatDays, if (reminderEnabled) normalizedTime.orEmpty() else "") else onSave(title.trim(), course, dueDate.trim()) }, enabled = valid && !isSaving, modifier = Modifier.weight(1.3f).heightIn(min = 48.dp).testTag("save_task")) {
                Icon(Icons.Rounded.Check, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isSaving) "در حال ذخیره…" else if (initialTask == null) "ثبت تکلیف" else "ذخیره تغییرات")
            }
        }
    }
}
