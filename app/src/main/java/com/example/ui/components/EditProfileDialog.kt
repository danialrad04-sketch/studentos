package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.StudentProfileEntity
import com.example.ui.theme.StudentOsColors

@Composable
fun EditProfileDialog(
    profile: StudentProfileEntity,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        studentId: String,
        university: String,
        major: String,
        entryYear: Int,
        currentSemester: Int,
        passedUnits: Int,
        activeUnits: Int
    ) -> Unit,
    onReopenOnboarding: () -> Unit = {},
    onOpenPastSemesters: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    var name by remember { mutableStateOf(profile.name) }
    var studentId by remember { mutableStateOf(profile.studentId) }
    var university by remember { mutableStateOf(profile.university) }
    var major by remember { mutableStateOf(profile.major) }
    var entryYearInput by remember { mutableStateOf(profile.entryYear.toString()) }
    var semesterInput by remember { mutableStateOf(profile.currentSemester.toString()) }
    var passedUnitsInput by remember { mutableStateOf(profile.passedUnits.toString()) }
    var activeUnitsInput by remember { mutableStateOf(profile.activeUnits.toString()) }

    var nameError by remember { mutableStateOf(false) }
    var studentIdError by remember { mutableStateOf(false) }

    StudentGlassModalSheet(
        onDismiss = onDismiss,
        maxWidth = 580.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
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
                            .background(StudentOsColors.ElectricBlue.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = StudentOsColors.ElectricBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "ویرایش مشخصات دانشجویی",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "پایگاه داده محلی امن بدون اتلاف داده (Zero Data Loss)",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "بستن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (it.isNotBlank()) nameError = false
                },
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("نام دانشجو نمی‌تواند خالی باشد", color = MaterialTheme.colorScheme.error, fontSize = 10.sp) }
                } else null,
                label = { Text("نام و نام خانوادگی دانشجو *", fontSize = 11.5.sp) },
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StudentOsColors.ElectricBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                ),
                singleLine = true
            )

            // Student ID & Semester
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = studentId,
                    onValueChange = {
                        studentId = it
                        if (it.isNotBlank()) studentIdError = false
                    },
                    isError = studentIdError,
                    supportingText = if (studentIdError) {
                        { Text("شماره دانشجویی الزامی است", color = MaterialTheme.colorScheme.error, fontSize = 9.5.sp) }
                    } else null,
                    label = { Text("شماره دانشجویی *", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Rounded.Badge, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudentOsColors.ElectricBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = semesterInput,
                    onValueChange = { semesterInput = it },
                    label = { Text("ترم جاری", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.8f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudentOsColors.ElectricBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    ),
                    singleLine = true
                )
            }

            // University & Major
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = university,
                    onValueChange = { university = it },
                    label = { Text("دانشگاه", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Rounded.School, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudentOsColors.ElectricBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = major,
                    onValueChange = { major = it },
                    label = { Text("رشته تحصیلی", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudentOsColors.ElectricBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    ),
                    singleLine = true
                )
            }

            // Units & Entry Year
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = passedUnitsInput,
                    onValueChange = { passedUnitsInput = it },
                    label = { Text("واحدهای پاس‌شده", fontSize = 10.5.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudentOsColors.ElectricBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = activeUnitsInput,
                    onValueChange = { activeUnitsInput = it },
                    label = { Text("واحدهای ترم جاری", fontSize = 10.5.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudentOsColors.ElectricBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = entryYearInput,
                    onValueChange = { entryYearInput = it },
                    label = { Text("سال ورود", fontSize = 10.5.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudentOsColors.ElectricBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    ),
                    singleLine = true
                )
            }

            // Shortcuts
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenPastSemesters()
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.HistoryEdu, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تنظیم سوابق گذرانده ترم‌های قبل (۱ تا ۸)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onReopenOnboarding()
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("اجرای مجدد راه‌اندازی سریع (Zero-Setup Wizard)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Text("انصراف", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        var hasError = false
                        if (name.isBlank()) {
                            nameError = true
                            hasError = true
                        }
                        if (studentId.isBlank()) {
                            studentIdError = true
                            hasError = true
                        }
                        if (!hasError) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val year = entryYearInput.toIntOrNull() ?: profile.entryYear
                            val sem = semesterInput.toIntOrNull() ?: profile.currentSemester
                            val passed = passedUnitsInput.toIntOrNull() ?: profile.passedUnits
                            val active = activeUnitsInput.toIntOrNull() ?: profile.activeUnits

                            onSave(name.trim(), studentId.trim(), university.trim(), major.trim(), year, sem, passed, active)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudentOsColors.ElectricBlue),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(46.dp)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ذخیره مشخصات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
