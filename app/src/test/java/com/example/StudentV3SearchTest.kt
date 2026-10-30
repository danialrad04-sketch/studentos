package com.example

import com.example.data.local.entity.*
import com.example.data.v3.*
import com.example.domain.model.StudentResourceKind
import org.junit.Assert.*
import org.junit.Test

class StudentV3SearchTest {
    @Test
    fun normalizedPersianSearchIncludesCourseContextAndHidesDeletedOrForeignResources() {
        val course = CourseEntity(id = "math", name = "رياضي ۱۲")
        val data =
            V3SearchData(
                files =
                    listOf(
                        AttachmentEntity(id = "f", courseId = "math", title = "جزوه"),
                        AttachmentEntity(
                            id = "hidden",
                            courseId = "math",
                            title = "ریاضی",
                            isDeleted = true,
                        ),
                        AttachmentEntity(id = "foreign", courseId = "gone", title = "ریاضی"),
                    ),
                projects = listOf(ProjectEntity(id = "p", courseId = "math", title = "گزارش")),
                questions =
                    listOf(
                        PracticeQuestionEntity(id = "q", courseId = "math", prompt = "تمرین"),
                        PracticeQuestionEntity(
                            id = "archived",
                            courseId = "math",
                            prompt = "ریاضی",
                            isArchived = true,
                        ),
                    ),
                notes = listOf(NoteEntity(id = "n", title = "یادداشت رياضي 12", content = "متن")),
            )
        val results = StudentV3Search.search("ریاضی 12", listOf(course), data)
        assertEquals(setOf("f", "p", "q", "n"), results.map { it.id }.toSet())
        assertEquals(
            setOf(
                StudentResourceKind.FILE,
                StudentResourceKind.PROJECT,
                StudentResourceKind.QUESTION,
                StudentResourceKind.NOTE,
            ),
            results.map { it.kind }.toSet(),
        )
        assertTrue(StudentV3Search.search("  ", listOf(course), data).isEmpty())
    }
}
