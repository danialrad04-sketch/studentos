package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.StudentProfileEntity

data class TourStepData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val badgeText: String,
    val highlights: List<Pair<String, String>>
)

@Composable
fun FirstTimeAppTourDialog(
    currentProfile: StudentProfileEntity,
    onSaveProfileEssentials: (name: String, studentId: String, targetGpa: Double, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val DarkOverlayBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val TourCardBg = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val TourBorder = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val TourAccentPrimary = if (isDark) Color(0xFF6366F1) else Color(0xFF4F46E5)
    val TourAccentSecondary = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val TourAccentSuccess = if (isDark) Color(0xFF10B981) else Color(0xFF059669)
    val TourAccentWarning = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)
    val TourTextPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val TourTextSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    var currentStepIndex by remember { mutableIntStateOf(0) }

    // Essential fields state for Step 1
    var studentName by remember(currentProfile.name) {
        mutableStateOf(if (currentProfile.name == "دانشجو" || currentProfile.name == "دانشجوی جدید") "" else currentProfile.name)
    }
    var studentNumber by remember(currentProfile.studentId) { mutableStateOf(currentProfile.studentId) }
    var targetGpaInput by remember {
        mutableStateOf(currentProfile.declaredGpa?.let { String.format("%.2f", it) } ?: "17.50")
    }
    var academicNotes by remember(currentProfile.notes) { mutableStateOf(currentProfile.notes) }

    val tourSteps = listOf(
        TourStepData(
            title = "خوش‌آمدید به دستیار هوشمند تحصیلی 🎓",
            subtitle = "سیستم‌عامل جامع مدیریت دانشگاه، برنامه هفتگی، چارت و معدل",
            icon = Icons.Default.RocketLaunch,
            iconColor = TourAccentPrimary,
            badgeText = "گام 1 از 6: معرفی",
            highlights = listOf(
                "⚡ معماری کاملاً آفلاین" to "تمام داده‌ها در پایگاه داده محلی دستگاه ذخیره شده و بدون اینترنت در دسترس هستند.",
                "📊 پردازش 360 درجه آکادمیک" to "موتور هوشمند پیش‌نیازها، ریسک مشروطی و شبیه‌ساز معدل همواره در کنار شماست.",
                "🎯 دستیار خودکار انتخاب واحد" to "پشتیبانی از فرمت‌های کپی‌شده گلستان و سمیاد با استخراج خودکار ساعت و ترم."
            )
        ),
        TourStepData(
            title = "اطلاعات ضروری دانشجو (پیشنهادی) ✍️",
            subtitle = "پر کردن این اطلاعات به دقت تحلیل معدل و گزارش‌های تحصیلی کمک می‌کند",
            icon = Icons.Default.Person,
            iconColor = TourAccentSecondary,
            badgeText = "گام 2 از 6: اطلاعات پایه",
            highlights = emptyList() // Will render custom form
        ),
        TourStepData(
            title = "میزکار زنده و Dynamic Island 🏝️",
            subtitle = "نمایش هوشمند وضعیت کلاس جاری، تایمر پومودورو و ویجت‌های سریع",
            icon = Icons.Default.Dashboard,
            iconColor = TourAccentSecondary,
            badgeText = "گام 3 از 6: داشبورد زنده",
            highlights = listOf(
                "⏰ کپسول کلاس بعدی" to "مشاهده در لحظه نام کلاس پیش رو، ساعت شروع و اتاق تشکیل کلاس بدون ورود به منو.",
                "🍅 تمرکز پومودورو 25 دقیقه‌ای" to "تایمر اختصاصی مطالعه با فواصل استراحت هوشمند و یادآور صوتی.",
                "🚨 رادار حضور و غیاب" to "هشدار خودکار هنگام نزدیک شدن به سقف مجاز 3 جلسه غیبت (یا 2 جلسه آزمایشگاه)."
            )
        ),
        TourStepData(
            title = "برنامه هفتگی و حذف و اضافه هوشمند 📅",
            subtitle = "مرتب‌سازی خودکار به وقت شروع کلاس‌ها و رفع تداخلات زمانی",
            icon = Icons.Default.Schedule,
            iconColor = TourAccentPrimary,
            badgeText = "گام 4 از 6: برنامه هفتگی",
            highlights = listOf(
                "🔢 اولویت‌بندی زمانی دقیق" to "کلاس‌های صبح زود (مثلاً 08:00) همیشه در بالاترین بخش قرار می‌گیرند و ساعات بعدی به ترتیب پشت سر هم هستند.",
                "🔄 نمای روزانه و هفتگی" to "امکان سوییچ فوری بین برنامه امروز و نمای جامع تمام روزهای هفته با یک کلیک.",
                "🎨 کارت‌های تعاملی 360°" to "کلیک روی هر درس کارپوشه تخصصی درس، غیبت‌ها، تکالیف و تخمین نمره را باز می‌کند."
            )
        ),
        TourStepData(
            title = "چارت تحصیلی و ماتریس پیش‌نیازها 🗺️",
            subtitle = "مدیریت 140 واحد کارشناسی، دروس پاس‌شده و نقشه راه فارغ‌التحصیلی",
            icon = Icons.Default.Timeline,
            iconColor = TourAccentWarning,
            badgeText = "گام 5 از 6: چارت آکادمیک",
            highlights = listOf(
                "🧩 چارت رسمی دانشگاه و سراسری" to "تفکیک دروس عمومی، پایه، تخصصی و اختیاری با واحدهای دقیق.",
                "🔗 ردیاب زنجیره پیش‌نیاز و هم‌نیاز" to "بررسی هوشمند امکان اخذ دروس بر اساس نمرات قبلی.",
                "📈 رادار پیشرفت فارغ‌التحصیلی" to "محاسبه درصد پیشرفت مقطع و پیش‌بینی ترم‌های باقی‌مانده."
            )
        ),
        TourStepData(
            title = "امتحانات، تکالیف و کوپایلوت نمرات 🤖",
            subtitle = "روزشمار امتحانات، مدیریت فرجه و دستیار هوش مصنوعی",
            icon = Icons.Default.AutoAwesome,
            iconColor = TourAccentSuccess,
            badgeText = "گام 6 از 6: نمرات و هوش مصنوعی",
            highlights = listOf(
                "⏳ شمارش معکوس فرجه و امتحانات" to "نمایش دقیق روز و ساعت آزمون‌های پایان‌ترم با هشدار تداخل تاریخ.",
                "🧮 شبیه‌ساز معدل و هدف‌گذاری نمره" to "مشاهده آنی تأثیر نمره هر درس در معدل کل و معدل ترم جاری.",
                "💬 کوپایلوت هوشمند" to "پیشنهاد خودکار سناریوهای انتخاب واحد و راهکارهای جبران معدل."
            )
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkOverlayBg.copy(alpha = 0.94f))
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = TourCardBg),
                border = BorderStroke(1.2.dp, TourBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Progress Bar & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = TourAccentPrimary.copy(alpha = 0.18f),
                            border = BorderStroke(0.8.dp, TourAccentPrimary.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = tourSteps[currentStepIndex].badgeText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TourAccentPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "بستن راهنما",
                                tint = TourTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Progress Dots
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tourSteps.indices.forEach { index ->
                            val isCurrent = index == currentStepIndex
                            Box(
                                modifier = Modifier
                                    .height(4.dp)
                                    .width(if (isCurrent) 24.dp else 8.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (isCurrent) TourAccentPrimary else TourBorder)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Step Icon
                    val currentStep = tourSteps[currentStepIndex]
                    Surface(
                        shape = CircleShape,
                        color = currentStep.iconColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.2.dp, currentStep.iconColor.copy(alpha = 0.45f)),
                        modifier = Modifier.size(62.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = currentStep.icon,
                                contentDescription = null,
                                tint = currentStep.iconColor,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = currentStep.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TourTextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentStep.subtitle,
                        fontSize = 11.5.sp,
                        color = TourTextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Step Content
                    if (currentStepIndex == 1) {
                        // Custom Interactive Form for Essential Student Info
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            border = BorderStroke(1.dp, TourBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = studentName,
                                    onValueChange = { studentName = it },
                                    label = { Text("نام و نام خانوادگی", fontSize = 11.5.sp, color = TourTextSecondary) },
                                    placeholder = { Text("مثلاً: علی رضایی", fontSize = 11.sp, color = TourTextSecondary.copy(alpha = 0.6f)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TourTextPrimary,
                                        unfocusedTextColor = TourTextPrimary,
                                        focusedBorderColor = TourAccentPrimary,
                                        unfocusedBorderColor = TourBorder,
                                        focusedContainerColor = Color(0xFF1E293B),
                                        unfocusedContainerColor = Color(0xFF1E293B),
                                        cursorColor = TourAccentPrimary
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = studentNumber,
                                        onValueChange = { studentNumber = it },
                                        label = { Text("شماره دانشجویی", fontSize = 11.sp, color = TourTextSecondary) },
                                        placeholder = { Text("40200000", fontSize = 10.5.sp, color = TourTextSecondary.copy(alpha = 0.6f)) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TourTextPrimary,
                                            unfocusedTextColor = TourTextPrimary,
                                            focusedBorderColor = TourAccentPrimary,
                                            unfocusedBorderColor = TourBorder,
                                            focusedContainerColor = Color(0xFF1E293B),
                                            unfocusedContainerColor = Color(0xFF1E293B),
                                            cursorColor = TourAccentPrimary
                                        )
                                    )

                                    OutlinedTextField(
                                        value = targetGpaInput,
                                        onValueChange = { targetGpaInput = it },
                                        label = { Text("معدل هدف ترم (از 20)", fontSize = 11.sp, color = TourTextSecondary) },
                                        placeholder = { Text("18.50", fontSize = 10.5.sp, color = TourTextSecondary.copy(alpha = 0.6f)) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TourTextPrimary,
                                            unfocusedTextColor = TourTextPrimary,
                                            focusedBorderColor = TourAccentPrimary,
                                            unfocusedBorderColor = TourBorder,
                                            focusedContainerColor = Color(0xFF1E293B),
                                            unfocusedContainerColor = Color(0xFF1E293B),
                                            cursorColor = TourAccentPrimary
                                        )
                                    )
                                }

                                OutlinedTextField(
                                    value = academicNotes,
                                    onValueChange = { academicNotes = it },
                                    label = { Text("یادداشت یا گرایش تحصیلی (اختیاری)", fontSize = 11.5.sp, color = TourTextSecondary) },
                                    placeholder = { Text("مثلاً: گرایش نرم‌افزار، علاقه‌مند به هوش مصنوعی", fontSize = 10.5.sp, color = TourTextSecondary.copy(alpha = 0.6f)) },
                                    maxLines = 2,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TourTextPrimary,
                                        unfocusedTextColor = TourTextPrimary,
                                        focusedBorderColor = TourAccentPrimary,
                                        unfocusedBorderColor = TourBorder,
                                        focusedContainerColor = Color(0xFF1E293B),
                                        unfocusedContainerColor = Color(0xFF1E293B),
                                        cursorColor = TourAccentPrimary
                                    )
                                )
                            }
                        }
                    } else {
                        // Highlight Cards
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            currentStep.highlights.forEach { (hTitle, hDesc) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    border = BorderStroke(0.8.dp, TourBorder)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(currentStep.iconColor)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = hTitle,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TourTextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = hDesc,
                                                fontSize = 11.sp,
                                                color = TourTextSecondary,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Bottom Navigation Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStepIndex > 0) {
                            OutlinedButton(
                                onClick = { currentStepIndex-- },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, TourBorder)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "قبلی",
                                    tint = TourTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "قبلی", fontSize = 11.5.sp, color = TourTextSecondary)
                            }
                        } else {
                            TextButton(onClick = onDismiss) {
                                Text(text = "رد کردن راهنما", fontSize = 11.5.sp, color = TourTextSecondary)
                            }
                        }

                        Button(
                            onClick = {
                                if (currentStepIndex == 1) {
                                    val gpa = targetGpaInput.toDoubleOrNull() ?: 17.5
                                    onSaveProfileEssentials(
                                        if (studentName.isBlank()) "دانشجو" else studentName.trim(),
                                        studentNumber.trim(),
                                        gpa,
                                        academicNotes.trim()
                                    )
                                }

                                if (currentStepIndex < tourSteps.size - 1) {
                                    currentStepIndex++
                                } else {
                                    onDismiss()
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentStepIndex == tourSteps.size - 1) TourAccentSuccess else TourAccentPrimary
                            )
                        ) {
                            Text(
                                text = if (currentStepIndex == tourSteps.size - 1) "شروع کار با برنامه 🚀" else "مرحله بعد",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (currentStepIndex == tourSteps.size - 1) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
