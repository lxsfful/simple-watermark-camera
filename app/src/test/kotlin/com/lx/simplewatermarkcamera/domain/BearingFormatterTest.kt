package com.lx.simplewatermarkcamera.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BearingFormatterTest {
    @Test
    fun mapsCardinalAndIntercardinalDirections() {
        assertEquals("北", BearingFormatter.format(0f).cardinalDirection)
        assertEquals("东北", BearingFormatter.format(45f).cardinalDirection)
        assertEquals("东", BearingFormatter.format(90f).cardinalDirection)
        assertEquals("东南", BearingFormatter.format(135f).cardinalDirection)
        assertEquals("南", BearingFormatter.format(180f).cardinalDirection)
        assertEquals("西南", BearingFormatter.format(225f).cardinalDirection)
        assertEquals("西", BearingFormatter.format(270f).cardinalDirection)
        assertEquals("西北", BearingFormatter.format(315f).cardinalDirection)
        assertEquals("北", BearingFormatter.format(359f).cardinalDirection)
    }

    @Test
    fun normalizesNegativeAngles() {
        assertEquals(315f, BearingFormatter.normalize(-45f), 0.001f)
    }
}
