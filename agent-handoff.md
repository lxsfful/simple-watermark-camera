# Agent Handoff — 简易水印相机

> 每次任务结束重写此文件。接手的 Agent 先读完再动手。

---

## 上次任务完成于

2026-06-13

---

## 项目当前状态

### 可编译 / 可运行

代码已全部编写完成，**尚未在此 session 执行过 Gradle build**。
接手后第一件事应是：

```bash
cd "D:\Projects\简易水印相机"
./gradlew assembleDebug
```

如构建失败先看 `WORKLOG.md` 的"已知风险"节。

### 文件结构（截至本次任务）

```
app/src/main/java/com/lx/simplewatermarkcamera/
├── MainActivity.kt                          ← 98 行，纯路由
├── core/
│   └── Result.kt                            ← OperationResult sealed + runOperation
├── data/
│   ├── HeadingRepository.kt                 ← 传感器方位
│   ├── LocationRepository.kt                ← GPS/网络定位，2.5s 超时
│   ├── LogoStore.kt                         ← Logo 复制到私有存储
│   ├── PhotoStorageRepository.kt            ← MediaStore 保存
│   ├── SettingsRepository.kt                ← DataStore 持久化配置
│   ├── WatermarkRenderer.kt                 ← Canvas 绘制水印
│   └── WeatherRepository.kt                 ← Open-Meteo，1.8s 超时
├── domain/
│   ├── Formatters.kt                        ← WatermarkFormatter / BearingFormatter /
│   │                                          WatermarkComposer / PhotoFileNameGenerator /
│   │                                          WatermarkLayoutCalculator
│   └── Models.kt                            ← 所有数据模型
└── feature/
    ├── camera/
    │   ├── CameraScreen.kt                  ← 相机 UI，闪光灯三档
    │   └── CameraViewModel.kt               ← 拍照主流程 ViewModel
    ├── preview/
    │   └── PhotoPreviewScreen.kt            ← 预览页：重拍/分享/相册/改标题
    └── settings/
        └── SettingsScreen.kt                ← 设置页：3 模板 + 字段开关 + Logo
```

---

## 本次任务做了什么

1. **更新 `docs/planning/04-task-list.md`** — Phase 0-4 已实现项全部打勾，新增 Phase 5 架构重构节。

2. **架构拆分** — 原 540 行 `MainActivity.kt` 拆分为：
   - `feature/camera/CameraViewModel.kt` + `CameraScreen.kt`
   - `feature/settings/SettingsScreen.kt`
   - `feature/preview/PhotoPreviewScreen.kt`
   - `MainActivity.kt`（98 行，纯路由）

3. **新功能（Phase 5）**：
   - 闪光灯三档循环切换（自动 → 常亮 → 关）
   - 拍照后自动跳转预览页（重拍 / 分享 / 打开相册 / 改标题保存到下次）
   - 设置页 3 个快速模板（工程巡检 / 室外勘察 / 仅时间）

4. **添加 Coil 依赖**（`libs.versions.toml` + `build.gradle.kts`）用于预览页图片加载。

5. **Kotlin Review 后修复所有 HIGH 问题**：
   - `OutlinedButton` 替换错误的 `Button + outlinedButtonColors`（模板按钮视觉 bug）
   - 去掉双重传感器注册（`isAtLeast(STARTED)` 手动 guard 与 Observer catch-up 重叠）
   - `DisposableEffect` 加 `cameraController` key 防止生命周期泄漏
   - 分享 Intent 加 `ClipData`（Android 12+ `FLAG_GRANT_READ_URI_PERMISSION` 修复）
   - Composition 期间 state 突变改为 `LaunchedEffect`
   - `titleSaved` 重置逻辑修复（再次展开改标题面板时重置为"保存"）
   - 拍照时一次性快照 `config + heading`，避免 TOCTOU

---

## 下一步优先工作

### P0：验证构建

```bash
./gradlew assembleDebug 2>&1 | tail -30
```

如失败，最可能的原因：
- Coil 2.7.0 与 Compose BOM `2024.09.02` 兼容性（降级到 coil `2.6.0` 可规避）
- `ButtonDefaults` import 残留（已清理，但需确认）

### P1：真机验证核心流程

| 场景 | 预期 |
|------|------|
| 首次安装 → 相机权限 | 弹系统权限框，拒绝后仍可看到相机页 |
| 拍照 → 预览 | 自动跳转预览，图片可见 |
| 预览 → 分享 | 系统分享面板弹出，其他 App 可接收图片 |
| 闪光灯按钮 | 图标在 Auto/On/Off 三档循环 |
| 设置 → 模板"室外勘察" | 所有字段开关按模板设置，Logo 路径保留 |
| 连续拍 10 张 | 不崩溃，每张都保存到相册 |

### P2：未完成 Phase 5 项

- 点击对焦（`LifecycleCameraController` 支持 `tapToFocus`）
- 单元测试（可交给其他 AI，见 `04-task-list.md` 测试节）

---

## 禁止做的事

- **不要删除旧的 `data/` 和 `domain/` 文件** — feature 层依赖它们
- **不要给 `WatermarkRenderer` 加线程检查** — 调用方（ViewModel）已在 `Dispatchers.IO` 上运行
- **不要把 ViewModel 改成 Hilt** — 当前规模不需要，会引入大量样板
- **不要修改 `minSdk = 29`** — `LocationManager.getCurrentLocation` API 29+

---

## 关键依赖版本

| 库 | 版本 |
|----|------|
| AGP | 8.5.2 |
| Kotlin | 1.9.24 |
| Compose BOM | 2024.09.02 |
| CameraX | 1.3.4 |
| Coil | 2.7.0（本次新增） |
| DataStore | 1.1.1 |
| ExifInterface | 1.3.7 |
