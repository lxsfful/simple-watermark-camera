package com.lx.simplewatermarkcamera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lx.simplewatermarkcamera.feature.camera.CameraScreen
import com.lx.simplewatermarkcamera.feature.camera.CameraViewModel
import com.lx.simplewatermarkcamera.feature.preview.PhotoPreviewScreen
import com.lx.simplewatermarkcamera.feature.settings.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                WatermarkCameraApp()
            }
        }
    }
}

private enum class Screen { Camera, Settings, Preview }

@Composable
private fun WatermarkCameraApp(viewModel: CameraViewModel = viewModel()) {
    var screen by remember { mutableStateOf(Screen.Camera) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 状态消息 → Snackbar（仅相机页显示）
    LaunchedEffect(uiState.status) {
        if (uiState.status.isNotBlank() && screen == Screen.Camera) {
            snackbarHostState.showSnackbar(uiState.status)
            viewModel.clearStatus()
        }
    }

    // 拍照完成后自动跳转预览
    val lastResult = uiState.lastResult
    LaunchedEffect(lastResult) {
        if (lastResult != null && !uiState.isCapturing) {
            screen = Screen.Preview
        }
    }

    // 防止 Preview 页在 lastResult 为 null 时停留（用 LaunchedEffect 而非 composition 期间突变）
    LaunchedEffect(screen, lastResult) {
        if (screen == Screen.Preview && lastResult == null) {
            screen = Screen.Camera
        }
    }

    when (screen) {
        Screen.Camera -> CameraScreen(
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            onOpenSettings = { screen = Screen.Settings },
            onCaptured = viewModel::processCapturedFile,
            onCreateTempFile = viewModel::createTempCaptureFile,
            onCapturing = viewModel::setCapturing,
            onCaptureError = viewModel::onCaptureError,
            onHeading = viewModel::updateHeading,
            onCycleFlash = viewModel::cycleFlash,
        )
        Screen.Settings -> SettingsScreen(
            config = uiState.config,
            onBack = { screen = Screen.Camera },
            onUpdateConfig = viewModel::updateConfig,
            onLogoPicked = { context, uri -> viewModel.saveLogo(context, uri) },
        )
        Screen.Preview -> {
            val result = uiState.lastResult
            if (result != null) {
                PhotoPreviewScreen(
                    result = result,
                    currentTitle = uiState.config.title,
                    onRetake = { screen = Screen.Camera },
                    onSaveTitle = { newTitle ->
                        viewModel.updateConfig { it.copy(title = newTitle) }
                    },
                )
            }
            // else: LaunchedEffect 上方已处理回退到 Camera
        }
    }
}
