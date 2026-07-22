package com.lx.simplewatermarkcamera.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object WatermarkFormatter {
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
    private val dateOfWeekFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE", Locale.CHINA)
    private val bigTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
    private val dayOfWeekFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.CHINA)
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)

    fun formatTime(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String =
        timeFormatter.format(instant.atZone(zoneId))

    fun formatTimeWithDayOfWeek(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String =
        dateOfWeekFormatter.format(instant.atZone(zoneId))

    fun formatBigTime(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String =
        bigTimeFormatter.format(instant.atZone(zoneId))

    fun formatDayOfWeek(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String =
        dayOfWeekFormatter.format(instant.atZone(zoneId))

    fun formatDate(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String =
        dateFormatter.format(instant.atZone(zoneId))

    fun formatCoordinates(location: LocationSnapshot): String =
        "${formatLatitude(location.latitude)}, ${formatLongitude(location.longitude)}"

    fun formatLatitude(latitude: Double): String = "%.6f°%s".format(Locale.US, abs(latitude), if (latitude >= 0) "N" else "S")

    fun formatLongitude(longitude: Double): String = "%.6f°%s".format(Locale.US, abs(longitude), if (longitude >= 0) "E" else "W")

    fun formatAccuracy(accuracyMeters: Float?): String? = accuracyMeters?.let {
        "±${it.roundToInt()}m"
    }

    fun formatAltitude(altitudeMeters: Double?): String? = altitudeMeters?.takeIf { abs(it) > 0.001 }?.let {
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
                add(WatermarkLine("时间", WatermarkFormatter.formatTime(metadata.capturedAt), icon = "🕒"))
            }
            if (config.showAddress) {
                WatermarkFormatter.formatAddress(location?.address)?.let { add(WatermarkLine("地点", it, icon = "📍")) }
            }
            if (config.showWeather) {
                WatermarkFormatter.formatWeather(metadata.weather)?.let { add(WatermarkLine("天气", it, icon = "☀️")) }
            }
            if (config.showCoordinates && location != null) {
                val accuracy = WatermarkFormatter.formatAccuracy(location.accuracyMeters)
                val value = listOfNotNull(WatermarkFormatter.formatCoordinates(location), accuracy).joinToString(" ")
                add(WatermarkLine("坐标", value, icon = "🌐"))
            }
            if (config.showAltitude) {
                WatermarkFormatter.formatAltitude(location?.altitudeMeters)?.let { add(WatermarkLine("海拔", it, icon = "🏔️")) }
            }
            if (config.showHeading) {
                metadata.heading?.let { add(WatermarkLine("方位", BearingFormatter.display(it), icon = "🧭")) }
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
    fun bottomCard(
        imageWidth: Int,
        imageHeight: Int,
        lineCount: Int,
        hasLogo: Boolean,
        fontScale: Float = 1.0f,
        logoPosition: LogoPosition = LogoPosition.Above
    ): RectSpec {
        val safeWidth = imageWidth.coerceAtLeast(1)
        val safeHeight = imageHeight.coerceAtLeast(1)
        val baseScale = (safeWidth / 1080f) * fontScale

        val padding = (18f * baseScale).roundToInt().coerceAtLeast(10)
        val titleHeight = (40f * baseScale).roundToInt().coerceAtLeast(20)
        val bigTimeHeight = (80f * baseScale).roundToInt().coerceAtLeast(36)
        val lineHeight = (32f * baseScale).roundToInt().coerceAtLeast(16)

        val logoExtra = if (hasLogo) {
            when (logoPosition) {
                LogoPosition.Above, LogoPosition.Below -> (130f * baseScale * 0.8f).roundToInt()
                LogoPosition.Left -> (20f * baseScale).roundToInt()
            }
        } else 0

        val desiredHeight = padding * 2 + titleHeight + bigTimeHeight + lineHeight * lineCount.coerceAtLeast(1) + logoExtra
        val maxHeight = (safeHeight * 0.55f).roundToInt().coerceAtLeast(1)
        val height = desiredHeight.coerceAtMost(maxHeight).coerceAtLeast(1)
        val margin = (safeWidth * 0.03f).roundToInt().coerceAtLeast(12)
        val width = (safeWidth - margin * 2).coerceAtLeast(1)
        val top = (safeHeight - height - margin).coerceAtLeast(0)
        return RectSpec(margin, top, width, height)
    }
}
