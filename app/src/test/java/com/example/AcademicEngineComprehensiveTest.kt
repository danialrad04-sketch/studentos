package com.example

import com.example.domain.engine.CourseIdentityNormalizer
import com.example.domain.engine.CourseStateResolver
import com.example.domain.engine.CurriculumMatcher
import com.example.domain.engine.CurriculumResolutionResult
import com.example.domain.engine.CurriculumResolver
import com.example.domain.engine.PrerequisiteEngine
import com.example.domain.engine.ResolutionFailureReason
import com.example.domain.engine.ResolvableCurriculumCourse
import com.example.domain.engine.ResolvedCurriculumVersion
import com.example.domain.engine.ResolvedMajor
import com.example.domain.engine.ResolvedUniversity
import com.example.domain.engine.StudentCourseAttempt
import com.example.domain.model.CoursePrerequisiteRule
import com.example.domain.model.CourseState
import com.example.domain.model.DataConfidence
import com.example.domain.model.DataSource
import com.example.domain.model.GpaState
import com.example.domain.model.PrerequisiteCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive Test Suite for Academic Intelligence Foundation (Phase 4).
 * Covers CurriculumResolver, CourseIdentityNormalizer, PrerequisiteEngine,
 * CourseStateResolver, CurriculumMatcher, and AcademicProgress.
 */
class AcademicEngineComprehensiveTest {

    private val sampleUniversities = listOf(
        ResolvedUniversity("UNI_AUT", "دانشگاه صنعتی امیرکبیر", "پلی‌تکنیک"),
        ResolvedUniversity("UNI_UT", "دانشگاه تهران", "دانشگاه تهران")
    )

    private val sampleMajors = listOf(
        ResolvedMajor("MAJ_AUT_CHEM_ENG", "UNI_AUT", "FAC_AUT_CHEM", "مهندسی شیمی"),
        ResolvedMajor("MAJ_UT_CHEM_ENG", "UNI_UT", "FAC_UT_ENG", "مهندسی شیمی"),
        ResolvedMajor("MAJ_AUT_COMP_ENG", "UNI_AUT", "FAC_AUT_COMP", "مهندسی کامپیوتر")
    )

    private val sampleVersions = listOf(
        ResolvedCurriculumVersion(
            id = "CURR_AUT_CE_1401",
            majorId = "MAJ_AUT_CHEM_ENG",
            title = "چارت مهندسی شیمی امیرکبیر ورودی‌های ۱۴۰۱ تا ۱۴۰۴",
            entryYearMin = 1401,
            entryYearMax = 1404,
            totalCreditsRequired = 140
        ),
        ResolvedCurriculumVersion(
            id = "CURR_AUT_CE_1399",
            majorId = "MAJ_AUT_CHEM_ENG",
            title = "چارت مهندسی شیمی امیرکبیر ورودی‌های ۱۳۹۹ و ۱۴۰۰",
            entryYearMin = 1399,
            entryYearMax = 1400,
            totalCreditsRequired = 142
        )
    )

    private val resolver = CurriculumResolver(sampleUniversities, sampleMajors, sampleVersions)

    // --- 1. CURRICULUM RESOLVER TESTS ---

    @Test
    fun test1_validUniversityAndMajorAndEntryYear_resolvesExplicitRangeMatch() {
        val result = resolver.resolve(
            universityId = "UNI_AUT",
            majorId = "MAJ_AUT_CHEM_ENG",
            entryYear = 1402
        )
        assertTrue(result is CurriculumResolutionResult.ExplicitRangeMatch)
        val match = (result as CurriculumResolutionResult.ExplicitRangeMatch).version
        assertEquals("CURR_AUT_CE_1401", match.id)
        assertEquals(140, match.totalCreditsRequired)
    }

    @Test
    fun test2_universityAndMajorBelongsToAnotherUniversity_failsWithMismatch() {
        // University is AUT, but Major is UT
        val result = resolver.resolve(
            universityId = "UNI_AUT",
            majorId = "MAJ_UT_CHEM_ENG",
            entryYear = 1402
        )
        assertTrue(result is CurriculumResolutionResult.NotFound)
        assertEquals(
            ResolutionFailureReason.MAJOR_UNIVERSITY_MISMATCH,
            (result as CurriculumResolutionResult.NotFound).reason
        )
    }

