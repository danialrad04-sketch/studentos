@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui.v3

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.*
import com.example.data.v3.*
import com.example.ui.StudentViewModel
import com.example.ui.components.*
import com.example.ui.theme.studentWorkspaceBackground
import java.util.UUID

enum class V3Section(val title: String) {
    LIBRARY("کتابخانه"),
    PROJECTS("پروژه‌ها"),
    PRACTICE("تمرین و آزمون"),
}

@Composable
internal fun V3Card(
    title: String,
    subtitle: String? = null,
    sticker: AppEmojiType? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    StudentGlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (sticker != null) {
                val palette = com.example.ui.theme.studentStickerColors(sticker)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = palette.container,
                        contentColor = palette.ink,
                    ) {
                        AppEmoji(sticker, size = 32.dp, modifier = Modifier.padding(10.dp))
                    }
                    Text(
                        title,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            if (subtitle != null)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            content()
        }
    }
}

@Composable
internal fun V3Status(vm: StudentV3ViewModel) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    message?.let { text ->
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { vm.message.value = null }) {
                    Icon(Icons.Rounded.Close, "بستن پیام")
                }
            }
        }
    }
}

@Composable
fun V3PanelSelector(labels: List<String>, selected: Int, select: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEachIndexed { i, label ->
            FilterChip(selected == i, { select(i) }, { Text(label) })
        }
    }
}

