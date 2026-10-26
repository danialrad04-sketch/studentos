package com.example

import com.example.domain.engine.GradeGoalCourse
import com.example.domain.engine.GradeGoalEngine
import org.junit.Assert.*
import org.junit.Test

class GradeGoalEngineTest {
    @Test fun weightedRemainingAverageKeepsRecordedScoresFixed() {
        val result = GradeGoalEngine.calculate(listOf(GradeGoalCourse(3, 12.0), GradeGoalCourse(1, null)), 14.0)
        assertEquals(20.0, result.requiredAverage!!, 0.0001)
        assertEquals(14.0, result.maximumAverage!!, 0.0001)
        assertTrue(result.achievable)
    }
    @Test fun impossibleTargetIsNotClampedIntoAFalseSuccess() {
        val result = GradeGoalEngine.calculate(listOf(GradeGoalCourse(3, 12.0), GradeGoalCourse(1, null)), 17.0)
        assertEquals(32.0, result.requiredAverage!!, 0.0001)
        assertFalse(result.achievable)
    }
    @Test fun explicitlyRecordedZeroStaysRecorded() {
        val result = GradeGoalEngine.calculate(listOf(GradeGoalCourse(3, 0.0), GradeGoalCourse(3, null)), 10.0)
        assertEquals(3, result.remainingUnits)
        assertEquals(0.0, result.recordedAverage!!, 0.0001)
        assertEquals(20.0, result.requiredAverage!!, 0.0001)
    }
    @Test fun allUnknownDoesNotInventRecordedAverage() {
        val result = GradeGoalEngine.calculate(listOf(GradeGoalCourse(3, null)), 17.0)
        assertNull(result.recordedAverage)
        assertEquals(17.0, result.requiredAverage!!, 0.0001)
    }
    @Test fun allRecordedReturnsActualMaximumAndNoRemainingPrediction() {
        val result = GradeGoalEngine.calculate(listOf(GradeGoalCourse(2, 18.0)), 17.0)
        assertNull(result.requiredAverage)
        assertTrue(result.achievable)
        assertEquals(18.0, result.maximumAverage!!, 0.0001)
    }
    @Test fun emptyAndInvalidRecordsCannotProduceAnInventedGpa() {
        val result = GradeGoalEngine.calculate(listOf(GradeGoalCourse(0, 18.0), GradeGoalCourse(3, Double.NaN), GradeGoalCourse(3, 21.0)), 17.0)
        assertEquals(3, result.invalidCourses)
        assertNull(result.maximumAverage)
        assertFalse(result.achievable)
    }
    @Test fun targetAlreadySecuredRequiresNoMorePoints() {
        val result = GradeGoalEngine.calculate(listOf(GradeGoalCourse(9, 20.0), GradeGoalCourse(1, null)), 17.0)
        assertEquals(0.0, result.requiredAverage!!, 0.0001)
        assertTrue(result.achievable)
    }
    @Test(expected = IllegalArgumentException::class) fun invalidTargetIsRejected() {
        GradeGoalEngine.calculate(emptyList(), Double.NaN)
    }
}
