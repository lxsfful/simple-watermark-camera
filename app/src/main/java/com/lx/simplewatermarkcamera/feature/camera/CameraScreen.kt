package com.lx.simplewatermarkcamera.feature.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lx.simplewatermarkcamera.data.HeadingRepository
import com.lx.simplewatermarkcamera.domain.HeadingSnapshot
import com.lx.simplewatermarkcamera.domain.WatermarkConfig
import com.lx.simplewatermarkcamera.domain.WatermarkContent
import java.io.File

@Composable
fun CameraScreen(
    uiState: CameraUiState,
    snackbarHostState: SnackbarHostState,
    onOpenSettings: () -> Unit,
    onCaptured: (File) -> Unit,
    onCreateTempFile: () -> File,
    onCapturing: (Boolean) -> Unit,
    onCaptureError: (String) -> Unit,
    onHeading: (HeadingSnapshot) -> Unit,
    onCycleFlash: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasLocationPermission by remember { mutableStateOf(hasAnyLocationPermission(context)) }

    val requestCameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }
    val requestLocationPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasLocationPermission =
            grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            hasAnyLocationPermission(context)
    }

    val headingRepository = remember { HeadingRepository(context) }
    DisposableEffect(lifecycleOwner, headingRepository) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> headingRepository.start(onHeading)
                Lifecycle.Event.ON_STOP -> headingRepository.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        // 不需要手动判断 isAtLeast(STARTED)：LifecycleEventObserver 注册时会自动补发
        // ON_CREATE/ON_START 等事件（catch-up delivery），避免双重 start 导致传感器重复注册
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            headingRepository.stop()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) requestCameraPermission.launch(Manifest.permission.CAMERA)
    }

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            imageCaptureFlashMode = uiState.flashMode
        }
    }
    LaunchedEffect(uiState.flashMode) {
        cameraController.imageCaptureFlashMode = uiState.flashMode
    }
    DisposableEffect(lifecycleOwner, hasCameraPermission, cameraController) {
        if (hasCameraPermission) cameraController.bindToLifecycle(lifecycleOwner)
        onDispose { cameraController.unbind() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (hasCameraPermission) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            controller = cameraController
                        }
                    },
                )
            } else {
                CameraPermissionPrompt { requestCameraPermission.launch(Manifest.permission.CAMERA) }
            }

            WatermarkPreviewCard(
                content = uiState.previewContent,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 144.dp),
            )

            // 顶栏：标题 + 闪光灯 + 设置
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "简易水印相机",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp),
                )
                Row {
                    IconButton(onClick = onCycleFlash) {
                        Icon(
                            imageVector = when (uiState.flashMode) {
                                ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                ImageCapture.FLASH_MODE_OFF -> Icons.Default.FlashOff
                                else -> Icons.Default.FlashAuto
                            },
                            contentDescription = "闪光灯",
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            }

            // 底部按钮区
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                val needsLocation = uiState.config.showAddress || uiState.config.showWeather ||
                    uiState.config.showCoordinates || uiState.config.showAltitude
                if (needsLocation && !hasLocationPermission) {
                    Button(onClick = {
                        requestLocationPermissions.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                            )
                        )
                    }) { Text("开启定位") }
                }
                Button(
                    enabled = hasCameraPermission && !uiState.isCapturing,
                    onClick = {
                        val outputFile = onCreateTempFile()
                        onCapturing(true)
                        val options = ImageCapture.OutputFileOptions.Builder(outputFile).build()
                        cameraController.takePicture(
                            options,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    onCaptured(outputFile)
                                }
                                override fun onError(exception: ImageCaptureException) {
                                    outputFile.delete()
                                    onCaptureError("拍照失败：${exception.message ?: "相机错误"}")
                                }
                            },
                        )
                    },
                ) {
                    if (uiState.isCapturing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Text("拍照", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WatermarkPreviewCard(content: WatermarkContent, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.72f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (content.logoPath != null) {
                Box(
                    Modifier
                        .size(52.dp)
                        .background(Color(0xFF26A269), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) { Text("LOGO", style = MaterialTheme.typography.labelSmall) }
                Spacer(Modifier.size(12.dp))
            }
            Column {
                Text(content.title, fontWeight = FontWeight.Bold, color = Color.White)
                content.lines.take(6).forEach { line ->
                    Text(line.text, color = Color(0xFFEAF5EF), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionPrompt(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("需要相机权限才能拍照", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text("定位权限是可选的；拒绝定位后仍可拍照，只是不显示地点、经纬度和海拔。")
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRequest) { Text("授予相机权限") }
    }
}

private fun hasAnyLocationPermission(context: android.content.Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

// CameraUiState 扩展：计算预览内容（避免在 UI 层直接依赖 domain 逻辑）
val CameraUiState.previewContent: WatermarkContent
    get() = WatermarkContent(
        title = config.title.trim().ifEmpty { WatermarkConfig.Default.title },
        lines = buildList {
            if (config.showTime) add(com.lx.simplewatermarkcamera.domain.WatermarkLine("时间", "2024-01-01 12:00:00"))
            if (config.showCoordinates) add(com.lx.simplewatermarkcamera.domain.WatermarkLine("坐标", "拍照时自动写入"))
            if (config.showAltitude) add(com.lx.simplewatermarkcamera.domain.WatermarkLine("海拔", "拍照时自动写入"))
            if (config.showHeading) latestHeading?.let {
                add(com.lx.simplewatermarkcamera.domain.WatermarkLine("方位", "${it.cardinalDirection} ${it.azimuthDegrees.toInt()}°"))
            }
            if (config.showWeather) add(com.lx.simplewatermarkcamera.domain.WatermarkLine("天气", "拍照时获取"))
            if (config.showAddress) add(com.lx.simplewatermarkcamera.domain.WatermarkLine("地点", "拍照时获取"))
        },
        logoPath = if (config.showLogo) config.logoPath else null,
    )
