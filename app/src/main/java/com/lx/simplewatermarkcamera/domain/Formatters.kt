package com.lx.simplewatermarkcamera.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

object WatermarkFormatter {
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA)

    fun formatTime(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String =
        timeFormatter.format(instant.atZone(zoneId))

    fun formatCoordinates(location: LocationSnapshot): String =
        "${formatLatitude(location.latitude)}, ${formatLongitude(location.longitude)}"

    fun formatLatitude(latitude: Double): String = "纬度 %.6f".format(Locale.US, latitude)

    fun formatLongitude(longitude: Double): String = "经度 %.6f".format(Locale.US, longitude)

    fun formatAccuracy(accuracyMeters: Float?): String? = accuracyMeters?.let {
        "±${it.roundToInt()}m"
    }

    fun formatAltitude(altitudeMeters: Double?): String? = altitudeMeters?.let {
        "%.1f m".format(Locale.US, it)
    }

    fun formatAddress(address: String?): String? = address?.trim()?.takeIf { it.isNotEmpty() }

    fun formatWeather(weather: WeatherSnapshot?): String? = weather?.text?.trim()?.takeIf { it.isNotEmpty() }
}

object BearingFormatter {
    private val directions = listOf("北", "东北", "东", "东南", "南", "西南", "西", "西北")

    fun format(azimuthDegrees: Float): HeadingSnapshot {
        val normalized = normalize(azimuthDegrees)
        val index = (((normalized + 22.5f) / 45f).toInt()) % directions.size
        return HeadingSnapshot(
            azimuthDegrees = normalized,
            cardinalDirection = directions[index],
            accuracy = HeadingAccuracy.Medium,
        )
    }

    fun normalize(azimuthDegrees: Float): Float {
        val mod = azimuthDegrees % 360f
        return if (mod < 0f) mod + 360f else mod
    }

    fun display(snapshot: HeadingSnapshot): String =
        "${snapshot.cardinalDirection} ${snapshot.azimuthDegrees.roundToInt()}°"
}

object WatermarkComposer {
    fun compose(config: WatermarkConfig, metadata: CaptureMetadata): WatermarkContent {
        val location = metadata.location
        val lines = buildList {
            if (config.showTime) {
                add(WatermarkLine("时间", WatermarkFormatter.formatTime(metadata.capturedAt)))
            }
            if (config.showAddress) {
                WatermarkFormatter.formatAddress(location?.address)?.let { add(WatermarkLine("地点", it)) }
            }
            if (config.showWeather) {
                WatermarkFormatter.formatWeather(metadata.weather)?.let { add(WatermarkLine("天气", it)) }
            }
            if (config.showCoordinates && location != null) {
                val accuracy = WatermarkFormatter.formatAccuracy(location.accuracyMeters)
                val value = listOfNotNull(WatermarkFormatter.formatCoordinates(location), accuracy).joinToString(" ")
                add(WatermarkLine("坐标", value))
            }
            if (config.showAltitude) {
                WatermarkFormatter.formatAltitude(location?.altitudeMeters)?.let { add(WatermarkLine("海拔", it)) }
            }
            if (config.showHeading) {
                metadata.heading?.let { add(WatermarkLine("方位", BearingFormatter.display(it))) }
            }
        }
        return WatermarkContent(
            title = config.title.trim().ifEmpty { WatermarkConfig.Default.title },
            lines = lines,
            logoPath = config.logoPath?.takeIf { config.showLogo },
        )
    }
}

object PhotoFileNameGenerator {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS", Locale.US)

    fun generate(instant: Instant, suffix: String): String {
        val safeSuffix = sanitize(suffix).ifBlank { "PHOTO" }
        return "IMG_${formatter.format(instant.atZone(ZoneId.systemDefault()))}_${safeSuffix}.jpg"
    }

    fun sanitize(input: String): String = input
        .uppercase(Locale.US)
        .replace(Regex("[^A-Z0-9_-]"), "_")
        .trim('_')
}

object WatermarkLayoutCalculator {
    fun bottomCard(imageWidth: Int, imageHeight: Int, lineCount: Int, hasLogo: Boolean): RectSpec {
        val safeWidth = imageWidth.coerceAtLeast(1)
        val safeHeight = imageHeight.coerceAtLeast(1)
        val padding = (safeWidth * 0.035f).roundToInt().coerceAtLeast(18)
        val titleHeight = (safeWidth * 0.045f).roundToInt().coerceAtLeast(32)
        val lineHeight = (safeWidth * 0.031f).roundToInt().coerceAtLeast(24)
        val logoExtra = if (hasLogo) (safeWidth * 0.01f).roundToInt() else 0
        val desiredHeight = padding * 2 + titleHeight + lineHeight * lineCount.coerceAtLeast(1) + logoExtra
        val maxHeight = (safeHeight * 0.42f).roundToInt().coerceAtLeast(1)
        val height = desiredHeight.coerceAtMost(maxHeight).coerceAtLeast(1)
        val margin = (safeWidth * 0.025f).roundToInt().coerceAtLeast(12)
        val width = (safeWidth - margin * 2).coerceAtLeast(1)
        val top = (safeHeight - height - margin).coerceAtLeast(0)
        return RectSpec(margin, top, width, height)
    }
}
