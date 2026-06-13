package com.lx.simplewatermarkcamera.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.lx.simplewatermarkcamera.domain.LocationSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.util.Locale
import kotlin.coroutines.resume

class LocationRepository(private val context: Context) {
    suspend fun currentOrLastKnownLocation(resolveAddress: Boolean): LocationSnapshot? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) return@withContext null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val current = withTimeoutOrNull(2_500) { requestCurrentLocation(manager) }
        val latest = current ?: lastKnown(manager)
        latest?.toSnapshot(if (resolveAddress) resolveAddress(latest) else null)
    }

    private fun hasLocationPermission(): Boolean = hasFineLocationPermission() || hasCoarseLocationPermission()

    private fun hasFineLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun hasCoarseLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private suspend fun requestCurrentLocation(manager: LocationManager): Location? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val providers = buildList {
            if (hasFineLocationPermission()) add(LocationManager.GPS_PROVIDER)
            if (hasCoarseLocationPermission() || hasFineLocationPermission()) add(LocationManager.NETWORK_PROVIDER)
        }.filter { provider -> runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false) }
        for (provider in providers) {
            val location = runCatching {
                suspendCancellableCoroutine { continuation ->
                    manager.getCurrentLocation(provider, null, context.mainExecutor) { result ->
                        if (continuation.isActive) continuation.resume(result)
                    }
                }
            }.getOrNull()
            if (location != null) return location
        }
        return null
    }

    private fun lastKnown(manager: LocationManager): Location? {
        val newestAllowedAgeMillis = 5 * 60 * 1000L
        val now = System.currentTimeMillis()
        return buildList {
            if (hasFineLocationPermission()) add(LocationManager.GPS_PROVIDER)
            if (hasCoarseLocationPermission() || hasFineLocationPermission()) add(LocationManager.NETWORK_PROVIDER)
        }
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .filter { location -> location.time <= 0L || now - location.time <= newestAllowedAgeMillis }
            .maxByOrNull { it.time }
    }

    private fun resolveAddress(location: Location): String? = runCatching {
        if (!Geocoder.isPresent()) return@runCatching null
        @Suppress("DEPRECATION")
        Geocoder(context, Locale.CHINA).getFromLocation(location.latitude, location.longitude, 1)
            ?.firstOrNull()
            ?.let { address ->
                listOfNotNull(address.adminArea, address.locality, address.subLocality, address.thoroughfare)
                    .distinct()
                    .joinToString("")
                    .ifBlank { null }
            }
    }.getOrNull()

    private fun Location.toSnapshot(address: String?): LocationSnapshot = LocationSnapshot(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
        altitudeMeters = if (hasAltitude()) altitude else null,
        address = address,
        capturedAt = Instant.ofEpochMilli(time.takeIf { it > 0L } ?: System.currentTimeMillis()),
    )
}
