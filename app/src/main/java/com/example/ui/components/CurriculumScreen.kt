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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CourseState
import com.example.domain.model.EvaluatedCurriculumCourse
import com.example.ui.models.CurriculumMatchUiState
import com.example.ui.models.SemesterCurriculum
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Rose600

/**
 * Integrated Curriculum Screen (Phase 4).
 * Supports both evaluated CurriculumMatchUiState (domain-verified with course status symbols ✓, ◉, →, 🔒, ○, ?)
 * and fallback legacy static SemesterCurriculum list for backward compatibility.
 */
@Composable
fun CurriculumScreen(
    matchState: CurriculumMatchUiState? = null,
    curriculumList: List<SemesterCurriculum> = emptyList(),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("curriculum_screen")
    ) {
        // Header
        val chartTitle = if (matchState is CurriculumMatchUiState.Ready) {
            matchState.output.version.title
        } else {
            "چارت کارشناسی مهندسی شیمی"
        }

        val totalUnits = if (matchState is CurriculumMatchUiState.Ready) {
            "${matchState.output.version.totalCreditsRequired} واحد مصوب"
        } else {
            "۱۴۰ واحد مصوب"
        }

        Column {
            Text(
                text = chartTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "وضعیت زنده دروس و زنجیره پیش‌نیازها · $totalUnits",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Status Symbol Legend
        CurriculumStatusLegend()

        Spacer(modifier = Modifier.height(14.dp))

        var selectedStateFilter by remember { mutableStateOf<CourseState?>(null) }
        var selectedTypeFilter by remember { mutableStateOf<String?>(null) }

        // Filter Bar (Phase 10)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedStateFilter == null && selectedTypeFilter == null,
                onClick = {
                    selectedStateFilter = null
                    selectedTypeFilter = null
                },
                label = { Text("همه دروس", fontSize = 10.5.sp) }
            )
            FilterChip(
                selected = selectedStateFilter == CourseState.PASSED,
                onClick = { selectedStateFilter = if (selectedStateFilter == CourseState.PASSED) null else CourseState.PASSED },
                label = { Text("✓ پاس‌شده", fontSize = 10.5.sp) }
            )
            FilterChip(
                selected = selectedStateFilter == CourseState.CURRENT,
                onClick = { selectedStateFilter = if (selectedStateFilter == CourseState.CURRENT) null else CourseState.CURRENT },
                label = { Text("◉ ترم جاری", fontSize = 10.5.sp) }
            )
            FilterChip(
                selected = selectedStateFilter == CourseState.AVAILABLE,
                onClick = { selectedStateFilter = if (selectedStateFilter == CourseState.AVAILABLE) null else CourseState.AVAILABLE },
                label = { Text("→ قابل اخذ", fontSize = 10.5.sp) }
            )
            FilterChip(
                selected = selectedStateFilter == CourseState.BLOCKED,
                onClick = { selectedStateFilter = if (selectedStateFilter == CourseState.BLOCKED) null else CourseState.BLOCKED },
                label = { Text("🔒 قفل پیش‌نیاز", fontSize = 10.5.sp) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (matchState) {
            is CurriculumMatchUiState.Loading -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = StudentShapeTokens.Card,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                        Text(
                            text = "در حال تطبیق دروس با چارت مصوب...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            is CurriculumMatchUiState.NotFound -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(width = 0.8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Rose600, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "چارت معتبر یافت نشد: ${matchState.explanation}", style = MaterialTheme.typography.bodySmall, color = Rose600)
                    }
                }
            }
            is CurriculumMatchUiState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = matchState.message,
                        color = Rose600,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            is CurriculumMatchUiState.Ready -> {
                // Production Evaluated Matrix
                val matrix = matchState.groupedBySemester
                val sortedSemesters = matrix.keys.sorted()
                val totalFilteredCourses = sortedSemesters.sumOf { semNumber ->
                    val raw = matrix[semNumber] ?: emptyList()
                    if (selectedStateFilter != null) raw.count { it.state == selectedStateFilter } else raw.size
                }

                if (totalFilteredCourses == 0 && selectedStateFilter != null) {
                    val filterName = when (selectedStateFilter) {
                        CourseState.PASSED -> "گذرانده‌شده"
                        CourseState.CURRENT -> "ترم جاری"
                        CourseState.AVAILABLE -> "قابل اخذ"
                        CourseState.BLOCKED -> "قفل پیش‌نیاز"
                        else -> "انتخاب‌شده"
                    }
                    ActionableEmptyState(
                        icon = Icons.Default.Map,
                        title = "درسی با وضعیت «$filterName» یافت نشد",
                        description = "هیچ درسی در چارت تحصیلی با این فیلتر مطابقت ندارد.",
                        primaryActionTitle = "نمایش همه دروس چارت",
                        onPrimaryAction = { selectedStateFilter = null }
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        sortedSemesters.forEach { semNumber ->
                            val rawCoursesInSem = matrix[semNumber] ?: emptyList()
                            val coursesInSem = if (selectedStateFilter != null) {
                                rawCoursesInSem.filter { it.state == selectedStateFilter }
                            } else {
                                rawCoursesInSem
                            }
                            if (coursesInSem.isNotEmpty()) {
                                EvaluatedSemesterCard(
                                    semesterNumber = semNumber,
                                    courses = coursesInSem
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                // Fallback to legacy static curriculumList
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    curriculumList.forEachIndexed { index, semester ->
                        LegacySemesterCard(semester = semester, termIndex = index + 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun CurriculumStatusLegend() {
    Surface(
        shape = StudentShapeTokens.Compact,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(symbol = "✓", label = "پاس شده", color = Emerald600)
            LegendItem(symbol = "◉", label = "ترم جاری", color = Amber600)
            LegendItem(symbol = "→", label = "قابل اخذ", color = MaterialTheme.colorScheme.primary)
            LegendItem(symbol = "🔒", label = "قفل پیش‌نیاز", color = Rose600)
            LegendItem(symbol = "○", label = "باقیمانده", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LegendItem(symbol: String, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = symbol, fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = color)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun EvaluatedSemesterCard(
    semesterNumber: Int,
    courses: List<EvaluatedCurriculumCourse>,
    modifier: Modifier = Modifier
) {
    val totalSemUnits = courses.sumOf { it.units }
    val isCurrentTerm = courses.any { it.state == CourseState.CURRENT }

    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = StudentShapeTokens.Card,
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTerm) primaryColor.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isCurrentTerm) {
            CardDefaults.outlinedCardBorder().copy(width = 1.2.dp, brush = androidx.compose.ui.graphics.SolidColor(primaryColor))
        } else {
            CardDefaults.outlinedCardBorder().copy(width = 0.8.dp)
        }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isCurrentTerm) primaryColor else outlineVariant)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ترم $semesterNumber",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp,
                        color = if (isCurrentTerm) primaryColor else MaterialTheme.colorScheme.onSurface
                    )
                    if (isCurrentTerm) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = primaryColor
                        ) {
                            Text(
                                text = "ترم جاری شما 📌",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "$totalSemUnits واحد",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Courses List
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                courses.forEach { course ->
                    EvaluatedCourseRow(course = course)
                }
            }
        }
    }
}

@Composable
private fun EvaluatedCourseRow(course: EvaluatedCurriculumCourse) {
    var isExpanded by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val (statusSymbol, statusColor, statusBg) = when (course.state) {
        CourseState.PASSED -> Triple("✓", Emerald600, Emerald600.copy(alpha = 0.08f))
        CourseState.CURRENT -> Triple("◉", Amber600, Amber600.copy(alpha = 0.12f))
        CourseState.AVAILABLE -> Triple("→", primaryColor, primaryColor.copy(alpha = 0.08f))
        CourseState.BLOCKED -> Triple("🔒", Rose600, Rose600.copy(alpha = 0.08f))
        CourseState.PLANNED -> Triple("○", primaryColor, primaryColor.copy(alpha = 0.05f))
        CourseState.FAILED -> Triple("✗", Rose600, Rose600.copy(alpha = 0.1f))
        CourseState.DROPPED -> Triple("–", onSurfaceVariant, surfaceVariant.copy(alpha = 0.6f))
        CourseState.UNKNOWN -> Triple("?", onSurfaceVariant, surfaceVariant.copy(alpha = 0.4f))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(statusBg)
            .clickable {
                if (course.blockedReason != null) {
                    isExpanded = !isExpanded
                }
            }
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = statusSymbol,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = statusColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = course.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (course.courseType.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "· ${course.courseType}",
                        fontSize = 10.sp,
                        color = onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${course.units} واحد",
                    fontSize = 10.5.sp,
                    color = onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                if (course.blockedReason != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "جزییات پیش‌نیاز",
                        tint = Rose600,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Expandable Blocked Explanation
        AnimatedVisibility(visible = isExpanded && course.blockedReason != null) {
            course.blockedReason?.let { reason ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "علت عدم امکان اخذ:",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Rose600
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = reason.explanation,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegacySemesterCard(
    semester: SemesterCurriculum,
    termIndex: Int,
    modifier: Modifier = Modifier
) {
    val isCurrent = semester.isCurrent
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) primaryColor.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isCurrent) {
            CardDefaults.outlinedCardBorder().copy(width = 1.2.dp, brush = androidx.compose.ui.graphics.SolidColor(primaryColor))
        } else {
            CardDefaults.outlinedCardBorder().copy(width = 0.8.dp)
        }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = semester.title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "ترم جاری شما 📌",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${semester.units} واحد",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                semester.courses.forEach { courseName ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = courseName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
