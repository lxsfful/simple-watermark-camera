package com.lx.simplewatermarkcamera.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class WatermarkFormatterTest {
    @Test
    fun formatsLatitudeAndLongitude() {
        assertEquals("纬度 39.916527", WatermarkFormatter.formatLatitude(39.916527))
        assertEquals("经度 116.397128", WatermarkFormatter.formatLongitude(116.397128))
        assertEquals(
            "纬度 39.916527, 经度 116.397128",
            WatermarkFormatter.formatCoordinates(LocationSnapshot(39.916527, 116.397128, capturedAt = Instant.now())),
        )
    }

    @Test
    fun formatsAltitudeAndAccuracy() {
        assertEquals("42.5 m", WatermarkFormatter.formatAltitude(42.5))
        assertEquals("±12m", WatermarkFormatter.formatAccuracy(12.4f))
        assertNull(WatermarkFormatter.formatAltitude(null))
        assertNull(WatermarkFormatter.formatAccuracy(null))
    }

    @Test
    fun formatsAddressAndWeather() {
        assertEquals("北京市朝阳区", WatermarkFormatter.formatAddress("  北京市朝阳区  "))
        assertNull(WatermarkFormatter.formatAddress("   "))
        assertEquals(
            "晴 28.0°",
            WatermarkFormatter.formatWeather(WeatherSnapshot("晴 28.0°", 28.0, Instant.now(), WeatherSource.Network)),
        )
        assertNull(WatermarkFormatter.formatWeather(WeatherSnapshot("  ", null, Instant.now(), WeatherSource.Network)))
    }

    @Test
    fun formatsTimeInExplicitZone() {
        assertEquals(
            "2026-06-13 01:03:07",
            WatermarkFormatter.formatTime(Instant.parse("2026-06-13T01:03:07Z"), ZoneId.of("UTC")),
        )
    }
}
