package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reference_universities")
data class UniversityEntity(
    @PrimaryKey val id: String, // e.g. "UNI_AUT", "UNI_UT", "UNI_SUT"
    val displayNameFa: String,  // "دانشگاه صنعتی امیرکبیر"
    val shortName: String       // "پلی‌تکنیک تهران"
)
