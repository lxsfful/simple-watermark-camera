# 技术方案：简易水印相机

## 1. 推荐技术栈

- Kotlin
- Android Native
- Jetpack Compose
- CameraX
- Kotlin Coroutines / Flow / StateFlow
- ViewModel
- DataStore Preferences
- MediaStore
- Android Bitmap / Canvas / Paint
- AndroidX ExifInterface
- SensorManager
- Fused Location Provider + LocationManager fallback
- OkHttp/Retrofit 或 Ktor Client（天气阶段再引入）

## 2. 总体架构

MVP 建议单 Gradle module，使用清晰 package 边界。后续变大后再拆模块。

```text
app/
├── app/                    # MainActivity、依赖组装
├── feature/camera/          # 相机页、相机 ViewModel
├── feature/settings/        # 设置页
├── feature/template/        # 水印模板设置，后续增强
├── domain/
│   ├── model/               # WatermarkData、LocationSnapshot 等
│   ├── repository/          # 仓储接口
│   └── usecase/             # 拍照、构建水印、保存等用例
├── data/
│   ├── camera/              # CameraX 适配
│   ├── location/            # 定位
│   ├── sensor/              # 方位
│   ├── storage/             # MediaStore
│   ├── watermark/           # Canvas 渲染
│   ├── settings/            # DataStore
│   └── weather/             # 天气，后置
├── core/
│   ├── permission/
│   ├── time/
│   ├── error/
│   └── dispatchers/
└── design/                  # Compose 组件与主题
```

## 3. 核心数据流

```text
用户点击拍照
→ CameraX 拍摄原图到 app cache 临时文件
→ 读取当前设置
→ 读取拍摄时刻元数据：时间、最近定位、海拔、方位、天气缓存
→ BuildWatermarkDataUseCase 组合水印文本
→ AndroidWatermarkRenderer 读取原图、修正 EXIF、绘制水印
→ MediaStore 保存水印图
→ 如果开启保存原图，将原图保存到 Original 目录
→ UI 显示保存成功/失败
```

关键原则：天气和地址不能阻塞拍照。定位失败也不能阻塞拍照。

## 4. CameraX 方案

使用：

- `Preview`
- `ImageCapture`
- `PreviewView`
- `CameraSelector`
- `ProcessCameraProvider` 或 `LifecycleCameraController`

Compose 页面中通过 `AndroidView` 嵌入 `PreviewView`。

MVP 推荐拍照输出到临时文件，然后再读文件绘制水印。原因：

- 原图保存更清晰
- 避免 ImageProxy/YUV 转 Bitmap 的复杂度
- 更容易处理原图和水印图双保存

## 5. 水印渲染

使用 Bitmap + Canvas 本地绘制。

组件：

- `WatermarkFormatter`：格式化时间、经纬度、海拔、方位、天气
- `WatermarkLayoutEngine`：根据图片尺寸计算水印位置、字号、行高
- `AndroidWatermarkRenderer`：Canvas 绘制文字、logo、半透明背景
- `LogoBitmapLoader`：加载并缩放 logo

MVP 样式：

- 底部半透明深色横条或卡片
- 左侧 logo，右侧多行文字
- 中文白字，有阴影或背景保证可读性
- 字号按图片宽度比例计算，不能写死像素

必须处理：

- EXIF orientation
- 横图/竖图
- 大图内存占用
- logo 加载失败降级
- 字段缺失时自动隐藏该行

## 6. 存储方案

Android 10+ 使用 MediaStore scoped storage。

推荐目录：

```text
Pictures/简易水印相机/Watermarked/
Pictures/简易水印相机/Original/
```

推荐文件名：

```text
IMG_yyyyMMdd_HHmmss_SSS_WM.jpg
IMG_yyyyMMdd_HHmmss_SSS_ORIGINAL.jpg
```

保存流程要处理：

- `IS_PENDING` 写入
- 写入失败清理未完成项
- 文件名冲突
- MIME type：`image/jpeg`
- 是否写 GPS EXIF：默认关闭

## 7. 定位与海拔

接口：

```text
LocationRepository
- getLastKnownLocation()
- requestCurrentLocation(timeout)
- observeLocation()
```

策略：

- 相机页打开时提前刷新定位
- 拍照时使用最近有效位置
- 如果位置过旧，最多短暂等待 2 秒
- 无定位时仍可拍照，水印隐藏定位字段或显示未获取

海拔优先使用 Location altitude。没有 altitude 就隐藏或显示“海拔未获取”。

## 8. 方位

优先使用 `TYPE_ROTATION_VECTOR`，fallback 为加速度计 + 磁力计。

水印显示：

```text
方位：东北 45°
```

注意：方位是设备朝向，不是行进方向。磁场干扰时应显示低精度或允许隐藏。

## 9. 天气与地址

天气和地址后置，不进入 MVP 主链路。

天气推荐策略：

- 可关闭
- 可缓存
- 请求短超时
- 网络失败使用缓存或隐藏字段
- API Key 不硬编码

候选服务：

- Open-Meteo：免费、无需 key，适合 MVP
- 和风天气/高德/彩云：国内体验更好，但需要 key

地址反查：

- MVP 不做详细地址，只显示经纬度
- 后续可接 Android Geocoder 或国内地图 API

## 10. 权限与隐私

权限：

- `CAMERA`：必需
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`：可选
- 不申请后台定位
- 选择 logo 优先用系统 Photo Picker，尽量不申请读取相册权限

隐私默认：

- 照片本地处理
- 不上传照片
- 天气仅发送经纬度，且可关闭
- GPS EXIF 默认关闭
- 定位权限拒绝后仍可拍照

## 11. 测试策略

单元测试：

- WatermarkFormatter
- BearingFormatter
- WatermarkLayoutEngine
- BuildWatermarkDataUseCase
- 文件名生成
- 天气缓存策略

Instrumentation：

- MediaStore 保存
- Exif 写入/清理
- DataStore 读写
- AndroidWatermarkRenderer 输出基本验证

真机测试：

- 连续拍照 50 张
- 横竖图
- 定位允许/拒绝/GPS 关闭
- 无网络
- Android 10/12/13/14+

## 12. 架构决策

- 使用 CameraX，而非 Camera2：开发效率和设备兼容性更好。
- 使用 Native Kotlin，而非 Flutter/RN：相机、传感器、MediaStore、EXIF 控制更强。
- 使用 Canvas 本地渲染，而非上传服务端：隐私好、离线可用。
- 使用 MediaStore，而非直接文件路径：符合现代 Android 存储模型。
- 天气后置并可降级：不让外部网络影响核心拍照。
