package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "curriculum_versions",
    foreignKeys = [
        ForeignKey(
            entity = MajorEntity::class,
            parentColumns = ["id"],
            childColumns = ["majorId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("majorId")]
)
data class CurriculumVersionEntity(
    @PrimaryKey val id: String,   // e.g. "CURR_AUT_CE_1401"
    val majorId: String,          // e.g. "MAJ_AUT_CHEM_ENG"
    val title: String,            // "چارت کارشناسی مهندسی شیمی ورودی‌های ۱۴۰۱ تا ۱۴۰۴"
    val entryYearMin: Int,        // 1401
    val entryYearMax: Int,        // 1404
    val totalCreditsRequired: Int // e.g. 140
)