    @Test
    fun test3_unknownUniversity_failsWithUniversityNotSupported() {
        val result = resolver.resolve(
            universityId = "UNI_UNKNOWN",
            majorId = "MAJ_AUT_CHEM_ENG",
            entryYear = 1402
        )
        assertTrue(result is CurriculumResolutionResult.NotFound)
        assertEquals(
            ResolutionFailureReason.UNIVERSITY_NOT_SUPPORTED,
            (result as CurriculumResolutionResult.NotFound).reason
        )
    }

    @Test
    fun test4_unknownMajor_failsWithMajorNotSupported() {
        val result = resolver.resolve(
            universityId = "UNI_AUT",
            majorId = "MAJ_UNKNOWN",
            entryYear = 1402
        )
        assertTrue(result is CurriculumResolutionResult.NotFound)
        assertEquals(
            ResolutionFailureReason.MAJOR_NOT_SUPPORTED,
            (result as CurriculumResolutionResult.NotFound).reason
        )
    }

    @Test
    fun test5_entryYearOutOfRange_failsWithEntryYearOutOfRange() {
        // Year 1395 is not covered by 1399-1400 or 1401-1404
        val result = resolver.resolve(
            universityId = "UNI_AUT",
            majorId = "MAJ_AUT_CHEM_ENG",
            entryYear = 1395
        )
        assertTrue(result is CurriculumResolutionResult.NotFound)
        assertEquals(
            ResolutionFailureReason.ENTRY_YEAR_OUT_OF_RANGE,
            (result as CurriculumResolutionResult.NotFound).reason
        )
    }

    @Test
    fun test6_incompleteProfileData_failsWithProfileDataIncomplete() {
        val result1 = resolver.resolve(null, "MAJ_AUT_CHEM_ENG", 1402)
        val result2 = resolver.resolve("UNI_AUT", null, 1402)
        val result3 = resolver.resolve("UNI_AUT", "MAJ_AUT_CHEM_ENG", null)

        assertTrue(result1 is CurriculumResolutionResult.NotFound)
        assertEquals(ResolutionFailureReason.PROFILE_DATA_INCOMPLETE, (result1 as CurriculumResolutionResult.NotFound).reason)
        assertTrue(result2 is CurriculumResolutionResult.NotFound)
        assertEquals(ResolutionFailureReason.PROFILE_DATA_INCOMPLETE, (result2 as CurriculumResolutionResult.NotFound).reason)
        assertTrue(result3 is CurriculumResolutionResult.NotFound)
        assertEquals(ResolutionFailureReason.PROFILE_DATA_INCOMPLETE, (result3 as CurriculumResolutionResult.NotFound).reason)
    }

    // --- 2. COURSE IDENTITY NORMALIZER TESTS ---

    @Test
    fun testCourseIdentityNormalizer_persianArabicDigitsAndNumerals() {
        val raw1 = "ترمودینامیک ۲"
        val raw2 = "ترمودینامیک II"
        val raw3 = "ترموديناميك 2" // Arabic 'ي' and 'ك'

        val n1 = CourseIdentityNormalizer.normalize(raw1)
        val n2 = CourseIdentityNormalizer.normalize(raw2)
        val n3 = CourseIdentityNormalizer.normalize(raw3)

        assertEquals("ترمودینامیک 2", n1)
        assertEquals("ترمودینامیک 2", n2)
        assertEquals("ترمودینامیک 2", n3)
        assertEquals(n1, n2)
        assertEquals(n2, n3)
    }

    // --- 3. PREREQUISITE ENGINE TESTS ---

    @Test
    fun testPrerequisiteEngine_singlePrerequisite_satisfiedAndUnsatisfied() {
        val rule = CoursePrerequisiteRule(
            rootCondition = PrerequisiteCondition.CoursePassed("CR_CHEM_1", "شیمی عمومی")
        )

        val satisfied = PrerequisiteEngine.evaluate(
            rule = rule,
            passedCourseIds = setOf("CR_CHEM_1"),
            currentCourseIds = emptySet(),
            passedCredits = 20
        )
        assertTrue(satisfied is PrerequisiteEngine.EvaluationResult.Satisfied)

        val unsatisfied = PrerequisiteEngine.evaluate(
            rule = rule,
            passedCourseIds = emptySet(),
            currentCourseIds = emptySet(),
            passedCredits = 20
        )
        assertTrue(unsatisfied is PrerequisiteEngine.EvaluationResult.Unsatisfied)
        val reason = (unsatisfied as PrerequisiteEngine.EvaluationResult.Unsatisfied).blockedReason
        assertEquals(listOf("شیمی عمومی"), reason.missingPrerequisiteNames)
    }

