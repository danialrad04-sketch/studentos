package com.example.ui.components.datepicker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BrandIndigo600
import com.example.ui.theme.BrandIndigo900
import com.example.ui.theme.Slate400

/**
 * A modern, accessible Material 3 Jalali (Persian) Date Picker Dialog.
 */
@Composable
fun JalaliDatePickerDialog(
    initialDate: String? = null,
    onDateSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val today = remember { JalaliCalendarUtil.today() }
    val initial = remember(initialDate) {
        if (!initialDate.isNullOrBlank()) {
            JalaliCalendarUtil.parse(initialDate) ?: today
        } else {
            today
        }
    }

    var selectedYear by remember { mutableIntStateOf(initial.year) }
    var selectedMonth by remember { mutableIntStateOf(initial.month) }
    var selectedDay by remember { mutableIntStateOf(initial.day) }

    val daysInCurrentMonth = remember(selectedYear, selectedMonth) {
        JalaliCalendarUtil.getDaysInMonth(selectedYear, selectedMonth)
    }

    // Clamp selected day if month changes
    if (selectedDay > daysInCurrentMonth) {
        selectedDay = daysInCurrentMonth
    }

    val selectedJalaliDate = remember(selectedYear, selectedMonth, selectedDay) {
        JalaliDate(selectedYear, selectedMonth, selectedDay)
    }

    val firstDayOffset = remember(selectedYear, selectedMonth) {
        JalaliCalendarUtil.getFirstDayOfWeekInMonth(selectedYear, selectedMonth)
    }

    var showYearDropdown by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(28.dp)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header: Selected Date Display
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "تقویم دانشگاهی (خورشیدی)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${selectedJalaliDate.getWeekdayName()}، ${selectedJalaliDate.day} ${selectedJalaliDate.getMonthName()} ${selectedJalaliDate.year}",
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Month & Year Selector Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (selectedMonth > 1) {
                                    selectedMonth--
                                } else {
                                    selectedMonth = 12
                                    selectedYear--
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "ماه قبل",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = JalaliCalendarUtil.PERSIAN_MONTH_NAMES[selectedMonth - 1],
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Box {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.clickable { showYearDropdown = true }
                                ) {
                                    Text(
                                        text = "${selectedYear}",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showYearDropdown,
                                    onDismissRequest = { showYearDropdown = false }
                                ) {
                                    for (y in (today.year - 2)..(today.year + 3)) {
                                        DropdownMenuItem(
                                            text = { Text("$y", fontWeight = if (y == selectedYear) FontWeight.Bold else FontWeight.Normal) },
                                            onClick = {
                                                selectedYear = y
                                                showYearDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                if (selectedMonth < 12) {
                                    selectedMonth++
                                } else {
                                    selectedMonth = 1
                                    selectedYear++
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "ماه بعد",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Days of week header (ش ی د س چ پ ج)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (w in JalaliCalendarUtil.PERSIAN_WEEKDAYS_SHORT) {
                            Text(
                                text = w,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (w == "ج") MaterialTheme.colorScheme.error else Slate400,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar Grid (7 columns, up to 42 cells)
                    val totalCells = firstDayOffset + daysInCurrentMonth
                    val rowsCount = (totalCells + 6) / 7

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (row in 0 until rowsCount) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                for (col in 0..6) {
                                    val cellIndex = (row * 7) + col
                                    val dayNumber = cellIndex - firstDayOffset + 1

                                    if (dayNumber in 1..daysInCurrentMonth) {
                                        val isSelected = (dayNumber == selectedDay)
                                        val isToday = (selectedYear == today.year && selectedMonth == today.month && dayNumber == today.day)

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(2.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        isSelected -> MaterialTheme.colorScheme.primary
                                                        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .clickable {
                                                    selectedDay = dayNumber
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$dayNumber",
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    col == 6 -> MaterialTheme.colorScheme.error
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom Quick Actions: Today button + Cancel/Confirm
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                selectedYear = today.year
                                selectedMonth = today.month
                                selectedDay = today.day
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "امروز", fontSize = 12.sp)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onDismissRequest,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("انصراف", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    onDateSelected(selectedJalaliDate.format())
                                    onDismissRequest()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("تأیید تاریخ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Drop-in Clickable Jalali Date Field that triggers the picker dialog.
 */
@Composable
fun JalaliDatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "تاریخ آزمون",
    placeholder: String = "۱۴۰۵/۰۳/۲۰",
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { /* Read-only via picker or direct input */ onValueChange(it) },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = {
                IconButton(onClick = { showDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "انتخاب از تقویم خورشیدی",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            readOnly = true,
            singleLine = true,
            isError = isError,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true }
        )

        // Transparent overlay to ensure clicking anywhere on the text field opens the dialog
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showDialog = true
                }
        )
    }

    if (showDialog) {
        JalaliDatePickerDialog(
            initialDate = value,
            onDateSelected = { selected ->
                onValueChange(selected)
            },
            onDismissRequest = { showDialog = false }
        )
    }
}
