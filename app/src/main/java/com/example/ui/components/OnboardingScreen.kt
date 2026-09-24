package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CurriculumCourseEntity
import com.example.data.parser.ParsedCourseDraft
import com.example.data.parser.RegistrationTextParser
import com.example.data.seed.CurriculumSeedData

data class OnboardingColors(
    val DarkCanvasTop: Color,
    val DarkCanvasMid: Color,
    val DarkCanvasBottom: Color,
    val SurfaceCardBg: Color,
    val SurfaceCardBorder: Color,
    val InputFieldBg: Color,
    val InputFieldBorder: Color,
    val AccentPrimary: Color,
    val AccentPrimaryBright: Color,
    val AccentSecondary: Color,
    val AccentSuccess: Color,
    val AccentWarning: Color,
    val TextPureWhite: Color,
    val TextMutedWhite: Color,
    val TextSubtleGray: Color,
    val TextPlaceholder: Color,
    val isDark: Boolean
)

@Composable
private fun rememberOnboardingColors(): OnboardingColors {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface.luminance() < 0.5f

    return remember(isDark, colorScheme) {
        if (isDark) {
            OnboardingColors(
                DarkCanvasTop = colorScheme.surface,
                DarkCanvasMid = colorScheme.surfaceContainerLowest,
                DarkCanvasBottom = colorScheme.surface,
                SurfaceCardBg = colorScheme.surfaceContainerLow,
                SurfaceCardBorder = colorScheme.outlineVariant.copy(alpha = 0.35f),
                InputFieldBg = colorScheme.surfaceContainer,
                InputFieldBorder = colorScheme.outline.copy(alpha = 0.3f),
                AccentPrimary = colorScheme.primary,
                AccentPrimaryBright = colorScheme.primary,
                AccentSecondary = colorScheme.tertiary,
                AccentSuccess = Color(0xFF10B981),
                AccentWarning = Color(0xFFF59E0B),
                TextPureWhite = colorScheme.onSurface,
                TextMutedWhite = colorScheme.onSurfaceVariant,
                TextSubtleGray = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                TextPlaceholder = colorScheme.outline,
                isDark = true
            )
        } else {
            OnboardingColors(
                DarkCanvasTop = colorScheme.surface,
                DarkCanvasMid = colorScheme.surfaceContainerLowest,
                DarkCanvasBottom = colorScheme.surface,
                SurfaceCardBg = colorScheme.surfaceContainerLow,
                SurfaceCardBorder = colorScheme.outlineVariant.copy(alpha = 0.45f),
                InputFieldBg = colorScheme.surfaceContainer,
                InputFieldBorder = colorScheme.outline.copy(alpha = 0.35f),
                AccentPrimary = colorScheme.primary,
                AccentPrimaryBright = colorScheme.primary,
                AccentSecondary = colorScheme.tertiary,
                AccentSuccess = Color(0xFF059669),
                AccentWarning = Color(0xFFD97706),
                TextPureWhite = colorScheme.onSurface,
                TextMutedWhite = colorScheme.onSurfaceVariant,
                TextSubtleGray = colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                TextPlaceholder = colorScheme.outline,
                isDark = false
            )
        }
    }
}

// Extended Universities List
private val COMPREHENSIVE_UNIVERSITIES = listOf(
    "دانشگاه مراغه",
    "دانشگاه تبریز",
    "دانشگاه صنعتی سهند",
    "دانشگاه تهران",
    "دانشگاه صنعتی شریف",
    "دانشگاه صنعتی امیرکبیر (پلی‌تکنیک)",
    "دانشگاه علم و صنعت ایران",
    "دانشگاه شهید بهشتی",
    "دانشگاه فردوسی مشهد",
    "دانشگاه شیراز",
    "دانشگاه صنعتی اصفهان",
    "دانشگاه اصفهان",
    "دانشگاه خواجه نصیرالدین طوسی",
    "دانشگاه ارومیه",
    "دانشگاه زنجان",
    "دانشگاه بناب",
    "دانشگاه بوعلی سینا همدان",
    "دانشگاه رازی کرمانشاه",
    "دانشگاه گیلان",
    "دانشگاه مازندران",
    "دانشگاه یزد",
    "دانشگاه کاشان",
    "دانشگاه فرهنگیان",
    "دانشگاه پیام نور",
    "دانشگاه آزاد اسلامی",
    "دانشگاه فنی و حرفه‌ای",
    "سایر دانشگاه‌ها"
)

// Maragheh University Faculties & Extensive Majors
data class FacultyInfo(
    val name: String,
    val iconEmoji: String,
    val majors: List<String>
)

