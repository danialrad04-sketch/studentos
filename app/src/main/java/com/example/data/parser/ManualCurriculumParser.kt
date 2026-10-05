package com.example.data.parser

import com.example.data.local.entity.CurriculumCourseEntity
import com.example.data.local.util.DateTimeNormalizer

/** Explicit student-supplied curriculum; never described as an accredited reference. */
object ManualCurriculumParser {
    fun parse(text: String, majorId: String): List<CurriculumCourseEntity> {
        val lines = text.lines().filter { it.isNotBlank() }
        require(lines.isNotEmpty() && lines.size <= 200) { "بین ۱ تا ۲۰۰ ردیف درس وارد کنید." }
        val rows = lines.mapIndexed { index, line ->
            val parts = line.split('|').map { it.trim() }
            require(parts.size in 4..5) { "ردیف ${index + 1}: کد | نام | واحد | ترم | پیش‌نیاز" }
            val units = DateTimeNormalizer.normalizeDigits(parts[2]).toIntOrNull()
            val term = DateTimeNormalizer.normalizeDigits(parts[3]).toIntOrNull()
            require(parts[0].isNotBlank() && parts[1].isNotBlank() && units != null && units in 1..10 && term != null && term in 1..20) { "ردیف ${index + 1}: کد، نام، واحد یا ترم معتبر نیست." }
            CurriculumCourseEntity(id = "$majorId:${parts[0]}", majorId = majorId, code = parts[0], name = parts[1], units = units,
                courseType = "چارت شخصی", recommendedSemester = term, prerequisites = parts.getOrElse(4) { "" })
        }
        require(rows.map { it.code.lowercase() }.distinct().size == rows.size) { "کد درس‌ها نباید تکراری باشد." }
        require(rows.map { it.name }.distinct().size == rows.size) { "نام درس‌ها نباید تکراری باشد." }
        val resolved = rows.map { row ->
            val prerequisites = row.prerequisites.split(',', '،').map { it.trim() }.filter { it.isNotEmpty() }
            val names = prerequisites.map { key ->
                val prerequisite = rows.find { it.code.equals(key, ignoreCase = true) || it.name == key }
                require(prerequisite != null && prerequisite.id != row.id) { "پیش‌نیاز درس ${row.name} در فهرست یافت نشد یا خود درس است: $key" }
                prerequisite.name
            }
            row.copy(prerequisites = names.joinToString("،"))
        }
        val byName = resolved.associateBy { it.name }
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        fun visit(name: String) {
            if (name in visited) return
            require(visiting.add(name)) { "پیش‌نیازها چرخه دارند؛ ارتباط درس $name را اصلاح کنید." }
            byName.getValue(name).prerequisites.split('،').filter { it.isNotBlank() }.forEach { visit(it) }
            visiting.remove(name)
            visited.add(name)
        }
        resolved.forEach { visit(it.name) }
        return resolved
    }
}
