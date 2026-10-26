package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.engine.ExamScoreEngine
import com.example.ui.theme.studentStickerColors
import java.util.Locale

@Composable
fun ExamScorePlannerCard() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var earned by rememberSaveable { mutableStateOf("") }
    var maximum by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("10") }
    val palette = studentStickerColors(AppEmojiType.CALENDAR)
    Surface(shape = MaterialTheme.shapes.large, color = palette.container, contentColor = palette.ink) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppEmoji(AppEmojiType.CALENDAR, size = 36.dp)
                Text("برای امتحان چند بگیرم؟", style = MaterialTheme.typography.titleSmall)
            }
            Text("حداقل نمرهٔ لازم را با بارم واقعی استاد حساب کنید.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.testTag("exam_score_expand")) {
                Text(if (expanded) "بستن محاسبه‌گر" else "محاسبهٔ نمرهٔ امتحان")
            }
            if (expanded) {
                @Composable fun field(value: String, label: String, tag: String, change: (String) -> Unit) {
                    OutlinedTextField(value, onValueChange = change, label = { Text(label) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag(tag))
                }
                field(earned, "نمرهٔ کسب‌شده پیش از امتحان", "exam_earned") { earned = it }
                field(maximum, "بارم امتحان پایانی (از ۲۰ نمرهٔ درس)", "exam_maximum") { maximum = it }
                field(target, "نمرهٔ کل دلخواه از ۲۰", "exam_target") { target = it }
                val result = examScoreNumber(earned)?.let { e -> examScoreNumber(maximum)?.let { m ->
                    examScoreNumber(target)?.let { t -> ExamScoreEngine.calculate(e, m, t) }
                } }
                val message = when {
                    earned.isBlank() || maximum.isBlank() || target.isBlank() -> "هر سه مقدار را وارد کنید؛ مثلاً ۶ نمره کسب‌شده و امتحان ۱۲ نمره‌ای."
                    result == null -> "بارم باید بیشتر از صفر و حداکثر ۲۰ باشد. نمرهٔ کسب‌شده نمی‌تواند از سهم بخش غیرامتحانی بیشتر باشد."
                    !result.achievable -> "این هدف با این بارم ممکن نیست؛ بیشترین نمرهٔ کل: ${String.format(Locale.US, "%.2f", result.maximumTotal)}"
                    result.required == 0.0 -> "نمرهٔ کسب‌شده برای این هدف کافی است؛ شرط حضور در امتحان را از استاد بپرسید."
                    else -> "در امتحان حداقل ${String.format(Locale.US, "%.2f", result.required)} از ${maximum.trim()} لازم دارید."
                }
                Text(message, modifier = Modifier.testTag("exam_score_result"), style = MaterialTheme.typography.bodyMedium)
                Text("محاسبه بر اساس جمع سادهٔ نمرات از ۲۰ است؛ شرط حداقل نمرهٔ پایانی و مقررات استاد جداست. این ابزار نمره‌ای ثبت نمی‌کند.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

internal fun examScoreNumber(value: String): Double? = value.trim().map {
    when (it) {
        in '۰'..'۹' -> '0' + (it - '۰')
        in '٠'..'٩' -> '0' + (it - '٠')
        '٫', '/' -> '.'
        else -> it
    }
}.joinToString("").toDoubleOrNull()?.takeIf { it.isFinite() }
