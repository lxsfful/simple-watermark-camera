# 水印相机

> 自用、无广告、无弹窗、本地优先的 Android 水印相机。

## 项目目标

解决市面水印相机收费、广告多、弹窗多、隐私不透明的问题。做一个稳定高画质的 Android 本地水印相机：拍照后自动生成带水印照片，并默认同步保留无水印高像素原图。

## 核心功能

- **硬件最大分辨率拍摄**：配置 CameraX 最高画质捕获模式，保持传感器原汁原味细节
- **马克相机排版**：大字时间（`17:22 | 星期二 / 2026-07-23`）、紫色标题 Tag、字段图标（📍、☀️、🌐、🏔️、🧭）
- **短边自适应比例**：以图片短边计算百分比，无论横屏（4000x3000）还是竖屏（3000x4000）均无遮挡、不截断
- **三档字号与透明度**：字号三档调节（0.5x 小号 / 1.0x 标准 / 1.5x 大号），透明度四档调节（100% / 85% / 70% / 50%）
- **内置企业 Logo & 自由定制**：内置京湖集团高清透明 Logo，支持关闭 / 内置 Logo / 个人 Logo 切换；支持 3 种位置排版（上方/左侧/下方）与 6 种指定颜色染色（原色/纯白/京湖蓝/亮金/品牌紫/纯黑）
- **高精度现场数据**：支持详细门牌/街道地址解析（自动清洗中国前缀）、Open-Meteo 天气与 TTL 缓存、0.0m 无效海拔自动过滤、方位角传感器防抖
- **EXIF GPS 元数据写入**：可选将经纬度与海拔直接写入保存的 JPEG EXIF Header
- **默认双版本保存与相册秒级刷新**：默认同时保存“带水印版”与“无水印原图”，广播通知系统相册实时刷新
- **隐私与本地优先**：照片 100% 本地处理，不上传图片，无广告、无会员、无弹窗

## 技术路线

- Android Native Kotlin + Jetpack Compose
- CameraX (`CAPTURE_MODE_MAXIMIZE_QUALITY`)
- MediaStore (`Pictures/水印相机/Watermarked/` & `Original/`) + `MediaScannerConnection`
- Android Bitmap + Canvas 渲染
- Jetpack DataStore Preferences 保存配置
- `ExifInterface` 写入 GPS 元数据
- Android LocationManager + Geocoder / SensorManager / Open-Meteo 天气

## 规划文档

- [开源调研](docs/planning/01-open-source-research.md)
- [PRD 产品需求](docs/planning/02-prd.md)
- [技术方案](docs/planning/03-technical-plan.md)
- [实施任务清单](docs/planning/04-task-list.md)
- [风险与决策](docs/planning/05-risk-and-decisions.md)
