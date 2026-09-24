package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.SemesterEntity
import com.example.data.parser.ParsedCourseDraft
import com.example.data.parser.RegistrationTextParser
import com.example.ui.theme.Amber600
import com.example.ui.theme.BrandIndigo600
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Rose600
import java.util.Locale

/**
 * Phase T: Semester Transition Flow Dialog
 * Safe multi-step wizard:
 * Step 1: Review Current Semester Summary
 * Step 2: Configure New Semester Metadata
 * Step 3: Add New Semester Courses (Manual or Natural Language Text Parse)
 * Step 4: Final Confirmation & Atomic Transition Execution
 */
@Composable
fun SemesterTransitionDialog(
    currentSemester: SemesterEntity?,
    currentCourses: List<CourseEntity>,
    currentGrades: List<GradeEntity>,
    onDismiss: () -> Unit,
    onExecuteTransition: (newTitle: String, newAcademicYear: String, newTermNumber: Int, newCourses: List<CourseEntity>) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(1) } // 1: Review, 2: New Semester Info, 3: Add Courses, 4: Confirm

    // New semester details
    val nextTermNum = (currentSemester?.termNumber ?: 1) + 1
    var newTermTitle by remember { mutableStateOf("ترم $nextTermNum (نیمسال جدید)") }
    var newAcademicYear by remember { mutableStateOf(currentSemester?.academicYear?.toString() ?: "1403") }
    var newTermNumberStr by remember { mutableStateOf(nextTermNum.toString()) }

    // New courses to register
    val newCourses = remember { mutableStateListOf<CourseEntity>() }
    var bulkText by remember { mutableStateOf("") }
    var showBulkInput by remember { mutableStateOf(false) }

    // Summary calculation for current semester
    val totalUnits = currentCourses.sumOf { it.units }
    val calculatedGpa = remember(currentGrades, currentCourses) {
        val totalGradedUnits = currentGrades.sumOf { it.units }
        if (totalGradedUnits > 0) {
            val weighted = currentGrades.sumOf { (it.midtermGrade + it.finalGrade) * it.units }
            weighted / totalGradedUnits
        } else {
            0.0
        }
    }

    StudentGlassModalSheet(
        onDismiss = onDismiss,
        maxWidth = 640.dp
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .testTag("semester_transition_dialog")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "بستن فرآیند انتقال ترم")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "فرآیند آغاز و انتقال ترم تحصیلی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "مرحله $step از ۴",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { step / 4f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Animated step content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                            .togetherWith(fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                    },
                    label = "SemesterTransitionStepAnimation"
                ) { targetStep ->
                    when (targetStep) {
                        1 -> Step1ReviewCurrentSemester(
                            currentSemester = currentSemester,
                            courses = currentCourses,
                            totalUnits = totalUnits,
                            gpa = calculatedGpa
                        )
                        2 -> Step2NewSemesterMetadata(
                            title = newTermTitle,
                            onTitleChange = { newTermTitle = it },
                            academicYear = newAcademicYear,
                            onAcademicYearChange = { newAcademicYear = it },
                            termNumber = newTermNumberStr,
                            onTermNumberChange = { newTermNumberStr = it }
                        )
                        3 -> Step3AddCourses(
                            courses = newCourses,
                            onAddCourse = { newCourses.add(it) },
                            onRemoveCourse = { newCourses.remove(it) },
                            bulkText = bulkText,
                            onBulkTextChange = { bulkText = it },
                            showBulkInput = showBulkInput,
                            onToggleBulkInput = { showBulkInput = !showBulkInput }
                        )
                        4 -> Step4Confirmation(
                            currentSemester = currentSemester,
                            newTitle = newTermTitle,
                            newYear = newAcademicYear,
                            newCoursesCount = newCourses.size,
                            gpa = calculatedGpa
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = { step-- },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مرحله قبل")
                    }
                } else {
                    TextButton(onClick = onDismiss) {
                        Text("انصراف")
                    }
                }

                if (step < 4) {
                    Button(
                        onClick = {
                            if (step == 2 && newTermTitle.isBlank()) return@Button
                            step++
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("مرحله بعد")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Button(
                        onClick = {
                            val termNum = newTermNumberStr.toIntOrNull() ?: nextTermNum
                            onExecuteTransition(newTermTitle, newAcademicYear, termNum, newCourses.toList())
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        modifier = Modifier.testTag("confirm_semester_transition_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("بایگانی ترم فعلی و آغاز ترم جدید")
                    }
                }
            }
        }
    }
}

@Composable
private fun Step1ReviewCurrentSemester(
    currentSemester: SemesterEntity?,
    courses: List<CourseEntity>,
    totalUnits: Int,
    gpa: Double
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentSemester?.title ?: "ترم جاری",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .background(Amber600.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "در انتظار بایگانی",
                                color = Amber600,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(title = "تعداد دروس", value = "${courses.size} درس")
                        StatItem(title = "مجموع واحدها", value = "$totalUnits واحد")
                        StatItem(
                            title = "معدل ثبت‌شده",
                            value = if (gpa > 0) String.format(Locale.US, "%.2f", gpa) else "ثبت نشده"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "دروس ترم جاری (${courses.size} درس):",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(courses) { course ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = course.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        val courseDetail = listOfNotNull(
                            course.courseCode.takeIf { it.isNotBlank() },
                            course.professor.takeIf { it.isNotBlank() }
                        ).joinToString(" | ").ifBlank { "درس ترم جاری" }
                        Text(
                            text = courseDetail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${course.units} واحد",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "نکته امنیتی: با آغاز ترم جدید، هیچ یک از دروس یا نمرات حذف نخواهند شد و همگی در بخش سوابق تحصیلی در دسترس خواهند بود.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Step2NewSemesterMetadata(
    title: String,
    onTitleChange: (String) -> Unit,
    academicYear: String,
    onAcademicYearChange: (String) -> Unit,
    termNumber: String,
    onTermNumberChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "مشخصات ترم تحصیلی جدید",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "عنوان، سال تحصیلی و شماره ترم جدید را وارد کنید.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("عنوان ترم") },
            placeholder = { Text("مثلاً ترم ۴ (نیمسال دوم ۱۴۰۳-۱۴۰۴)") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("new_semester_title_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = academicYear,
                onValueChange = onAcademicYearChange,
                label = { Text("سال تحصیلی") },
                placeholder = { Text("1403-1404") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = termNumber,
                onValueChange = onTermNumberChange,
                label = { Text("شماره ترم") },
                placeholder = { Text("4") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun Step3AddCourses(
    courses: MutableList<CourseEntity>,
    onAddCourse: (CourseEntity) -> Unit,
    onRemoveCourse: (CourseEntity) -> Unit,
    bulkText: String,
    onBulkTextChange: (String) -> Unit,
    showBulkInput: Boolean,
    onToggleBulkInput: () -> Unit
) {
    var manualName by remember { mutableStateOf("") }
    var manualUnits by remember { mutableStateOf("3") }
    var manualDay by remember { mutableIntStateOf(0) }
    var manualStart by remember { mutableStateOf("08:00") }
    var manualEnd by remember { mutableStateOf("10:00") }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "دروس ترم جدید (${courses.size} درس ثبت‌شده)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onToggleBulkInput) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showBulkInput) "ورود دستی" else "ورود گروهی متنی")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (showBulkInput) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "متن دروس را پیست کنید (مثال: ریاضی ۲ | ۳ واحد | شنبه ۱۰ تا ۱۲)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bulkText,
                            onValueChange = onBulkTextChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            placeholder = { Text("فیزیک ۲ | ۳ واحد | یکشنبه ۸ تا ۱۰\nمعادلات دیفرانسیل | ۳ واحد | دوشنبه ۱۰ تا ۱۲") },
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val parsed = RegistrationTextParser.parse(bulkText)
                                for (draft in parsed) {
                                    val course = CourseEntity(
                                        name = draft.name,
                                        units = draft.units,
                                        colorHex = "#3B82F6",
                                        examDate = draft.examDate,
                                        examTime = draft.examTime
                                    )
                                    onAddCourse(course)
                                }
                                onBulkTextChange("")
                                onToggleBulkInput()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("استخراج و افزودن دروس")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            item {
                // Quick Manual Add Row
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = manualName,
                                onValueChange = { manualName = it },
                                label = { Text("نام درس") },
                                modifier = Modifier.weight(2f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = manualUnits,
                                onValueChange = { manualUnits = it },
                                label = { Text("واحد") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (manualName.isNotBlank()) {
                                    onAddCourse(
                                        CourseEntity(
                                            name = manualName.trim(),
                                            units = manualUnits.toIntOrNull() ?: 3,
                                            colorHex = "#6366F1"
                                        )
                                    )
                                    manualName = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("افزودن به لیست")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // List of registered courses
        if (courses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "هنوز درسی برای ترم جدید اضافه نشده است.\nمی‌توانید با استفاده از فرم بالا یا ورود متنی گروهی دروس را اضافه کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(courses) { course ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = course.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(text = "${course.units} واحد", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onRemoveCourse(course) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف درس ${course.name}", tint = Rose600)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Step4Confirmation(
    currentSemester: SemesterEntity?,
    newTitle: String,
    newYear: String,
    newCoursesCount: Int,
    gpa: Double
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Emerald600.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .border(1.dp, Emerald600.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Archive, contentDescription = null, tint = Emerald600)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "آماده‌سازی انتقال و بایگانی اتمیک",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Emerald600
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "با تأیید نهایی، عملیات زیر در پایگاه‌داده به صورت تراکنش یکپارچه انجام خواهد شد:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ActionItemRow(
            number = "۱",
            title = "بایگانی ترم «${currentSemester?.title ?: "فعلی"}»",
            subtitle = "محاسبه و حفظ معدل (${if (gpa > 0) String.format(Locale.US, "%.2f", gpa) else "بدون تغییر"}) و واحدهای گذرانده"
        )
        ActionItemRow(
            number = "۲",
            title = "انتقال دروس ترم قبل به تاریخچه تحصیلی",
            subtitle = "حفظ تمام نمرات، غیبت‌ها و یادداشت‌ها بدون حذف داده"
        )
        ActionItemRow(
            number = "۳",
            title = "فعال‌سازی ترم جدید «$newTitle»",
            subtitle = "ثبت $newCoursesCount درس جدید و ایجاد کارنامه و سیستم حضور و غیاب مجزا"
        )
    }
}

@Composable
private fun ActionItemRow(number: String, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
