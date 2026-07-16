package com.lx.simplewatermarkcamera.data

import com.lx.simplewatermarkcamera.domain.LocationSnapshot
import com.lx.simplewatermarkcamera.domain.WeatherSnapshot
import com.lx.simplewatermarkcamera.domain.WeatherSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class WeatherRepositoryTest {
    private fun location(lat: Double, lon: Double) = LocationSnapshot(
        latitude = lat,
        longitude = lon,
        capturedAt = Instant.now(),
    )

    private fun snapshot(text: String) = WeatherSnapshot(text, null, Instant.now(), WeatherSource.Network)

    @Test
    fun returnsNullWhenLocationIsNull() = runTest {
        var calls = 0
        val repo = WeatherRepository { calls++; snapshot("晴") }

        assertNull(repo.currentWeather(null))
        assertEquals(0, calls)
    }

    @Test
    fun fetchesFromNetworkOnFirstCall() = runTest {
        var calls = 0
        val repo = WeatherRepository { calls++; snapshot("晴 20.0°") }

        val result = repo.currentWeather(location(39.9, 116.4))

        assertEquals(WeatherSource.Network, result?.source)
        assertEquals(1, calls)
    }

    @Test
    fun returnsCacheWithoutRefetchForSameLocation() = runTest {
        var calls = 0
        val repo = WeatherRepository { calls++; snapshot("晴 20.0°") }

        repo.currentWeather(location(39.9, 116.4))
        val cached = repo.currentWeather(location(39.9, 116.4))

        assertEquals(WeatherSource.Cache, cached?.source)
        assertEquals(1, calls)
    }

    @Test
    fun refetchesForDifferentLocation() = runTest {
        var calls = 0
        val repo = WeatherRepository { calls++; snapshot("晴 20.0°") }

        repo.currentWeather(location(39.9, 116.4))
        repo.currentWeather(location(40.0, 116.5))

        assertEquals(2, calls)
    }
}
