package com.example.data.parser

import com.example.data.local.util.DateTimeNormalizer

enum class DraftValidationState {
    VALID,
    NEEDS_REVIEW,
    INCOMPLETE,
    CONFLICT
}

data class ParsedCourseDraft(
    val tempId: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val units: Int = 3,
    val targetSemester: Int = 0, // 0 = unassigned / current semester, 1..10 = specific semester
    val instructor: String = "استاد دانشکده",
    val dayOfWeek: Int = 0, // 0 = شنبه, ..., 5 = پنج‌شنبه
    val dayName: String = "شنبه",
    val startTime: String = "08:00",
    val endTime: String = "10:00",
    val location: String = "دانشکده",
    val courseCode: String = "",
    val examDate: String = "",
    val examTime: String = "",
    val examLocation: String = "",
    val notes: String = "",
    val confidence: Float = 0.95f,
    val rawSnippet: String = "",
    val validationIssues: List<String> = emptyList(),
    val missingFields: List<String> = emptyList(),
    val validationState: DraftValidationState = DraftValidationState.VALID
)

/**
 * Production-ready Persian Natural Language & Bulk Course Parser.
 * Handles both structured pipe/table format and conversational Persian text.
 */
object RegistrationTextParser {

    private val dayPatterns = listOf(
        "چهارشنبه" to (4 to "چهارشنبه"),
        "چهار شنبه" to (4 to "چهارشنبه"),
        "پنج‌شنبه" to (5 to "پنج‌شنبه"),
        "پنجشنبه" to (5 to "پنج‌شنبه"),
        "پنج شنبه" to (5 to "پنج‌شنبه"),
        "سه‌شنبه" to (3 to "سه‌شنبه"),
        "سه شنبه" to (3 to "سه‌شنبه"),
        "یکشنبه" to (1 to "یکشنبه"),
        "يكشنبه" to (1 to "یکشنبه"),
        "یک شنبه" to (1 to "یکشنبه"),
        "دوشنبه" to (2 to "دوشنبه"),
        "دو شنبه" to (2 to "دوشنبه"),
        "جمعه" to (6 to "جمعه"),
        "شنبه" to (0 to "شنبه")
    )

    private val persianWordToNumber = mapOf(
        "یک" to 1, "دو" to 2, "سه" to 3, "چهار" to 4,
        "پنج" to 5, "شش" to 6, "هفت" to 7, "هشت" to 8,
        "نه" to 9, "ده" to 10, "یازده" to 11, "دوازده" to 12,
        "سیزده" to 13, "چهارده" to 14, "پانزده" to 15, "شانزده" to 16,
        "هفده" to 17, "هجده" to 18, "نوزده" to 19, "بیست" to 20
    )

    fun parse(rawText: String, systemPreset: UniversitySystem = UniversitySystem.AUTO_DETECT): List<ParsedCourseDraft> {
        if (rawText.isBlank()) return emptyList()

        val results = mutableListOf<ParsedCourseDraft>()
        val normalized = preNormalize(rawText)

        // Split by lines or delimiters
        val lines = normalized.lines()
            .flatMap { it.split(Regex("""[;\n]""")) }
            .map { it.trim() }
            .filter { it.isNotBlank() }

        for (line in lines) {
            val draft = parseSingleLine(line, systemPreset)
            if (draft != null) {
                results.add(draft)
            }
        }

        return results
    }

    enum class UniversitySystem(val displayName: String) {
        AUTO_DETECT("تشخیص هوشمند (خودکار)"),
        GOLESTAN("سامانه گلستان (دولتی / پیام نور)"),
        AMOZESHYAR("سامانه آموزشیار / سیدا (دانشگاه آزاد)"),
        HAMAVA("سامانه هم‌آوا (جامع علمی-کاربردی)")
    }

    private fun preNormalize(text: String): String {
        return text.replace("ي", "ی").replace("ك", "ک")
    }

    private fun parseSingleLine(line: String, systemPreset: UniversitySystem): ParsedCourseDraft? {
        val issues = mutableListOf<String>()
        val missing = mutableListOf<String>()

        if (line.contains("|")) {
            return parsePipeFormat(line, issues, missing)
        }

        if (systemPreset == UniversitySystem.GOLESTAN || line.contains("گلستان") || line.contains("زمان برگزاری") || line.contains("گروه")) {
            val golestanResult = parseGolestanFormat(line, issues, missing)
            if (golestanResult != null) return golestanResult
        }

        if (systemPreset == UniversitySystem.AMOZESHYAR || line.contains("آموزشیار") || line.contains("سیدا") || line.contains("تئوری/عملی") || line.contains("برنامه هفتگی:")) {
            val amozeshyarResult = parseAmozeshyarFormat(line, issues, missing)
            if (amozeshyarResult != null) return amozeshyarResult
        }

        if (systemPreset == UniversitySystem.HAMAVA || line.contains("هم آوا") || line.contains("هم‌آوا") || line.contains("روز و ساعت:")) {
            val hamavaResult = parseHamavaFormat(line, issues, missing)
            if (hamavaResult != null) return hamavaResult
        }

        // Natural Language Parsing
        return parseNaturalLanguage(line, issues, missing)
    }

