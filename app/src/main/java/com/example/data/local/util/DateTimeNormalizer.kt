package com.example.data.local.util

/**
 * Standard utility for deterministic date and time representation, normalization,
 * sorting, and conflict detection.
 */
object DateTimeNormalizer {

    /**
     * Converts Persian and Arabic digits (۰-۹, ٠-٩) to standard ASCII Latin digits (0-9).
     */
    fun normalizeDigits(input: String): String {
        val persian = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val arabic = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        var res = input
        for (i in 0..9) {
            res = res.replace(persian[i], ('0' + i)).replace(arabic[i], ('0' + i))
        }
        return res
    }

    /**
     * Normalizes a time string to 24-hour "HH:mm" format (e.g. "8:0" -> "08:00", "14:30" -> "14:30").
     */
    fun normalizeTime(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""
        val parts = trimmed.split(":")
        if (parts.size == 2) {
            val h = parts[0].trim().toIntOrNull() ?: 0
            val m = parts[1].trim().toIntOrNull() ?: 0
            return "%02d:%02d".format(h.coerceIn(0, 23), m.coerceIn(0, 59))
        } else if (parts.size == 1) {
            val h = parts[0].trim().toIntOrNull()
            if (h != null) {
                return "%02d:00".format(h.coerceIn(0, 23))
            }
        }
        return trimmed
    }

    /**
     * Normalizes a solar date string to "YYYY/MM/DD" format (e.g. "1403/7/1" -> "1403/07/01").
     */
    fun normalizeSolarDate(raw: String): String {
        val trimmed = raw.trim().replace("-", "/")
        if (trimmed.isBlank()) return ""
        val parts = trimmed.split("/")
        if (parts.size == 3) {
            val y = parts[0].trim().toIntOrNull() ?: 1403
            val m = parts[1].trim().toIntOrNull() ?: 1
            val d = parts[2].trim().toIntOrNull() ?: 1
            return "%04d/%02d/%02d".format(y, m.coerceIn(1, 12), d.coerceIn(1, 31))
        }
        return trimmed
    }

    /**
     * Converts a time string to total minutes from midnight for robust numerical sorting.
     */
    fun timeToMinutes(raw: String): Int {
        val clean = normalizeDigits(raw).trim()
        if (clean.isBlank()) return 0
        val parts = clean.split(":", "-", " ", "تا", "الی")
        val h = parts.firstOrNull { it.isNotBlank() && it.all { c -> c.isDigit() } }?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        return (h.coerceIn(0, 23) * 60) + m.coerceIn(0, 59)
    }

    /**
     * Detects overlapping time slots on the same day.
     * True if day1 == day2 and the time ranges [start1, end1) and [start2, end2) intersect.
     */
    fun hasTimeConflict(
        day1: Int,
        start1: String,
        end1: String,
        day2: Int,
        start2: String,
        end2: String
    ): Boolean {
        if (day1 != day2) return false
        val s1 = normalizeTime(start1)
        val e1 = normalizeTime(end1)
        val s2 = normalizeTime(start2)
        val e2 = normalizeTime(end2)
        if (s1.isEmpty() || e1.isEmpty() || s2.isEmpty() || e2.isEmpty()) return false
        // [s1, e1) overlaps with [s2, e2) if max(s1, s2) < min(e1, e2)
        val maxStart = if (s1 > s2) s1 else s2
        val minEnd = if (e1 < e2) e1 else e2
        return maxStart < minEnd
    }
}
