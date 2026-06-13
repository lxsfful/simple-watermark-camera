package com.lx.simplewatermarkcamera.feature.preview

import android.content.Intent
import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lx.simplewatermarkcamera.data.SavedPhotoResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPreviewScreen(
    result: SavedPhotoResult,
    currentTitle: String,
    onRetake: () -> Unit,
    onSaveTitle: (newTitle: String) -> Unit,
) {
    val context = LocalContext.current
    var titleInput by remember(currentTitle) { mutableStateOf(currentTitle) }
    var showTitleEdit by remember { mutableStateOf(false) }
    var titleSaved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预览") },
                navigationIcon = {
                    IconButton(onClick = onRetake) {
                        Icon(Icons.Default.Close, contentDescription = "重拍")
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    // 重拍
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = onRetake) {
                            Icon(Icons.Default.Close, contentDescription = "重拍")
                        }
                        Text("重拍", style = MaterialTheme.typography.labelSmall)
                    }

                    // 修改标题（下次拍照生效）
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = {
                            showTitleEdit = !showTitleEdit
                            if (showTitleEdit) titleSaved = false
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "修改标题")
                        }
                        Text("改标题", style = MaterialTheme.typography.labelSmall)
                    }

                    // 分享
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/jpeg"
                                putExtra(Intent.EXTRA_STREAM, result.watermarkedUri)
                                clipData = ClipData.newRawUri("", result.watermarkedUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "分享照片"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "分享")
                        }
                        Text("分享", style = MaterialTheme.typography.labelSmall)
                    }

                    // 打开相册
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = {
                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(result.watermarkedUri, "image/jpeg")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(viewIntent)
                        }) {
                            Icon(Icons.Default.Photo, contentDescription = "打开相册")
                        }
                        Text("相册", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = result.watermarkedUri,
                    contentDescription = "水印照片预览",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }

            if (showTitleEdit) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("新标题") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            showTitleEdit = false
                            titleSaved = true
                            onSaveTitle(titleInput)
                        },
                    ) { Text(if (titleSaved) "已保存" else "保存") }
                }
            }
        }
    }
}
