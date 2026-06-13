package com.lx.simplewatermarkcamera.feature.camera

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lx.simplewatermarkcamera.core.OperationResult
import com.lx.simplewatermarkcamera.core.runOperation
import com.lx.simplewatermarkcamera.data.LocationRepository
import com.lx.simplewatermarkcamera.data.LogoStore
import com.lx.simplewatermarkcamera.data.PhotoStorageRepository
import com.lx.simplewatermarkcamera.data.SavedPhotoResult
import com.lx.simplewatermarkcamera.data.SettingsRepository
import com.lx.simplewatermarkcamera.data.WatermarkRenderer
import com.lx.simplewatermarkcamera.data.WeatherRepository
import com.lx.simplewatermarkcamera.domain.CaptureMetadata
import com.lx.simplewatermarkcamera.domain.HeadingSnapshot
import com.lx.simplewatermarkcamera.domain.PhotoFileNameGenerator
import com.lx.simplewatermarkcamera.domain.WatermarkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant

data class CameraUiState(
    val config: WatermarkConfig = WatermarkConfig.Default,
    val latestHeading: HeadingSnapshot? = null,
    val flashMode: Int = ImageCapture.FLASH_MODE_AUTO,
    val isCapturing: Boolean = false,
    val status: String = "",
    val lastResult: SavedPhotoResult? = null,
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val storageRepository = PhotoStorageRepository(application)
    private val locationRepository = LocationRepository(application)
    private val weatherRepository = WeatherRepository()
    private val renderer = WatermarkRenderer(application)
    private val mutable = MutableStateFlow(CameraUiState())

    val uiState: StateFlow<CameraUiState> = combine(
        settingsRepository.configFlow,
        mutable,
    ) { config, state -> state.copy(config = config) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CameraUiState())

    fun createTempCaptureFile(): File = storageRepository.createTempCaptureFile()

    fun updateHeading(heading: HeadingSnapshot) {
        mutable.update { it.copy(latestHeading = heading) }
    }

    fun setCapturing(isCapturing: Boolean) {
        mutable.update { it.copy(isCapturing = isCapturing) }
    }

    fun cycleFlash() {
        mutable.update {
            val next = when (it.flashMode) {
                ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                else -> ImageCapture.FLASH_MODE_AUTO
            }
            it.copy(flashMode = next)
        }
    }

    fun onCaptureError(message: String) {
        mutable.update { it.copy(isCapturing = false, status = message) }
    }

    fun clearStatus() {
        mutable.update { it.copy(status = "") }
    }

    fun updateConfig(transform: (WatermarkConfig) -> WatermarkConfig) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun saveLogo(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runOperation("Logo 保存失败") { LogoStore.copyToPrivateStorage(context, uri) }
            when (result) {
                is OperationResult.Success ->
                    settingsRepository.update { it.copy(logoPath = result.value, showLogo = true) }
                is OperationResult.Failure ->
                    mutable.update { it.copy(status = result.message) }
            }
        }
    }

    fun processCapturedFile(source: File) {
        viewModelScope.launch {
            mutable.update { it.copy(isCapturing = true, status = "正在生成水印…") }
            // 拍照瞬间快照，避免 config 和 heading 来自不同时间点
            val snapshot = uiState.value
            val config = snapshot.config
            val heading = snapshot.latestHeading
            val now = Instant.now()
            val result = withContext(Dispatchers.IO) {
                runOperation("照片处理失败") {
                    val needsLocation = config.showAddress || config.showWeather ||
                        config.showCoordinates || config.showAltitude
                    val location = if (needsLocation) {
                        locationRepository.currentOrLastKnownLocation(resolveAddress = config.showAddress)
                    } else null
                    val weather = if (config.showWeather) weatherRepository.currentWeather(location) else null
                    val metadata = CaptureMetadata(
                        capturedAt = now,
                        location = location,
                        weather = weather,
                        heading = heading,
                    )
                    val watermarkedBytes = renderer.renderJpeg(source, config, metadata)
                    val watermarkedUri = storageRepository.saveJpeg(
                        bytes = watermarkedBytes,
                        fileName = PhotoFileNameGenerator.generate(now, "WM"),
                        subFolder = "Watermarked",
                    )
                    val originalUri = if (config.saveOriginal) {
                        storageRepository.saveJpeg(
                            bytes = source.readBytes(),
                            fileName = PhotoFileNameGenerator.generate(now, "ORIGINAL"),
                            subFolder = "Original",
                        )
                    } else null
                    source.delete()
                    SavedPhotoResult(watermarkedUri, originalUri)
                }
            }
            when (result) {
                is OperationResult.Success -> {
                    val originalText = if (result.value.originalUri != null) "，原图已保存" else ""
                    mutable.update {
                        it.copy(
                            isCapturing = false,
                            status = "水印照片已保存$originalText",
                            lastResult = result.value,
                        )
                    }
                }
                is OperationResult.Failure -> {
                    source.delete()
                    mutable.update { it.copy(isCapturing = false, status = result.message) }
                }
            }
        }
    }
}
