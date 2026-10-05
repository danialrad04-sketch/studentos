package com.example.ui.components.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.StudentProfileEntity
import com.example.domain.model.AcademicProgress
import com.example.domain.model.GpaState
import com.example.ui.theme.Amber500
import com.example.ui.theme.Amber600
import com.example.ui.theme.BrandIndigo600
import com.example.ui.theme.BrandIndigo900
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.OledCanvasBlack
import com.example.ui.theme.OledCardDark
import com.example.ui.theme.Rose500
import com.example.ui.theme.Rose600
import com.example.ui.theme.Sky500
import com.example.ui.theme.Sky600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.VioletNeon
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.util.Locale

/**
 * Visual Theme Presets for Story & Export Cards
 */
enum class ExportThemePreset(
    val title: String,
    val primaryColor: Color,
    val accentColor: Color,
    val backgroundColors: List<Color>,
    val textColor: Color,
    val isDark: Boolean
) {
    OBSIDIAN_NEON(
        title = "زیتونی تیره",
        primaryColor = Color(0xFFC0CD8C),
        accentColor = Color(0xFF38BDF8),
        backgroundColors = listOf(Color(0xFF090D16), Color(0xFF131A2B), Color(0xFF090D16)),
        textColor = Color(0xFFF8FAFC),
        isDark = true
    ),
    ROYAL_INDIGO(
        title = "زیتونی کلاسیک",
        primaryColor = Color(0xFFDEE6C4),
        accentColor = Color(0xFFC0CD8C),
        backgroundColors = listOf(Color(0xFF3D4821), Color(0xFF59652F), Color(0xFF3D4821)),
        textColor = Color(0xFFFFFFFF),
        isDark = true
    ),
    EMERALD_HONORS(
        title = "زمرد افتخار 🌿",
        primaryColor = Color(0xFF6EE7B7),
        accentColor = Color(0xFFFBBF24),
        backgroundColors = listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF022C22)),
        textColor = Color(0xFFFFFFFF),
        isDark = true
    ),
    CLEAN_MINIMAL(
        title = "گزارش مینیمال",
        primaryColor = Color(0xFF1E293B),
        accentColor = Color(0xFF59652F),
        backgroundColors = listOf(Color(0xFFFAFAFA), Color(0xFFF1F5F9), Color(0xFFE2E8F0)),
        textColor = Color(0xFF0F172A),
        isDark = false
    )
}

enum class ExportFormatMode(val title: String, val icon: ImageVector) {
    STORY_CARD_9_16("کارت استوری (۹:۱۶)", Icons.Default.Image),
    OFFICIAL_DOC_A4("گزارش شخصی (A4 PDF)", Icons.Default.PictureAsPdf)
}

sealed class ExportSourcePayload {
    data class Passport(
        val profile: StudentProfileEntity,
        val progress: AcademicProgress?,
        val curriculumTitle: String
    ) : ExportSourcePayload()

    data class Grades(
        val profile: StudentProfileEntity,
        val grades: List<GradeEntity>,
        val termGpa: Double,
        val totalUnits: Int
    ) : ExportSourcePayload()
}

