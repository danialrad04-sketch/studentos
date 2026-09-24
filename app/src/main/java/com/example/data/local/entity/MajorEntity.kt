package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reference_majors",
    foreignKeys = [
        ForeignKey(
            entity = UniversityEntity::class,
            parentColumns = ["id"],
            childColumns = ["universityId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("universityId")]
)
data class MajorEntity(
    @PrimaryKey val id: String,      // e.g. "MAJ_AUT_CHEM_ENG"
    val universityId: String,        // e.g. "UNI_AUT"
    val facultyId: String,           // e.g. "FAC_AUT_CHEM_OIL"
    val facultyDisplayNameFa: String,// "دانشکده مهندسی شیمی و نفت"
    val majorDisplayNameFa: String   // "مهندسی شیمی"
)
