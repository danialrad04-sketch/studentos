package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.StudentProfileEntity

/** Long identity forms use a full page; actions stay above the keyboard. */
@Composable
fun EditProfileDialog(
    profile: StudentProfileEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Int, Int, Int, Int) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(profile.name) }
    var studentId by rememberSaveable { mutableStateOf(profile.studentId) }
    var university by rememberSaveable { mutableStateOf(profile.university) }
    var major by rememberSaveable { mutableStateOf(profile.major) }
    var year by rememberSaveable { mutableStateOf(profile.entryYear.takeIf { it > 0 }?.toString().orEmpty()) }
    var semester by rememberSaveable { mutableStateOf(profile.currentSemester.takeIf { it > 0 }?.toString().orEmpty()) }
    var passed by rememberSaveable { mutableStateOf(profile.passedUnits.toString()) }
    var active by rememberSaveable { mutableStateOf(profile.activeUnits.toString()) }
    val numbers = listOf(year, semester, passed, active).map(::studentProfileNumber)
    val valid = name.isNotBlank() && numbers.all { it != null }
    StudentSettingsPage("پروفایل دانشجویی", onDismiss) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).testTag("profile_editor")) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("اطلاعات هویتی و تحصیلی‌ات را اینجا مدیریت کن.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ProfileTextField("نام و نام خانوادگی", name, { name = it }, isError = name.isBlank())
                ProfileTextField("شمارهٔ دانشجویی (اختیاری)", studentId, { studentId = it })
                ProfileTextField("دانشگاه", university, { university = it })
                ProfileTextField("رشتهٔ تحصیلی", major, { major = it })
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("نیم‌سال و واحدها", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                ProfileTextField("سال ورود", year, { year = it }, numeric = true, isError = numbers[0] == null)
                ProfileTextField("نیم‌سال جاری", semester, { semester = it }, numeric = true, isError = numbers[1] == null)
                ProfileTextField("واحدهای گذرانده", passed, { passed = it }, numeric = true, isError = numbers[2] == null)
                ProfileTextField("واحدهای ترم جاری", active, { active = it }, numeric = true, isError = numbers[3] == null)
                Spacer(Modifier.height(8.dp))
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("انصراف") }
                Button(
                    onClick = { onSave(name.trim(), studentId.trim(), university.trim(), major.trim(), numbers[0]!!, numbers[1]!!, numbers[2]!!, numbers[3]!!) },
                    enabled = valid, modifier = Modifier.weight(1.4f).heightIn(min = 48.dp).testTag("save_profile")
                ) { Text("ذخیره مشخصات") }
            }
        }
    }
}

@Composable
private fun ProfileTextField(label: String, value: String, onValueChange: (String) -> Unit, numeric: Boolean = false, isError: Boolean = false) {
    OutlinedTextField(
        value, onValueChange, label = { Text(label) }, singleLine = true,
        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
        isError = isError,
        supportingText = if (isError) { { Text(if (numeric) "یک عدد صحیح و غیرمنفی وارد کن" else "نام نمی‌تواند خالی باشد") } } else null
    )
}

/** Accept the Persian/Arabic digits produced by local keyboards, without silent fallback. */
internal fun studentProfileNumber(raw: String): Int? {
    if (raw.isBlank()) return 0
    val normalized = raw.trim().map { ch -> if (ch.isDigit()) Character.getNumericValue(ch).toString() else ch.toString() }.joinToString("")
    return normalized.toIntOrNull()?.takeIf { it >= 0 }
}