/**
 * Premium Share & Export Modal Sheet (Phase 2).
 * Allows instant generation and live preview of 9:16 Social Story Cards and Official A4 Transcripts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumShareExportModal(
    payload: ExportSourcePayload,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storyGraphicsLayer = rememberGraphicsLayer()

    var selectedFormat by remember { mutableStateOf(ExportFormatMode.STORY_CARD_9_16) }
    var selectedTheme by remember { mutableStateOf(ExportThemePreset.OBSIDIAN_NEON) }

    // Customization Options
    var showStudentId by remember { mutableStateOf(true) }
    var showGpa by remember { mutableStateOf(true) }
    var showOfficialStamp by remember { mutableStateOf(true) }
    val showQrVerification = false

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BrandIndigo600.copy(alpha = 0.12f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "خروجی پرمیوم",
                                tint = BrandIndigo600,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "مرکز صدور و اشتراک‌گذاری پرمیوم",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "کارت اشتراک‌گذاری و گزارش شخصی تحصیلی",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "بستن")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Format Mode Selector (Story vs Document)
            TabRow(
                selectedTabIndex = selectedFormat.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedFormat.ordinal]),
                        color = MaterialTheme.colorScheme.primary,
                        height = 3.dp
                    )
                }
            ) {
                ExportFormatMode.entries.forEach { mode ->
                    val isSelected = selectedFormat == mode
                    Tab(
                        selected = isSelected,
                        onClick = { selectedFormat = mode },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = mode.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = mode.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Theme Preset Selector (for Story mode)
            if (selectedFormat == ExportFormatMode.STORY_CARD_9_16) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportThemePreset.entries.forEach { theme ->
                        val isSelected = selectedTheme == theme
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTheme = theme },
                            label = { Text(theme.title, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.8.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Customization Options Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = showGpa,
                    onClick = { showGpa = !showGpa },
                    label = { Text("نمایش معدل", fontSize = 11.sp) },
                    leadingIcon = {
                        if (showGpa) Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                    }
                )
                FilterChip(
                    selected = showStudentId,
                    onClick = { showStudentId = !showStudentId },
                    label = { Text("شماره دانشجویی", fontSize = 11.sp) },
                    leadingIcon = {
                        if (showStudentId) Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                    }
                )
                FilterChip(
                    selected = showOfficialStamp,
                    onClick = { showOfficialStamp = !showOfficialStamp },
                    label = { Text("نشان Student OS", fontSize = 11.sp) },
                    leadingIcon = {
                        if (showOfficialStamp) Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                    }
                )

            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scrollable Live Interactive Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0B0F19))
                    .border(BorderStroke(1.dp, Slate700.copy(alpha = 0.6f)), RoundedCornerShape(20.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (selectedFormat) {
                        ExportFormatMode.STORY_CARD_9_16 -> {
                            Box(
                                modifier = Modifier.drawWithContent {
                                    storyGraphicsLayer.record {
                                        this@drawWithContent.drawContent()
                                    }
                                    drawLayer(storyGraphicsLayer)
                                }
                            ) {
                                StoryCardVisualCanvas(
                                    payload = payload,
                                    theme = selectedTheme,
                                    showGpa = showGpa,
                                    showStudentId = showStudentId,
                                    showStamp = showOfficialStamp,
                                    showQr = showQrVerification
                                )
                            }
                        }
                        ExportFormatMode.OFFICIAL_DOC_A4 -> {
                            OfficialDocumentVisualCanvas(
                                payload = payload,
                                showGpa = showGpa,
                                showStudentId = showStudentId,
                                showStamp = showOfficialStamp,
                                showQr = showQrVerification
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons (Share, Copy, Export PDF/Image)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        executeShareAction(context, payload, selectedFormat, showGpa)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandIndigo600)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedFormat == ExportFormatMode.STORY_CARD_9_16) "اشتراک استوری" else "اشتراک مدرک",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        executeCopyTextSummary(context, payload)
                    },
                    modifier = Modifier.height(46.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "کپی متن", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        executeSaveFileReal(
                            context = context,
                            format = selectedFormat,
                            payload = payload,
                            theme = selectedTheme,
                            showGpa = showGpa,
                            showStudentId = showStudentId,
                            showStamp = showOfficialStamp,
                            showQr = showQrVerification,
                            graphicsLayer = storyGraphicsLayer,
                            scope = scope
                        )
                    },
                    modifier = Modifier.height(46.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = if (selectedFormat == ExportFormatMode.STORY_CARD_9_16) Icons.Default.Download else Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (selectedFormat == ExportFormatMode.STORY_CARD_9_16) "ذخیره عکس" else "خروجی PDF",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * 9:16 Social Story Visual Layout
 */
