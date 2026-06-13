# 简易水印相机

> 自用、无广告、无弹窗、本地优先的 Android 水印相机。

## 项目目标

解决市面水印相机收费、广告多、弹窗多、隐私不透明的问题。第一版优先做一个稳定可用的 Android 本地相机：拍照后自动生成带水印照片，并可选择是否保留无水印原图。

## 核心需求

- 拍照并生成水印图
- 自定义水印标题：如“项目检查”“安全检查”“某某项目”
- 水印字段：logo、时间、地点/经纬度、天气、海拔、方位
- 可选择是否同时保存无水印原图
- 本地优先：照片默认不上传，天气/地址作为可关闭的增强能力
- 无广告、无会员、无营销弹窗

## 推荐技术路线

- Android Native Kotlin
- Jetpack Compose
- CameraX
- MediaStore 保存照片
- Bitmap + Canvas 绘制水印
- DataStore 保存配置
- 位置：Fused Location Provider + Android LocationManager fallback
- 方位：SensorManager Rotation Vector / 加速度计 + 磁力计
- 天气：后置阶段接入，可先用 Open-Meteo 或可配置 provider

## 规划文档

- [开源调研](docs/planning/01-open-source-research.md)
- [PRD 产品需求](docs/planning/02-prd.md)
- [技术方案](docs/planning/03-technical-plan.md)
- [实施任务清单](docs/planning/04-task-list.md)

## 第一版 MVP

建议第一版只做：

1. CameraX 拍照预览
2. 标题 + 时间水印
3. 经纬度、海拔、方位水印
4. 保存水印图到系统相册
5. 可选保存原图
6. 简单设置页：标题、字段开关、保存原图开关

天气和详细地址后置，避免外部 API、Key、网络失败拖慢主链路。
