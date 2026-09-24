package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.example.ui.components.datepicker.JalaliDatePickerField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseSessionEntity
import com.example.ui.theme.studentColors

private val DAY_LABELS = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه")

private val PALETTE_COLORS = listOf(
    "#0D9488" to "فیروزه‌ای دژ‌پیکر",
    "#06B6D4" to "فیروزه‌ای نئونی",
    "#10B981" to "سبز زمردی",
    "#6366F1" to "نیلی مدرن",
    "#8B5CF6" to "بنفش نئونی",
    "#D97757" to "مسی گرم",
    "#F59E0B" to "کهربایی آذرخش",
    "#EF4444" to "سرخ مرجانی"
)

private val STANDARD_TIME_SLOTS = listOf(
    "08:00" to "10:00",
    "10:00" to "12:00",
    "13:30" to "15:30",
    "15:30" to "17:30",
    "17:30" to "19:30"
)

private val POPULAR_COURSE_PRESETS = listOf(
    "ریاضی عمومی ۱", "ریاضی عمومی ۲", "معادلات دیفرانسیل", "فیزیک ۱", "فیزیک ۲",
    "برنامه‌نویسی مقدماتی", "ساختمان داده‌ها", "طراحی الگوریتم", "شبکه‌های کامپیوتری",
    "سیستم‌های عامل", "مدار منطقی", "معماری کامپیوتر", "آمار و احتمالات",
    "پایگاه داده‌ها", "اندیشه اسلامی ۱", "اخلاق اسلامی", "زبان عمومی",
    "فارسی عمومی", "تربیت بدنی"
)

private val FieldShape = RoundedCornerShape(12.dp)
private val ChipShape = RoundedCornerShape(10.dp)
private val CardShape = RoundedCornerShape(16.dp)
private val ButtonShape = RoundedCornerShape(12.dp)

data class SessionDraft(
    val id: String = java.util.UUID.randomUUID().toString(),
    var day: Int = 0,
    var start: String = "08:00",
    var end: String = "10:00",
    var location: String = ""
)