@Composable
private fun StoryCardVisualCanvas(
    payload: ExportSourcePayload,
    theme: ExportThemePreset,
    showGpa: Boolean,
    showStudentId: Boolean,
    showStamp: Boolean,
    showQr: Boolean
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .wrapContentHeight()
            .shadow(16.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.2.dp, theme.primaryColor.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(theme.backgroundColors))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Brand Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = theme.primaryColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = theme.primaryColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "STUDENT OS • 2026",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = theme.primaryColor,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = theme.accentColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = theme.accentColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "گزارش شخصی",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.accentColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Profile Avatar & Info
                when (payload) {
                    is ExportSourcePayload.Passport -> {
                        val profile = payload.profile
                        val progress = payload.progress

                        Surface(
                            shape = CircleShape,
                            color = theme.primaryColor.copy(alpha = 0.18f),
                            border = BorderStroke(2.dp, theme.primaryColor),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (profile.name.isNotEmpty()) profile.name.take(1) else "🎓",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = theme.textColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = profile.name.ifEmpty { "دانشجوی گرامی" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = theme.textColor
                        )

                        if (showStudentId) {
                            Text(
                                text = "کد دانشجویی: ${profile.studentId.ifEmpty { "وارد نشده" }}",
                                fontSize = 10.sp,
                                color = theme.textColor.copy(alpha = 0.7f)
                            )
                        }

                        Text(
                            text = profile.term.ifEmpty { "ترم ۳ مهندسی شیمی" },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.primaryColor
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Bento Stats Box for Passport
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (theme.isDark) Color(0xFF141A29).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, theme.primaryColor.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (showGpa && progress != null) {
                                    val gpaVal = when (val state = progress.gpaState) {
                                        is GpaState.Known -> String.format(Locale.US, "%.2f", state.value)
                                        is GpaState.PartiallyCalculated -> String.format(Locale.US, "%.2f", state.currentSemesterGpa)
                                        GpaState.Unknown -> "—"
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = "معدل کل تجمیعی", fontSize = 10.sp, color = Slate400)
                                            Text(
                                                text = gpaVal,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Black,
                                                color = theme.accentColor
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Emerald500.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "معدل الف ⭐️",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Emerald500,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = Slate700.copy(alpha = 0.4f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StatStoryItem(
                                        label = "واحدهای پاس‌شده",
                                        value = "${progress?.passedCredits ?: "—"} واحد",
                                        color = theme.textColor
                                    )
                                    StatStoryItem(
                                        label = "واحدهای مانده",
                                        value = "${progress?.remainingCredits ?: "—"} واحد",
                                        color = theme.textColor
                                    )
                                    StatStoryItem(
                                        label = "پیشرفت",
                                        value = "${progress?.progressPercentage?.toInt() ?: "—"}٪",
                                        color = theme.primaryColor
                                    )
                                }
                            }
                        }
                    }
                    is ExportSourcePayload.Grades -> {
                        val profile = payload.profile
                        val gpaFormatted = academicGpaText(payload)
                        val isHonors = payload.termGpa >= 17.0

                        Surface(
                            shape = CircleShape,
                            color = theme.primaryColor.copy(alpha = 0.18f),
                            border = BorderStroke(2.dp, theme.primaryColor),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = theme.accentColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = profile.name.ifEmpty { "دانشجو" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = theme.textColor
                        )

                        Text(
                            text = "کارنامه ترم جاری",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryColor
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // GPA Showcase Bento
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (theme.isDark) Color(0xFF141A29).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, theme.primaryColor.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (showGpa) {
                                    Text(text = "معدل پیش‌بینی ترم ۳", fontSize = 10.sp, color = Slate400)
                                    Text(
                                        text = gpaFormatted,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isHonors) Amber500 else theme.primaryColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isHonors) Amber500.copy(alpha = 0.15f) else Emerald500.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (isHonors) "نمره‌های ثبت‌شده: عملکرد عالی" else "براساس نمره‌های ثبت‌شده",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isHonors) Amber500 else Emerald500,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    StatStoryItem(
                                        label = "واحدهای ترم",
                                        value = "${payload.totalUnits} واحد",
                                        color = theme.textColor
                                    )
                                    StatStoryItem(
                                        label = "تعداد دروس",
                                        value = "${payload.grades.size} درس",
                                        color = theme.textColor
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer with Stamp & Mock QR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showStamp) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = theme.accentColor.copy(alpha = 0.12f),
                            border = BorderStroke(0.8.dp, theme.accentColor.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = theme.accentColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Student OS",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = theme.accentColor
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }


                }
            }
        }
    }
}

@Composable
private fun StatStoryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 8.5.sp, color = Slate400)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

/**
 * Official Institutional A4 Document Visual Layout
 */
