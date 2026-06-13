package com.lx.simplewatermarkcamera.data

import com.lx.simplewatermarkcamera.domain.LocationSnapshot
import com.lx.simplewatermarkcamera.domain.WeatherSnapshot
import com.lx.simplewatermarkcamera.domain.WeatherSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.Locale
import java.util.concurrent.CancellationException

class WeatherRepository {
    suspend fun currentWeather(location: LocationSnapshot?): WeatherSnapshot? = withContext(Dispatchers.IO) {
        if (location == null) return@withContext null
        var connection: HttpURLConnection? = null
        try {
            val latitude = "%.3f".format(Locale.US, location.latitude)
            val longitude = "%.3f".format(Locale.US, location.longitude)
            val url = URL(
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$latitude" +
                    "&longitude=$longitude" +
                    "&current=temperature_2m,weather_code" +
                    "&timezone=auto"
            )
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 1800
                readTimeout = 1800
                requestMethod = "GET"
                instanceFollowRedirects = false
            }
            if (connection.responseCode !in 200..299) return@withContext null
            connection.inputStream.bufferedReader().use { it.readText() }.let { body ->
                val temperature = Regex("\\\"temperature_2m\\\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").find(body)
                    ?.groupValues
                    ?.get(1)
                    ?.toDoubleOrNull()
                val code = Regex("\\\"weather_code\\\"\\s*:\\s*(\\d+)").find(body)
                    ?.groupValues
                    ?.get(1)
                    ?.toIntOrNull()
                val condition = weatherCodeText(code)
                val text = listOfNotNull(condition, temperature?.let { "%.1f℃".format(Locale.US, it) })
                    .joinToString(" ")
                    .ifBlank { "天气未获取" }
                WeatherSnapshot(text, temperature, Instant.now(), WeatherSource.Network)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun weatherCodeText(code: Int?): String? = when (code) {
        0 -> "晴"
        1, 2 -> "少云"
        3 -> "多云"
        45, 48 -> "雾"
        51, 53, 55 -> "毛毛雨"
        61, 63, 65 -> "雨"
        71, 73, 75 -> "雪"
        80, 81, 82 -> "阵雨"
        95, 96, 99 -> "雷暴"
        else -> code?.let { "天气码 $it" }
    }
}