private val MARAGHEH_FACULTIES = listOf(
    FacultyInfo(
        name = "فنی و مهندسی",
        iconEmoji = "⚙️",
        majors = listOf(
            "مهندسی شیمی",
            "مهندسی عمران",
            "مهندسی مکانیک",
            "مهندسی کامپیوتر",
            "مهندسی مواد و متالورژی",
            "مهندسی برق",
            "مهندسی پلیمر",
            "مهندسی معدن",
            "مهندسی صنایع"
        )
    ),
    FacultyInfo(
        name = "علوم پایه",
        iconEmoji = "🧪",
        majors = listOf(
            "شیمی کاربردی",
            "شیمی محض",
            "ریاضیات و کاربردها",
            "علوم کامپیوتر",
            "فیزیک",
            "زیست‌شناسی سلولی و مولکولی",
            "زیست‌شناسی گیاهی",
            "زیست‌شناسی جانوری",
            "زیست‌فناوری (بیوتکنولوژی)",
            "آمار و کاربردها"
        )
    ),
    FacultyInfo(
        name = "کشاورزی",
        iconEmoji = "🌾",
        majors = listOf(
            "مهندسی تولید و ژنتیک گیاهی",
            "علوم و مهندسی باغبانی",
            "علوم دامی",
            "مهندسی آب",
            "گیاه‌پزشکی",
            "علوم و مهندسی خاک",
            "صنایع غذایی",
            "اقتصاد کشاورزی و ترویج"
        )
    ),
    FacultyInfo(
        name = "علوم انسانی",
        iconEmoji = "📚",
        majors = listOf(
            "حقوق",
            "زبان و ادبیات فارسی",
            "زبان و ادبیات انگلیسی",
            "آموزش زبان انگلیسی",
            "روانشناسی",
            "علوم تربیتی",
            "معارف اسلامی و علوم قرآن",
            "مدیریت بازرگانی",
            "مدیریت دولتی",
            "حسابداری"
        )
    )
)

private val ALL_ENTRY_YEARS = listOf(1405, 1404, 1403, 1402, 1401, 1400, 1399, 1398, 1397)
private val ALL_SEMESTERS = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

enum class SetupMode {
    QUICK_SETUP,    // ⚡ شروع فوق‌سریع دانشجویان ترم ۴ و بالاتر
    PASTE_TEXT,     // 📋 چسباندن متن انتخاب واحد گلستان
    SCREENSHOT_OCR, // 📸 ایمپورت از اسکرین‌شات انتخاب واحد
    CURRICULUM,     // 📚 انتخاب از چارت تحصیلی
    SKIP            // ⏩ ورود مستقیم
}

