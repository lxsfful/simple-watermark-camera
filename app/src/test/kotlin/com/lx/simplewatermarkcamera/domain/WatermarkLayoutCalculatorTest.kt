package com.lx.simplewatermarkcamera.domain

import org.junit.Assert.assertTrue
import org.junit.Test

class WatermarkLayoutCalculatorTest {
    @Test
    fun keepsBottomCardInsidePortraitImage() {
        val rect = WatermarkLayoutCalculator.bottomCard(1080, 1920, 6, true)

        assertTrue(rect.left >= 0)
        assertTrue(rect.top >= 0)
        assertTrue(rect.right <= 1080)
        assertTrue(rect.bottom <= 1920)
    }

    @Test
    fun keepsBottomCardInsideTinyImage() {
        val rect = WatermarkLayoutCalculator.bottomCard(1, 1, 12, true)

        assertTrue(rect.left >= 0)
        assertTrue(rect.top >= 0)
        assertTrue(rect.width >= 1)
        assertTrue(rect.height >= 1)
    }
}