    private fun parseGolestanFormat(line: String, issues: MutableList<String>, missing: MutableList<String>): ParsedCourseDraft? {
        // e.g. "ریاضی عمومی ۱ - کد 102030 - 3 واحد - استاد: دکتر حسینی - برگزاری: شنبه 08:00 تا 10:00 - مکان: تالار ۵ - امتحان: 1403/10/20 09:00"
        return parseNaturalLanguage(line, issues, missing)?.copy(confidence = 0.98f)
    }

    private fun parseAmozeshyarFormat(line: String, issues: MutableList<String>, missing: MutableList<String>): ParsedCourseDraft? {
        // e.g. "مبانی کامپیوتر - 3 واحد - تئوری - دکتر کاظمی - دوشنبه 10:00-12:00 (کلاس 204) - آزمون: 1403/10/25"
        return parseNaturalLanguage(line, issues, missing)?.copy(confidence = 0.98f)
    }

    private fun parseHamavaFormat(line: String, issues: MutableList<String>, missing: MutableList<String>): ParsedCourseDraft? {
        // e.g. "شبکه‌های کامپیوتر | ۳ واحد | مدرس: مهندس رضایی | روز و ساعت: چهارشنبه (14:00 تا 16:30)"
        return parseNaturalLanguage(line, issues, missing)?.copy(confidence = 0.98f)
    }

    private fun parsePipeFormat(line: String, issues: MutableList<String>, missing: MutableList<String>): ParsedCourseDraft? {
        val parts = line.split("|").map { it.trim() }
        if (parts.isEmpty()) return null

        val rawName = parts[0].replace(Regex("""^[0-9۰-۹\-\.\s\:\*]+"""), "").trim()
        if (rawName.isBlank()) return null

        var units = 3
        var targetSemester = 0
        var dayOfWeek = 0
        var dayName = "شنبه"
        var startTime = "08:00"
        var endTime = "10:00"
        var instructor = "استاد دانشکده"
        var location = "دانشکده"
        var courseCode = ""
        var examDate = ""
        var examTime = ""
        var examLocation = ""

        // Extract Course Code if in name (e.g. "CE101 - ترمودینامیک")
        val codeMatch = Regex("""([A-Za-z]{2,4}\s*\d{2,4})""").find(rawName)
        if (codeMatch != null) {
            courseCode = codeMatch.value.replace(" ", "")
        }

        for (i in 1 until parts.size) {
            val rawPart = parts[i]
            val part = DateTimeNormalizer.normalizeDigits(rawPart)

            // Semester (e.g. "ترم ۴" or "ترم 4" or "term 4")
            val semMatch = Regex("""(?:ترم|term|t|sem)\s*(\d{1,2})""").find(part)
            if (semMatch != null) {
                targetSemester = semMatch.groupValues[1].toIntOrNull() ?: 0
            }

            // Units (e.g. "۳ واحد" or "3")
            val unitMatch = Regex("""(\d)\s*(?:واحد|cr|units)?""").find(part)
            if (unitMatch != null && (rawPart.contains("واحد") || part.length <= 2)) {
                units = unitMatch.groupValues[1].toIntOrNull() ?: 3
            }

            // Weekday & Times (e.g. "شنبه ۱۰ تا ۱۲" or "یکشنبه 08:00-10:00")
            for ((key, pair) in dayPatterns) {
                if (rawPart.contains(key)) {
                    dayOfWeek = pair.first
                    dayName = pair.second
                    break
                }
            }

            val timeMatch = Regex("""(\d{1,2}(?::\d{2})?)\s*(?:تا|-|الی|to)\s*(\d{1,2}(?::\d{2})?)""").find(part)
            if (timeMatch != null) {
                startTime = DateTimeNormalizer.normalizeTime(timeMatch.groupValues[1])
                endTime = DateTimeNormalizer.normalizeTime(timeMatch.groupValues[2])
            }

            // Instructor (e.g. "دکتر رضایی" or "استاد حسینی")
            if (rawPart.contains("دکتر") || rawPart.contains("استاد") || rawPart.contains("مهندس")) {
                instructor = rawPart.replace("استاد:", "").replace("مدرس:", "").trim()
            }

            // Location / Room (e.g. "کلاس ۱۰۲" or "آمفی‌تئاتر")
            if (rawPart.contains("کلاس") || rawPart.contains("اتاق") || rawPart.contains("تالار") || rawPart.contains("دانشکده") || rawPart.contains("سالن") || rawPart.contains("آزمایشگاه")) {
                location = if (rawPart.contains("نامشخص")) "دانشکده" else rawPart.trim()
            }

            // Exam info (e.g. "امتحان: 1403/10/20 ساعت 09:00")
            if (rawPart.contains("امتحان") || rawPart.contains("آزمون") || rawPart.contains("فاینال")) {
                val dateMatch = Regex("""(\d{4}[/-]\d{1,2}[/-]\d{1,2})""").find(part)
                if (dateMatch != null) {
                    examDate = DateTimeNormalizer.normalizeSolarDate(dateMatch.value)
                }
                val exTimeMatch = Regex("""ساعت\s*(\d{1,2}(?::\d{2})?)""").find(part)
                if (exTimeMatch != null) {
                    examTime = DateTimeNormalizer.normalizeTime(exTimeMatch.groupValues[1])
                }
            }
        }

        val state = if (missing.isNotEmpty()) DraftValidationState.INCOMPLETE
        else if (issues.isNotEmpty()) DraftValidationState.NEEDS_REVIEW
        else DraftValidationState.VALID

        return ParsedCourseDraft(
            name = rawName,
            units = units,
            targetSemester = targetSemester,
            instructor = instructor,
            dayOfWeek = dayOfWeek,
            dayName = dayName,
            startTime = startTime,
            endTime = endTime,
            location = location,
            courseCode = courseCode,
            examDate = examDate,
            examTime = examTime,
            examLocation = examLocation,
            rawSnippet = line,
            validationIssues = issues,
            missingFields = missing,
            validationState = state
        )
    }