    @Test
    fun testPrerequisiteEngine_compositeAndGroup_allRequired() {
        val rule = CoursePrerequisiteRule(
            rootCondition = PrerequisiteCondition.AndGroup(
                listOf(
                    PrerequisiteCondition.CoursePassed("CR_MATH_DIFF", "معادلات دیفرانسیل"),
                    PrerequisiteCondition.CoursePassed("CR_BALANCE", "موازنه انرژی و مواد")
                )
            )
        )

        // Missing balance
        val eval1 = PrerequisiteEngine.evaluate(
            rule = rule,
            passedCourseIds = setOf("CR_MATH_DIFF"),
            currentCourseIds = emptySet(),
            passedCredits = 30
        )
        assertTrue(eval1 is PrerequisiteEngine.EvaluationResult.Unsatisfied)
        val reason = (eval1 as PrerequisiteEngine.EvaluationResult.Unsatisfied).blockedReason
        assertEquals(listOf("موازنه انرژی و مواد"), reason.missingPrerequisiteNames)

        // All passed
        val eval2 = PrerequisiteEngine.evaluate(
            rule = rule,
            passedCourseIds = setOf("CR_MATH_DIFF", "CR_BALANCE"),
            currentCourseIds = emptySet(),
            passedCredits = 30
        )
        assertTrue(eval2 is PrerequisiteEngine.EvaluationResult.Satisfied)
    }

    @Test
    fun testPrerequisiteEngine_minimumCreditsRequirement_forInternship() {
        val rule = CoursePrerequisiteRule(
            rootCondition = PrerequisiteCondition.MinimumTotalPassedCredits(80)
        )

        val deficit = PrerequisiteEngine.evaluate(
            rule = rule,
            passedCourseIds = emptySet(),
            currentCourseIds = emptySet(),
            passedCredits = 74
        )
        assertTrue(deficit is PrerequisiteEngine.EvaluationResult.Unsatisfied)
        val reason = (deficit as PrerequisiteEngine.EvaluationResult.Unsatisfied).blockedReason
        assertEquals(80, reason.requiredMinUnits)
        assertEquals(74, reason.currentPassedUnits)

        val met = PrerequisiteEngine.evaluate(
            rule = rule,
            passedCourseIds = emptySet(),
            currentCourseIds = emptySet(),
            passedCredits = 82
        )
        assertTrue(met is PrerequisiteEngine.EvaluationResult.Satisfied)
    }

    // --- 4. COURSE STATE RESOLVER TESTS ---

    @Test
    fun testCourseStateResolver_priorityTree() {
        val course = ResolvableCurriculumCourse(
            id = "CR_HEAT_1",
            curriculumVersionId = "CURR_AUT_CE_1401",
            code = "1115301",
            canonicalName = "انتقال حرارت 1",
            units = 3,
            courseType = "تخصصی",
            recommendedSemester = 5,
            rule = CoursePrerequisiteRule(PrerequisiteCondition.CoursePassed("CR_FLUID_1", "مکانیک سیالات 1"))
        )

        // 1. Current enrollment takes top priority
        val resCurrent = CourseStateResolver.resolve(
            course = course,
            enrolledCurrentCourseIds = setOf("CR_HEAT_1"),
            enrolledCurrentCourseNames = emptySet(),
            studentAttempts = emptyList(),
            passedCourseIds = setOf("CR_FLUID_1"),
            passedCredits = 70
        )
        assertEquals(CourseState.CURRENT, resCurrent.state)

        // 2. Failed attempt takes priority if not current
        val resFailed = CourseStateResolver.resolve(
            course = course,
            enrolledCurrentCourseIds = emptySet(),
            enrolledCurrentCourseNames = emptySet(),
            studentAttempts = listOf(
                StudentCourseAttempt(courseId = "CR_HEAT_1", courseName = "انتقال حرارت 1", units = 3, status = "FAILED", grade = 8.5)
            ),
            passedCourseIds = setOf("CR_FLUID_1"),
            passedCredits = 70
        )
        assertEquals(CourseState.FAILED, resFailed.state)

        // 3. Blocked when prerequisite not met
        val resBlocked = CourseStateResolver.resolve(
            course = course,
            enrolledCurrentCourseIds = emptySet(),
            enrolledCurrentCourseNames = emptySet(),
            studentAttempts = emptyList(),
            passedCourseIds = emptySet(),
            passedCredits = 70
        )
        assertEquals(CourseState.BLOCKED, resBlocked.state)
        assertNotNull(resBlocked.blockedReason)

        // 4. Available when prerequisite is passed
        val resAvailable = CourseStateResolver.resolve(
            course = course,
            enrolledCurrentCourseIds = emptySet(),
            enrolledCurrentCourseNames = emptySet(),
            studentAttempts = emptyList(),
            passedCourseIds = setOf("CR_FLUID_1"),
            passedCredits = 70
        )
        assertEquals(CourseState.AVAILABLE, resAvailable.state)
    }

