package com.lx.simplewatermarkcamera.feature.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.lx.simplewatermarkcamera.domain.WatermarkConfig

private data class WatermarkTemplate(val name: String, val config: WatermarkConfig)

private val builtInTemplates = listOf(
    WatermarkTemplate(
        "工程巡检",
        WatermarkConfig(
            title = "工程巡检",
            showTime = true,
            showCoordinates = true,
            showAltitude = false,
            showHeading = true,
            showAddress = false,
            showWeather = false,
            showLogo = true,
        ),
    ),
    WatermarkTemplate(
        "室外勘察",
        WatermarkConfig(
            title = "室外勘察",
            showTime = true,
            showCoordinates = true,
            showAltitude = true,
            showHeading = true,
            showAddress = true,
            showWeather = true,
            showLogo = true,
        ),
    ),
    WatermarkTemplate(
        "仅时间",
        WatermarkConfig(
            title = "",
            showTime = true,
            showCoordinates = false,
            showAltitude = false,
            showHeading = false,
            showAddress = false,
            showWeather = false,
            showLogo = false,
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    config: WatermarkConfig,
    onBack: () -> Unit,
    onUpdateConfig: ((WatermarkConfig) -> WatermarkConfig) -> Unit,
    onLogoPicked: (Context, Uri) -> Unit,
) {
    val context = LocalContext.current
    var title by remember(config.title) { mutableStateOf(config.title) }
    val pickLogo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onLogoPicked(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("水印设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 快速模板
            Text("快速模板", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                builtInTemplates.forEach { template ->
                    OutlinedButton(
                        onClick = {
                            val logoPath = config.logoPath
                            onUpdateConfig { template.config.copy(logoPath = logoPath) }
                            title = template.config.title
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(template.name, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            HorizontalDivider()

            // 标题
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = title,
                onValueChange = {
                    title = it
                    onUpdateConfig { current -> current.copy(title = it) }
                },
                label = { Text("自定义标题") },
                singleLine = true,
            )

            // Logo
            Button(onClick = {
                pickLogo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Icon(Icons.Default.Image, contentDescription = null)
                Text("选择 Logo", modifier = Modifier.padding(start = 8.dp))
            }

            HorizontalDivider()

            Text("水印字段", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            SettingSwitch("显示 Logo", config.showLogo) { v -> onUpdateConfig { it.copy(showLogo = v) } }
            SettingSwitch("显示时间", config.showTime) { v -> onUpdateConfig { it.copy(showTime = v) } }
            SettingSwitch("显示方位", config.showHeading) { v -> onUpdateConfig { it.copy(showHeading = v) } }
            SettingSwitch("显示经纬度", config.showCoordinates) { v -> onUpdateConfig { it.copy(showCoordinates = v) } }
            SettingSwitch("显示海拔", config.showAltitude) { v -> onUpdateConfig { it.copy(showAltitude = v) } }
            SettingSwitch("显示地点", config.showAddress) { v -> onUpdateConfig { it.copy(showAddress = v) } }
            SettingSwitch("显示天气", config.showWeather) { v -> onUpdateConfig { it.copy(showWeather = v) } }

            HorizontalDivider()

            Text("存储", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            SettingSwitch("同时保存无水印原图", config.saveOriginal) { v -> onUpdateConfig { it.copy(saveOriginal = v) } }

            Spacer(Modifier.height(4.dp))
            Text(
                "说明：照片全程本机处理。天气开启后仅在拍照时用约 100 米精度坐标请求 Open-Meteo，网络失败自动跳过。",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB7C8BF),
            )
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
