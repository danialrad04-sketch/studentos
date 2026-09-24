package com.example.domain.engine

/**
 * Clean data structures required for deterministic Curriculum Resolution.
 */
data class ResolvedUniversity(
    val id: String,
    val displayNameFa: String,
    val shortName: String
)

data class ResolvedMajor(
    val id: String,
    val universityId: String,
    val facultyId: String,
    val displayNameFa: String
)

data class ResolvedCurriculumVersion(
    val id: String,
    val majorId: String,
    val title: String,
    val entryYearMin: Int,
    val entryYearMax: Int,
    val totalCreditsRequired: Int
)

sealed interface CurriculumResolutionResult {
    data class ExactMatch(val version: ResolvedCurriculumVersion) : CurriculumResolutionResult
    data class ExplicitRangeMatch(val version: ResolvedCurriculumVersion) : CurriculumResolutionResult
    data class NotFound(val reason: ResolutionFailureReason) : CurriculumResolutionResult
}

enum class ResolutionFailureReason {
    PROFILE_DATA_INCOMPLETE,
    UNIVERSITY_NOT_SUPPORTED,
    MAJOR_NOT_SUPPORTED,
    MAJOR_UNIVERSITY_MISMATCH,
    ENTRY_YEAR_OUT_OF_RANGE
}

/**
 * Strict Curriculum Resolver.
 * Validates the full chain: universityId -> majorId -> major.universityId == universityId -> entryYear in [min, max].
 * Zero guessing, zero closest fallbacks, zero fake defaults.
 */
class CurriculumResolver(
    private val universities: List<ResolvedUniversity>,
    private val majors: List<ResolvedMajor>,
    private val curriculumVersions: List<ResolvedCurriculumVersion>
) {

    fun resolve(
        universityId: String?,
        majorId: String?,
        entryYear: Int?
    ): CurriculumResolutionResult {
        // 1. Validate complete profile arguments
        if (universityId.isNullOrBlank() || majorId.isNullOrBlank() || entryYear == null) {
            return CurriculumResolutionResult.NotFound(ResolutionFailureReason.PROFILE_DATA_INCOMPLETE)
        }

        // 2. Validate University existence
        val universityExists = universities.any { it.id.equals(universityId, ignoreCase = true) }
        if (!universityExists) {
            return CurriculumResolutionResult.NotFound(ResolutionFailureReason.UNIVERSITY_NOT_SUPPORTED)
        }

        // 3. Validate Major existence
        val major = majors.firstOrNull { it.id.equals(majorId, ignoreCase = true) }
            ?: return CurriculumResolutionResult.NotFound(ResolutionFailureReason.MAJOR_NOT_SUPPORTED)

        // 4. Validate Major belongs to the selected University (CRITICAL INTEGRITY CHECK)
        if (!major.universityId.equals(universityId, ignoreCase = true)) {
            return CurriculumResolutionResult.NotFound(ResolutionFailureReason.MAJOR_UNIVERSITY_MISMATCH)
        }

        // 5. Query candidate curriculum versions strictly associated with this major
        val candidateVersions = curriculumVersions.filter {
            it.majorId.equals(majorId, ignoreCase = true)
        }
        if (candidateVersions.isEmpty()) {
            return CurriculumResolutionResult.NotFound(ResolutionFailureReason.ENTRY_YEAR_OUT_OF_RANGE)
        }

        // 6. Look for exact single-year curriculum match
        val exactMatch = candidateVersions.firstOrNull {
            it.entryYearMin == entryYear && it.entryYearMax == entryYear
        }
        if (exactMatch != null) {
            return CurriculumResolutionResult.ExactMatch(exactMatch)
        }

        // 7. Look for explicit accredited range match
        val rangeMatch = candidateVersions.firstOrNull {
            entryYear in it.entryYearMin..it.entryYearMax
        }
        if (rangeMatch != null) {
            return CurriculumResolutionResult.ExplicitRangeMatch(rangeMatch)
        }

        // 8. Explicit out of range failure (strictly no closest fallback or estimation)
        return CurriculumResolutionResult.NotFound(ResolutionFailureReason.ENTRY_YEAR_OUT_OF_RANGE)
    }
}
