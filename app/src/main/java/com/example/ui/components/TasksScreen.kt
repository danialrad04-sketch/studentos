package com.example.ui.components

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.TaskEntity
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StudentShapeTokens
import com.example.ui.theme.StudentSpacing

@Composable
fun TasksScreen(
    tasks: List<TaskEntity>,
    courses: List<CourseEntity> = emptyList(),
    onAddTask: () -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onOpenCourseWorkspace: ((CourseEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("all") }

    val filteredTasks = remember(tasks, selectedFilter) {
        when (selectedFilter) {
            "pending" -> tasks.filter { !it.isCompleted }
            "completed" -> tasks.filter { it.isCompleted }
            else -> tasks
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header & Add Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "تکالیف و پروژه‌ها",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "تکالیف، پروژه‌ها و گزارش‌کارهای تحویلی",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onAddTask,
                shape = StudentShapeTokens.Card,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "افزودن تکلیف",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(StudentSpacing.Md))

        // Filter chips
        Row(horizontalArrangement = Arrangement.spacedBy(StudentSpacing.Sm)) {
            FilterChip(
                selected = selectedFilter == "all",
                onClick = { selectedFilter = "all" },
                label = { Text("همه (${tasks.size})", style = MaterialTheme.typography.labelMedium) },
                shape = StudentShapeTokens.Compact,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.minimumInteractiveComponentSize()
            )
            FilterChip(
                selected = selectedFilter == "pending",
                onClick = { selectedFilter = "pending" },
                label = { Text("در انتظار (${tasks.count { !it.isCompleted }})", style = MaterialTheme.typography.labelMedium) },
                shape = MaterialTheme.shapes.small,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.minimumInteractiveComponentSize()
            )
            FilterChip(
                selected = selectedFilter == "completed",
                onClick = { selectedFilter = "completed" },
                label = { Text("انجام شده (${tasks.count { it.isCompleted }})", style = MaterialTheme.typography.labelMedium) },
                shape = MaterialTheme.shapes.small,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.minimumInteractiveComponentSize()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (tasks.isEmpty()) {
            ActionableEmptyState(
                title = "لیست تکالیف و پروژه‌ها خالی است",
                description = "تمرین‌ها، گزارش‌کار آزمایشگاه و پروژه‌های کلاسی را ثبت کنید تا تاریخ تحویل آن‌ها به صورت خودکار رصد شود.",
                icon = Icons.Default.CalendarToday,
                primaryActionTitle = "ثبت اولین تکلیف",
                onPrimaryAction = onAddTask
            )
        } else if (filteredTasks.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(StudentSpacing.Xxl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "هیچ تکلیفی در این دسته‌بندی یافت نشد.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredTasks.forEach { task ->
                    val matchedCourse = courses.find { it.name == task.courseName }
                    TaskCard(
                        task = task,
                        onToggle = { onToggleTask(task) },
                        onDelete = { onDeleteTask(task) },
                        onOpenWorkspace = if (matchedCourse != null && onOpenCourseWorkspace != null) {
                            { onOpenCourseWorkspace(matchedCourse) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: TaskEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onOpenWorkspace: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val containerColor = if (task.isCompleted) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .tactileClickable { onToggle() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = StudentSpacing.Md, vertical = StudentSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.width(StudentSpacing.Sm))

                Column {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = if (onOpenWorkspace != null) {
                                Modifier.clickable { onOpenWorkspace() }
                            } else Modifier
                        ) {
                            Text(
                                text = if (onOpenWorkspace != null) "${task.courseName} ↗" else task.courseName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (task.dueDate.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "موعد: ${task.dueDate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف تکلیف",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(name = "TasksScreen Light", showBackground = true)
@Composable
private fun TasksScreenPreviewLight() {
    MyApplicationTheme(darkTheme = false) {
        TasksScreen(
            tasks = listOf(
                TaskEntity(id = 1, title = "پروژه درس طراحی راکتور", courseName = "طراحی راکتور", dueDate = "۱۴۰۳/۰۸/۲۰", isCompleted = false),
                TaskEntity(id = 2, title = "تمرین سری دوم انتقال حرارت", courseName = "انتقال حرارت", dueDate = "۱۴۰۳/۰۸/۱۵", isCompleted = true)
            ),
            onAddTask = {},
            onToggleTask = {},
            onDeleteTask = {}
        )
    }
}

@Preview(name = "TasksScreen Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TasksScreenPreviewDark() {
    MyApplicationTheme(darkTheme = true) {
        TasksScreen(
            tasks = listOf(
                TaskEntity(id = 1, title = "پروژه درس طراحی راکتور", courseName = "طراحی راکتور", dueDate = "۱۴۰۳/۰۸/۲۰", isCompleted = false),
                TaskEntity(id = 2, title = "تمرین سری دوم انتقال حرارت", courseName = "انتقال حرارت", dueDate = "۱۴۰۳/۰۸/۱۵", isCompleted = true)
            ),
            onAddTask = {},
            onToggleTask = {},
            onDeleteTask = {}
        )
    }
}
