package com.example.ui.components

import com.example.domain.model.AcademicCommandEngine
import com.example.ui.theme.AcademicOlive
import com.example.ui.theme.AcademicNavy
import com.example.ui.theme.StudentSpacing
import com.example.ui.theme.StudentShapeTokens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.CourseState
import com.example.domain.model.EvaluatedCurriculumCourse
import com.example.domain.model.GlobalSearchResult
import com.example.ui.theme.Amber500
import com.example.ui.theme.Amber600
import com.example.ui.theme.AcademicOlive
import com.example.ui.theme.Rose600

/**
 * Phase 24: Spotlight Global Search Dialog.
 * Omnibox search supporting natural queries over Courses, Curriculum chart, Tasks, Exams, and Formula Notes.
 */
@Composable
fun SpotlightSearchDialog(
    searchResults: List<GlobalSearchResult>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelectCourse: (String) -> Unit,
    onSelectCurriculumCourse: ((EvaluatedCurriculumCourse) -> Unit)? = null,
    onNavigateToTasks: () -> Unit,
    onNavigateToExams: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onExecuteCommand: (com.example.domain.model.AcademicCommand) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<String?>("ALL") }
    val resolvedCommand = remember(searchQuery) { AcademicCommandEngine.resolve(searchQuery) }

    val filteredResults = remember(searchResults, selectedFilter) {
        if (selectedFilter == null || selectedFilter == "ALL") {
            searchResults
        } else {
            searchResults.filter { result ->
                when (selectedFilter) {
                    "COURSE" -> result is GlobalSearchResult.CourseItem && result.state == CourseState.CURRENT
                    "CURRICULUM" -> result is GlobalSearchResult.CourseItem && result.state != CourseState.CURRENT
                    "TASK" -> result is GlobalSearchResult.TaskItem
                    "EXAM" -> result is GlobalSearchResult.ExamItem
                    "NOTE" -> result is GlobalSearchResult.NoteFormulaItem
                    else -> true
                }
            }
        }
    }

    StudentGlassModalSheet(
        onDismiss = onDismiss,
        maxWidth = 620.dp
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
        ) {
            // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = StudentShapeTokens.Compact,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "جستجوی اسپات‌لایت سراسری",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "جستجو میان دروس، چارت سرفصل‌ها، تکالیف، امتحانات و فرمول‌ها",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن جستجو",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Omnibox Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "جستجوی نام درس، استاد، کد درس، تمرین یا فرمول...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "پاک کردن متن جستجو",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = StudentShapeTokens.Card,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                )

                if (resolvedCommand != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = StudentShapeTokens.Compact,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(StudentSpacing.Md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NorthEast,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(StudentSpacing.Sm))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Command پیدا شد",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    resolvedCommand.titleFa,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Button(
                                onClick = {
                                    onExecuteCommand(resolvedCommand)
                                    onDismiss()
                                },
                                modifier = Modifier.heightIn(min = 48.dp),
                                shape = StudentShapeTokens.Compact
                            ) {
                                Text("اجرا")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Categorization Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filters = listOf(
                        "ALL" to "همه",
                        "COURSE" to "دروس فعال",
                        "CURRICULUM" to "چارت مصوب",
                        "TASK" to "تکالیف",
                        "EXAM" to "امتحانات",
                        "NOTE" to "فرمول‌ها"
                    )

                    filters.forEach { (key, label) ->
                        val isSelected = selectedFilter == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = key },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)) },
                            shape = StudentShapeTokens.Compact,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                selectedBorderColor = Color.Transparent
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Results Counter Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "پیشنهادات سریع و پرکاربرد" else "نتایج جستجو",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${filteredResults.size} مورد یافت شد",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Results Container
                if (filteredResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "هیچ نتیجه‌ای متناسب با «$searchQuery» یافت نشد",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "می‌توانید کلمه دیگری را امتحان کنید یا فیلتر را روی «همه» قرار دهید.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredResults, key = { result ->
                            when (result) {
                                is GlobalSearchResult.CourseItem -> "course_${result.courseId}_${result.code}"
                                is GlobalSearchResult.TaskItem -> "task_${result.taskId}"
                                is GlobalSearchResult.ExamItem -> "exam_${result.examId}"
                                is GlobalSearchResult.NoteFormulaItem -> "note_${result.courseName}_${result.title}"
                            }
                        }) { result ->
                            SearchResultItemCard(
                                result = result,
                                onClick = {
                                    when (result) {
                                        is GlobalSearchResult.CourseItem -> {
                                            onSelectCourse(result.courseId)
                                            onDismiss()
                                        }
                                        is GlobalSearchResult.TaskItem -> {
                                            onNavigateToTasks()
                                            onDismiss()
                                        }
                                        is GlobalSearchResult.ExamItem -> {
                                            onNavigateToExams()
                                            onDismiss()
                                        }
                                        is GlobalSearchResult.NoteFormulaItem -> {
                                            onNavigateToNotes()
                                            onDismiss()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
    }
}

@Composable
private fun SearchResultItemCard(
    result: GlobalSearchResult,
    onClick: () -> Unit
) {
    val title = when (result) {
        is GlobalSearchResult.CourseItem -> result.name
        is GlobalSearchResult.TaskItem -> result.title
        is GlobalSearchResult.ExamItem -> "آزمون ${result.courseName}"
        is GlobalSearchResult.NoteFormulaItem -> result.title
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = "$title، برای مشاهده لمس کنید"
            }
            .tactileClickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = CardDefaults.outlinedCardBorder().copy(width = 0.8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category Icon Capsule
                val (icon, tintColor, badgeText) = when (result) {
                    is GlobalSearchResult.CourseItem -> {
                        if (result.state == CourseState.CURRENT) {
                            Triple(Icons.AutoMirrored.Filled.MenuBook, MaterialTheme.colorScheme.primary, "درس ترم")
                        } else {
                            Triple(Icons.Default.Bookmark, Amber600, "چارت مصوب")
                        }
                    }
                    is GlobalSearchResult.TaskItem -> Triple(Icons.AutoMirrored.Filled.FormatListBulleted, AcademicOlive, "تکلیف")
                    is GlobalSearchResult.ExamItem -> Triple(Icons.Default.Warning, Rose600, "امتحان")
                    is GlobalSearchResult.NoteFormulaItem -> Triple(Icons.Default.AutoAwesome, MaterialTheme.colorScheme.primary, "فرمول")
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = tintColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tintColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = tintColor.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = tintColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val subtitle = when (result) {
                        is GlobalSearchResult.CourseItem -> "کد درس ${result.code} · ${result.units} واحد · وضعیت: ${result.state}"
                        is GlobalSearchResult.TaskItem -> "درس: ${result.courseName} · تحویل: ${result.deadline}"
                        is GlobalSearchResult.ExamItem -> "تاریخ: ${result.examDate} ساعت ${result.examTime}"
                        is GlobalSearchResult.NoteFormulaItem -> "درس: ${result.courseName} · فرمول: ${result.content}"
                    }

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.NorthEast,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
