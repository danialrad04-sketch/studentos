package com.example.ui.v3

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.GlobalSearchResult
import com.example.domain.model.StudentResourceKind
import com.example.ui.StudentViewModel

@Composable
fun StudentResourceDialog(
    student: StudentViewModel,
    result: GlobalSearchResult.ResourceItem,
    onDismiss: () -> Unit,
    onNavigateAway: () -> Unit = onDismiss,
) {
    if (result.kind == StudentResourceKind.NOTE) {
        val notes by student.studyNotes.collectAsStateWithLifecycle()
        val note = notes.find { it.id == result.id && !it.isArchived }
        StudentV3Dialog(result.title, onDismiss) {
            if (note != null) {
                Text(note.content)
                TextButton({
                    student.selectTab(com.example.ui.models.AppTab.NOTES)
                    onNavigateAway()
                }) {
                    Text("رفتن به دفتر یادداشت‌ها")
                }
            } else Text("این یادداشت دیگر در دسترس نیست.")
        }
    } else {
        val section =
            when (result.kind) {
                StudentResourceKind.FILE -> V3Section.LIBRARY
                StudentResourceKind.PROJECT -> V3Section.PROJECTS
                else -> V3Section.PRACTICE
            }
        StudentV3Dialog(result.kind.title, onDismiss) {
            StudentV3Workspace(
                student,
                section,
                result.courseId,
                initialEntityId = result.id,
                initialQuery = if (result.kind == StudentResourceKind.QUESTION) result.title else "",
            )
        }
    }
}
