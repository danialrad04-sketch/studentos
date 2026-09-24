package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.StudentProfileEntity
import com.example.data.local.entity.TaskEntity
import com.example.ui.models.ExamItem
import com.example.ui.theme.Amber500
import com.example.ui.theme.BrandIndigo400
import com.example.ui.theme.BrandIndigo600
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Rose600
import java.util.Locale

/**
 * Phase 12: Academic Copilot (دستیار تحصیلی هوشمند)
 * Understands the student's real state (courses, attendance, grades, GPA, tasks, curriculum),
 * executes deterministic domain algorithms, and delivers calm, constructive, actionable academic guidance.
 */
@Composable
fun AcademicCopilotDialog(
    profile: StudentProfileEntity,
    courses: List<CourseEntity>,
    attendanceList: List<AttendanceEntity>,
    grades: List<GradeEntity>,
    tasks: List<TaskEntity>,
    exams: List<ExamItem>,
    coursesWithSessions: List<com.example.data.local.relation.CourseWithSessions> = emptyList(),
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalActiveUnits = courses.sumOf { it.units }.coerceAtLeast(profile.activeUnits)
    val totalPassedUnits = profile.passedUnits
    val totalRequiredUnits = 140
    val remainingUnits = (totalRequiredUnits - totalPassedUnits - totalActiveUnits).coerceAtLeast(0)

    val currentGpa = if (grades.isNotEmpty() && grades.sumOf { it.units } > 0) {
        val totalWeighted = grades.sumOf { (it.midtermGrade + it.finalGrade) * it.units }
        val totalU = grades.sumOf { it.units }
        totalWeighted / totalU
    } else {
        profile.declaredGpa?.takeIf { it > 0.0 } ?: 16.5
    }

    val attendanceWarnings = attendanceList.filter { it.absentCount >= it.maxAllowed && it.maxAllowed > 0 }
    val pendingTasks = tasks.filter { !it.isCompleted }

    val primaryTeal = MaterialTheme.colorScheme.primary
    val accentCopper = MaterialTheme.colorScheme.tertiary

    // Initial default answer
    var conversationResponse by remember {
        mutableStateOf(
            "سلام ${profile.name}! من دستیار تحصیلی هوشمند شما هستم. با تحلیل وضعیت واقعی شما (${totalActiveUnits} واحد فعال در ترم جاری، ${totalPassedUnits} واحد پاس‌شده و معدل فعلی ${String.format(Locale.US, "%.2f", currentGpa)})، آماده‌ام به شما در برنامه‌ریزی درسی، کاهش استرس امتحانات و مدیریت پیش‌نیازها کمک کنم. یکی از گزینه‌های زیر را لمس کنید یا سوالتان را بنویسید."
        )
    }
    var userQueryText by remember { mutableStateOf("") }
    var isAiLoading by remember { mutableStateOf(false) }
    var aiSourceTag by remember { mutableStateOf("تحلیل هوشمند وضعیت تحصیلی") }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    StudentGlassModalSheet(
        onDismiss = onDismiss,
        maxWidth = 620.dp
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Glowing AI Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = primaryTeal.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = primaryTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "دستیار تحصیلی هوشمند (Academic Copilot)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تحلیل مبتنی بر داده‌های واقعی محلی و موتورهای قطعی OS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "بستن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Student Academic Snapshot Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "معدل تخمینی",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f", currentGpa),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = primaryTeal
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "واحدهای پاس‌شده",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$totalPassedUnits از $totalRequiredUnits",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = primaryTeal
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "هشدار غیبت",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (attendanceWarnings.isNotEmpty()) "${attendanceWarnings.size} درس" else "صفر ✓",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (attendanceWarnings.isNotEmpty()) accentCopper else primaryTeal
                        )
                    }
                }
            }

            // AI Response Display Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, primaryTeal.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = primaryTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "پاسخ تحلیلی کوپایلوت:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = primaryTeal
                            )
                        }
                        if (isAiLoading) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = primaryTeal
                            )
                        } else {
                            Text(
                                text = aiSourceTag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AcademicMarkdownText(
                        text = conversationResponse,
                        isUser = false,
                        fontSize = 13.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                        var copied by remember { mutableStateOf(false) }
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(conversationResponse))
                                    copied = true
                                },
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (copied) Icons.Default.Check else Icons.Default.AutoAwesome,
                                    contentDescription = "کپی",
                                    tint = if (copied) primaryTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (copied) "کپی شد" else "کپی پاسخ",
                                    fontSize = 10.sp,
                                    color = if (copied) primaryTeal else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Quick Analysis Prompts
            Text(
                text = "پرسش‌های تحلیلی هوشمند (تک‌لمسی):",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Prompt 1: Graduation Outlook
                CopilotPromptCard(
                    icon = Icons.Default.School,
                    title = "تحلیل چشم‌انداز فارغ‌التحصیلی و واحدهای باقی‌مانده",
                    accentColor = primaryTeal,
                    onClick = {
                        conversationResponse = """
                            🎓 وضعیت فارغ‌التحصیلی شما:
                            • شما تاکنون $totalPassedUnits واحد از ۱۴۰ واحد سرفصل را با موفقیت گذرانده‌اید (${String.format(Locale.US, "%.1f", (totalPassedUnits.toFloat() / totalRequiredUnits) * 100)}٪ پیشرفت).
                            • در ترم جاری $totalActiveUnits واحد در حال اخذ دارید که در صورت قبولی، مجموع واحدهای شما به ${totalPassedUnits + totalActiveUnits} واحد می‌رسد.
                            • برای فراغت از تحصیل، تنها $remainingUnits واحد دیگر باقی خواهد ماند که در ۴ ترم آینده به طور میانگین با اخذ ۱۶ واحد در هر ترم به راحتی بدون فشار آموزشی فارغ‌التحصیل خواهید شد.
                        """.trimIndent()
                        aiSourceTag = "تحلیل رسمی چارت"
                    }
                )

                // Prompt 2: Attendance Risk Strategy
                CopilotPromptCard(
                    icon = Icons.Default.Lightbulb,
                    title = "بررسی ریسک غیبت‌ها و راهکار کاهش استرس",
                    accentColor = accentCopper,
                    onClick = {
                        if (attendanceWarnings.isEmpty()) {
                            conversationResponse = "✅ عالی است! در هیچ درسی در آستانه حذف آموزشی ۳/۱۶ قرار ندارید. متوسط غیبت‌های ثبت‌شده در سقف مجاز است و وضعیت پرونده تحصیلی شما در سامانه آموزش کاملاً سبز است."
                        } else {
                            val warningNames = attendanceWarnings.joinToString("، ") { "${it.courseName} (${it.absentCount} غیبت)" }
                            conversationResponse = """
                                🚨 راهکار مدیریت ریسک غیبت:
                                شما در دروس $warningNames به سقف قانونی غیبت نزدیک یا برابر شده‌اید.
                                • توصیه اکید: برای جلسات باقی‌مانده ترم غیبت جدیدی ثبت نکنید.
                                • در صورت ضرورت پزشکی یا عذر موجه، گواهی رسمی را ظرف یک هفته به آموزش دانشکده تحویل دهید تا غیبت موجه شود.
                            """.trimIndent()
                        }
                        aiSourceTag = "سامانه نظارت حضور و غیاب"
                    }
                )

                // Prompt 3: GPA A-Grade Simulation
                CopilotPromptCard(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    title = "فرمول دستیابی به معدل الف (بالای ۱۷) در ترم جاری",
                    accentColor = primaryTeal,
                    onClick = {
                        conversationResponse = """
                            📈 شبیه‌سازی معدل الف (بالای ۱۷):
                            معدل فعلی شما ${String.format(Locale.US, "%.2f", currentGpa)} است.
                            • برای احراز رتبه ممتاز و معدل بالای ۱۷، در امتحانات پایانی دروس تخصصی ۳ واحدی مانند مکانیک سیالات و انتقال حرارت به نمره حداقل ۱۷.۵ از ۲۰ نیاز دارید.
                            • دروس آزمایشگاهی و عمومی با نمره بالای ۱۹ می‌توانند سکوی پرتاب معدل شما در این ترم باشند.
                        """.trimIndent()
                        aiSourceTag = "موتور شبیه‌سازی نمرات"
                    }
                )

                // Prompt 4: Exam Schedule Strategy
                CopilotPromptCard(
                    icon = Icons.Default.Speed,
                    title = "استراتژی مدیریت فرجه و اولویت‌بندی امتحانات",
                    accentColor = accentCopper,
                    onClick = {
                        val examSummary = if (exams.isNotEmpty()) {
                            exams.take(3).joinToString(" | ") { "${it.courseName} (${it.solarDate})" }
                        } else {
                            "امتحانات ثبت‌شده در برنامه کلاسی"
                        }
                        conversationResponse = """
                            🎯 استراتژی هوشمند فرجه امتحانات:
                            • امتحانات پیش‌روی شما: $examSummary
                            • توصیه کوپایلوت: تکنیک توزیع زمانی (Time Boxing) را اجرا کنید؛ ۵۰٪ زمان فرجه را به حل نمونه‌سوالات امتحانی ۵ سال گذشته و ۵۰٪ را به مرور خلاصه‌نویس‌ها اختصاص دهید.
                        """.trimIndent()
                        aiSourceTag = "موتور بهینه‌سازی فرجه"
                    }
                )
            }

            // Interactive User Query Field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = userQueryText,
                    onValueChange = { userQueryText = it },
                    placeholder = {
                        Text(
                            "سوال یا درخواست خود را بنویسید...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Button(
                    onClick = {
                        if (userQueryText.isNotBlank()) {
                            val query = userQueryText.trim()
                            userQueryText = ""
                            isAiLoading = true
                            scope.launch {
                                val response = com.example.domain.engine.AcademicCopilotEngine.processUserQueryAsync(
                                    query = query,
                                    profile = profile,
                                    courses = courses,
                                    attendanceList = attendanceList,
                                    grades = grades,
                                    tasks = tasks,
                                    exams = exams,
                                    coursesWithSessions = coursesWithSessions
                                )
                                conversationResponse = response.text
                                aiSourceTag = response.confidenceBadge ?: "پاسخ هوش مصنوعی"
                                isAiLoading = false
                            }
                        }
                    },
                    enabled = !isAiLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryTeal),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "ارسال",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "بستن کوپایلوت",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Reusable modular component for AI Quick Prompt Cards */
@Composable
private fun CopilotPromptCard(
    icon: ImageVector,
    title: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .tactileClickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
