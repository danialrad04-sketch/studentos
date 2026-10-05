package com.example

import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.StudentProfileEntity
import com.example.ui.components.export.ExportSourcePayload
import com.example.ui.components.export.academicGpaText
import org.junit.Assert.assertEquals
import org.junit.Test

class AcademicExportFactsTest {
    @Test fun unenteredGradesDoNotBecomeZeroOrBorrowAnIllustrativeGpa() {
        val payload = ExportSourcePayload.Grades(StudentProfileEntity(), listOf(GradeEntity(courseName = "ریاضی", units = 3)), 17.5, 3)
        assertEquals("—", academicGpaText(payload))
        assertEquals("0.00", academicGpaText(payload.copy(grades = listOf(payload.grades.single().copy(isRecorded = true)))))
        val scores = listOf(GradeEntity(courseName = "ریاضی", units = 3, midtermGrade = 6.0, finalGrade = 12.0), GradeEntity(courseName = "فیزیک", units = 3, isRecorded = true))
        assertEquals("9.00", academicGpaText(payload.copy(grades = scores)))
    }
    @Test fun missingPassportGpaRemainsUnknownAndDeclaredZeroIsPreserved() {
        val payload = ExportSourcePayload.Passport(StudentProfileEntity(), null, "")
        assertEquals("—", academicGpaText(payload))
        assertEquals("0.00", academicGpaText(payload.copy(profile = payload.profile.copy(declaredGpa = 0.0))))
    }
}
