package com.lx.simplewatermarkcamera.domain

import java.time.Instant

enum class LogoSource { None, Company, Custom }

enum class LogoPosition(val label: String) {
    Above("文字上方"),
    Left("文字左侧"),
    Below("文字下方")
}

enum class LogoColorOption(val label: String, val hexColor: String?) {
    Original("原色", null),
    White("纯白", "#FFFFFF"),
    Blue("京湖蓝", "#0072C6"),
    Gold("亮金色", "#FFD700"),
    Purple("品牌紫", "#7A36D6"),
    Black("纯黑", "#000000")
}

enum class OpacityLevel(val alpha: Float, val label: String) {
    Full(1.0f, "100% (不透明)"),
    High(0.85f, "85%"),
    Medium(0.70f, "70%"),
    Low(0.50f, "50% (半透明)")
}

enum class FontSizeLevel(val scale: Float, val label: String) {
    Small(0.8f, "小"),
    Medium(1.0f, "标准"),
    Large(1.25f, "大")
}

data class WatermarkConfig(
    val title: String = "项目检查",
    val showLogo: Boolean = true,
    val logoSource: LogoSource = LogoSource.Company,
    val logoPosition: LogoPosition = LogoPosition.Above,
    val logoColorOption: LogoColorOption = LogoColorOption.Original,
    val opacityLevel: OpacityLevel = OpacityLevel.Full,
    val fontSizeLevel: FontSizeLevel = FontSizeLevel.Medium,
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

    val needsLocation: Boolean
        get() = showAddress || showWeather || showCoordinates || showAltitude || writeGpsExif
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
    val icon: String = "",
) {
    val text: String = buildString {
        if (icon.isNotBlank()) append("$icon ")
        if (label.isNotBlank()) append("$label：")
        append(value)
    }
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
