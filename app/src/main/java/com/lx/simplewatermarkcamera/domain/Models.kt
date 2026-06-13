package com.lx.simplewatermarkcamera.domain

import java.time.Instant

data class WatermarkConfig(
    val title: String = "项目检查",
    val showLogo: Boolean = true,
    val showTime: Boolean = true,
    val showAddress: Boolean = false,
    val showWeather: Boolean = false,
    val showCoordinates: Boolean = false,
    val showAltitude: Boolean = false,
    val showHeading: Boolean = true,
    val saveOriginal: Boolean = false,
    val writeGpsExif: Boolean = false,
    val logoPath: String? = null,
) {
    companion object {
        val Default = WatermarkConfig()
    }
}

data class LocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val altitudeMeters: Double? = null,
    val address: String? = null,
    val capturedAt: Instant = Instant.now(),
)

data class WeatherSnapshot(
    val text: String,
    val temperatureCelsius: Double? = null,
    val fetchedAt: Instant = Instant.now(),
    val source: WeatherSource = WeatherSource.Unavailable,
)

enum class WeatherSource { Network, Cache, Unavailable }

data class HeadingSnapshot(
    val azimuthDegrees: Float,
    val cardinalDirection: String,
    val accuracy: HeadingAccuracy = HeadingAccuracy.Medium,
)

enum class HeadingAccuracy { High, Medium, Low, Unreliable }

data class CaptureMetadata(
    val capturedAt: Instant,
    val location: LocationSnapshot? = null,
    val weather: WeatherSnapshot? = null,
    val heading: HeadingSnapshot? = null,
)

data class WatermarkLine(
    val label: String,
    val value: String,
) {
    val text: String = if (label.isBlank()) value else "$label：$value"
}

data class WatermarkContent(
    val title: String,
    val lines: List<WatermarkLine>,
    val logoPath: String?,
)

enum class WatermarkPosition { Bottom }

data class RectSpec(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
) {
    val right: Int = left + width
    val bottom: Int = top + height
}