    private fun parseNaturalLanguage(line: String, issues: MutableList<String>, missing: MutableList<String>): ParsedCourseDraft? {
        val text = line.trim()
        val normalizedDigitsText = DateTimeNormalizer.normalizeDigits(text)

        // 0. Detect Semester (e.g. "ترم ۴", "ترم 4", "ترم چهارم", "term 4", "t4")
        var targetSemester = 0
        val semDigitMatch = Regex("""(?:ترم|term|t|sem)\s*(\d{1,2})""").find(normalizedDigitsText)
        if (semDigitMatch != null) {
            targetSemester = semDigitMatch.groupValues[1].toIntOrNull() ?: 0
        } else {
            val semWordMatch = Regex("""ترم\s*(اول|دوم|سوم|چهارم|پنجم|ششم|هفتم|هشتم|نهم|دهم)""").find(text)
            if (semWordMatch != null) {
                targetSemester = when (semWordMatch.groupValues[1]) {
                    "اول" -> 1
                    "دوم" -> 2
                    "سوم" -> 3
                    "چهارم" -> 4
                    "پنجم" -> 5
                    "ششم" -> 6
                    "هفتم" -> 7
                    "هشتم" -> 8
                    "نهم" -> 9
                    "دهم" -> 10
                    else -> 0
                }
            }
        }

        // 1. Detect and parse Weekday
        var dayOfWeek = 0
        var dayName = "شنبه"
        var foundDay = false
        for ((key, pair) in dayPatterns) {
            if (text.contains(key)) {
                dayOfWeek = pair.first
                dayName = pair.second
                foundDay = true
                break
            }
        }
        if (!foundDay) {
            missing.add("روز کلاس")
        }

        // 2. Detect Times (Digit or Word format e.g. "۱۰ تا ۱۲" or "ده تا دوازده")
        var startTime = "08:00"
        var endTime = "10:00"
        val timeDigitMatch = Regex("""(\d{1,2}(?::\d{2})?)\s*(?:تا|-|الی)\s*(\d{1,2}(?::\d{2})?)""").find(normalizedDigitsText)
        if (timeDigitMatch != null) {
            startTime = DateTimeNormalizer.normalizeTime(timeDigitMatch.groupValues[1])
            endTime = DateTimeNormalizer.normalizeTime(timeDigitMatch.groupValues[2])
        } else {
            // Word numbers (e.g. "ده تا دوازده")
            val wordTimeMatch = Regex("""(یک|دو|سه|چهار|پنج|شش|هفت|هشت|نه|ده|یازده|دوازده)\s*تا\s*(یک|دو|سه|چهار|پنج|شش|هفت|هشت|نه|ده|یازده|دوازده)""").find(text)
            if (wordTimeMatch != null) {
                val sNum = persianWordToNumber[wordTimeMatch.groupValues[1]] ?: 8
                val eNum = persianWordToNumber[wordTimeMatch.groupValues[2]] ?: 10
                startTime = String.format("%02d:00", sNum)
                endTime = String.format("%02d:00", eNum)
            } else {
                missing.add("ساعت کلاس")
            }
        }

        // 3. Units (e.g. "۳ واحد" or "سه واحد" or "3cr" or "(3)")
        var units = 3
        val unitDigitMatch = Regex("""(\d)\s*(?:واحد|cr|units)""").find(normalizedDigitsText)
        if (unitDigitMatch != null) {
            units = unitDigitMatch.groupValues[1].toIntOrNull() ?: 3
        } else {
            val unitWordMatch = Regex("""(یک|دو|سه|چهار)\s*واحد""").find(text)
            if (unitWordMatch != null) {
                units = persianWordToNumber[unitWordMatch.groupValues[1]] ?: 3
            }
        }

        // 4. Location Extraction
        var location = "دانشکده"
        var matchedLocationRaw: String? = null

        val hasExplicitUnknownLocation = Regex("""(?<=^|\s)(?:کلاس|اتاق|تالار|سالن|مکان|محل برگزاری)\s*[:：]?\s*نامشخص""").containsMatchIn(text)
        if (!hasExplicitUnknownLocation) {
            val locationRegex = Regex("""(?:(?<=^|\s)(?:مکان\s*[:：]|محل برگزاری\s*[:：]?)\s*)?(?<=^|\s)(?:کلاس|اتاق)\s*[:：]?\s*([0-9۰-۹\w]+)|(?<=^|\s)(?:تالار|سالن)\s*[:：]?\s*([^\s\|،\(\)]+)|(?<=^|\s)(?:آزمایشگاه)\s*[:：]?\s*([^\s\|،\(\)]+)""")
            for (m in locationRegex.findAll(text)) {
                val matchedVal = m.value.trim()
                if (matchedVal.contains("نامشخص")) continue
                // If match starts at index 0 and begins with آزمایشگاه or کلاس, it is the course title, not location
                if (m.range.first == 0 && (matchedVal.startsWith("آزمایشگاه") || matchedVal.startsWith("کلاس"))) continue

                matchedLocationRaw = matchedVal
                location = matchedVal
                break
            }
        }

        // 5. Instructor Extraction (Capturing multi-word names like دکتر سیامک علیپور)
        var instructor = "استاد دانشکده"
        var matchedInstructorRaw: String? = null
        val stopTokens = setOf(
            "واحد", "cr", "units",
            "شنبه", "یکشنبه", "يكشنبه", "دوشنبه", "سه‌شنبه", "سه", "چهارشنبه", "چهار", "پنج‌شنبه", "پنجشنبه", "پنج", "جمعه",
            "کلاس", "اتاق", "تالار", "سالن", "آزمایشگاه", "مکان", "امتحان", "آزمون", "ترم", "ساعت", "تا", "الی", "نامشخص"
        )
        val profMatch = Regex("""(?:(?:استاد|مدرس)\s*[:：]?\s*|(?:دکتر|مهندس)\s+)([^\s\|،0-9۰-۹\(\)]+(?:\s+[^\s\|،0-9۰-۹\(\)]+){0,2})""").find(text)
        if (profMatch != null) {
            val title = if (profMatch.value.startsWith("استاد") || profMatch.value.startsWith("مدرس")) "استاد"
            else if (profMatch.value.startsWith("دکتر")) "دکتر"
            else "مهندس"

            val rawNameWords = profMatch.groupValues[1].trim().split(Regex("""\s+"""))
            val cleanWords = mutableListOf<String>()
            for (w in rawNameWords) {
                if (stopTokens.contains(w) || w.contains("واحد")) break
                cleanWords.add(w)
            }
            if (cleanWords.isNotEmpty()) {
                val fullName = cleanWords.joinToString(" ")
                instructor = "$title $fullName".trim()
                matchedInstructorRaw = "$title $fullName".trim()
            }
        }

        // 6. Exam
        var examDate = ""
        var examTime = ""
        val examMatch = Regex("""امتحان\s*(.*?)(?:ساعت\s*([0-9۰-۹]{1,2}(?::[0-9۰-۹]{2})?)|$)""").find(text)
        if (examMatch != null) {
            val rawDate = examMatch.groupValues[1].trim()
            if (rawDate.isNotBlank()) {
                examDate = DateTimeNormalizer.normalizeSolarDate(DateTimeNormalizer.normalizeDigits(rawDate))
            }
            if (examMatch.groupValues.size > 2 && examMatch.groupValues[2].isNotBlank()) {
                examTime = DateTimeNormalizer.normalizeTime(DateTimeNormalizer.normalizeDigits(examMatch.groupValues[2]))
            }
        }

        // 7. Extract Course Name with Full Cleanup
        var cleanName = text

        // Strip matched location first
        if (matchedLocationRaw != null) {
            cleanName = cleanName.replace(matchedLocationRaw, " ")
        }
        // Strip general location / room patterns and "کلاس نامشخص"
        cleanName = cleanName
            .replace(Regex("""(?<=^|\s)(?:مکان\s*[:：]|محل برگزاری\s*[:：]?)\s*[^\s\|،\(\)]+"""), " ")
            .replace(Regex("""(?<=^|\s)(?:کلاس|اتاق)\s*[:：]?\s*(?:[0-9۰-۹]+|نامشخص|[^\s\|،\(\)]+)"""), " ")
            .replace(Regex("""(?<=^|\s)(?:تالار|سالن)\s*[:：]?\s*(?:[0-9۰-۹]+|نامشخص|[^\s\|،\(\)]+)"""), " ")
            .replace(Regex("""(?<=^|\s)(?:کلاس|اتاق|تالار|سالن|مکان)\s*نامشخص"""), " ")

        // Strip matched instructor
        if (matchedInstructorRaw != null) {
            cleanName = cleanName.replace(matchedInstructorRaw, " ")
        }
        // Strip any residual instructor mentions (multi-word and single-word)
        cleanName = cleanName
            .replace(Regex("""(?:(?:استاد|مدرس)\s*[:：]?\s*|(?:دکتر|مهندس)\s+)[^\s\|،0-9۰-۹\(\)]+(?:\s+[^\s\|،0-9۰-۹\(\)]+){0,2}"""), " ")
            .replace(Regex("""(?:استاد|دکتر|مهندس)\s+[^\s]+"""), " ")

        // Strip time ranges
        cleanName = cleanName
            .replace(Regex("""[0-9۰-۹]{1,2}(?::[0-9۰-۹]{2})?\s*(?:تا|-|الی)\s*[0-9۰-۹]{1,2}(?::[0-9۰-۹]{2})?"""), " ")
            .replace(Regex("""(یک|دو|سه|چهار|پنج|شش|هفت|هشت|نه|ده|یازده|دوازده)\s*تا\s*(یک|دو|سه|چهار|پنج|شش|هفت|هشت|نه|ده|یازده|دوازده)"""), " ")
            .replace(Regex("""(?:ترم|term|t|sem)\s*[0-9۰-۹]{1,2}"""), " ")
            .replace(Regex("""ترم\s*(اول|دوم|سوم|چهارم|پنجم|ششم|هفتم|هشتم|نهم|دهم)"""), " ")
            .replace(Regex("""[0-9۰-۹]\s*(?:واحد|cr|units)"""), " ")
            .replace(Regex("""(یک|دو|سه|چهار)\s*واحد"""), " ")
            .replace(Regex("""امتحان.*"""), " ")

        for ((dayStr, _) in dayPatterns) {
            cleanName = cleanName.replace(dayStr, " ")
        }
        cleanName = cleanName.replace(Regex("""[\|،\-\:\*\(\)]+"""), " ").trim()
        cleanName = cleanName.replace(Regex("""\s+"""), " ")

        if (cleanName.isBlank() || cleanName.length < 2) {
            cleanName = "درس ثبت‌شده"
            issues.add("نام درس شناسایی نشد، نام پیش‌فرض اختصاص یافت.")
        }

        val state = if (missing.isNotEmpty()) DraftValidationState.INCOMPLETE
        else if (issues.isNotEmpty()) DraftValidationState.NEEDS_REVIEW
        else DraftValidationState.VALID

        return ParsedCourseDraft(
            name = cleanName,
            units = units,
            targetSemester = targetSemester,
            instructor = instructor,
            dayOfWeek = dayOfWeek,
            dayName = dayName,
            startTime = startTime,
            endTime = endTime,
            location = location,
            courseCode = "",
            examDate = examDate,
            examTime = examTime,
            examLocation = "",
            rawSnippet = line,
            validationIssues = issues,
            missingFields = missing,
            validationState = state
        )
    }
}
