# 开源调研：Android 水印相机 / CameraX / 图片水印

调研日期：2026-06-13

## 调研说明

原计划优先使用 `gh search repos` / `gh search code` 做 GitHub 代码搜索，但当前 GitHub CLI 未登录：`gh auth login` required。因此本次用 Web 搜索 + GitHub API/README 直连补充信息。后续如果需要更完整代码级调研，建议先在会话里执行 `! gh auth login` 登录后再跑 GitHub code search。

## 候选项目对比

| 项目 | 技术栈 | 许可 | 活跃度/规模 | 可复用点 | 风险 |
|---|---|---:|---:|---|---|
| [android/camera-samples](https://github.com/android/camera-samples) | Kotlin, CameraX/Camera2 | Apache-2.0 | ⭐ 5421 / Fork 2420 / 2026-03 push | 官方 CameraXBasic、CameraXAdvanced、CameraXExtensions，适合做相机骨架参考 | 只是相机样例，没有水印业务 |
| [Knightwood/CameraX-Helper](https://github.com/Knightwood/CameraX-Helper) | Kotlin, CameraX | Apache-2.0 | ⭐ 64 / Fork 13 / 2024-09 push | CameraX 封装、拍照/录像、MediaStore/SAF/File 存储、Compose 使用计划 | 功能偏重人脸/MLKit，库较重，文档中文编码抓取显示乱码；建议参考而非整库引入 |
| [arindamxd/android-camerax](https://github.com/arindamxd/android-camerax) | Kotlin, CameraX | Apache-2.0 | ⭐ 131 / Fork 26 / 2025-08 push | 简洁 CameraX Kotlin 示例 | 通用样例，不含水印、定位、存储策略 |
| [akexorcist/CameraX-Sample](https://github.com/akexorcist/CameraX-Sample) | Kotlin, CameraX | Apache-2.0 | ⭐ 12 / Fork 3 / 2020-11 push | 早期 CameraX 示例 | 版本旧，仅适合参考思路 |
| [Shah-Sahab/WatermarkCamera](https://github.com/Shah-Sahab/WatermarkCamera) | 旧 Android 项目 | Apache-2.0 | 0 stars / 2014 push | “拍照 + 自定义水印”方向相近 | 太旧，Camera API/Gradle/存储模型都过时，不建议复用 |
| [CoderMikeHe/MHWatermarkCamera](https://github.com/CoderMikeHe/MHWatermarkCamera) | Objective-C | MIT | ⭐ 7 / 2018 push | 水印相机概念参考 | iOS 项目，不适合 Android 直接复用 |
| [warpthatdot/phimpme-android](https://github.com/warpthatdot/phimpme-android) | Java, Android Photo Editor | GPL-3.0 | 旧项目 / 2019 push | 图片编辑、滤镜、相机、保存流程参考 | GPL-3.0 传染性强；项目大且旧，不建议复制代码 |
| [cammingcai/AndroidCamera](https://github.com/cammingcai/AndroidCamera) | Android 音视频/MediaCodec | 未明确 | 2020 push | 视频水印、滤镜、音视频处理概念 | 与静态照片水印目标不一致，复杂且许可不清 |
| [yunianvh/CameraDemo](https://github.com/yunianvh/CameraDemo) | Kotlin, Camera/Camera2/CameraX/UVC | 未明确 | ⭐ 36 / Fork 13 / 2023 push | Camera API 学习材料 | 许可证不明确，不建议复用代码 |

## 结论

### 不建议直接 fork 现成水印相机

找到的“水印相机”类开源项目普遍存在：

- 年代久远，仍基于旧 Camera API 或旧存储模型
- 与现代 Android scoped storage、Android 13+ 权限模型不匹配
- 水印需求不完整，缺少天气、方位、海拔、保存原图等
- 许可证或维护状态不理想

### 推荐“官方样例 + 自研业务层”

最稳妥路线：

1. 相机主链路参考 `android/camera-samples` 的 CameraXBasic。
2. CameraX 封装与存储可参考 `Knightwood/CameraX-Helper`，但不直接引入重库。
3. 水印业务自研：Bitmap + Canvas，本地渲染。
4. 定位、天气、方位、模板配置按本项目需求单独设计。

## 技术选型建议

推荐使用 Android Native Kotlin，而不是 Flutter / React Native：

- 相机、传感器、MediaStore、EXIF、定位这些能力都是 Android 原生能力，Native 控制更强。
- Flutter/RN 需要插件桥接，相机和传感器细节更容易遇到兼容问题。
- 本项目核心是 Android 手机自用，不需要跨平台收益。

## 可复用模块清单

| 能力 | 复用/参考来源 | 本项目处理 |
|---|---|---|
| CameraX 预览/拍照 | android/camera-samples CameraXBasic | 参考模式，自己实现 |
| MediaStore 保存 | Android 官方样例、CameraX-Helper | 自己封装 StorageRepository |
| 水印绘制 | 旧水印项目、Phimp.me 概念 | 自己写 Canvas renderer |
| EXIF 方向处理 | AndroidX ExifInterface 文档/样例 | 必须纳入核心流程 |
| 位置/经纬度/海拔 | Android Location/Fused Location | 抽象 LocationRepository |
| 方位 | Android SensorManager | 抽象 HeadingRepository |
| 天气 | Open-Meteo / 和风天气等 | 后置、可降级、可关闭 |
