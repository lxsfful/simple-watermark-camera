package com.lx.simplewatermarkcamera.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OptimizationAndExifTest {
    @Test
    fun testDecToDmsConversion() {
        val dmsPositive = PhotoStorageRepository.decToDms(39.9042)
        assertNotNull(dmsPositive)
        // 39.9042 deg -> 39 deg, (0.9042 * 60) = 54.252 min -> 54 min, (0.252 * 60 * 1000) = 15120 sec
        assertEquals("39/1,54/1,15120/1000", dmsPositive)

        val dmsLongitude = PhotoStorageRepository.decToDms(116.4074)
        assertNotNull(dmsLongitude)
        // 116.4074 deg -> 116 deg, (0.4074 * 60) = 24.444 min -> 24 min, (0.444 * 60 * 1000) = 26640 sec
        assertEquals("116/1,24/1,26640/1000", dmsLongitude)
    }
}
