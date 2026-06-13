package com.lx.simplewatermarkcamera.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lx.simplewatermarkcamera.domain.WatermarkConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "watermark_settings")

class SettingsRepository(private val context: Context) {
    val configFlow: Flow<WatermarkConfig> = context.settingsDataStore.data.map { preferences ->
        WatermarkConfig(
            title = preferences[Keys.title] ?: WatermarkConfig.Default.title,
            showLogo = preferences[Keys.showLogo] ?: WatermarkConfig.Default.showLogo,
            showTime = preferences[Keys.showTime] ?: WatermarkConfig.Default.showTime,
            showAddress = preferences[Keys.showAddress] ?: WatermarkConfig.Default.showAddress,
            showWeather = preferences[Keys.showWeather] ?: WatermarkConfig.Default.showWeather,
            showCoordinates = preferences[Keys.showCoordinates] ?: WatermarkConfig.Default.showCoordinates,
            showAltitude = preferences[Keys.showAltitude] ?: WatermarkConfig.Default.showAltitude,
            showHeading = preferences[Keys.showHeading] ?: WatermarkConfig.Default.showHeading,
            saveOriginal = preferences[Keys.saveOriginal] ?: WatermarkConfig.Default.saveOriginal,
            writeGpsExif = preferences[Keys.writeGpsExif] ?: WatermarkConfig.Default.writeGpsExif,
            logoPath = preferences[Keys.logoPath],
        )
    }

    suspend fun update(transform: (WatermarkConfig) -> WatermarkConfig) {
        context.settingsDataStore.edit { preferences ->
            val current = WatermarkConfig(
                title = preferences[Keys.title] ?: WatermarkConfig.Default.title,
                showLogo = preferences[Keys.showLogo] ?: WatermarkConfig.Default.showLogo,
                showTime = preferences[Keys.showTime] ?: WatermarkConfig.Default.showTime,
                showAddress = preferences[Keys.showAddress] ?: WatermarkConfig.Default.showAddress,
                showWeather = preferences[Keys.showWeather] ?: WatermarkConfig.Default.showWeather,
                showCoordinates = preferences[Keys.showCoordinates] ?: WatermarkConfig.Default.showCoordinates,
                showAltitude = preferences[Keys.showAltitude] ?: WatermarkConfig.Default.showAltitude,
                showHeading = preferences[Keys.showHeading] ?: WatermarkConfig.Default.showHeading,
                saveOriginal = preferences[Keys.saveOriginal] ?: WatermarkConfig.Default.saveOriginal,
                writeGpsExif = preferences[Keys.writeGpsExif] ?: WatermarkConfig.Default.writeGpsExif,
                logoPath = preferences[Keys.logoPath],
            )
            val next = transform(current)
            preferences[Keys.title] = next.title
            preferences[Keys.showLogo] = next.showLogo
            preferences[Keys.showTime] = next.showTime
            preferences[Keys.showAddress] = next.showAddress
            preferences[Keys.showWeather] = next.showWeather
            preferences[Keys.showCoordinates] = next.showCoordinates
            preferences[Keys.showAltitude] = next.showAltitude
            preferences[Keys.showHeading] = next.showHeading
            preferences[Keys.saveOriginal] = next.saveOriginal
            preferences[Keys.writeGpsExif] = next.writeGpsExif
            next.logoPath?.let { preferences[Keys.logoPath] = it } ?: preferences.remove(Keys.logoPath)
        }
    }

    private object Keys {
        val title = stringPreferencesKey("title")
        val showLogo = booleanPreferencesKey("show_logo")
        val showTime = booleanPreferencesKey("show_time")
        val showAddress = booleanPreferencesKey("show_address")
        val showWeather = booleanPreferencesKey("show_weather")
        val showCoordinates = booleanPreferencesKey("show_coordinates")
        val showAltitude = booleanPreferencesKey("show_altitude")
        val showHeading = booleanPreferencesKey("show_heading")
        val saveOriginal = booleanPreferencesKey("save_original")
        val writeGpsExif = booleanPreferencesKey("write_gps_exif")
        val logoPath = stringPreferencesKey("logo_path")
    }
}
