package com.lx.simplewatermarkcamera.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lx.simplewatermarkcamera.domain.WatermarkConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "watermark_settings")

class SettingsRepository(private val context: Context) {
    val configFlow: Flow<WatermarkConfig> = context.settingsDataStore.data.map { it.toWatermarkConfig() }

    suspend fun update(transform: (WatermarkConfig) -> WatermarkConfig) {
        context.settingsDataStore.edit { preferences ->
            val next = transform(preferences.toWatermarkConfig())
            next.writeInto(preferences)
        }
    }

    private fun Preferences.toWatermarkConfig(): WatermarkConfig = WatermarkConfig(
        title = this[Keys.title] ?: WatermarkConfig.Default.title,
        showLogo = this[Keys.showLogo] ?: WatermarkConfig.Default.showLogo,
        showTime = this[Keys.showTime] ?: WatermarkConfig.Default.showTime,
        showAddress = this[Keys.showAddress] ?: WatermarkConfig.Default.showAddress,
        showWeather = this[Keys.showWeather] ?: WatermarkConfig.Default.showWeather,
        showCoordinates = this[Keys.showCoordinates] ?: WatermarkConfig.Default.showCoordinates,
        showAltitude = this[Keys.showAltitude] ?: WatermarkConfig.Default.showAltitude,
        showHeading = this[Keys.showHeading] ?: WatermarkConfig.Default.showHeading,
        saveOriginal = this[Keys.saveOriginal] ?: WatermarkConfig.Default.saveOriginal,
        writeGpsExif = this[Keys.writeGpsExif] ?: WatermarkConfig.Default.writeGpsExif,
        logoPath = this[Keys.logoPath],
    )

    private fun WatermarkConfig.writeInto(prefs: MutablePreferences) {
        prefs[Keys.title] = title
        prefs[Keys.showLogo] = showLogo
        prefs[Keys.showTime] = showTime
        prefs[Keys.showAddress] = showAddress
        prefs[Keys.showWeather] = showWeather
        prefs[Keys.showCoordinates] = showCoordinates
        prefs[Keys.showAltitude] = showAltitude
        prefs[Keys.showHeading] = showHeading
        prefs[Keys.saveOriginal] = saveOriginal
        prefs[Keys.writeGpsExif] = writeGpsExif
        logoPath?.let { prefs[Keys.logoPath] = it } ?: prefs.remove(Keys.logoPath)
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