@Composable
internal fun <T> V3Paged(rows: List<T>, key: String, content: @Composable (T) -> Unit) {
    var page by rememberSaveable(key) { mutableIntStateOf(0) }
    val last = ((rows.size - 1).coerceAtLeast(0) / 20)
    LaunchedEffect(last) { page = page.coerceAtMost(last) }
    rows.drop(page.coerceAtMost(last) * 20).take(20).forEach { content(it) }
    if (rows.size > 20)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton({ page-- }, enabled = page > 0) { Text("قبلی") }
            Text(
                "صفحهٔ ${page+1} از ${last+1}",
                Modifier.padding(12.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            TextButton({ page++ }, enabled = page < last) { Text("بعدی") }
        }
}

@Composable
fun StudentV3Workspace(
    student: StudentViewModel,
    section: V3Section = V3Section.LIBRARY,
    courseId: String? = null,
    showSections: Boolean = false,
    initialEntityId: String = "",
    initialQuery: String = "",
) {
    StudentV3WorkspaceContent(
        studentV3ViewModel(student),
        { student.selectTab(com.example.ui.models.AppTab.SCHEDULE) },
        section,
        courseId,
        showSections,
        initialEntityId,
        initialQuery,
    )
}

@Composable
fun StudentV3WorkspaceContent(
    vm: StudentV3ViewModel,
    onOpenCourses: () -> Unit,
    section: V3Section = V3Section.LIBRARY,
    courseId: String? = null,
    showSections: Boolean = false,
    initialEntityId: String = "",
    initialQuery: String = "",
) {
    key(vm.ownerKey) {
        StudentV3WorkspaceBody(
            vm,
            onOpenCourses,
            section,
            courseId,
            showSections,
            initialEntityId,
            initialQuery,
        )
    }
}

@Composable
private fun StudentV3WorkspaceBody(
    vm: StudentV3ViewModel,
    onOpenCourses: () -> Unit,
    section: V3Section,
    courseId: String?,
    showSections: Boolean,
    initialEntityId: String,
    initialQuery: String,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var courseSelection by rememberSaveable(courseId) { mutableStateOf(courseId.orEmpty()) }
    var currentSection by rememberSaveable(section) { mutableStateOf(section.name) }
    var query by rememberSaveable(initialQuery) { mutableStateOf(initialQuery) }
    var reader by
        rememberSaveable(initialEntityId) {
            mutableStateOf(if (section == V3Section.LIBRARY) initialEntityId else "")
        }
    var projectId by
        rememberSaveable(initialEntityId) {
            mutableStateOf(if (section == V3Section.PROJECTS) initialEntityId else "")
        }
    var editingProjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var showDeleted by rememberSaveable { mutableStateOf(false) }
    var purgeId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameId by rememberSaveable { mutableStateOf<String?>(null) }
    var backup by rememberSaveable { mutableStateOf(false) }
    var importCourse by rememberSaveable { mutableStateOf("") }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null && importCourse.isNotBlank()) vm.importFile(uri, importCourse)
        }
    val courses = state.courses
    val selectedCourse =
        courseId
            ?: courseSelection.takeIf { id -> courses.any { it.id == id } }
            ?: courses.firstOrNull { !it.isArchived }?.id
            ?: courses.firstOrNull()?.id.orEmpty()
    val currentCourse = courses.find { it.id == selectedCourse }
    val chosen = V3Section.valueOf(currentSection)
    val writable = currentCourse != null && !currentCourse.isArchived && !busy
    Column(
        Modifier.fillMaxWidth().testTag("v3_workspace"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showSections)
            V3PanelSelector(V3Section.entries.map { it.title }, chosen.ordinal) {
                currentSection = V3Section.entries[it].name
                query = ""
            }
        V3Status(vm)
        if (!state.loaded) {
            CircularProgressIndicator()
            return@Column
        }
        if (state.error != null) {
            V3Card("اطلاعات باز نشد", state.error) { Button(vm::retry) { Text("تلاش دوباره") } }
            return@Column
        }
        if (courses.isEmpty()) {
            V3Card(
                "اولین درس را اضافه کن",
                "کتابخانه، پروژه و سؤال‌های هر درس کنار هم قرار می‌گیرند.",
            ) {
                Button(onOpenCourses) { Text("رفتن به درس‌ها") }
            }
            if (chosen == V3Section.LIBRARY) V3BackupControls(vm)
            return@Column
        }
        if (courseId == null)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                courses.forEach { course ->
                    FilterChip(
                        selectedCourse == course.id,
                        {
                            courseSelection = course.id
                            query = ""
                        },
                        {
                            Text(
                                course.name + if (course.isArchived) " · بایگانی" else "",
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        },
                        modifier = Modifier.widthIn(max = 260.dp),
                    )
                }
            }
        if (currentCourse == null) {
            V3Card("این درس در دسترس نیست", "به فهرست درس‌ها برگرد و اطلاعات را بررسی کن.")
            return@Column
        }
        if (chosen != V3Section.PRACTICE)
            FilterChip(showDeleted, { showDeleted = !showDeleted }, { Text("حذف‌شده‌ها") })
        Text(currentCourse.name, style = MaterialTheme.typography.titleMedium)
        if (currentCourse?.isArchived == true)
            Text(
                "درس بایگانی‌شده؛ منابع قبلی قابل مطالعه‌اند.",
                style = MaterialTheme.typography.bodySmall,
            )
        OutlinedTextField(
            query,
            { query = it },
            Modifier.fillMaxWidth().testTag("v3_search"),
            label = { Text("جست‌وجو در ${chosen.title}") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            singleLine = true,
        )
        val needle = normalizedStudentSearch(query)
        when (chosen) {
            V3Section.LIBRARY -> {
                V3Card(
                    "جزوه‌ها و منابع",
                    "PDF و تصویر؛ مطالعه از همان جایی که مانده بودی",
                    sticker = AppEmojiType.BOOK,
                ) {
                    Button(
                        {
                            importCourse = selectedCourse
                            picker.launch(
                                arrayOf("application/pdf", "image/jpeg", "image/png", "image/webp")
                            )
                        },
                        enabled = writable,
                        modifier = Modifier.testTag("v3_import_file"),
                    ) {
                        Icon(Icons.Rounded.Add, null)
                        Text("افزودن فایل")
                    }
                    Text(
                        "حداکثر اندازهٔ هر فایل ۵۱۲ مگابایت است.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                val files =
                    state.data.files.filter {
                        it.isDeleted == showDeleted &&
                            it.courseId == selectedCourse &&
                            normalizedStudentSearch(it.title).contains(needle)
                    }
                if (files.isEmpty())
                    V3Card(
                        if (query.isBlank()) "کتابخانهٔ این درس خالی است" else "فایلی پیدا نشد",
                        if (query.isBlank()) "یک جزوه یا تصویر اضافه کن تا مطالعه را شروع کنی."
                        else "نام دیگری را جست‌وجو کن.",
                    )
                V3Paged(files, "files:$selectedCourse:$query") { file ->
                    V3Card(
                        file.title,
                        "${if(file.mimeType=="application/pdf") "PDF" else "تصویر"} · ${file.pageCount} صفحه · ${android.text.format.Formatter.formatShortFileSize(androidx.compose.ui.platform.LocalContext.current,file.byteCount)}",
                    ) {
                        Text(
                            "ادامه از صفحهٔ ${file.lastPage+1}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (!file.isDeleted)
                                FilledTonalButton(
                                    { reader = file.id },
                                    Modifier.testTag("v3_open_${file.id}"),
                                ) {
                                    Text("مطالعه")
                                }
                            if (!file.isPurged)
                                TextButton(
                                    {
                                        vm.perform({
                                            saveFile(file.copy(isDeleted = !file.isDeleted))
                                        })
                                    },
                                    enabled = writable,
                                ) {
                                    Text(if (file.isDeleted) "بازیابی" else "برداشتن")
                                }
                            if (!file.isDeleted)
                                TextButton({ renameId = file.id }, enabled = writable) {
                                    Text("تغییر نام")
                                }
                            if (file.isDeleted && !file.isPurged)
                                TextButton({ purgeId = file.id }, enabled = writable) {
                                    Text("حذف دائمی فایل")
                                }
                            if (file.isPurged)
                                Text(
                                    "فایل از حافظه پاک شده است.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                        }
                    }
                }
                OutlinedButton({ backup = true }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Backup, null)
                    Spacer(Modifier.width(8.dp))
                    Text("پشتیبان کامل و انتقال فایل‌ها")
                }
            }
            V3Section.PROJECTS -> {
                V3Card(
                    "پروژه‌های این درس",
                    "از اولین قدم تا تحویل نهایی",
                    sticker = AppEmojiType.CHECK,
                ) {
                    FilledTonalButton(
                        { editingProjectId = UUID.randomUUID().toString() },
                        enabled = writable,
                        modifier = Modifier.testTag("v3_add_project"),
                    ) {
                        Icon(Icons.Rounded.Add, null)
                        Text("پروژهٔ جدید")
                    }
                }
                val projects =
                    state.data.projects.filter {
                        it.isDeleted == showDeleted &&
                            it.courseId == selectedCourse &&
                            normalizedStudentSearch(it.title + " " + it.description)
                                .contains(needle)
                    }
                if (projects.isEmpty())
                    V3Card(
                        "پروژه‌ها را قدم‌به‌قدم جلو ببر",
                        "مرحله‌های تحویل، تکلیف‌ها و فایل‌های هر پروژه کنار هم می‌مانند.",
                    )
                V3Paged(projects, "projects:$selectedCourse:$query") { project ->
                    val steps =
                        state.data.steps.filter { it.projectId == project.id && !it.isDeleted }
                    val done = steps.count { it.completedAt != null }
                    V3Card(
                        project.title,
                        if (project.completedAt != null) "انجام‌شده"
                        else if (project.dueDate.isBlank()) "بدون مهلت"
                        else "مهلت: ${project.dueDate}",
                    ) {
                        if (steps.isNotEmpty()) {
                            LinearProgressIndicator(
                                progress = { done.toFloat() / steps.size },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                "$done از ${steps.size} مرحله انجام شده",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (!project.isDeleted)
                                FilledTonalButton({ projectId = project.id }) {
                                    Text("باز کردن پروژه")
                                }
                            if (project.isDeleted)
                                TextButton(
                                    { vm.perform({ project(project.copy(isDeleted = false)) }) },
                                    enabled = writable,
                                ) {
                                    Text("بازیابی")
                                }
                            else
                                TextButton({ editingProjectId = project.id }, enabled = writable) {
                                    Text("ویرایش")
                                }
                        }
                    }
                }
            }
            V3Section.PRACTICE -> StudentPracticeContent(vm, state, selectedCourse, query, writable)
        }
    }
    state.data.files
        .find { it.id == reader && !it.isDeleted }
        ?.let { file ->
            StudentFileReader(
                vm,
                file,
                state.data.bookmarks.filter { it.attachmentId == file.id && !it.isDeleted },
            ) {
                reader = ""
            }
        }
    state.data.projects
        .find { it.id == projectId && !it.isDeleted }
        ?.let { project -> StudentProjectDetail(vm, state, project) { projectId = "" } }
    editingProjectId?.let { id ->
        ProjectEditor(
            vm,
            state.data.projects.find { it.id == id }
                ?: ProjectEntity(id = id, courseId = selectedCourse),
        ) {
            editingProjectId = null
        }
    }
    state.data.files
        .find { it.id == renameId }
        ?.let { row ->
            var title by rememberSaveable(row.id) { mutableStateOf(row.title) }
            val revision by rememberSaveable(row.id) { mutableLongStateOf(row.updatedAt) }
            V3Editor(
                "نام جزوه",
                vm,
                { renameId = null },
                {
                    vm.perform({ saveFile(row.copy(title = title, updatedAt = revision)) }) {
                        renameId = null
                    }
                },
            ) {
                OutlinedTextField(
                    title,
                    { title = it },
                    Modifier.fillMaxWidth(),
                    label = { Text("نام فایل در کتابخانه") },
                )
            }
        }
    state.data.files
        .find { it.id == purgeId }
        ?.let { file ->
            AlertDialog(
                onDismissRequest = { purgeId = null },
                title = { Text("فایل برای همیشه پاک شود؟") },
                text = {
                    Text(
                        "فایل «${file.title}» از حافظهٔ برنامه پاک می‌شود و از حذف‌شده‌ها قابل بازیابی نخواهد بود. نسخه‌های پشتیبان قبلی که خودت نگه داشته‌ای تغییر نمی‌کنند."
                    )
                },
                confirmButton = {
                    TextButton({
                        vm.purgeFile(file)
                        purgeId = null
                    }) {
                        Text("حذف دائمی")
                    }
                },
                dismissButton = { TextButton({ purgeId = null }) { Text("انصراف") } },
            )
        }
    if (backup)
        StudentGlassModalSheet(
            title = "پشتیبان کامل",
            subtitle = "اطلاعات و جزوه‌ها در یک فایل",
            onDismiss = { backup = false },
        ) {
            V3BackupControls(vm)
        }
}

@Composable
fun V3BackupControls(vm: StudentV3ViewModel) {
    key(vm.ownerKey) { V3BackupControlsBody(vm) }
}

@Composable
private fun V3BackupControlsBody(vm: StudentV3ViewModel) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    var confirm by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val export =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/zip")
        ) { uri ->
            if (uri != null) vm.exportBackup(uri)
        }
    val restore =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                selected = uri.toString()
                confirm = true
            }
        }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        V3Status(vm)
        Text(
            "پشتیبان کامل، اطلاعات تحصیلی و فایل‌های کتابخانه را همراه هم نگه می‌دارد.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            { export.launch("StudentOS-full-backup.zip") },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("ذخیرهٔ پشتیبان کامل ZIP")
        }
        OutlinedButton(
            { restore.launch(arrayOf("application/zip", "application/octet-stream")) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("بازیابی پشتیبان کامل")
        }
        Text(
            "برای فایل‌های JSON قدیمی از بخش بازیابی متن استفاده کن. پشتیبان کامل را در محل امن خودت نگه دار.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (confirm)
        AlertDialog(
            onDismissRequest = {
                confirm = false
                selected = null
            },
            title = { Text("بازیابی اطلاعات و فایل‌ها؟") },
            text = {
                Text(
                    "اطلاعات فایل با اطلاعات فعلی ترکیب و تنظیمات آن بازیابی می‌شود. ویرایش‌ها و حذف‌های تازه‌ترِ کتابخانه، پروژه و تمرین حفظ می‌شوند."
                )
            },
            confirmButton = {
                TextButton({
                    selected?.let { vm.restoreBackup(android.net.Uri.parse(it)) }
                    confirm = false
                    selected = null
                }) {
                    Text("بازیابی")
                }
            },
            dismissButton = {
                TextButton({
                    confirm = false
                    selected = null
                }) {
                    Text("انصراف")
                }
            },
        )
}

@Composable
fun V3CourseTools(student: StudentViewModel, courseId: String) {
    var open by rememberSaveable { mutableStateOf(false) }
    FilledTonalButton({ open = true }, Modifier.fillMaxWidth().testTag("v3_course_tools")) {
        Icon(Icons.Rounded.AutoStories, null)
        Spacer(Modifier.width(8.dp))
        Text("کتابخانه، پروژه و تمرین این درس")
    }
    if (open)
        StudentV3Dialog("فضای درس", { open = false }) {
            StudentV3Workspace(student, courseId = courseId, showSections = true)
        }
}

@Composable
internal fun StudentV3Dialog(
    title: String,
    onDismiss: () -> Unit,
    tag: String = "v3_dialog",
    requestClose: ((() -> Unit) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    StudentDialog(
        onDismiss,
        DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val dismiss = LocalStudentModalDismiss.current ?: onDismiss
        val close = { if (requestClose != null) requestClose.invoke(dismiss) else dismiss() }
        androidx.activity.compose.BackHandler(enabled = requestClose != null, onBack = close)
        Surface(
            Modifier.fillMaxSize().testTag(tag).studentWorkspaceBackground(),
            color = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            Column(
                Modifier.fillMaxSize().safeDrawingPadding().imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    Modifier.widthIn(max = 840.dp).fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(close) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "بازگشت") }
                    Text(
                        title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(
                    Modifier.weight(1f)
                        .widthIn(max = 840.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    content()
                }
            }
        }
    }
}

internal fun normalizedV3Date(text: String): String {
    if (text.isBlank()) return ""
    return requireNotNull(
            com.example.domain.util.JalaliCalendarUtil.parse(
                com.example.data.local.util.DateTimeNormalizer.normalizeDigits(text)
            )
        ) {
            "تاریخ معتبر نیست."
        }
        .format()
}