@Composable
fun OnboardingScreen(
    onCompleteQuickSetup: (name: String, studentId: String, university: String, major: String, entryYear: Int, currentSemester: Int, passedCredits: Int, currentGpa: Double, courses: List<CurriculumCourseEntity>) -> Unit,
    onCompleteTextImport: (drafts: List<ParsedCourseDraft>, name: String, studentId: String, university: String, major: String, entryYear: Int, currentSemester: Int) -> Unit = { _, _, _, _, _, _, _ -> },
    onSkip: (name: String, studentId: String, university: String, major: String, entryYear: Int, currentSemester: Int) -> Unit = { _, _, _, _, _, _ -> },
    onOpenPrivacyPolicy: () -> Unit = {},
    initialName: String = "",
    initialStudentId: String = "",
    initialUniversity: String = "",
    initialMajor: String = "",
    initialEntryYear: Int = 1402,
    initialSemester: Int = 3,
    modifier: Modifier = Modifier
) {
    val (
        DarkCanvasTop,
        DarkCanvasMid,
        DarkCanvasBottom,
        SurfaceCardBg,
        SurfaceCardBorder,
        InputFieldBg,
        InputFieldBorder,
        AccentPrimary,
        AccentPrimaryBright,
        AccentSecondary,
        AccentSuccess,
        AccentWarning,
        TextPureWhite,
        TextMutedWhite,
        TextSubtleGray,
        TextPlaceholder,
        isDark
    ) = rememberOnboardingColors()

    val haptic = LocalHapticFeedback.current
    var currentStep by remember { mutableIntStateOf(1) } // 1: Info, 2: Setup Mode, 3: Review

    // Identity & Academic Info State (initialized from existing state if available)
    var studentName by remember { mutableStateOf(initialName) }
    var studentId by remember { mutableStateOf(initialStudentId) }
    var university by remember { mutableStateOf(initialUniversity.ifBlank { "دانشگاه مراغه" }) }
    var major by remember { mutableStateOf(initialMajor.ifBlank { "مهندسی شیمی" }) }
    var entryYear by remember { mutableIntStateOf(if (initialEntryYear > 0) initialEntryYear else 1402) }
    var currentSemester by remember { mutableIntStateOf(if (initialSemester > 0) initialSemester else 3) }

    // Setup Option State
    var selectedMode by remember { mutableStateOf(SetupMode.QUICK_SETUP) }

    // Quick Setup States (Dynamically compute passed credits for the selected semester: e.g. (3 - 1) * 18 = 36)
    var passedCreditsInput by remember { mutableStateOf("36") }
    var currentGpaInput by remember { mutableStateOf("17.40") }

    // Text Paste States
    var rawRegistrationText by remember { mutableStateOf("") }
    var parsedDrafts by remember { mutableStateOf<List<ParsedCourseDraft>>(emptyList()) }

    // Dynamic Curriculum Selection States based on selected major and current semester
    val activeMajorCourses = remember(major) {
        CurriculumSeedData.getCoursesForMajor(major)
    }
    val activeSemesterCourses = remember(major, currentSemester, activeMajorCourses) {
        val filtered = activeMajorCourses.filter { it.recommendedSemester == currentSemester }
        if (filtered.isNotEmpty()) filtered else activeMajorCourses.take(6)
    }
    var selectedCurriculumCourseIds by remember(activeSemesterCourses) {
        mutableStateOf(activeSemesterCourses.map { it.id }.toSet())
    }

    // Auto-update estimated passed credits when user changes currentSemester
    LaunchedEffect(currentSemester) {
        val estimatedPassed = ((currentSemester - 1).coerceAtLeast(0) * 18).toString()
        passedCreditsInput = estimatedPassed
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        DarkCanvasTop,
                        DarkCanvasMid,
                        DarkCanvasBottom
                    )
                )
            )
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header & Steps Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentStep--
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = TextPureWhite
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }

                // Step Indicator Pills
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..3).forEach { step ->
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (currentStep == step) 30.dp else 12.dp)
                                .clip(CircleShape)
                                .background(if (currentStep >= step) AccentPrimary else SurfaceCardBorder)
                        )
                    }
                }

                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSkip(
                            studentName.ifBlank { "دانشجو" },
                            studentId.trim(),
                            university,
                            major,
                            entryYear,
                            currentSemester
                        )
                    }
                ) {
                    Text(
                        text = "رد شدن ⏩",
                        color = TextSubtleGray,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Step Content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding_step"
            ) { step ->
                when (step) {
                    1 -> StepOneAcademicIdentity(
                        name = studentName,
                        onNameChange = { studentName = it },
                        studentId = studentId,
                        onStudentIdChange = { studentId = it },
                        university = university,
                        onUniversityChange = { university = it },
                        major = major,
                        onMajorChange = { major = it },
                        entryYear = entryYear,
                        onEntryYearChange = { entryYear = it },
                        currentSemester = currentSemester,
                        onSemesterChange = { currentSemester = it },
                        onNext = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentStep = 2
                        }
                    )
                    2 -> StepTwoSetupMode(
                        selectedMode = selectedMode,
                        onSelectMode = { selectedMode = it },
                        currentSemester = currentSemester,
                        passedCredits = passedCreditsInput,
                        onPassedCreditsChange = { passedCreditsInput = it },
                        currentGpa = currentGpaInput,
                        onCurrentGpaChange = { currentGpaInput = it },
                        rawText = rawRegistrationText,
                        onRawTextChange = {
                            rawRegistrationText = it
                            parsedDrafts = RegistrationTextParser.parse(it)
                        },
                        parsedDrafts = parsedDrafts,
                        onParsedDraftsChange = { parsedDrafts = it },
                        availableCourses = activeSemesterCourses,
                        selectedCourseIds = selectedCurriculumCourseIds,
                        onToggleCourse = { id ->
                            selectedCurriculumCourseIds = if (selectedCurriculumCourseIds.contains(id)) {
                                selectedCurriculumCourseIds - id
                            } else {
                                selectedCurriculumCourseIds + id
                            }
                        },
                        onNext = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (selectedMode == SetupMode.PASTE_TEXT && parsedDrafts.isEmpty() && rawRegistrationText.isNotBlank()) {
                                parsedDrafts = RegistrationTextParser.parse(rawRegistrationText)
                            }
                            currentStep = 3
                        }
                    )
                    3 -> StepThreeReviewAndConfirm(
                        name = studentName,
                        studentId = studentId,
                        university = university,
                        major = major,
                        entryYear = entryYear,
                        semester = currentSemester,
                        mode = selectedMode,
                        passedCredits = passedCreditsInput.toIntOrNull() ?: ((currentSemester - 1).coerceAtLeast(0) * 18),
                        currentGpa = currentGpaInput.toDoubleOrNull() ?: 17.40,
                        parsedDrafts = parsedDrafts,
                        selectedCourses = activeSemesterCourses.filter { selectedCurriculumCourseIds.contains(it.id) },
                        onConfirm = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            when (selectedMode) {
                                SetupMode.QUICK_SETUP, SetupMode.CURRICULUM -> {
                                    val activeCourses = if (selectedMode == SetupMode.CURRICULUM) {
                                        activeSemesterCourses.filter { selectedCurriculumCourseIds.contains(it.id) }
                                    } else {
                                        activeSemesterCourses
                                    }
                                    onCompleteQuickSetup(
                                        studentName.ifBlank { "دانشجو" },
                                        studentId.trim(),
                                        university,
                                        major,
                                        entryYear,
                                        currentSemester,
                                        passedCreditsInput.toIntOrNull() ?: ((currentSemester - 1).coerceAtLeast(0) * 18),
                                        currentGpaInput.toDoubleOrNull() ?: 17.40,
                                        activeCourses
                                    )
                                }
                                SetupMode.PASTE_TEXT, SetupMode.SCREENSHOT_OCR -> {
                                    if (parsedDrafts.isNotEmpty()) {
                                        onCompleteTextImport(
                                            parsedDrafts,
                                            studentName.ifBlank { "دانشجو" },
                                            studentId.trim(),
                                            university,
                                            major,
                                            entryYear,
                                            currentSemester
                                        )
                                    } else {
                                        onSkip(
                                            studentName.ifBlank { "دانشجو" },
                                            studentId.trim(),
                                            university,
                                            major,
                                            entryYear,
                                            currentSemester
                                        )
                                    }
                                }
                                SetupMode.SKIP -> onSkip(
                                    studentName.ifBlank { "دانشجو" },
                                    studentId.trim(),
                                    university,
                                    major,
                                    entryYear,
                                    currentSemester
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenPrivacyPolicy() }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Policy,
                        contentDescription = "سیاست حفظ حریم خصوصی",
                        tint = AccentPrimaryBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "سیاست حفظ حریم خصوصی و امنیت داده‌ها",
                        fontSize = 11.5.sp,
                        color = AccentPrimaryBright,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun StepOneAcademicIdentity(
    name: String,
    onNameChange: (String) -> Unit,
    studentId: String,
    onStudentIdChange: (String) -> Unit,
    university: String,
    onUniversityChange: (String) -> Unit,
    major: String,
    onMajorChange: (String) -> Unit,
    entryYear: Int,
    onEntryYearChange: (Int) -> Unit,
    currentSemester: Int,
    onSemesterChange: (Int) -> Unit,
    onNext: () -> Unit
) {
    val (
        DarkCanvasTop,
        DarkCanvasMid,
        DarkCanvasBottom,
        SurfaceCardBg,
        SurfaceCardBorder,
        InputFieldBg,
        InputFieldBorder,
        AccentPrimary,
        AccentPrimaryBright,
        AccentSecondary,
        AccentSuccess,
        AccentWarning,
        TextPureWhite,
        TextMutedWhite,
        TextSubtleGray,
        TextPlaceholder,
        isDark
    ) = rememberOnboardingColors()

    var selectedFacultyIndex by remember { mutableIntStateOf(0) }
    var uniSearchQuery by remember { mutableStateOf("") }
    var isCustomUniInput by remember { mutableStateOf(false) }
    var isCustomMajorInput by remember { mutableStateOf(false) }

    val filteredUniversities = remember(uniSearchQuery) {
        if (uniSearchQuery.isBlank()) {
            COMPREHENSIVE_UNIVERSITIES
        } else {
            COMPREHENSIVE_UNIVERSITIES.filter { it.contains(uniSearchQuery.trim(), ignoreCase = true) }
        }
    }

    val activeFacultyMajors = remember(selectedFacultyIndex) {
        if (selectedFacultyIndex == 0) {
            MARAGHEH_FACULTIES.flatMap { it.majors }
        } else {
            MARAGHEH_FACULTIES.getOrNull(selectedFacultyIndex - 1)?.majors ?: emptyList()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = AccentPrimary.copy(alpha = 0.18f),
            border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.4f)),
            modifier = Modifier.size(68.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = AccentPrimaryBright,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "راه‌اندازی هوشمند دستیار تحصیلی",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPureWhite
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "مشخصات دانشگاهی خود را وارد کنید تا چارت، تقویم و رادار تحصیلی پیکربندی شود",
            fontSize = 11.5.sp,
            color = TextSubtleGray,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Main Academic Identity Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 0. STUDENT PERSONAL IDENTITY
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text(
                            text = "نام دانشجو:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPureWhite
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = onNameChange,
                            placeholder = { Text("مثال: علی رضایی", color = TextPlaceholder, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = AccentPrimaryBright,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPureWhite,
                                unfocusedTextColor = TextMutedWhite,
                                focusedBorderColor = AccentPrimaryBright,
                                unfocusedBorderColor = InputFieldBorder,
                                focusedContainerColor = InputFieldBg,
                                unfocusedContainerColor = InputFieldBg,
                                cursorColor = AccentPrimaryBright
                            )
                        )
                    }

                    Column(modifier = Modifier.weight(1.1f)) {
                        Text(
                            text = "شماره دانشجویی:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPureWhite
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = studentId,
                            onValueChange = onStudentIdChange,
                            placeholder = { Text("مثال: ۴۰۲۰۱۲۳۴", color = TextPlaceholder, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = AccentSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPureWhite,
                                unfocusedTextColor = TextMutedWhite,
                                focusedBorderColor = AccentPrimaryBright,
                                unfocusedBorderColor = InputFieldBorder,
                                focusedContainerColor = InputFieldBg,
                                unfocusedContainerColor = InputFieldBg,
                                cursorColor = AccentPrimaryBright
                            )
                        )
                    }
                }

                // 1. UNIVERSITY SELECTION
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "دانشگاه محل تحصیل:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPureWhite
                        )
                        TextButton(onClick = { isCustomUniInput = !isCustomUniInput }) {
                            Text(
                                text = if (isCustomUniInput) "انتخاب از لیست 📋" else "تایپ دستی نام دانشگاه ✍️",
                                fontSize = 10.5.sp,
                                color = AccentSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (isCustomUniInput) {
                        OutlinedTextField(
                            value = university,
                            onValueChange = onUniversityChange,
                            placeholder = { Text("مثال: دانشگاه مراغه", color = TextPlaceholder, fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPureWhite,
                                unfocusedTextColor = TextMutedWhite,
                                focusedBorderColor = AccentPrimaryBright,
                                unfocusedBorderColor = InputFieldBorder,
                                focusedContainerColor = InputFieldBg,
                                unfocusedContainerColor = InputFieldBg,
                                cursorColor = AccentPrimaryBright
                            )
                        )
                    } else {
                        // University Filter Chips Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            filteredUniversities.forEach { uni ->
                                val isSelected = university == uni
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onUniversityChange(uni) },
                                    label = {
                                        Text(
                                            text = uni,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextMutedWhite
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentPrimary,
                                        selectedLabelColor = Color.White,
                                        containerColor = InputFieldBg,
                                        labelColor = TextMutedWhite
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) AccentPrimaryBright else InputFieldBorder
                                    )
                                )
                            }
                        }
                    }
                }

                // 2. FACULTY & MAJOR SELECTION (MARAGHEH SPECIALIZED)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "دانشکده و رشته تحصیلی:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPureWhite
                        )
                        TextButton(onClick = { isCustomMajorInput = !isCustomMajorInput }) {
                            Text(
                                text = if (isCustomMajorInput) "انتخاب از دانشکده‌ها 🏛️" else "تایپ رشته دلخواه ✍️",
                                fontSize = 10.5.sp,
                                color = AccentSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (isCustomMajorInput) {
                        OutlinedTextField(
                            value = major,
                            onValueChange = onMajorChange,
                            placeholder = { Text("مثال: مهندسی شیمی / شیمی کاربردی", color = TextPlaceholder, fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPureWhite,
                                unfocusedTextColor = TextMutedWhite,
                                focusedBorderColor = AccentPrimaryBright,
                                unfocusedBorderColor = InputFieldBorder,
                                focusedContainerColor = InputFieldBg,
                                unfocusedContainerColor = InputFieldBg,
                                cursorColor = AccentPrimaryBright
                            )
                        )
                    } else {
                        // Faculty Tabs Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedFacultyIndex == 0,
                                onClick = { selectedFacultyIndex = 0 },
                                label = {
                                    Text(
                                        text = "همه رشته‌ها 🌐",
                                        fontSize = 10.5.sp,
                                        fontWeight = if (selectedFacultyIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedFacultyIndex == 0) Color.White else TextMutedWhite
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentSecondary.copy(alpha = 0.85f),
                                    selectedLabelColor = Color.White,
                                    containerColor = InputFieldBg,
                                    labelColor = TextMutedWhite
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedFacultyIndex == 0,
                                    borderColor = if (selectedFacultyIndex == 0) AccentSecondary else InputFieldBorder
                                )
                            )

                            MARAGHEH_FACULTIES.forEachIndexed { idx, fac ->
                                val isFacSelected = selectedFacultyIndex == idx + 1
                                FilterChip(
                                    selected = isFacSelected,
                                    onClick = { selectedFacultyIndex = idx + 1 },
                                    label = {
                                        Text(
                                            text = "${fac.iconEmoji} ${fac.name}",
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isFacSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isFacSelected) Color.White else TextMutedWhite
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentPrimary,
                                        selectedLabelColor = Color.White,
                                        containerColor = InputFieldBg,
                                        labelColor = TextMutedWhite
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isFacSelected,
                                        borderColor = if (isFacSelected) AccentPrimaryBright else InputFieldBorder
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Majors Chip Flow Row (Scrollable)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            activeFacultyMajors.forEach { m ->
                                val isSelected = major == m
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onMajorChange(m) },
                                    label = {
                                        Text(
                                            text = m,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextMutedWhite
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentPrimary,
                                        selectedLabelColor = Color.White,
                                        containerColor = InputFieldBg,
                                        labelColor = TextMutedWhite
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) AccentPrimaryBright else InputFieldBorder
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. ENTRY YEAR (ورودی‌ها)
                Column {
                    Text(
                        text = "سال ورود (ورودی):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPureWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ALL_ENTRY_YEARS.forEach { year ->
                            val isSelected = entryYear == year
                            FilterChip(
                                selected = isSelected,
                                onClick = { onEntryYearChange(year) },
                                label = {
                                    Text(
                                        text = if (year == 1405) "ورودی ۱۴۰۵ (جدید)" else if (year == 1397) "۱۳۹۷ و قبل" else "$year",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextMutedWhite
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = InputFieldBg,
                                    labelColor = TextMutedWhite
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) AccentPrimaryBright else InputFieldBorder
                                )
                            )
                        }
                    }
                }

                // 4. CURRENT SEMESTER (ترم جاری)
                Column {
                    Text(
                        text = "ترم تحصیلی جاری:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPureWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ALL_SEMESTERS.forEach { sem ->
                            val isSelected = currentSemester == sem
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSemesterChange(sem) },
                                label = {
                                    Text(
                                        text = if (sem == 10) "ترم ۱۰ به بعد" else "ترم $sem",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextMutedWhite
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = InputFieldBg,
                                    labelColor = TextMutedWhite
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) AccentPrimaryBright else InputFieldBorder
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
        ) {
            Text(
                text = "مرحله بعد: استقرار برنامه ترم 🚀",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StepTwoSetupMode(
    selectedMode: SetupMode,
    onSelectMode: (SetupMode) -> Unit,
    currentSemester: Int,
    passedCredits: String,
    onPassedCreditsChange: (String) -> Unit,
    currentGpa: String,
    onCurrentGpaChange: (String) -> Unit,
    rawText: String,
    onRawTextChange: (String) -> Unit,
    parsedDrafts: List<ParsedCourseDraft>,
    onParsedDraftsChange: (List<ParsedCourseDraft>) -> Unit,
    availableCourses: List<CurriculumCourseEntity>,
    selectedCourseIds: Set<String>,
    onToggleCourse: (String) -> Unit,
    onNext: () -> Unit
) {
    val (
        DarkCanvasTop,
        DarkCanvasMid,
        DarkCanvasBottom,
        SurfaceCardBg,
        SurfaceCardBorder,
        InputFieldBg,
        InputFieldBorder,
        AccentPrimary,
        AccentPrimaryBright,
        AccentSecondary,
        AccentSuccess,
        AccentWarning,
        TextPureWhite,
        TextMutedWhite,
        TextSubtleGray,
        TextPlaceholder,
        isDark
    ) = rememberOnboardingColors()
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "شیوه استقرار برنامه ترم",
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPureWhite
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "یکی از روش‌های زیر را برای ساخت فوری دیتابیس انتخاب کنید:",
            fontSize = 11.5.sp,
            color = TextSubtleGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mode Options Cards
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Option 1: Quick Academic Setup
            SetupOptionCard(
                title = "⚡ شروع فوق‌سریع ترم $currentSemester (پیشنهادی)",
                description = "تنها با وارد کردن واحدهای پاس‌شده و معدل فعلی، چارت و برنامه ترم $currentSemester بدون نیاز به ورود ترم‌های گذشته آماده می‌شود.",
                icon = Icons.Default.Speed,
                isSelected = selectedMode == SetupMode.QUICK_SETUP,
                onClick = { onSelectMode(SetupMode.QUICK_SETUP) }
            ) {
                if (selectedMode == SetupMode.QUICK_SETUP) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = passedCredits,
                            onValueChange = onPassedCreditsChange,
                            label = { Text("تعداد واحد پاس‌شده", fontSize = 11.sp, color = TextSubtleGray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPureWhite,
                                unfocusedTextColor = TextMutedWhite,
                                focusedBorderColor = AccentPrimaryBright,
                                unfocusedBorderColor = InputFieldBorder,
                                focusedContainerColor = InputFieldBg,
                                unfocusedContainerColor = InputFieldBg,
                                cursorColor = AccentPrimaryBright
                            )
                        )

                        OutlinedTextField(
                            value = currentGpa,
                            onValueChange = onCurrentGpaChange,
                            label = { Text("معدل کل فعلی (از ۲۰)", fontSize = 11.sp, color = TextSubtleGray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPureWhite,
                                unfocusedTextColor = TextMutedWhite,
                                focusedBorderColor = AccentPrimaryBright,
                                unfocusedBorderColor = InputFieldBorder,
                                focusedContainerColor = InputFieldBg,
                                unfocusedContainerColor = InputFieldBg,
                                cursorColor = AccentPrimaryBright
                            )
                        )
                    }
                }
            }

            // Option 2: Paste Text
            SetupOptionCard(
                title = "📋 چسباندن متن برگه انتخاب واحد (Paste Text)",
                description = "متن کپی‌شده از سامane گلستان یا سیستم سمیاد را اینجا قرار دهید تا ساعات و نام دروس به صورت خودکار استخراج شوند.",
                icon = Icons.Default.ContentPaste,
                isSelected = selectedMode == SetupMode.PASTE_TEXT,
                onClick = { onSelectMode(SetupMode.PASTE_TEXT) }
            ) {
                if (selectedMode == SetupMode.PASTE_TEXT) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "متن برگه را Paste کنید:", fontSize = 11.sp, color = TextSubtleGray)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = {
                                    val sample = """
                                        ترمودینامیک ۲ ترم ۴ ۳ واحد دوشنبه ۱۰ تا ۱۲ دکتر کریمی کلاس ۱۰۹
                                        مکانیک سیالات ۱ ترم ۴ ۳ واحد شنبه ۰۸:۰۰ تا ۱۰:۰۰ دکتر رضوی کلاس ۱۰۷
                                        آزمایشگاه فیزیک ۲ ترم ۲ ۱ واحد یکشنبه ۱۴ تا ۱۶ مهندس احمدی آز مرکزی
                                        کنترل فرایندها ترم ۶ ۳ واحد چهارشنبه ۱۰ تا ۱۲ دکتر حسینی کلاس ۱۱۰
                                        زبان تخصصی مهندسی ترم ۳ ۲ واحد سه‌شنبه ۱۶ تا ۱۸ استاد ناصری کلاس ۲۰۱
                                    """.trimIndent()
                                    onRawTextChange(sample)
                                }
                            ) {
                                Text("نمونه ترکیبی فاطی (چند ترمی) 🔀", fontSize = 10.sp, color = AccentSecondary)
                            }
                            TextButton(
                                onClick = {
                                    val sample = """
                                        مکانیک سیالات ۱ دکتر رضوی ۳ واحد شنبه ۱۰-۱۲ کلاس ۱۰۷
                                        انتقال حرارت ۱ دکتر محمدی ۳ واحد یکشنبه ۰۸-۱۰ کلاس ۱۰۸
                                        ترمودینامیک ۲ دکتر کریمی ۳ واحد دوشنبه ۱۰-۱۲ کلاس ۱۰۹
                                        ریاضیات مهندسی دکتر حسینی ۳ واحد سه‌شنبه ۱۴-۱۶ کلاس ۱۱۰
                                    """.trimIndent()
                                    onRawTextChange(sample)
                                }
                            ) {
                                Text("نمونه گلستان 📋", fontSize = 10.sp, color = AccentPrimaryBright)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = rawText,
                        onValueChange = onRawTextChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("نام درس، ساعت، روز، ترم و تعداد واحد را پیست یا تایپ کنید...", fontSize = 11.sp, color = TextPlaceholder) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPureWhite,
                            unfocusedTextColor = TextMutedWhite,
                            focusedBorderColor = AccentPrimaryBright,
                            unfocusedBorderColor = InputFieldBorder,
                            focusedContainerColor = InputFieldBg,
                            unfocusedContainerColor = InputFieldBg,
                            cursorColor = AccentPrimaryBright
                        )
                    )

                    if (parsedDrafts.isNotEmpty()) {
                        val totalExtractedUnits = parsedDrafts.sumOf { it.units }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Header with stats and total units pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AccentSuccess.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentSuccess.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✓ ${parsedDrafts.size} درس استخراج شد (قابل ویرایش دستی)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentSuccess
                                )
                                Text(
                                    text = "$totalExtractedUnits واحد مجموع",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPureWhite
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Editable cards for each parsed course
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            parsedDrafts.forEachIndexed { index, draft ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = InputFieldBg),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, InputFieldBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = draft.name,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPureWhite
                                            )
                                            Text(
                                                text = "${draft.dayName} ${draft.startTime}-${draft.endTime}",
                                                fontSize = 11.sp,
                                                color = AccentSecondary
                                            )
                                        }

                                        // Semester Selector Chips for this specific course
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(text = "ترم درس:", fontSize = 10.5.sp, color = TextSubtleGray)
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                (1..8).forEach { semNum ->
                                                    val isSemSelected = (if (draft.targetSemester > 0) draft.targetSemester else 4) == semNum
                                                    FilterChip(
                                                        selected = isSemSelected,
                                                        onClick = {
                                                            val updated = parsedDrafts.mapIndexed { i, d ->
                                                                if (i == index) d.copy(targetSemester = semNum) else d
                                                            }
                                                            onParsedDraftsChange(updated)
                                                        },
                                                        label = { Text("ترم $semNum", fontSize = 9.5.sp) },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = AccentPrimary,
                                                            selectedLabelColor = Color.White
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        // Units Selector Chips for this specific course
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(text = "تعداد واحد:", fontSize = 10.5.sp, color = TextSubtleGray)
                                            (1..4).forEach { u ->
                                                val isUnitSelected = draft.units == u
                                                FilterChip(
                                                    selected = isUnitSelected,
                                                    onClick = {
                                                        val updated = parsedDrafts.mapIndexed { i, d ->
                                                            if (i == index) d.copy(units = u) else d
                                                        }
                                                        onParsedDraftsChange(updated)
                                                    },
                                                    label = { Text("$u واحد", fontSize = 9.5.sp) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = AccentSuccess,
                                                        selectedLabelColor = Color.White
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Option 3: Screenshot Import (OCR)
            SetupOptionCard(
                title = "📸 ایمپورت از اسکرین‌شات انتخاب واحد (OCR)",
                description = "تصویر برگه انتخاب واحد یا کارنامه گلستان را انتخاب کنید تا دوره‌ها هوشمندانه استخراج گردند.",
                icon = Icons.Default.Image,
                isSelected = selectedMode == SetupMode.SCREENSHOT_OCR,
                onClick = { onSelectMode(SetupMode.SCREENSHOT_OCR) }
            ) {
                if (selectedMode == SetupMode.SCREENSHOT_OCR) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(InputFieldBg)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "💡 پردازش هوشمند تصویر انتخاب واحد",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentSecondary
                        )
                        Text(
                            text = "سامانه با پردازش متن تصویر جدول دروس انتخابی، کلاس‌ها را به صورت خودکار در دیتابیس ثبت می‌کند. همچنین امکان استفاده از کپی متن گلستان نیز مهیاست.",
                            fontSize = 10.5.sp,
                            color = TextMutedWhite,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Option 4: Pick from Curriculum
            SetupOptionCard(
                title = "📚 انتخاب دروس از چارت تحصیلی مصوب",
                description = "دروس استاندارد سرفصل مصوب وزارت علوم برای ترم را با یک کلیک علامت بزنید.",
                icon = Icons.AutoMirrored.Filled.LibraryBooks,
                isSelected = selectedMode == SetupMode.CURRICULUM,
                onClick = { onSelectMode(SetupMode.CURRICULUM) }
            ) {
                if (selectedMode == SetupMode.CURRICULUM) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        availableCourses.forEach { course ->
                            val isChecked = selectedCourseIds.contains(course.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isChecked) AccentPrimary.copy(alpha = 0.25f) else Color.Transparent)
                                    .tactileClickable { onToggleCourse(course.id) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isChecked) Icons.Default.Check else Icons.Default.EditNote,
                                        contentDescription = null,
                                        tint = if (isChecked) AccentSuccess else TextSubtleGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = course.name,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                        color = TextPureWhite
                                    )
                                }
                                Text(
                                    text = "${course.units} واحد",
                                    fontSize = 10.5.sp,
                                    color = TextSubtleGray
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
        ) {
            Text(
                text = "مرحله نهایی: بازبینی و ساخت سیستم‌عامل 🚀",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StepThreeReviewAndConfirm(
    name: String,
    studentId: String,
    university: String,
    major: String,
    entryYear: Int,
    semester: Int,
    mode: SetupMode,
    passedCredits: Int,
    currentGpa: Double,
    parsedDrafts: List<ParsedCourseDraft>,
    selectedCourses: List<CurriculumCourseEntity>,
    onConfirm: () -> Unit
) {
    val (
        DarkCanvasTop,
        DarkCanvasMid,
        DarkCanvasBottom,
        SurfaceCardBg,
        SurfaceCardBorder,
        InputFieldBg,
        InputFieldBorder,
        AccentPrimary,
        AccentPrimaryBright,
        AccentSecondary,
        AccentSuccess,
        AccentWarning,
        TextPureWhite,
        TextMutedWhite,
        TextSubtleGray,
        TextPlaceholder,
        isDark
    ) = rememberOnboardingColors()
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = AccentSuccess.copy(alpha = 0.2f),
            border = androidx.compose.foundation.BorderStroke(1.dp, AccentSuccess.copy(alpha = 0.5f)),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.RocketLaunch,
                    contentDescription = null,
                    tint = AccentSuccess,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "آماده‌سازی سیستم‌عامل تحصیلی",
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPureWhite
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "اطلاعات زیر در پایگاه داده محلی و آفلاین دستگاه شما مستقر می‌شوند:",
            fontSize = 11.5.sp,
            color = TextSubtleGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "دانشجو:", fontSize = 11.5.sp, color = TextSubtleGray)
                    Text(
                        text = if (studentId.isNotBlank()) "$name (شماره: $studentId)" else name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPureWhite
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "دانشگاه و رشته:", fontSize = 11.5.sp, color = TextSubtleGray)
                    Text(text = "$university · $major", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPureWhite)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "ورودی و ترم جاری:", fontSize = 11.5.sp, color = TextSubtleGray)
                    Text(text = "ورودی $entryYear · ترم $semester", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPureWhite)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "سابقه آکادمیک پایه:", fontSize = 11.5.sp, color = TextSubtleGray)
                    Text(text = "$passedCredits واحد پاس‌شده · معدل $currentGpa", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentWarning)
                }

                Spacer(modifier = Modifier.height(2.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SurfaceCardBorder))
                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "دروس آماده استقرار در برنامه ترم:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentPrimaryBright
                )

                if (mode == SetupMode.PASTE_TEXT && parsedDrafts.isNotEmpty()) {
                    parsedDrafts.forEach { draft ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "• ${draft.name} (${draft.instructor})", fontSize = 11.sp, color = TextPureWhite)
                            Text(text = "${draft.dayName} ${draft.startTime}-${draft.endTime}", fontSize = 10.5.sp, color = TextSubtleGray)
                        }
                    }
                } else {
                    selectedCourses.forEach { course ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "• ${course.name}", fontSize = 11.sp, color = TextPureWhite)
                            Text(text = "${course.units} واحد · ${course.courseType}", fontSize = 10.5.sp, color = TextSubtleGray)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentSuccess)
        ) {
            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "تأیید نهایی و راه‌اندازی سیستم‌عامل 🚀",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun SetupOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val (
        DarkCanvasTop,
        DarkCanvasMid,
        DarkCanvasBottom,
        SurfaceCardBg,
        SurfaceCardBorder,
        InputFieldBg,
        InputFieldBorder,
        AccentPrimary,
        AccentPrimaryBright,
        AccentSecondary,
        AccentSuccess,
        AccentWarning,
        TextPureWhite,
        TextMutedWhite,
        TextSubtleGray,
        TextPlaceholder,
        isDark
    ) = rememberOnboardingColors()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .tactileClickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AccentPrimary.copy(alpha = 0.18f) else SurfaceCardBg
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) AccentPrimaryBright else SurfaceCardBorder
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) AccentPrimary else InputFieldBg,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) AccentPrimaryBright else InputFieldBorder
                    ),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPureWhite
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = description,
                        fontSize = 10.5.sp,
                        color = TextSubtleGray,
                        lineHeight = 15.sp
                    )
                }
            }

            content()
        }
    }
}
