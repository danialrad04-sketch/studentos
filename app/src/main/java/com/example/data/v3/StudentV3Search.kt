package com.example.data.v3

import com.example.data.local.entity.*
import com.example.domain.model.GlobalSearchResult
import com.example.domain.model.StudentResourceKind

data class V3SearchData(
    val files: List<AttachmentEntity>,
    val projects: List<ProjectEntity>,
    val questions: List<PracticeQuestionEntity>,
    val notes: List<NoteEntity>,
)

object StudentV3Search {
    fun search(
        query: String,
        courses: List<CourseEntity>,
        data: V3SearchData,
    ): List<GlobalSearchResult.ResourceItem> {
        val needle = normalizedStudentSearch(query)
        if (needle.isBlank()) return emptyList()
        val names = courses.associate { it.id to it.name }
        val results = mutableListOf<GlobalSearchResult.ResourceItem>()
        fun add(
            id: String,
            courseId: String,
            title: String,
            searchable: String,
            kind: StudentResourceKind,
        ) {
            if (
                normalizedStudentSearch(searchable + " " + names[courseId].orEmpty())
                    .contains(needle)
            )
                results +=
                    GlobalSearchResult.ResourceItem(
                        id,
                        courseId,
                        title,
                        names[courseId].orEmpty(),
                        kind,
                    )
        }
        data.files
            .filter { !it.isDeleted && it.courseId in names }
            .forEach { add(it.id, it.courseId, it.title, it.title, StudentResourceKind.FILE) }
        data.projects
            .filter { !it.isDeleted && it.courseId in names }
            .forEach {
                add(
                    it.id,
                    it.courseId,
                    it.title,
                    it.title + " " + it.description,
                    StudentResourceKind.PROJECT,
                )
            }
        data.questions
            .filter { !it.isArchived && it.courseId in names }
            .forEach { add(it.id, it.courseId, it.prompt, it.prompt, StudentResourceKind.QUESTION) }
        data.notes
            .filter { !it.isArchived }
            .forEach {
                add(
                    it.id,
                    it.courseId.orEmpty(),
                    it.title.ifBlank { "یادداشت" },
                    it.title + " " + it.content,
                    StudentResourceKind.NOTE,
                )
            }
        return results.take(60)
    }
}
