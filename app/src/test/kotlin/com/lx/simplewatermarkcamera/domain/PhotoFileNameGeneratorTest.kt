package com.lx.simplewatermarkcamera.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class PhotoFileNameGeneratorTest {
    @Test
    fun generatesSafeJpegFileName() {
        val name = PhotoFileNameGenerator.generate(Instant.parse("2026-06-13T01:03:07.123Z"), "wm")

        assertTrue(name.startsWith("IMG_"))
        assertTrue(name.endsWith("_WM.jpg"))
        assertFalse(name.contains(" "))
    }

    @Test
    fun sanitizesUnsafeSuffix() {
        assertEquals("ORIGINAL_1", PhotoFileNameGenerator.sanitize(" original 1!"))
    }
}
