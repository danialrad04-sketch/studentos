package com.example.ui.v3

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.*
import com.example.ui.components.StudentGlassModalSheet
import java.util.UUID

@Composable
internal fun V3Editor(
    title: String,
    vm: StudentV3ViewModel,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    StudentGlassModalSheet(
        title = title,
        subtitle = "اطلاعات روی همین دستگاه ذخیره می‌شود",
        onDismiss = {
            vm.message.value = null
            onDismiss()
        },
    ) {
        V3Status(vm)
        Column(
            Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
        Button(
            onSave,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("v3_editor_save"),
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (busy) "در حال ذخیره…" else "ذخیره")
        }
    }
}

@Composable
fun ProjectEditor(vm: StudentV3ViewModel, row: ProjectEntity, onDismiss: () -> Unit) {
    val saving by vm.busy.collectAsStateWithLifecycle()
    var title by rememberSaveable(row.id) { mutableStateOf(row.title) }
    var due by rememberSaveable(row.id) { mutableStateOf(row.dueDate) }
    var description by rememberSaveable(row.id) { mutableStateOf(row.description) }
    var estimate by
        rememberSaveable(row.id) {
            mutableStateOf(row.estimatedMinutes.takeIf { it > 0 }?.toString().orEmpty())
        }
    val revision by rememberSaveable(row.id) { mutableLongStateOf(row.updatedAt) }
    V3Editor(
        "پروژهٔ درسی",
        vm,
        onDismiss,
        {
            vm.perform(
                {
                    project(
                        row.copy(
                            updatedAt = revision,
                            title = title,
                            dueDate = normalizedV3Date(due),
                            description = description,
                            estimatedMinutes =
                                if (estimate.isBlank()) 0
                                else
                                    com.example.data.local.util.DateTimeNormalizer.normalizeDigits(
                                            estimate
                                        )
                                        .toIntOrNull() ?: -1,
                        )
                    )
                },
                onDismiss,
            )
        },
    ) {
        OutlinedTextField(
            title,
            { title = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving,
            label = { Text("نام پروژه") },
            singleLine = true,
        )
        OutlinedTextField(
            due,
            { due = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving,
            label = { Text("مهلت تحویل، اختیاری") },
            placeholder = { Text("۱۴۰۵/۰۸/۱۰") },
            singleLine = true,
        )
        OutlinedTextField(
            estimate,
            { estimate = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving,
            label = { Text("زمان لازم (دقیقه)") },
            supportingText = { Text("با ثبت زمان و مهلت، پروژه وارد برنامهٔ مطالعه می‌شود.") },
            singleLine = true,
            keyboardOptions =
                androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                ),
        )
        OutlinedTextField(
            description,
            { description = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving,
            label = { Text("توضیحات") },
            minLines = 3,
        )
    }
}

@Composable
private fun StepEditor(vm: StudentV3ViewModel, row: ProjectStepEntity, onDismiss: () -> Unit) {
    val saving by vm.busy.collectAsStateWithLifecycle()
    var title by rememberSaveable(row.id) { mutableStateOf(row.title) }
    var due by rememberSaveable(row.id) { mutableStateOf(row.dueDate) }
    val revision by rememberSaveable(row.id) { mutableLongStateOf(row.updatedAt) }
    V3Editor(
        "مرحلهٔ پروژه",
        vm,
        onDismiss,
        {
            vm.perform(
                {
                    step(
                        row.copy(
                            updatedAt = revision,
                            title = title,
                            dueDate = normalizedV3Date(due),
                        )
                    )
                },
                onDismiss,
            )
        },
    ) {
        OutlinedTextField(
            title,
            { title = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving,
            label = { Text("این مرحله چه کاری دارد؟") },
        )
        OutlinedTextField(
            due,
            { due = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving,
            label = { Text("مهلت مرحله، اختیاری") },
            placeholder = { Text("۱۴۰۵/۰۸/۱۰") },
            singleLine = true,
        )
    }
}

@Composable
fun StudentProjectDetail(
    vm: StudentV3ViewModel,
    state: V3UiState,
    project: ProjectEntity,
    onDismiss: () -> Unit,
) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    var editingStepId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletedSteps by rememberSaveable { mutableStateOf(false) }
    var reader by rememberSaveable { mutableStateOf("") }
    var attachTask by rememberSaveable { mutableStateOf(false) }
    var remove by rememberSaveable { mutableStateOf(false) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) vm.importFile(uri, project.courseId, project.id)
        }
    val writable = state.courses.any { it.id == project.courseId && !it.isArchived } && !busy
    StudentV3Dialog(project.title, onDismiss, tag = "v3_project_detail") {
        V3Status(vm)
        if (project.description.isNotBlank())
            Text(project.description, style = MaterialTheme.typography.bodyLarge)
        Text(
            if (project.dueDate.isBlank()) "مهلت تعیین نشده" else "مهلت تحویل: ${project.dueDate}",
            style = MaterialTheme.typography.labelLarge,
        )
        val steps =
            state.data.steps
                .filter { it.projectId == project.id && it.isDeleted == deletedSteps }
                .sortedWith(compareBy({ it.completedAt != null }, { it.dueDate }, { it.id }))
        V3Card(
            "مرحله‌های تحویل",
            "${steps.count { it.completedAt!=null }} از ${steps.size} مرحله انجام شده",
        ) {
            FilterChip(
                deletedSteps,
                { deletedSteps = !deletedSteps },
                { Text("مرحله‌های حذف‌شده") },
            )
            V3Paged(steps, "steps:${project.id}") { step ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        step.completedAt != null,
                        { checked ->
                            vm.perform({
                                step(
                                    step.copy(
                                        completedAt =
                                            if (checked) System.currentTimeMillis() else null
                                    )
                                )
                            })
                        },
                        enabled = writable && !step.isDeleted,
                        modifier = Modifier.testTag("v3_step_${step.id}"),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(step.title)
                        if (step.dueDate.isNotBlank())
                            Text(step.dueDate, style = MaterialTheme.typography.bodySmall)
                    }
                    var menu by rememberSaveable(step.id) { mutableStateOf(false) }
                    Box {
                        IconButton({ menu = true }, enabled = writable) {
                            Icon(
                                androidx.compose.material.icons.Icons.Rounded.MoreVert,
                                "گزینه‌های مرحله",
                            )
                        }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("ویرایش") },
                                onClick = {
                                    menu = false
                                    editingStepId = step.id
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(if (step.isDeleted) "بازیابی" else "حذف") },
                                onClick = {
                                    menu = false
                                    vm.perform({ step(step.copy(isDeleted = !step.isDeleted)) })
                                },
                            )
                        }
                    }
                }
            }
            FilledTonalButton(
                { editingStepId = UUID.randomUUID().toString() },
                enabled = writable,
                modifier = Modifier.testTag("v3_add_step"),
            ) {
                Text("افزودن مرحله")
            }
        }
        val tasks = state.tasks.filter { it.projectId == project.id }
        V3Card("تکلیف‌های مرتبط", "تکلیف‌ها در بخش کارها هم در دسترس‌اند.") {
            V3Paged(tasks, "linked:${project.id}") { task ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        (if (task.isCompleted) "انجام‌شده · " else "") + task.title,
                        Modifier.weight(1f),
                    )
                    TextButton({ vm.perform({ linkTask(task.id, null) }) }, enabled = writable) {
                        Text("جدا کردن")
                    }
                }
            }
            TextButton({ attachTask = true }, enabled = writable) { Text("پیوند تکلیف موجود") }
        }
        V3Card("فایل‌های پروژه") {
            V3Paged(
                state.data.files.filter { it.projectId == project.id && !it.isDeleted },
                "project-files:${project.id}",
            ) { file ->
                TextButton({ reader = file.id }) { Text(file.title) }
            }
            FilledTonalButton(
                {
                    picker.launch(
                        arrayOf("application/pdf", "image/jpeg", "image/png", "image/webp")
                    )
                },
                enabled = writable,
            ) {
                Text("افزودن فایل به پروژه")
            }
        }
        FilledTonalButton(
            {
                vm.perform({
                    project(
                        project.copy(
                            completedAt =
                                if (project.completedAt == null) System.currentTimeMillis()
                                else null
                        )
                    )
                })
            },
            enabled = writable,
        ) {
            Text(if (project.completedAt == null) "پروژه انجام شد" else "برگرداندن به کارهای باز")
        }
        TextButton({ remove = true }, enabled = writable) { Text("برداشتن پروژه از فهرست") }
    }
    editingStepId?.let { id ->
        StepEditor(
            vm,
            state.data.steps.find { it.id == id }
                ?: ProjectStepEntity(id = id, projectId = project.id),
        ) {
            editingStepId = null
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
    if (attachTask)
        StudentGlassModalSheet(
            title = "انتخاب تکلیف",
            subtitle = "تکلیف‌های همین درس",
            onDismiss = { attachTask = false },
        ) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val available =
                    state.tasks.filter {
                        it.courseId == project.courseId && it.projectId != project.id
                    }
                if (available.isEmpty()) Text("ابتدا یک تکلیف برای این درس در بخش کارها ثبت کن.")
                V3Paged(available, "task-picker:${project.id}") { task ->
                    TextButton(
                        { vm.perform({ linkTask(task.id, project.id) }) { attachTask = false } },
                        enabled = !busy,
                    ) {
                        Text(task.title)
                    }
                }
            }
        }
    if (remove)
        AlertDialog(
            onDismissRequest = { remove = false },
            title = { Text("پروژه از فهرست برداشته شود؟") },
            text = { Text("از بخش حذف‌شده‌ها قابل بازیابی است. تکلیف‌های اصلی حذف نمی‌شوند.") },
            confirmButton = {
                TextButton(
                    {
                        vm.perform({ project(project.copy(isDeleted = true)) }) {
                            remove = false
                            onDismiss()
                        }
                    },
                    enabled = !busy,
                ) {
                    Text("برداشتن پروژه")
                }
            },
            dismissButton = { TextButton({ remove = false }) { Text("انصراف") } },
        )
}
