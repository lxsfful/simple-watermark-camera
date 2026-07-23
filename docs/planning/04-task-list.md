# 实施任务清单

## Phase 0：项目骨架与技术验证

- [x] 新建 Android Kotlin + Compose 项目
- [x] 配置包名：`com.lx.simplewatermarkcamera`
- [x] 配置 CameraX、Compose、DataStore、Coroutines、测试依赖
- [x] 建立 package 边界：feature/domain/data/core
- [x] 技术验证：CameraX 预览
- [x] 技术验证：拍照到临时文件
- [x] 技术验证：Canvas 给测试图片绘制标题 + 时间水印
- [x] 技术验证：MediaStore 保存图片

验收：真机可打开预览，可拍一张普通照片，可保存到相册。✅

## Phase 1：MVP 主链路：拍照 + 标题时间水印

- [x] 相机权限请求与拒绝处理
- [x] CameraScreen：预览、拍照按钮、设置入口
- [x] CameraViewModel：拍照状态、保存状态、错误消息
- [x] `WatermarkData` / `WatermarkConfig` 模型
- [x] `WatermarkFormatter`：标题、时间格式
- [x] `WatermarkLayoutCalculator`：底部水印布局
- [x] `WatermarkRenderer`：绘制标题、时间
- [x] `PhotoStorageRepository`：保存水印图到 MediaStore
- [x] SettingsScreen：默认标题
- [x] DataStore 保存默认标题

验收：可生成"标题 + 时间"水印图并保存到相册。✅

## Phase 2：现场信息：经纬度、海拔、方位

- [x] 定位权限请求：可选，不阻塞拍照
- [x] `LocationRepository`：最近位置、当前位置、超时策略
- [x] 水印增加经纬度
- [x] 水印增加海拔
- [x] CameraScreen 显示定位状态（定位按钮）
- [x] `HeadingRepository`：监听方位传感器
- [x] `BearingFormatter`：角度转中文方向
- [x] 拍照瞬间冻结定位和方位快照
- [x] 水印增加方位

验收：定位成功时水印显示经纬度/海拔；方位显示中文方向 + 角度；定位失败仍可拍照。✅

## Phase 3：设置增强：logo、字段开关、保存原图

- [x] 使用系统 Photo Picker 选择 logo
- [x] 将 logo 复制到 App 私有目录或持久化 Uri 权限
- [x] 水印绘制 logo
- [x] SettingsScreen 增加字段开关
- [x] DataStore 保存字段开关
- [x] 保存原图开关（默认开启）
- [x] 开启时保存原图到 `Pictures/水印相机/Original/`
- [x] 水印图保存到 `Pictures/水印相机/Watermarked/`
- [x] EXIF GPS 写入（基于 `ExifInterface` 写入标准 DMS 格式）

验收：可选择 logo，可开关字段，默认同时保存带水印图与无水印原图。✅

## Phase 4：天气与地址

- [x] `WeatherRepository`：Open-Meteo，15 秒超时，失败返回 null
- [x] 天气数据模型（WeatherSnapshot）
- [x] 请求超时与失败降级
- [x] SettingsScreen 增加天气开关
- [x] 水印增加天气字段
- [x] 天气 TTL 缓存（10 分钟同粗坐标复用）
- [x] 详细地址解析（使用 `getAddressLine(0)` 并剥离“中国”前缀）

验收：网络可用时可显示天气/详细地址；失败时不影响拍照。✅

## Phase 5：体验、稳定性与视觉重构

- [x] 闪光灯开关（关/自动/常亮三档，相机页顶栏）
- [x] 拍照后预览页（PhotoPreviewScreen），明确提示相册已自动保存
- [x] 支持重拍（返回相机重新拍）
- [x] 支持分享（系统分享 Intent）
- [x] 支持打开相册
- [x] 点击对焦 + 白色圆圈指示器
- [x] 大图内存优化（`inMutable = true`，矩阵旋转后立即 recycle）
- [x] 缓存回收（`PhotoStorageRepository.cleanupOldTempFiles` 自动清理 >24h 临时文件）
- [x] 导航增强（`MainActivity` 接入 `BackHandler` 物理返回）
- [x] 应用更名为“水印相机”，自动生成“印”字桌面图标
- [x] 开启 CameraX 硬件最高质量拍摄模式 (`CAPTURE_MODE_MAXIMIZE_QUALITY`)

## 2026-07-23 马克相机排版与全能定制重构

### 视觉与马克排版
- [x] **去除半透明黑框底板**：改为纯文字 + 4px 黑色描边阴影叠加在照片上。
- [x] **大字时间 + 双行日期**：实现 `17:22 | 星期二 / 2026-07-23` 超大字排版（占短边 6.7%）。
- [x] **紫色标题 Tag**：标题呈现小巧精致的 `#7A36D6` 紫色圆角 Tag。
- [x] **字段前缀 Icon**：增加 📍地点、☀️天气、🌐坐标、🏔️海拔、🧭方位。
- [x] **短边自适应百分比**：使用 `min(width, height)` 短边计算 `scale`，彻底解决横屏 (4000x3000) 拍照水印截断 Bug。

### Logo 与定制选项
- [x] **Logo 保持比例**：基于 `aspectRatio` 等比缩放，彻底解决拉伸变形 Bug。
- [x] **内置京湖集团透明 Logo**：处理并集成 `ic_jinghu_logo.png`。
- [x] **Logo 3 种位置排版**：支持文字上方 (Above) / 文字左侧 (Left) / 文字下方 (Below)。
- [x] **Logo 6 种染色调色盘**：原色 / 纯白 / 京湖蓝 (`#0072C6`) / 亮金 (`#FFD700`) / 品牌紫 (`#7A36D6`) / 纯黑 (`#000000`)。
- [x] **字号与透明度调节**：三档字号（小 0.5x / 标准 1.0x 马克标准 / 大 1.5x），四档透明度（100% / 85% / 70% / 50%）。
- [x] **过滤 0.0m 无效海拔**：室内 2D 定位阶段 0.0m 海拔自动隐藏，不显示 `0.0 m`。
- [x] **相册广播强制刷新**：`MediaScannerConnection.scanFile` 确保华为等系统相册秒级刷新图库。

验收：全套 21 项单元测试与编译测试全部通过，真机横竖屏拍摄水印完整完美。✅
