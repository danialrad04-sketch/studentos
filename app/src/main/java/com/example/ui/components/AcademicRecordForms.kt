package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.util.DateTimeNormalizer
import com.example.domain.util.AcademicInputValidator
import com.example.ui.components.datepicker.JalaliCalendarUtil
import com.example.ui.components.datepicker.JalaliDatePickerField

@Composable
fun ExamEditorDialog(courses: List<CourseEntity>, initialCourseId: String?, onDismiss: () -> Unit,
    onSave: (CourseEntity, String, String, String) -> Unit, isSaving: Boolean = false) {
    var courseId by rememberSaveable { mutableStateOf(initialCourseId ?: courses.firstOrNull()?.id.orEmpty()) }
    val course = courses.find { it.id == courseId }
    var date by rememberSaveable(courseId) { mutableStateOf(course?.examDate.orEmpty()) }
    var time by rememberSaveable(courseId) { mutableStateOf(course?.examTime.orEmpty()) }
    var location by rememberSaveable(courseId) { mutableStateOf(course?.examLocation.orEmpty()) }
    var deleting by remember { mutableStateOf(false) }
    val valid = course != null && JalaliCalendarUtil.parse(date) != null && AcademicInputValidator.time(time) != null
    StudentGlassModalSheet(title = "برنامه امتحان", subtitle = "زمان آزمون را برای درس مرتبط ثبت کنید", onDismiss = onDismiss, maxWidth = 560.dp) {
        val dismiss = LocalStudentModalDismiss.current ?: onDismiss
        Column(Modifier.weight(1f, false).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (courses.isEmpty()) Text("ابتدا یک درس در برنامه کلاسی ثبت کنید.")
            else {
                Text("درس", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(courses, key = { it.id }) { item -> FilterChip(selected = courseId == item.id, onClick = { if (!isSaving) courseId = item.id }, label = { Text(item.name) }) }
                }
                JalaliDatePickerField(value = date, onValueChange = { date = it }, label = "تاریخ آزمون", modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("ساعت آزمون") },
                    supportingText = { Text("ساعت ۲۴ ساعته؛ مانند ۰۸:۳۰") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("exam_time"))
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("محل آزمون (اختیاری)") }, modifier = Modifier.fillMaxWidth())
                if (!course?.examDate.isNullOrBlank()) TextButton(onClick = { deleting = true }, enabled = !isSaving) { Text("حذف برنامه امتحان", color = MaterialTheme.colorScheme.error) }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = dismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("انصراف") }
            Button(onClick = { course?.let { onSave(it, date, time, location) } }, enabled = valid && !isSaving,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("save_exam")) { Text(if (isSaving) "در حال ذخیره…" else "ذخیره آزمون") }
        }
    }
    if (deleting && course != null) AlertDialog(onDismissRequest = { deleting = false }, title = { Text("حذف برنامه امتحان؟") },
        text = { Text("زمان امتحان و یادآور آن حذف می‌شود؛ اطلاعات درس حفظ می‌شود.") },
        confirmButton = { TextButton(onClick = { onSave(course, "", "", ""); deleting = false }, enabled = !isSaving) { Text("حذف") } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("انصراف") } })
}

@Composable
fun RecordGradeDialog(course: CourseEntity, grade: GradeEntity?, onDismiss: () -> Unit,
    onSave: (GradeEntity, Double, Double) -> Unit, isSaving: Boolean = false) {
    var mid by rememberSaveable(course.id) { mutableStateOf(grade?.takeIf { it.hasRecordedScore() }?.midtermGrade?.toString().orEmpty()) }
    var final by rememberSaveable(course.id) { mutableStateOf(grade?.takeIf { it.hasRecordedScore() }?.finalGrade?.toString().orEmpty()) }
    fun number(value: String) = DateTimeNormalizer.normalizeDigits(value).replace('٫', '.').replace(',', '.').toDoubleOrNull()
    val m = number(mid)
    val f = number(final)
    val valid = m != null && f != null && m.isFinite() && f.isFinite() && m >= 0 && f >= 0 && m + f <= 20
    StudentGlassModalSheet(title = if (grade == null) "ثبت اولین نمره" else "ویرایش نمره", subtitle = course.name,
        onDismiss = onDismiss, maxWidth = 560.dp) {
        val dismiss = LocalStudentModalDismiss.current ?: onDismiss
        Column(Modifier.weight(1f, false).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("مجموع میان‌ترم و پایان‌ترم باید بین صفر و ۲۰ باشد. اگر نمره جداگانه ندارید، میان‌ترم را صفر و نمره کل را در پایان‌ترم وارد کنید.", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(value = mid, onValueChange = { mid = it }, label = { Text("میان‌ترم") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("grade_mid"))
            OutlinedTextField(value = final, onValueChange = { final = it }, label = { Text("پایان‌ترم") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("grade_final"))
            if (valid) Text("نمره کل: ${m!! + f!!}", style = MaterialTheme.typography.titleMedium)
            else Text("هر دو نمره را وارد کنید؛ مجموع نباید بیشتر از ۲۰ باشد.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = dismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("انصراف") }
            Button(onClick = { onSave(grade ?: GradeEntity(courseId = course.id, courseName = course.name, units = course.units), m!!, f!!) },
                enabled = valid && !isSaving, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("save_grade")) {
                Text(if (isSaving) "در حال ذخیره…" else "ذخیره نمره")
            }
        }
    }
}
