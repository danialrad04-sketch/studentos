package com.example

import com.example.domain.engine.ExamScoreEngine
import org.junit.Assert.*
import org.junit.Test

class ExamScoreEngineTest {
    @Test fun instructorWeightIsUsedWithoutScalingItToTwenty() {
        val result = ExamScoreEngine.calculate(6.0, 12.0, 10.0)!!
        assertEquals(4.0, result.required, 0.00001)
        assertEquals(18.0, result.maximumTotal, 0.00001)
        assertTrue(result.achievable)
    }
    @Test fun unreachableGoalRemainsUnreachable() {
        val result = ExamScoreEngine.calculate(4.0, 12.0, 18.0)!!
        assertFalse(result.achievable)
        assertEquals(14.0, result.required, 0.00001)
    }
    @Test fun alreadySecuredGoalNeedsZeroMorePoints() {
        assertEquals(0.0, ExamScoreEngine.calculate(12.0, 8.0, 10.0)!!.required, 0.00001)
    }
    @Test fun invalidWeightsAndNonFiniteInputsAreRejected() {
        assertNull(ExamScoreEngine.calculate(9.0, 12.0, 10.0))
        assertNull(ExamScoreEngine.calculate(6.0, 0.0, 10.0))
        assertNull(ExamScoreEngine.calculate(6.0, 21.0, 10.0))
        assertNull(ExamScoreEngine.calculate(6.0, 12.0, Double.NaN))
        assertNull(ExamScoreEngine.calculate(-1.0, 12.0, 10.0))
    }
}
