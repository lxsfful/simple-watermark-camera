# 技术方案：水印相机

## 1. 推荐技术栈

- Kotlin + Android Native
- Jetpack Compose
- CameraX (`CAPTURE_MODE_MAXIMIZE_QUALITY`)
- Kotlin Coroutines / Flow / StateFlow / ViewModel
- DataStore Preferences
- MediaStore + `MediaScannerConnection`
- Android Bitmap / Canvas / Paint (`PorterDuffColorFilter` 染色)
- AndroidX ExifInterface (DMS 格式写入)
- SensorManager / LocationManager / Geocoder

## 2. 核心数据流

```text
点击拍照
→ CameraX 最高画质拍摄原图到 cache 临时文件
→ 读取当前设置 (Logo/字号/透明度/位置/保存原图)
→ 读取时刻快照：大字时间、定位、海拔、方位、天气缓存
→ WatermarkComposer 组合带图标 WatermarkLine
→ WatermarkRenderer 基于短边百分比计算 scale 绘制 Canvas
→ 写入 MediaStore (Watermarked + Original)
→ 写入 ExifInterface GPS DMS 属性
→ 触发 MediaScannerConnection 图库即时刷新
```

## 3. 水印渲染与百分比自适应

### 短边比例计算
使用 `shortEdge = min(imageWidth, imageHeight)` 计算 `scale`：
```kotlin
val shortEdge = min(bitmap.width, bitmap.height)
val scale = (shortEdge / 1080f) * fontScale
```
解决横屏 (4000x3000) 下因宽度膨胀导致的字号虚高与 `maxAllowedBottom` 截断问题。

### 百分比比例定义 (1.0x 标准)
- 大字时间 (`17:22`)：`72f * scale` (6.7% 短边比例)
- 标题 Tag (`现场实拍`)：`36f * scale` (3.3% 短边比例)
- 正文 details (地点/天气/坐标)：`28f * scale` (2.6% 短边比例)
- Logo 尺寸：`130f * scale` (12.0% 短边比例)

### 渲染控制
- **Logo 保持比例**：基于 Bitmap `aspectRatio = width / height` 计算 `RectF`，绝不挤压拉伸
- **Logo 染色**：使用 `PorterDuffColorFilter(Color.parseColor(hex), PorterDuff.Mode.SRC_IN)`
- **透明度**：给 `titlePaint`、`linePaint`、`tagBgPaint` 和 `logoPaint` 统一应用 `alphaInt = (255 * alphaFactor)`
- **防溢出安全边距**：所有绘制点施加 `coerceAtMost(bitmap.height - margin)` 限制

## 4. 存储与图库广播

- MediaStore 写入 `Pictures/水印相机/`
- 保存完成后调用 `MediaScannerConnection.scanFile(context, arrayOf(path), arrayOf("image/jpeg"), null)` 强制刷新相册。
