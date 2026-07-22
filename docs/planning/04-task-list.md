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
- [x] `WatermarkRenderer`：绘制半透明背景、标题、时间
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
- [x] 保存原图开关
- [x] 开启时保存原图到 `Pictures/简易水印相机/Original/`
- [x] 水印图保存到 `Pictures/简易水印相机/Watermarked/`
- [ ] EXIF GPS 写入开关（DataStore key 已有，渲染器暂未实际写入）

验收：可选择 logo，可开关字段，可选择保存原图。✅（EXIF GPS 写入后置）

## Phase 4：天气与地址（后置）

- [x] `WeatherRepository`：Open-Meteo，1.8 秒超时，失败返回 null
- [x] 天气数据模型（WeatherSnapshot）
- [x] 请求超时与失败降级
- [x] SettingsScreen 增加天气开关
- [x] 水印增加天气字段
- [ ] 天气缓存和 TTL（当前每次拍照都请求）
- [ ] `GeocodingRepository` 接口（当前用 Android Geocoder 直接反查）
- [ ] 地址反查 provider 抽象（可选）

验收：网络可用时可显示天气/地址；失败时不影响拍照。✅

## Phase 5：体验与稳定性

- [x] 闪光灯开关（关/自动/常亮三档，相机页顶栏）
- [x] 拍照后预览页（PhotoPreviewScreen）
- [x] 支持重拍（返回相机重新拍）
- [x] 支持分享（系统分享 Intent）
- [x] 支持打开相册
- [x] 临时修改标题后重新生成水印
- [x] 设置页 2-3 个固定模板快速套用
- [x] 对焦/曝光基础体验（点击对焦）— 2026-07-16 完成
- [ ] 大图内存优化（已有 inSampleSize，持续观察）
- [ ] 连续拍照压力测试（真机验证）
- [ ] Release APK 打包

## 2026-07-16 优化迭代补丁（Session 2）

### 构建
- [x] 首次 `./gradlew assembleDebug` 验证通过
- [x] 单元测试 4 → 20 个，全部通过

### Kotlin-reviewer 审查修复
- [x] **H2** LocationRepository 不再吞 CancellationException
- [x] **H3** HeadingRepository 方位值未变则跳过（去重），消除 ~60Hz 整屏重组
- [ ] **H1** ~~相机预览黑屏（缺 PREVIEW flag）~~ — **审查误报**：CameraX 1.3.4 无 PREVIEW 常量，预览隐式绑定；真机验证原代码预览正常（`docs/camera_preview.png`）
- [x] **M1/M2** SnackbarHost 提升到 app 级 MainActivity
- [x] **M3** saveLogo 改用 application Context
- [x] **M4** SettingsRepository 映射去重（toWatermarkConfig / writeInto）
- [x] **M5** 部分失败清理已插入 MediaStore Uri（防孤儿照片）
- [x] **L1** WatermarkRenderer 去无用 context 参数
- [x] **L3** WeatherRepository 超时 1.8s→15s、允许重定向
- [x] **L4** HeadingRepository 传递真实传感器精度

### 新增
- [x] 天气 TTL 缓存（10 分钟同粗坐标复用，可注入 fetcher 测试）
- [x] 点击对焦 + 白色圆圈指示器
- [x] `WatermarkConfig.needsLocation` DRY 提取
- [x] 新增 `WatermarkFormatterTest`、`WeatherRepositoryTest`
- [x] 真机部署验证（Huawei Mate 30 Pro, Android 10）

### 跳过（保留）
- [x] L2 保留 `@Suppress("DEPRECATION")`（Geocoder 3-arg 在 API 34 仍 deprecated）

验收：拍照后可预览、分享、重拍；闪光灯可控；模板一键套用。

## 2026-07-22 内存渲染优化与 EXIF/导航增强迭代

### 内存与渲染性能
- [x] **WatermarkRenderer 内存优化**：解码可写 Bitmap (`inMutable = true`)，减少原图复制；矩阵旋转后立即 recycle 中间 Bitmap。
- [x] **Paint.breakText 文本截断**：取代逐字循环截断，性能提升至 O(1)。

### 逻辑与功能补全
- [x] **EXIF GPS 元数据写入**：使用 `ExifInterface` 实现标准 DMS 格式经纬度及海拔写入保存的 JPEG。
- [x] **needsLocation 修复**：更新 `WatermarkConfig.needsLocation` 包含 `writeGpsExif`，确保纯 EXIF 写入模式下成功获取定位。
- [x] **缓存文件回收**：`PhotoStorageRepository.cleanupOldTempFiles()` 在 ViewModel 初始化时自动清理超过 24 小时的临时缓存照片。

### 交互与导航
- [x] **BackHandler 接入**：`MainActivity` 设置页与预览页支持系统物理/手势返回键退回相机主页。
- [x] **Compose 预览卡片防抖**：`remember(uiState.config, uiState.latestHeading)` 缓存 `previewContent` 降低高频重绘。

### 测试与质量
- [x] 增加 EXIF DMS 转换测试 `OptimizationAndExifTest`。
- [x] 单元测试全套 21 项通过 (`./gradlew test`)。
- [x] APK 构建通过 (`./gradlew assembleDebug`)。

## 测试任务

### 单元测试（待补充，可交给其他 AI）

- [ ] `BearingFormatter`：0/45/90/135/180/225/270/315/359 度
- [ ] `WatermarkFormatter`：时间、经纬度、海拔、方位、缺失字段
- [ ] `WatermarkLayoutCalculator`：横图/竖图/长标题/无 logo/字段很多
- [ ] 文件名生成：同秒多张不冲突
- [ ] `WatermarkComposer`：字段开关、隐私开关、缺失定位

### Android 测试（待补充）

- [ ] DataStore 默认值与更新
- [ ] MediaStore 保存水印图
- [ ] MediaStore 同时保存原图和水印图
- [ ] EXIF orientation 修正
- [ ] Renderer 输出图片非空且尺寸正确

### 真机验收

- [ ] 首次安装权限流
- [ ] 拒绝定位仍可拍照
- [ ] GPS 关闭仍可拍照
- [ ] 无网络仍可拍照
- [ ] 横屏/竖屏照片水印正确
- [ ] Android 10+ 相册可见
- [ ] 开启保存原图后相册出现两张图
- [ ] 连续拍照 50 张不崩溃
