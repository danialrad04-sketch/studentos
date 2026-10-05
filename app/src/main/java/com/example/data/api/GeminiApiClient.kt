package com.example.data.api

import org.json.JSONArray

/** Server-backed academic advice and timetable parsing. No provider keys in the app. */
object GeminiApiClient {
    suspend fun generateAcademicAdvice(
        prompt: String,
        studentContext: String,
        history: List<Pair<String, String>> = emptyList(),
        customApiKey: String? = null,
        kind: String = "chat"
    ): Result<String> = PremiumApiClient.result {
        PremiumApiClient.advice(prompt, studentContext, history, kind)
    }

    /**
     * Extracts courses and sessions from schedule photos using the current multimodal Gemini model.
     */
    suspend fun extractScheduleFromImage(
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg",
        customApiKey: String? = null
    ): Result<List<com.example.data.parser.ParsedCourseDraft>> = PremiumApiClient.result {
        require(imageBytes.isNotEmpty()) { "فایل تصویر انتخابی خالی است." }
        val parsed = parseScheduleJson(PremiumApiClient.schedule(imageBytes, mimeType))
        check(parsed.isNotEmpty()) { "جدولی از تصویر خوانده نشد؛ تصویر واضح‌تر انتخاب کنید." }
        parsed
    }

    private fun parseScheduleJson(rawText: String): List<com.example.data.parser.ParsedCourseDraft> {
        val clean = rawText
            .replace("```json", "")
            .replace("```", "")
            .trim()
        val startIdx = clean.indexOf('[')
        val endIdx = clean.lastIndexOf(']')
        if (startIdx == -1 || endIdx == -1 || endIdx <= startIdx) {
            return emptyList()
        }
        val jsonArray = JSONArray(clean.substring(startIdx, endIdx + 1))
        val list = mutableListOf<com.example.data.parser.ParsedCourseDraft>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val name = obj.optString("name", "").trim()
            if (name.isBlank()) continue
            val units = obj.optInt("units", -1)
            val dayOfWeek = obj.optInt("dayOfWeek", -1)
            val dayName = obj.optString("dayName", dayOfWeekToName(dayOfWeek))
            val startTime = obj.optString("startTime", "")
            val endTime = obj.optString("endTime", "")
            val location = obj.optString("location", "")
            val instructor = obj.optString("instructor", "")

            if (units !in 1..10 || dayOfWeek !in 0..6 || !Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]").matches(startTime) || !Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]").matches(endTime)) continue
            val start = com.example.data.local.util.DateTimeNormalizer.timeToMinutes(startTime)
            val end = com.example.data.local.util.DateTimeNormalizer.timeToMinutes(endTime)
            if (end <= start) continue
            list.add(
                com.example.data.parser.ParsedCourseDraft(
                    name = name,
                    units = units,
                    dayOfWeek = dayOfWeek,
                    dayName = dayName,
                    startTime = startTime,
                    endTime = endTime,
                    location = location,
                    instructor = instructor,
                    confidence = 0.70f
                )
            )
        }
        return list
    }

    private fun dayOfWeekToName(day: Int): String = when (day) {
        0 -> "شنبه"
        1 -> "یکشنبه"
        2 -> "دوشنبه"
        3 -> "سه‌شنبه"
        4 -> "چهارشنبه"
        5 -> "پنج‌شنبه"
        6 -> "جمعه"
        else -> "شنبه"
    }

}