    // --- 5. CURRICULUM MATCHER & ACADEMIC PROGRESS TESTS ---

    @Test
    fun testCurriculumMatcher_comprehensiveProgressCalculation() {
        val version = sampleVersions[0] // 140 total credits required

        // 1. Zero credits - Freshman state
        val zeroProgress = CurriculumMatcher.computeProgress(
            version = version,
            declaredPassedCredits = 0,
            declaredGpa = null,
            studentAttempts = emptyList(),
            currentEnrolledUnits = 18,
            matchOutput = null
        )
        assertEquals(140, zeroProgress.totalRequiredCredits)
        assertEquals(0, zeroProgress.passedCredits)
        assertEquals(18, zeroProgress.currentCredits)
        assertEquals(122, zeroProgress.remainingCredits)
        assertEquals(0f, zeroProgress.progressPercentage, 0.01f)
        assertTrue(zeroProgress.gpaState is GpaState.Unknown)

        // 2. Quick Setup Credit-Only (84 credits passed, no granular history, GPA 17.42)
        val creditOnlyProgress = CurriculumMatcher.computeProgress(
            version = version,
            declaredPassedCredits = 84,
            declaredGpa = 17.42,
            studentAttempts = emptyList(),
            currentEnrolledUnits = 18,
            matchOutput = null
        )
        assertEquals(140, creditOnlyProgress.totalRequiredCredits)
        assertEquals(84, creditOnlyProgress.passedCredits)
        assertEquals(18, creditOnlyProgress.currentCredits)
        assertEquals(38, creditOnlyProgress.remainingCredits)
        assertEquals(60.0f, creditOnlyProgress.progressPercentage, 0.01f)
        assertEquals(DataConfidence.CREDIT_ONLY, creditOnlyProgress.confidence)
        assertEquals(DataSource.USER_DECLARED, creditOnlyProgress.passedCreditsSource)
        assertTrue(creditOnlyProgress.gpaState is GpaState.Known)
        assertEquals(17.42, (creditOnlyProgress.gpaState as GpaState.Known).value, 0.001)

        // 3. Verified Full History from logged course attempts
        val attempts = listOf(
            StudentCourseAttempt("CR_MATH_1", "ریاضی عمومی 1", 3, "PASSED", 18.0),
            StudentCourseAttempt("CR_PHYS_1", "فیزیک 1", 3, "PASSED", 16.5),
            StudentCourseAttempt("CR_CHEM_1", "شیمی عمومی", 3, "PASSED", 17.0)
        )
        val fullHistoryProgress = CurriculumMatcher.computeProgress(
            version = version,
            declaredPassedCredits = 9,
            declaredGpa = 17.16,
            studentAttempts = attempts,
            currentEnrolledUnits = 16,
            matchOutput = null
        )
        assertEquals(9, fullHistoryProgress.passedCredits)
        assertEquals(DataConfidence.VERIFIED_FULL, fullHistoryProgress.confidence)
        assertEquals(DataSource.COURSE_RECORDS_SUM, fullHistoryProgress.passedCreditsSource)
    }
}
