package com.lx.simplewatermarkcamera.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class WatermarkComposerTest {
    @Test
    fun composesEnabledFieldsAndSkipsMissingValues() {
        val metadata = CaptureMetadata(
            capturedAt = Instant.parse("2026-06-13T08:05:09Z"),
            location = LocationSnapshot(
                latitude = 39.916527,
                longitude = 116.397128,
                accuracyMeters = 12.4f,
                altitudeMeters = 42.5,
                address = "北京市朝阳区",
                capturedAt = Instant.parse("2026-06-13T08:05:09Z")
            ),
            weather = WeatherSnapshot("晴 28.0°", 28.0, Instant.parse("2026-06-13T08:05:09Z"), WeatherSource.Network),
            heading = BearingFormatter.format(45f)
        )

        val config = WatermarkConfig(
            title = "项目检查",
            showLogo = true,
            showTime = true,
            showAddress = true,
            showWeather = true,
            showCoordinates = true,
            showAltitude = true,
            showHeading = true,
            saveOriginal = false,
            writeGpsExif = false,
            logoPath = null
        )
        val content = WatermarkComposer.compose(config, metadata)

        assertEquals("项目检查", content.title)
        assertTrue(content.lines.any { it.text == "地点：北京市朝阳区" })
        assertTrue(content.lines.any { it.text.contains("坐标：纬度 39.916527, 经度 116.397128") })
        assertTrue(content.lines.any { it.text == "海拔：42.5 m" })
        assertTrue(content.lines.any { it.text == "方位：东北 45°" })
    }

    @Test
    fun omitsDisabledFields() {
        val metadata = CaptureMetadata(
            capturedAt = Instant.parse("2026-06-13T08:05:09Z"),
            location = null,
            weather = WeatherSnapshot("晴 30.0°", 30.0, Instant.parse("2026-06-13T08:05:09Z"), WeatherSource.Network),
            heading = null
        )
        val config = WatermarkConfig(
            title = "项目检查",
            showLogo = true,
            showTime = false,
            showAddress = true,
            showWeather = false,
            showCoordinates = true,
            showAltitude = true,
            showHeading = true,
            saveOriginal = false,
            writeGpsExif = false,
            logoPath = null
        )

        val content = WatermarkComposer.compose(config, metadata)

        assertFalse(content.lines.any { it.label == "时间" })
        assertFalse(content.lines.any { it.label == "天气" })
    }

    @Test
    fun formatsFixedTimeWithZeroPadding() {
        val text = WatermarkFormatter.formatTime(
            Instant.parse("2026-06-13T01:03:07Z"),
            ZoneId.of("UTC")
        )

        assertEquals("2026-06-13 01:03:07", text)
    }
}
