package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.StudentProfileEntity
import com.example.data.local.util.DateTimeNormalizer
import com.example.data.parser.ManualCurriculumParser

@Composable
fun PersonalCurriculumDialog(profile: StudentProfileEntity, isSaving: Boolean, onDismiss: () -> Unit,
    onSave: (String, String, Int, Int, String) -> Unit) {
    var university by rememberSaveable { mutableStateOf(profile.university) }
    var major by rememberSaveable { mutableStateOf(profile.major) }
    var year by rememberSaveable { mutableStateOf(profile.entryYear.takeIf { it > 0 }?.toString().orEmpty()) }
    var credits by rememberSaveable { mutableStateOf("") }
    var source by rememberSaveable { mutableStateOf("") }
    val parsed = remember(source) { runCatching { ManualCurriculumParser.parse(source, "preview") } }
    val y = DateTimeNormalizer.normalizeDigits(year).toIntOrNull()
    val c = DateTimeNormalizer.normalizeDigits(credits).toIntOrNull()
    val valid = university.isNotBlank() && major.isNotBlank() && y != null && y in 1300..1500 && c != null && c in 1..500 && parsed.isSuccess
    StudentGlassModalSheet(title = "چارت رشته من", subtitle = "ورود چارت شخصی برای پیشنهاد درس و بررسی پیش‌نیاز", onDismiss = onDismiss, maxWidth = 620.dp) {
        val dismiss = LocalStudentModalDismiss.current ?: onDismiss
        Column(Modifier.fillMaxWidth().weight(1f, false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("چارت را از برنامه دانشگاه خود وارد کنید. این داده شخصی است و تأیید دانشگاه محسوب نمی‌شود. برای انتقال به دستگاه دیگر، پشتیبان کامل بگیرید.", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(university, { university = it }, label = { Text("دانشگاه") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(major, { major = it }, label = { Text("رشته") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(year, { year = it }, label = { Text("سال ورود") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(credits, { credits = it }, label = { Text("کل واحد لازم برای فارغ‌التحصیلی") }, modifier = Modifier.fillMaxWidth())
            Text("هر درس در یک ردیف: کد | نام درس | واحد | ترم پیشنهادی | پیش‌نیازها\nپیش‌نیازها اختیاری‌اند؛ نام یا کد درس‌ها را با ویرگول جدا کنید.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(source, { source = it }, label = { Text("دروس چارت") }, minLines = 5,
                placeholder = { Text("M1 | ریاضی عمومی ۱ | ۳ | ۱\nM2 | ریاضی عمومی ۲ | ۳ | ۲ | M1") },
                modifier = Modifier.fillMaxWidth().testTag("personal_curriculum_source"))
            if (source.isNotBlank()) Text(parsed.getOrNull()?.let { "${it.size} درس · ${it.sumOf { row -> row.units }} واحد در فهرست" }
                ?: parsed.exceptionOrNull()?.message.orEmpty(), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onSave(university, major, y!!, c!!, source) }, enabled = valid && !isSaving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("save_personal_curriculum")) { Text(if (isSaving) "در حال ذخیره…" else "ذخیره چارت شخصی") }
        TextButton(onClick = dismiss, modifier = Modifier.fillMaxWidth()) { Text("انصراف") }
    }
}
