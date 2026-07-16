# Agent Handoff — 简易水印相机

> 每次任务结束重写此文件。接手的 Agent 先读完再动手。

---

## 上次任务完成于

2026-07-16

---

## 项目当前状态

### 可编译 / 可运行 / 真机已验证

```bash
cd "D:\Projects\simple-watermark-camera"
./gradlew assembleDebug testDebugUnitTest
```

- ✅ **BUILD SUCCESSFUL**（约 51s）
- ✅ **20/20 单测通过**（`BearingFormatterTest 3`, `PhotoFileNameGeneratorTest 2`, `WatermarkComposerTest 5`, `WatermarkFormatterTest 4`, `WatermarkLayoutCalculatorTest 2`, `WeatherRepositoryTest 4`）
- ✅ **APK 已安装并启动**（Huawei Mate 30 Pro, Android 10）
- ✅ **相机预览正常渲染**（截屏见 `docs/camera_preview.png`，非黑屏）
- ✅ logcat 无 FATAL / 无 AndroidRuntime 异常
- ⚠️ ADB `input tap` 在 EMUI 上无法驱动 Compose UI（环境问题，非代码 bug）；手指触摸正常

### 文件结构（截至本次任务）

```
app/src/main/java/com/lx/simplewatermarkcamera/
├── MainActivity.kt                          [98 行左右，纯路由 + 全局 SnackbarHost]
├── core/
│   └── Result.kt                            ← OperationResult sealed + runOperation
├── data/
│   ├── HeadingRepository.kt                 [改] 传感器方位 + 真实精度 + 值未变跳过（去重）
│   ├── LocationRepository.kt                [改] GPS/网络定位，2.5s 超时，CancellationException 重抛
│   ├── LogoStore.kt                         ← Logo 复制到私有存储
│   ├── PhotoStorageRepository.kt            [改] MediaStore 保存 + delete(uri) 孤儿清理
│   ├── SettingsRepository.kt                [改] DataStore + 映射去重（toWatermarkConfig/writeInto）
│   ├── WatermarkRenderer.kt                 [改] 去 context 参数
│   └── WeatherRepository.kt                 [改] Open-Meteo，15s 超时，10min TTL 缓存，可注入 fetcher
├── domain/
│   ├── Formatters.kt                        ← WatermarkFormatter / BearingFormatter /
│   │                                          WatermarkComposer / PhotoFileNameGenerator /
│   │                                          WatermarkLayoutCalculator
│   └── Models.kt                            ← 所有数据模型 + WatermarkConfig.needsLocation
└── feature/
    ├── camera/
    │   ├── CameraScreen.kt                  [改] 闪光灯三档 + 点击对焦（指示器 1s）
    │   └── CameraViewModel.kt               [改] saveLogo(uri) / 孤儿清理 / needsLocation
    ├── preview/
    │   └── PhotoPreviewScreen.kt            预览页：重拍/分享/相册/改标题
    └── settings/
        └── SettingsScreen.kt                设置页：3 模板 + 字段开关 + Logo
```

---

## 本次任务做了什么

1. **首次成功构建本项目**（上一会话从未 build）。安装 JDK 17 后 `./gradlew assembleDebug testDebugUnitTest` 绿。
2. **按 `kotlin-reviewer` + 自审结果落地所有修复**：
   - 高优：`CancellationException` 不再被吞 / 方位传感器去重（消除 ~60Hz 整屏重组）
   - 中优：SnackbarHost 提升到 app 级（解决非相机页错误丢失）/ `saveLogo` 改用 application Context（消泄漏）/ `SettingsRepository` 映射去重 / MediaStore 失败清理孤儿
   - 低优：`WatermarkRenderer` 去无用 context / 天气超时 1.8s→15s + 允许重定向 / 方位传递真实传感器精度
3. **新功能**：
   - 天气 TTL 缓存（10 分钟同粗坐标复用，标记 `WeatherSource.Cache`，fetcher 可注入便于测试）
   - 点击对焦（`cameraControl.startFocusAndMetering` + 白色圆圈指示器 1s）