/**
 * 2026 Material 3 Expressive Minimalist Course Editor Dialog.
 * Supports multi-session weekly scheduling per logical course.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditCourseDialog(
    initialCourse: CourseEntity?,
    initialSessions: List<CourseSessionEntity> = emptyList(),
    existingCourses: List<CourseEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (CourseEntity) -> Unit = {},
    onSaveWithSessions: ((CourseEntity, List<CourseSessionEntity>) -> Unit)? = null,
    onDelete: ((String) -> Unit)? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: مشخصات و جلسات هفتگی, 1: استاد، آزمون و جزئیات

    var name by remember { mutableStateOf(initialCourse?.name ?: "") }
    var courseCode by remember { mutableStateOf(initialCourse?.courseCode ?: "") }
    var professor by remember { mutableStateOf(initialCourse?.professor ?: "") }
    var units by remember { mutableIntStateOf(initialCourse?.units ?: 3) }
    var selectedColorHex by remember { mutableStateOf(initialCourse?.colorHex ?: "#0D9488") }
    var examDate by remember { mutableStateOf(initialCourse?.examDate ?: "") }
    var examTime by remember { mutableStateOf(initialCourse?.examTime ?: "09:00") }
    var examLocation by remember { mutableStateOf(initialCourse?.examLocation ?: "") }
    var notes by remember { mutableStateOf(initialCourse?.notes ?: "") }

    // Multi-session drafts state
    var sessionsList by remember {
        val drafts = if (initialSessions.isNotEmpty()) {
            initialSessions.map {
                SessionDraft(id = it.id, day = it.day, start = it.start, end = it.end, location = it.location)
            }
        } else {
            listOf(SessionDraft(day = 0, start = "08:00", end = "10:00", location = ""))
        }
        mutableStateOf(drafts)
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showQuickPresets by remember { mutableStateOf(false) }

    val activeColor = remember(selectedColorHex) {
        try {
            Color(android.graphics.Color.parseColor(selectedColorHex))
        } catch (_: Exception) {
            Color(0xFF0D9488)
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = activeColor,
        focusedLabelColor = activeColor,
        cursorColor = activeColor,
        focusedLeadingIconColor = activeColor,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    )

    StudentGlassModalSheet(
        onDismiss = onDismiss,
        maxWidth = 580.dp,
        maxHeightPercent = 0.88f
    ) {
        // ── Sticky Header ─────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(activeColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = activeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = if (initialCourse == null) "افزودن درس جدید" else "ویرایش مشخصات درس",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val primarySession = sessionsList.firstOrNull()
                    val sessionSummary = if (primarySession != null) {
                        "${DAY_LABELS.getOrElse(primarySession.day) { "شنبه" }} • ${primarySession.start} تا ${primarySession.end}"
                    } else ""
                    Text(
                        text = "$sessionSummary • $units واحد${if (sessionsList.size > 1) " • ${sessionsList.size} جلسه در هفته" else ""}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "بستن",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── Segmented Tab Switcher (Minimalist & Compact) ─────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tab0Selected = selectedTab == 0
            val tab1Selected = selectedTab == 1

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTab = 0 },
                shape = RoundedCornerShape(9.dp),
                color = if (tab0Selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                shadowElevation = if (tab0Selected) 2.dp else 0.dp
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "مشخصات و جلسات هفتگی",
                        fontSize = 11.5.sp,
                        fontWeight = if (tab0Selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (tab0Selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTab = 1 },
                shape = RoundedCornerShape(9.dp),
                color = if (tab1Selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                shadowElevation = if (tab1Selected) 2.dp else 0.dp
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "استاد، آزمون و جزئیات",
                            fontSize = 11.5.sp,
                            fontWeight = if (tab1Selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (tab1Selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (examDate.isNotBlank() || professor.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(activeColor)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── Scrollable Tab Body ───────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (selectedTab == 0) {
                // ════════ TAB 0: Core Info & Weekly Sessions ════════
                // 1. Course Name & Quick Presets
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FormSectionLabel(Icons.Default.School, "عنوان درس", activeColor)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("نام درس") },
                        placeholder = { Text("مثلاً ریاضی عمومی ۱") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = FieldShape,
                        colors = fieldColors
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "پیشنهاد عنوان‌های پرتکرار",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { showQuickPresets = !showQuickPresets }) {
                            Text(
                                text = if (showQuickPresets) "بستن لیست" else "مشاهده لیست",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = activeColor
                            )
                        }
                    }

                    AnimatedVisibility(visible = showQuickPresets) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            POPULAR_COURSE_PRESETS.forEach { preset ->
                                SelectableChip(
                                    text = preset,
                                    selected = name == preset,
                                    activeColor = activeColor,
                                    onClick = {
                                        name = preset
                                        showQuickPresets = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 2. Weekly Sessions (Multi-session per course)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FormSectionLabel(Icons.Default.Schedule, "جلسات هفتگی کلاس (${sessionsList.size} جلسه)", activeColor)
                        TextButton(
                            onClick = {
                                val nextDay = if (sessionsList.isNotEmpty()) (sessionsList.last().day + 2) % 5 else 0
                                sessionsList = sessionsList + SessionDraft(day = nextDay, start = "08:00", end = "10:00", location = "")
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = activeColor)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ افزودن جلسه هفتگی دیگر", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = activeColor)
                        }
                    }

                    sessionsList.forEachIndexed { sIndex, sessionItem ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = CardShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            border = BorderStroke(1.dp, if (sIndex == 0) activeColor.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (sIndex == 0) "جلسه اول (اصلی)" else "جلسه ${sIndex + 1} (حل‌تمرین / جلسه دوم)",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeColor
                                    )
                                    if (sessionsList.size > 1) {
                                        IconButton(
                                            onClick = {
                                                sessionsList = sessionsList.filterIndexed { idx, _ -> idx != sIndex }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "حذف جلسه",
                                                tint = MaterialTheme.studentColors.attendanceCritical,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Day of week selector
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    DAY_LABELS.forEachIndexed { dIdx, dLabel ->
                                        val isSelected = sessionItem.day == dIdx
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    sessionsList = sessionsList.mapIndexed { idx, s ->
                                                        if (idx == sIndex) s.copy(day = dIdx) else s
                                                    }
                                                },
                                            shape = ChipShape,
                                            color = if (isSelected) activeColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = BorderStroke(1.dp, if (isSelected) activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = dLabel,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                // Quick slot pills
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    STANDARD_TIME_SLOTS.forEach { (slotStart, slotEnd) ->
                                        val isSelected = sessionItem.start == slotStart && sessionItem.end == slotEnd
                                        SelectableChip(
                                            text = "$slotStart–$slotEnd",
                                            selected = isSelected,
                                            activeColor = activeColor,
                                            onClick = {
                                                sessionsList = sessionsList.mapIndexed { idx, s ->
                                                    if (idx == sIndex) s.copy(start = slotStart, end = slotEnd) else s
                                                }
                                            }
                                        )
                                    }
                                }

                                // Start, End, Location
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = sessionItem.start,
                                        onValueChange = { newStart ->
                                            sessionsList = sessionsList.mapIndexed { idx, s ->
                                                if (idx == sIndex) s.copy(start = newStart) else s
                                            }
                                        },
                                        label = { Text("شروع") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = FieldShape,
                                        colors = fieldColors
                                    )
                                    OutlinedTextField(
                                        value = sessionItem.end,
                                        onValueChange = { newEnd ->
                                            sessionsList = sessionsList.mapIndexed { idx, s ->
                                                if (idx == sIndex) s.copy(end = newEnd) else s
                                            }
                                        },
                                        label = { Text("پایان") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = FieldShape,
                                        colors = fieldColors
                                    )
                                    OutlinedTextField(
                                        value = sessionItem.location,
                                        onValueChange = { newLoc ->
                                            sessionsList = sessionsList.mapIndexed { idx, s ->
                                                if (idx == sIndex) s.copy(location = newLoc) else s
                                            }
                                        },
                                        label = { Text("محل") },
                                        placeholder = { Text("کلاس ۱۰۲") },
                                        modifier = Modifier.weight(1.2f),
                                        singleLine = true,
                                        shape = FieldShape,
                                        colors = fieldColors
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Units Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FormSectionLabel(Icons.Default.School, "تعداد واحد: $units واحد (یک‌بار برای کل درس)", activeColor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 3, 4).forEach { u ->
                            val isSelected = units == u
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { units = u },
                                shape = ChipShape,
                                color = if (isSelected) activeColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                )
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$u واحد",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Color Palette
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FormSectionLabel(Icons.Default.Palette, "رنگ شاخص درس", activeColor)
                        Text(
                            text = PALETTE_COLORS.firstOrNull { it.first.equals(selectedColorHex, true) }?.second ?: "",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PALETTE_COLORS.forEach { (hex, _) ->
                            val c = try {
                                Color(android.graphics.Color.parseColor(hex))
                            } catch (_: Exception) {
                                Color(0xFF6366F1)
                            }
                            val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // ════════ TAB 1: Professor, Exam & Details ════════
                // Professor & Code
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FormSectionLabel(Icons.Default.Person, "اطلاعات مدرس و درس", activeColor)

                    OutlinedTextField(
                        value = professor,
                        onValueChange = { professor = it },
                        label = { Text("نام استاد / مدرس") },
                        placeholder = { Text("مثال: دکتر رضایی") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        shape = FieldShape,
                        colors = fieldColors
                    )

                    OutlinedTextField(
                        value = courseCode,
                        onValueChange = { courseCode = it },
                        label = { Text("کد درس") },
                        placeholder = { Text("۱۲-۳۴") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = FieldShape,
                        colors = fieldColors
                    )
                }

                // Exam Information Card
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormSectionLabel(Icons.Default.CalendarToday, "آزمون پایان‌ترم", activeColor)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = CardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                JalaliDatePickerField(
                                    value = examDate,
                                    onValueChange = { examDate = it },
                                    label = "تاریخ امتحان",
                                    placeholder = "۱۴۰۵/۰۳/۲۰",
                                    modifier = Modifier.weight(1.2f)
                                )
                                OutlinedTextField(
                                    value = examTime,
                                    onValueChange = { examTime = it },
                                    label = { Text("ساعت") },
                                    placeholder = { Text("۰۸:۳۰") },
                                    modifier = Modifier.weight(0.8f),
                                    singleLine = true,
                                    shape = FieldShape,
                                    colors = fieldColors
                                )
                            }
                            OutlinedTextField(
                                value = examLocation,
                                onValueChange = { examLocation = it },
                                label = { Text("محل آزمون") },
                                placeholder = { Text("سالن امتحانات") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = FieldShape,
                                colors = fieldColors
                            )
                        }
                    }
                }

                // Notes
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FormSectionLabel(Icons.Default.EditNote, "یادداشت و سرفصل", activeColor)
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("سرفصل‌ها، بارم‌بندی، منابع یا تکالیف این درس...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = FieldShape,
                        colors = fieldColors
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        Spacer(modifier = Modifier.height(8.dp))

        // ── Sticky Action Bar ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (initialCourse != null && onDelete != null) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.studentColors.attendanceCritical
                    )
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف درس", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Spacer(modifier = Modifier.width(4.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = ButtonShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Text("انصراف", fontSize = 11.5.sp)
                }

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val id = initialCourse?.id ?: java.util.UUID.randomUUID().toString()
                            val course = CourseEntity(
                                id = id,
                                name = name.trim(),
                                colorHex = selectedColorHex,
                                units = units,
                                semesterId = initialCourse?.semesterId ?: "current",
                                courseCode = courseCode.trim(),
                                professor = professor.trim(),
                                examDate = examDate.trim(),
                                examTime = examTime.trim(),
                                examLocation = examLocation.trim(),
                                notes = notes.trim()
                            )
                            val sessions = sessionsList.map { s ->
                                CourseSessionEntity(
                                    id = s.id,
                                    courseId = id,
                                    day = s.day,
                                    start = s.start.trim(),
                                    end = s.end.trim(),
                                    location = s.location.trim()
                                )
                            }
                            if (onSaveWithSessions != null) {
                                onSaveWithSessions(course, sessions)
                            } else {
                                onSave(course)
                            }
                        }
                    },
                    shape = ButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeColor
                    ),
                    enabled = name.isNotBlank()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when {
                            initialCourse == null -> "ثبت درس"
                            else -> "ذخیره تغییرات"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Confirmation Alert for Course Deletion
    if (showDeleteConfirm && initialCourse != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.studentColors.attendanceCritical,
                    modifier = Modifier.size(30.dp)
                )
            },
            title = {
                Text(
                    text = "حذف «${initialCourse.name}»؟",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "با حذف این درس، حضور و غیاب، نمرات، جلسات و تکالیف مرتبط با آن نیز پاک خواهند شد. این عملیات قابل بازگشت نیست.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(initialCourse.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.studentColors.attendanceCritical),
                    shape = ButtonShape
                ) {
                    Text("بله، حذف کن", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("انصراف", fontSize = 11.5.sp)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

/** Small, consistent section header used to visually separate every group of fields. */
@Composable
private fun FormSectionLabel(icon: ImageVector, text: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** One reusable pill/chip style shared by presets & time slots so both look identical. */
@Composable
private fun SelectableChip(
    text: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = ChipShape,
        color = if (selected) activeColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, if (selected) activeColor else Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) activeColor else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}