@Composable
private fun OfficialDocumentVisualCanvas(
    payload: ExportSourcePayload,
    showGpa: Boolean,
    showStudentId: Boolean,
    showStamp: Boolean,
    showQr: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .shadow(12.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFCFD)),
        border = BorderStroke(1.dp, Color(0xFFD1D5DB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Ministry / Institutional Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "جمهوری اسلامی ایران",
                    fontSize = 9.sp,
                    color = Color(0xFF4B5563),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "وزارت علوم، تحقیقات و فناوری",
                    fontSize = 10.sp,
                    color = Color(0xFF1F2937),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "سامانه مدیریت آموزش و پرونده تحصیلی دانشجو (Student OS)",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1E3A8A)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color(0xFF93C5FD),
                    thickness = 1.5.dp
                )
            }

            // Student Identity Metadata Grid
            val studentName = when (payload) {
                is ExportSourcePayload.Passport -> payload.profile.name.ifEmpty { "دانشجوی گرامی" }
                is ExportSourcePayload.Grades -> payload.profile.name.ifEmpty { "دانشجوی گرامی" }
            }
            val studentId = when (payload) {
                is ExportSourcePayload.Passport -> payload.profile.studentId.ifEmpty { "وارد نشده" }
                is ExportSourcePayload.Grades -> payload.profile.studentId.ifEmpty { "وارد نشده" }
            }
            val fieldMajor = "مهندسی شیمی - مقطع کارشناسی پیوسته"

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF3F4F6),
                border = BorderStroke(0.6.dp, Color(0xFFE5E7EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "نام و نام خانوادگی: $studentName", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                        if (showStudentId) {
                            Text(text = "شماره دانشجویی: $studentId", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "رشته تحصیلی: $fieldMajor", fontSize = 8.5.sp, color = Color(0xFF374151))
                        Text(text = "نیمسال: اول ۱۴۰۲-۱۴۰۳", fontSize = 8.5.sp, color = Color(0xFF374151))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body Table (Passport progress or Grade list)
            when (payload) {
                is ExportSourcePayload.Passport -> {
                    val progress = payload.progress
                    Text(
                        text = "پیشرفت بر پایه چارت و سوابق ثبت‌شده:",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    DocTableRow("کل واحدهای چارت", "${progress?.totalRequiredCredits ?: "—"} واحد")
                    DocTableRow("واحدهای گذرانده‌شده ثبت‌شده", "${progress?.passedCredits ?: "—"} واحد")
                    DocTableRow("واحدهای در حال گذراندن (ترم جاری)", "${progress?.currentCredits ?: "—"} واحد")
                    DocTableRow("واحدهای باقیمانده تا فراغت", "${progress?.remainingCredits ?: "—"} واحد")
                    if (showGpa && progress != null) {
                        val gpaVal = when (val state = progress.gpaState) {
                            is GpaState.Known -> String.format(Locale.US, "%.2f", state.value)
                            is GpaState.PartiallyCalculated -> String.format(Locale.US, "%.2f", state.currentSemesterGpa)
                            GpaState.Unknown -> "—"
                        }
                        DocTableRow("معدل کل تجمیعی", gpaVal, isHighlight = true)
                    }
                }
                is ExportSourcePayload.Grades -> {
                    Text(
                        text = "ریز نمرات ثبت‌شده ترم جاری:",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    payload.grades.take(5).forEach { g ->
                        val total = g.midtermGrade + g.finalGrade
                        DocTableRow(
                            title = "${g.courseName} (${g.units} واحد)",
                            value = if (g.hasRecordedScore()) "${String.format(Locale.US, "%.1f", total)} از ۲۰" else "وارد نشده",
                            isHighlight = total >= 17.0
                        )
                    }

                    if (showGpa) {
                        DocTableRow(
                            title = "معدل وزنی ترم",
                            value = "${academicGpaText(payload)}",
                            isHighlight = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Official Signature & Seals Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {


                if (showStamp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E40AF).copy(alpha = 0.08f),
                            border = BorderStroke(1.2.dp, Color(0xFF1E40AF)),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Student OS", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                                    Text(text = "گزارش شخصی", fontSize = 6.5.sp, color = Color(0xFF1E40AF))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "گزارش تهیه‌شده توسط دانشجو", fontSize = 7.5.sp, color = Color(0xFF4B5563))
                    }
                }
            }
        }
    }
}

@Composable
private fun DocTableRow(title: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isHighlight) Color(0xFFEFF6FF) else Color.Transparent)
            .padding(vertical = 3.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 9.sp,
            color = if (isHighlight) Color(0xFF1E3A8A) else Color(0xFF374151),
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = 9.sp,
            color = if (isHighlight) Color(0xFF1D4ED8) else Color(0xFF111827),
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Share execution helper using standard Android Intent
 */
private fun executeShareAction(
    context: Context,
    payload: ExportSourcePayload,
    format: ExportFormatMode,
    showGpa: Boolean
) {
    val shareSubject = when (payload) {
        is ExportSourcePayload.Passport -> "شناسنامه تحصیلی آکادمیک - Student OS"
        is ExportSourcePayload.Grades -> "کارنامه و وضعیت تحصیلی - Student OS"
    }

    val shareBody = buildString {
        appendLine("🎓 $shareSubject")
        when (payload) {
            is ExportSourcePayload.Passport -> {
                appendLine("دانشجو: ${payload.profile.name.ifEmpty { "دانشجو" }}")
                appendLine("رشته: ${payload.profile.term.ifEmpty { "مهندسی شیمی" }}")
                if (payload.progress != null) {
                    appendLine("📊 واحدهای گذرانده: ${payload.progress.passedCredits} از ${payload.progress.totalRequiredCredits} واحد")
                    appendLine("📈 پیشرفت کل: ${payload.progress.progressPercentage.toInt()}%")
                    if (showGpa) {
                        when (val state = payload.progress.gpaState) {
                            is GpaState.Known -> appendLine("⭐️ معدل کل: ${String.format(Locale.US, "%.2f", state.value)}")
                            is GpaState.PartiallyCalculated -> appendLine("⭐️ معدل ترم جاری: ${String.format(Locale.US, "%.2f", state.currentSemesterGpa)}")
                            GpaState.Unknown -> Unit
                        }
                    }
                }
            }
            is ExportSourcePayload.Grades -> {
                appendLine("دانشجو: ${payload.profile.name.ifEmpty { "دانشجو" }}")
                appendLine("📚 واحدهای ثبت‌شده: ${payload.totalUnits} واحد")
                if (showGpa) {
                    appendLine("⭐️ معدل نمره‌های ثبت‌شده: ${academicGpaText(payload)}")
                    appendLine("این گزارش شخصی براساس اطلاعات ثبت‌شده دانشجو است.")
                }
            }
        }
        appendLine("────────────")
        appendLine("صادرشده از طریق سامانه یکپارچه Student OS 2026")
    }

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, shareSubject)
        putExtra(Intent.EXTRA_TEXT, shareBody)
    }

    val chooser = Intent.createChooser(sendIntent, "اشتراک‌گذاری گزارش تحصیلی")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}

private fun executeCopyTextSummary(context: Context, payload: ExportSourcePayload) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val text = when (payload) {
        is ExportSourcePayload.Passport -> {
            "شناسنامه تحصیلی ${payload.profile.name} - پیشرفت: ${payload.progress?.progressPercentage?.toInt() ?: "—"}٪ - گذرانده: ${payload.progress?.passedCredits ?: "—"} واحد"
        }
        is ExportSourcePayload.Grades -> {
            "کارنامه تحصیلی ${payload.profile.name} - معدل ترم: ${academicGpaText(payload)} - تعداد واحد: ${payload.totalUnits}"
        }
    }
    val clip = ClipData.newPlainText("Academic Summary", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "خلاصه آکادمیک در حافظه کپی شد ✨", Toast.LENGTH_SHORT).show()
}

private fun executeSaveFileReal(
    context: Context,
    format: ExportFormatMode,
    payload: ExportSourcePayload,
    theme: ExportThemePreset,
    showGpa: Boolean,
    showStudentId: Boolean,
    showStamp: Boolean,
    showQr: Boolean,
    graphicsLayer: androidx.compose.ui.graphics.layer.GraphicsLayer?,
    scope: kotlinx.coroutines.CoroutineScope
) {
    scope.launch(Dispatchers.IO) {
        try {
            if (format == ExportFormatMode.STORY_CARD_9_16) {
                val bitmap: Bitmap = try {
                    if (graphicsLayer != null && graphicsLayer.size.width > 0 && graphicsLayer.size.height > 0) {
                        graphicsLayer.toImageBitmap().asAndroidBitmap()
                    } else {
                        AcademicExportManager.createHighResStoryBitmap(
                            payload, theme, showGpa, showStudentId, showStamp, showQr
                        )
                    }
                } catch (_: Exception) {
                    AcademicExportManager.createHighResStoryBitmap(
                        payload, theme, showGpa, showStudentId, showStamp, showQr
                    )
                }

                val uri = AcademicExportManager.saveBitmapToGallery(context, bitmap)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "تصویر کارت استوری در گالری ذخیره شد (Pictures/StudentOS) 📸",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                val uri = AcademicExportManager.generateAndSaveAcademicPdf(
                    context = context,
                    payload = payload,
                    showGpa = showGpa,
                    showStudentId = showStudentId,
                    showStamp = showStamp,
                    showQr = showQr
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "گزارش شخصی PDF در پوشه دانلودها ذخیره شد (Downloads/StudentOS) 📄",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "خطا در ایجاد یا ذخیره فایل: ${e.localizedMessage ?: e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
