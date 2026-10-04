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

@Composable
fun AddTaskDialog(courseNames: List<String>, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    val courses = courseNames.filter { it.isNotBlank() }.distinct().ifEmpty { listOf("عمومی", "پروژه", "امتحان") }
    var title by rememberSaveable { mutableStateOf("") }
    var course by rememberSaveable { mutableStateOf(courses.first()) }
    var dueDate by rememberSaveable { mutableStateOf(JalaliCalendarUtil.today().format()) }
    val valid = title.isNotBlank() && JalaliCalendarUtil.parse(dueDate) != null
    StudentGlassModalSheet(onDismiss = onDismiss, title = "افزودن تکلیف", subtitle = "یک کار روشن با موعد مشخص", maxWidth = 560.dp) {
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
            Spacer(Modifier.height(4.dp))
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = dismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("انصراف") }
            Button(onClick = { onSave(title.trim(), course, dueDate.trim()) }, enabled = valid, modifier = Modifier.weight(1.3f).heightIn(min = 48.dp).testTag("save_task")) {
                Icon(Icons.Rounded.Check, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("ثبت تکلیف")
            }
        }
    }
}
