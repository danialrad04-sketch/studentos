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
import com.example.data.parser.ParsedCourseDraft
import com.example.data.parser.DraftValidationState
import com.example.data.local.util.DateTimeNormalizer
import com.example.domain.util.AcademicInputValidator
import com.example.domain.util.JalaliCalendarUtil
import com.example.ui.components.datepicker.JalaliDatePickerField

@Composable
fun ImportDraftEditor(draft: ParsedCourseDraft, onDismiss: () -> Unit, onSave: (ParsedCourseDraft) -> Unit) {
    var name by rememberSaveable(draft.tempId) { mutableStateOf(draft.name) }
    var units by rememberSaveable(draft.tempId) { mutableStateOf(draft.units.toString()) }
    var day by rememberSaveable(draft.tempId) { mutableIntStateOf(draft.dayOfWeek) }
    var start by rememberSaveable(draft.tempId) { mutableStateOf(draft.startTime) }
    var end by rememberSaveable(draft.tempId) { mutableStateOf(draft.endTime) }
    var location by rememberSaveable(draft.tempId) { mutableStateOf(draft.location) }
    var instructor by rememberSaveable(draft.tempId) { mutableStateOf(draft.instructor) }
    var examDate by rememberSaveable(draft.tempId) { mutableStateOf(draft.examDate) }
    var examTime by rememberSaveable(draft.tempId) { mutableStateOf(draft.examTime) }
    var examLocation by rememberSaveable(draft.tempId) { mutableStateOf(draft.examLocation) }
    val edited = draft.copy(name = name.trim(), units = DateTimeNormalizer.normalizeDigits(units).toIntOrNull() ?: 0,
        dayOfWeek = day, dayName = JalaliCalendarUtil.getWeekdayName(day), startTime = AcademicInputValidator.time(start) ?: start,
        endTime = AcademicInputValidator.time(end) ?: end, instructor = instructor.trim(), location = location.trim(),
        examDate = JalaliCalendarUtil.parse(examDate)?.format() ?: examDate.trim(), examTime = AcademicInputValidator.time(examTime) ?: examTime.trim(), examLocation = examLocation.trim())
    val issues = AcademicInputValidator.draftIssues(edited)
    StudentGlassModalSheet(title = "اصلاح درس پیش از ثبت", subtitle = "مقادیر استخراج‌شده را با برنامه دانشگاه تطبیق دهید", onDismiss = onDismiss, maxWidth = 560.dp) {
        val dismiss = LocalStudentModalDismiss.current ?: onDismiss
        Column(Modifier.fillMaxWidth().weight(1f, false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام درس") }, modifier = Modifier.fillMaxWidth().testTag("import_draft_name"))
            OutlinedTextField(value = units, onValueChange = { units = it }, label = { Text("تعداد واحد") }, modifier = Modifier.fillMaxWidth())
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items((0..6).toList()) { d -> FilterChip(selected = day == d, onClick = { day = d }, label = { Text(JalaliCalendarUtil.getWeekdayName(d)) }) } }
            OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("شروع کلاس · ۰۸:۳۰") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("پایان کلاس · ۱۰:۰۰") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("محل کلاس") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = instructor, onValueChange = { instructor = it }, label = { Text("استاد") }, modifier = Modifier.fillMaxWidth())
            Text("امتحان (اختیاری)", style = MaterialTheme.typography.titleSmall)
            JalaliDatePickerField(value = examDate, onValueChange = { examDate = it }, label = "تاریخ امتحان", modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = examTime, onValueChange = { examTime = it }, label = { Text("ساعت امتحان") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = examLocation, onValueChange = { examLocation = it }, label = { Text("محل امتحان") }, modifier = Modifier.fillMaxWidth())
            issues.forEach { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onSave(edited.copy(validationState = DraftValidationState.VALID, validationIssues = emptyList(), missingFields = emptyList())) },
            enabled = issues.isEmpty(), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("save_import_draft")) { Text("تأیید اصلاحات") }
        TextButton(onClick = dismiss, modifier = Modifier.fillMaxWidth()) { Text("انصراف") }
    }
}
