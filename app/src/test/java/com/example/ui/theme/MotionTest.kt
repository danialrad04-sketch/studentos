package com.example.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTest {
    @Test
    fun zero_animation_scale_enables_reduced_motion() {
        assertTrue(shouldReduceMotion(0f))
    }

    @Test
    fun positive_animation_scale_keeps_normal_motion() {
        assertFalse(shouldReduceMotion(1f))
        assertFalse(shouldReduceMotion(0.5f))
    }

    @Test
    fun negative_animation_scale_is_treated_as_reduced_motion() {
        assertTrue(shouldReduceMotion(-1f))
    }
}
