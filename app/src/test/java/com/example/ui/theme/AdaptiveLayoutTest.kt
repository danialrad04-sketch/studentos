package com.example.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutTest {

    @Test
    fun compact_width_uses_spacious_mobile_metrics() {
        val metrics = studentAdaptiveMetricsForWidth(599)
        assertEquals(StudentWindowWidth.Compact, metrics.width)
        assertEquals(16, metrics.horizontalPadding.value.toInt())
        assertFalse(metrics.isDense)
    }

    @Test
    fun medium_width_uses_tablet_metrics() {
        val metrics = studentAdaptiveMetricsForWidth(600)
        assertEquals(StudentWindowWidth.Medium, metrics.width)
        assertEquals(24, metrics.horizontalPadding.value.toInt())
        assertFalse(metrics.isDense)
    }

    @Test
    fun expanded_width_enables_dense_large_screen_layout() {
        val metrics = studentAdaptiveMetricsForWidth(840)
        assertEquals(StudentWindowWidth.Expanded, metrics.width)
        assertEquals(32, metrics.horizontalPadding.value.toInt())
        assertTrue(metrics.isDense)
    }
}
