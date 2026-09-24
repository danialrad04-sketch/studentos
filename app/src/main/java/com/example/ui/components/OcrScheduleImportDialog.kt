package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.parser.DraftValidationState
import com.example.data.parser.ParsedCourseDraft
import com.example.data.parser.RegistrationTextParser
import com.example.ui.theme.Amber600
import com.example.ui.theme.BrandIndigo600
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Rose600
import androidx.compose.ui.platform.LocalContext
import com.example.data.api.GeminiApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Phase H, I, J: Bulk Import & OCR Schedule Import Dialog.
 * Dual-Mode:
 * 1. Image OCR with Android Photo Picker
 * 2. Persian Natural Language & Structured Text Bulk Parser
 * Includes Pre-Commit Schema Validation, Conflict and Missing Field checks.
 */
@Composable
fun OcrScheduleImportDialog(
    onDismiss: () -> Unit,
    onConfirmImport: (List<ParsedCourseDraft>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(1) } // Default to Bulk Text (0: OCR Image, 1: Bulk Text)

    // OCR State
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessingOcr by remember { mutableStateOf(false) }
    var ocrErrorMessage by remember { mutableStateOf<String?>(null) }

    // Bulk Text State
    var bulkInputText by remember { mutableStateOf("") }

    // Extracted drafts
    val extractedDrafts = remember { mutableStateListOf<ParsedCourseDraft>() }

    // Selected University System Preset
    var selectedPreset by remember { mutableStateOf(RegistrationTextParser.UniversitySystem.AUTO_DETECT) }

    // Android Photo Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            ocrErrorMessage = null
            extractedDrafts.clear()
            isProcessingOcr = true
        }
    }

    // Real OCR processing using GeminiApiClient vision model
    LaunchedEffect(selectedImageUri, isProcessingOcr) {
        val uri = selectedImageUri
        if (isProcessingOcr && uri != null) {
            ocrErrorMessage = null
            try {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                if (bytes == null || bytes.isEmpty()) {
                    ocrErrorMessage = "خطا در خواندن فایل تصویر انتخابی."
                    isProcessingOcr = false
                    return@LaunchedEffect
                }
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val result = GeminiApiClient.extractScheduleFromImage(bytes, mimeType)
                if (result.isSuccess) {
                    val courses = result.getOrNull().orEmpty()
                    if (courses.isEmpty()) {
                        ocrErrorMessage = "هیچ درسی در تصویر برنامه شناسایی نشد. لطفاً از واضح بودن تصویر برنامه هفتگی اطمینان حاصل کرده یا از تب ورود متنی استفاده نمایید."
                    } else {
                        extractedDrafts.clear()
                        extractedDrafts.addAll(courses)
                    }
                } else {
                    ocrErrorMessage = result.exceptionOrNull()?.localizedMessage
                        ?: "خطا در برقراری ارتباط با مدل هوش مصنوعی برای استخراج دروس. لطفاً اتصال اینترنت یا کلید API را بررسی فرمایید."
                }
            } catch (e: Exception) {
                ocrErrorMessage = "خطای غیرمنتظره در پردازش تصویر: ${e.message}"
            } finally {
                isProcessingOcr = false
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
                .testTag("ocr_schedule_import_dialog"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = BrandIndigo600.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = BrandIndigo600,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ورود گروهی و هوشمند دروس",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "پردازش متن طبیعی فارسی یا اسکن تصویر با اعتبارسنجی پیش از ثبت",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Tabs: OCR vs Bulk Text
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Scanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("اسکن تصویر (OCR)", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ورود متنی و طبیعی", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    )
                }

                // System Preset Selector Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "سامانه آموزشی دانشگاه:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RegistrationTextParser.UniversitySystem.values().forEach { preset ->
                            val isSelected = selectedPreset == preset
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPreset = preset },
                                label = { Text(preset.displayName, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // Tab Contents
                if (selectedTab == 0) {
                    // OCR Mode
                    if (ocrErrorMessage != null && !isProcessingOcr) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "خطا در پردازش هوشمند تصویر",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Text(
                                    text = ocrErrorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            ocrErrorMessage = null
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandIndigo600),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("انتخاب مجدد تصویر")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            selectedTab = 1
                                            ocrErrorMessage = null
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("ورود متنی")
                                    }
                                }
                            }
                        }
                    }

                    if (extractedDrafts.isEmpty() && !isProcessingOcr && ocrErrorMessage == null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = BrandIndigo600, modifier = Modifier.size(40.dp))
                                Text(
                                    text = "تصویر فرم یا اسکرین‌شات انتخاب واحد را انتخاب کنید",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandIndigo600),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("انتخاب از گالری (Photo Picker)")
                                }
                            }
                        }
                    }

                    if (isProcessingOcr) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("در حال ارسال و تحلیل هوشمند مشخصات دروس از تصویر...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    // Bulk Text Mode
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "متن دروس را وارد یا پیست کنید (پشتیبانی از فرمت پایپ یا جملات فارسی):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = bulkInputText,
                            onValueChange = { bulkInputText = it },
                            placeholder = {
                                Text(
                                    "نمونه ۱: ریاضی عمومی ۲ | ۳ واحد | شنبه ۱۰ تا ۱۲ | استاد احمدی | امتحان ۲۰ دی ساعت ۹\nنمونه ۲: شیمی فیزیک سه واحد یکشنبه هشت تا ده دکتر کاظمی",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("bulk_text_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val parsed = RegistrationTextParser.parse(bulkInputText, selectedPreset)
                                    extractedDrafts.clear()
                                    extractedDrafts.addAll(parsed)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandIndigo600),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("parse_bulk_text_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("پردازش و اعتبارسنجی متن")
                            }

                            OutlinedButton(
                                onClick = {
                                    bulkInputText = "ریاضی عمومی ۲ | ۳ واحد | شنبه ۱۰ تا ۱۲ | استاد احمدی | امتحان ۲۰ دی ساعت ۹\nفیزیک ۲ | ۳ واحد | یکشنبه ۸ تا ۱۰ | دکتر حسینی | امتحان ۲۵ دی ساعت ۱۰\nمعادلات دیفرانسیل | ۳ واحد | دوشنبه ۱۳:۳۰ تا ۱۵:۳۰ | دکتر صادقی"
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("نمونه آماده")
                            }
                        }
                    }
                }

                // Extracted drafts preview list
                if (extractedDrafts.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "دروس شناسایی‌شده (${extractedDrafts.size} درس):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .background(Emerald600.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "پیش‌نمایش قبل از ثبت در Room",
                                color = Emerald600,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(extractedDrafts) { draft ->
                            DraftCardItem(
                                draft = draft,
                                onRemove = { extractedDrafts.remove(draft) }
                            )
                        }
                    }

                    // Confirm and Save Button
                    Button(
                        onClick = {
                            onConfirmImport(extractedDrafts.toList())
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("confirm_bulk_import_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تأیید و ذخیره کلیه دروس در پایگاه‌داده", fontWeight = FontWeight.Bold)
                    }
                }
            }
    }
}

@Composable
private fun DraftCardItem(
    draft: ParsedCourseDraft,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = draft.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    if (draft.validationState == DraftValidationState.INCOMPLETE) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Amber600, modifier = Modifier.size(14.dp))
                    }
                }
                Text(
                    text = "${draft.dayName} (${draft.startTime} تا ${draft.endTime}) | ${draft.instructor}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (draft.examDate.isNotBlank()) {
                    Text(
                        text = "امتحان: ${draft.examDate} ساعت ${draft.examTime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BrandIndigo600.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${draft.units} واحد",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = BrandIndigo600,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "حذف ${draft.name}",
                        tint = Rose600,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
