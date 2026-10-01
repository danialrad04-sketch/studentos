package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseSessionEntity
import com.example.ui.theme.NumericDisplayStat
import com.example.ui.theme.StudentOsShapes
import com.example.ui.theme.studentColors

@Composable
fun ModernScheduleCourseCard(
    course: CourseEntity,
    session: CourseSessionEntity,
    isPrimarySession: Boolean,
    hasConflict: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?,
    onOpenWorkspace: (() -> Unit)?
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val fallback = MaterialTheme.colorScheme.primary
    val accent = remember(course.colorHex, fallback) {
        runCatching { Color(android.graphics.Color.parseColor(course.colorHex)) }.getOrDefault(fallback)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp, max = 136.dp)
            .tactileClickable(onClick = onClick),
        shape = RoundedCornerShape(StudentOsShapes.mediumCard),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.26f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 2.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(13.dp),
                color = accent.copy(alpha = 0.10f),
                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.20f))
            ) {
                Column(
                    Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Schedule, null, tint = accent, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(
                        "\\u200E" + session.start + "\\u200E",
                        style = NumericDisplayStat,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = accent
                    )
                    Text(
                        "\\u200E" + session.end + "\\u200E",
                        style = NumericDisplayStat,
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    StudentCardTitle(
                        text = course.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black
                    )
                    if (!isPrimarySession) {
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                "جلسه ۲",
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                    ) {
                        StudentCardMeta(
                            text = course.units.toString() + " واحد",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    if (course.professor.isNotBlank()) {
                        Text(
                            course.professor,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (session.location.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            session.location,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (hasConflict) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.studentColors.attendanceCritical.copy(alpha = 0.10f)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.WarningAmber,
                                null,
                                tint = MaterialTheme.studentColors.attendanceCritical,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "تداخل",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.studentColors.attendanceCritical
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onOpenWorkspace != null) {
                        IconButton(
                            onClick = onOpenWorkspace,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.Default.OpenInFull,
                                "باز کردن فضای درس",
                                tint = accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            "ویرایش درس",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (onDelete != null) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                "حذف درس",
                                tint = MaterialTheme.studentColors.attendanceCritical,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = {
                Icon(
                    Icons.Default.WarningAmber,
                    null,
                    tint = MaterialTheme.studentColors.attendanceCritical
                )
            },
            title = { Text("حذف این درس؟") },
            text = { Text("این عملیات از برنامه هفتگی حذفش می‌کند. برای ادامه تأیید کنید.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.studentColors.attendanceCritical
                    )
                ) { Text("حذف درس") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("انصراف") }
            }
        )
    }

    }
}