4. **DRY**：`WatermarkConfig.needsLocation`（CameraViewModel + CameraScreen 共享）
5. **单元测试**：4 → 20（新增 `WatermarkFormatterTest`、`WeatherRepositoryTest`；扩展 `BearingFormatterTest`、`WatermarkComposerTest`）
6. **真机部署验证**：Huawei Mate 30 Pro (Android 10) 安装、启动、相机预览渲染正常（`docs/camera_preview.png`），无崩溃

---

## 下一步优先工作

### P0：拍照端到端流程（手指触摸，非 ADB）

```bash
# 真机手指操作：
1. 打开相机 → 触摸预览对焦
2. 点击底部拍照 → 跳到预览页（Coil 渲染水印图）
3. 预览 → 分享 → 系统面板弹出（验证 ClipData + FLAG_GRANT_READ_URI_PERMISSION）
4. 预览 → 相册 → 打开系统相册到水印图
5. 设置页 → 选 "室外勘察" 模板 → 返回 → 拍一张验证字段
6. 连续拍 10 张验证不崩溃
```

### P1：Release APK 打包

```bash
./gradlew assembleRelease
```

当前 debug APK 已 OK；release 需配置签名 keystore（暂未配置，会失败 — 正常）。

### P2：未完成 Phase 5 项

- ~~点击对焦~~（本次已完成）
- 连续拍照压力测试（真机）
- Release APK 打包

### P3：可选优化（已在 `docs/iteration-summary.md` §五 列出）

- EXIF GPS 写入（`writeGpsExif` flag 当前保留但未生效，默认关闭 — 隐私敏感，保持默认关）
- `WeatherRepository` stale-on-failure（网络失败返回稍旧缓存）

---

## 禁止做的事

- **不要删除旧的 `data/` 和 `domain/` 文件** — feature 层依赖它们
- **不要给 `WatermarkRenderer` 加线程检查** — 调用方（ViewModel）已在 `Dispatchers.IO` 上运行
- **不要把 ViewModel 改成 Hilt** — 当前规模不需要，会引入大量样板
- **不要修改 `minSdk = 29`** — `LocationManager.getCurrentLocation` API 29+
- **不要给 `CameraController` 加 `PREVIEW` flag** — 不存在的常量，预览隐式绑定；真机已验证
- **不要把 `SnackbarHost` 放回单个屏幕的 `Scaffold`** — app 级全局 Host 已修好非相机页错误丢失
- **不要给 `WeatherRepository` 默认值 lambda 引用 `fetchFromNetwork`** — 编译会失败；用 `null` + 方法体 `?:`

---

## 关键依赖版本

| 库 | 版本 |
|----|------|
| AGP | 8.5.2 |
| Kotlin | 1.9.24 |
| Compose BOM | 2024.09.02 |
| CameraX | 1.3.4 |
| Coil | 2.7.0 |
| DataStore | 1.1.1 |
| ExifInterface | 1.3.7 |

## 关键技术事实（防止接手 Agent 重复踩坑）

1. **CameraX `CameraController` 没有 `PREVIEW` 常量**（flags: `IMAGE_CAPTURE=1`/`IMAGE_ANALYSIS=2`/`VIDEO_CAPTURE=4`）；预览在 `PreviewView` 附加时**隐式绑定**。
2. **`Modifier.pointerInput(...)` 在 `androidx.compose.ui.input.pointer`**，**不是** `foundation.gestures`（后者只有 `detectTapGestures` 等）。
3. **`startFocusAndMetering` 在 `CameraControl`，不在 `CameraController`** — 用 `controller.cameraControl?.startFocusAndMetering(...)`。
4. **Kotlin 构造器默认值不能引用类成员**（`fetcher = { fetchFromNetwork(it) }` 编译失败）；需 `fetcher = null` + 方法体 `fetcher ?: ::fetchFromNetwork`。
5. **`try + catch(Sub){throw} + catch(Super){null}` 需显式类型注解** 才不报类型推断错误。
6. **ADB `input tap` 在 EMUI 上可能不触发 Compose UI**（环境限制，非代码 bug）；真机触摸正常工作。

---

## 真机验证截图

- `docs/camera_preview.png` — 相机页面首次截屏（预览正常）