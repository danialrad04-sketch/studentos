package com.example.domain.model

/** Portable study preferences; an active timer is deliberately never restored as running. */
data class StudySettingsSnapshot(
    val dailyGoalMinutes: Int = 120,
    val defaultFocusMinutes: Int = 25,
    val targetGpa: Double? = null,
    val history: List<StudyDay> = emptyList()
) {
    fun validate() {
        require(dailyGoalMinutes in 30..480 && defaultFocusMinutes in setOf(15, 25, 45, 60)) { "تنظیمات زمان مطالعه در پشتیبان معتبر نیست." }
        require(targetGpa == null || (targetGpa.isFinite() && targetGpa in 0.0..20.0)) { "معدل هدف در پشتیبان معتبر نیست." }
        require(history.size <= 90 && history.map { it.date }.distinct().size == history.size) { "سوابق مطالعه در پشتیبان معتبر نیست." }
        history.forEach {
            require(it.minutes in 1..1440 && it.sessions in 1..288) { "زمان یا تعداد جلسه‌های مطالعه معتبر نیست." }
            require(it.date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { "تاریخ مطالعه در پشتیبان معتبر نیست." }
            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply { isLenient = false }
            require(runCatching { format.parse(it.date) != null }.getOrDefault(false)) { "تاریخ مطالعه در پشتیبان معتبر نیست." }
        }
    }
}
