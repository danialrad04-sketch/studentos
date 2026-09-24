package com.example.domain.engine

/**
 * Normalizer for Persian / Arabic scripts, numerals, punctuation, and aliases.
 * Enables deterministic Course Identity resolution regardless of typing variations.
 */
object CourseIdentityNormalizer {

    /**
     * Normalizes text by:
     * 1. Converting Arabic 'ي' to Persian 'ی' and 'ك' to 'ک'
     * 2. Converting Persian/Arabic digits (۰-۹ / ٠-٩) and Roman numerals (I-VIII) to standard ASCII digits
     * 3. Removing redundant honorifics/prefixes like "درس", "کلاس"
     * 4. Collapsing excessive whitespace and half-spaces
     */
    fun normalize(raw: String): String {
        if (raw.isBlank()) return ""

        var s = raw.trim()

        // 1. Arabic character normalization
        s = s.replace('ي', 'ی')
            .replace('ئ', 'ی')
            .replace('ك', 'ک')
            .replace('ة', 'ه')
            .replace('\u200C', ' ') // Zero-width non-joiner to standard space

        // 2. Digits normalization (Persian & Arabic to standard ASCII)
        val farsiDigits = "۰۱۲۳۴۵۶۷۸۹"
        val arabicDigits = "٠١٢٣٤٥٦٧٨٩"
        for (i in 0..9) {
            s = s.replace(farsiDigits[i], ('0' + i))
            s = s.replace(arabicDigits[i], ('0' + i))
        }

        // 3. Roman numerals normalization (standard course suffixes)
        s = s.replace(Regex("(?i)\\bviii\\b"), "8")
            .replace(Regex("(?i)\\bvii\\b"), "7")
            .replace(Regex("(?i)\\bvi\\b"), "6")
            .replace(Regex("(?i)\\biv\\b"), "4")
            .replace(Regex("(?i)\\bv\\b"), "5")
            .replace(Regex("(?i)\\biii\\b"), "3")
            .replace(Regex("(?i)\\bii\\b"), "2")
            .replace(Regex("(?i)\\bi\\b"), "1")

        // 4. Common prefixes removal
        s = s.replace(Regex("^درس\\s+"), "")
            .replace(Regex("^آزمایشگاه\\s+"), "آز ")
            .replace(Regex("^کارگاه\\s+"), "کارگاه ")

        // 5. Whitespace collapsing
        s = s.replace(Regex("\\s+"), " ").trim()

        return s
    }

    /**
     * Checks if two course representations match by code or normalized name.
     */
    fun isMatching(codeA: String?, nameA: String, codeB: String?, nameB: String): Boolean {
        if (!codeA.isNullOrBlank() && !codeB.isNullOrBlank()) {
            if (normalize(codeA) == normalize(codeB)) return true
        }
        val normA = normalize(nameA)
        val normB = normalize(nameB)
        if (normA.isEmpty() || normB.isEmpty()) return false
        return normA == normB || normA.contains(normB) || normB.contains(normA)
    }
}
