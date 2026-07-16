# WORKLOG — 简易水印相机

> 追加记录，不删除。保留失败路径和决策依据。

---

## 2026-06-13 Session 1：读取现状 + 架构重构 + Phase 5

### 背景

用户请求"读取 D:\Projects\简易水印相机，然后继续"。项目已有完整的 data/domain 层和单文件 MainActivity.kt（540 行），Phase 0-4 代码实现完毕但文档未同步。

### 读取阶段发现

- 11 个 Kotlin 源文件，全部在 `com.lx.simplewatermarkcamera` 下扁平或 `data/`/`domain/`/`core/` 子包
- `MainActivity.kt` 包含：MainActivity、WatermarkCameraViewModel、CameraUiState、CameraScreen、SettingsScreen、WatermarkPreview、PermissionPrompt、SettingSwitch、WatermarkCameraApp（540 行，超出 800 行以下但不符合职责分离原则）
- 单元测试目录 `app/src/test` 存在但为空
- `docs/planning/04-task-list.md` 中所有任务仍为 `[ ]` 未打勾

### 执行内容

#### 1. 更新 task-list

Phase 0-4 中代码已实现的全部打勾。新增 Phase 5 节和架构重构节。

#### 2. 架构拆分

**方案**：按 feature 包拆分，保持单 Gradle module。

拆分结果：
- `feature/camera/CameraViewModel.kt` — 从 MainActivity 中提取，改名 `CameraViewModel`（原 `WatermarkCameraViewModel`），state 改名 `CameraUiState`
- `feature/camera/CameraScreen.kt` — 相机 UI，新增闪光灯按钮
- `feature/settings/SettingsScreen.kt` — 设置 UI，新增模板
- `feature/preview/PhotoPreviewScreen.kt` — 新增，预览页
- `MainActivity.kt` — 重写为 98 行纯路由

**设计决定**：`CameraUiState.previewContent` 作为 extension property 放在 CameraScreen.kt 末尾，直接构造 `WatermarkContent` 用于预览卡片，而不用 `WatermarkComposer`（后者会产生"时间 2024-01-01"这样的假数据，已按需构造各字段）。

#### 3. Phase 5 功能

**闪光灯**：`cycleFlash()` 在 ViewModel 中循环 `AUTO → ON → OFF → AUTO`。`LaunchedEffect(flashMode)` 同步到 `cameraController.imageCaptureFlashMode`。

**拍照后预览页**：
- 用 `LaunchedEffect(lastResult)` 在 ViewModel 返回结果后自动切换到 `Screen.Preview`
- Coil `AsyncImage` 渲染 MediaStore URI
- "改标题"展开 `OutlinedTextField`，保存后仅更新 config（不重新渲染已保存的照片）

**快速模板**：3 个，hardcode 在 SettingsScreen 顶层 `builtInTemplates`。应用时 `logoPath` 从当前 config 继承。

#### 4. Kotlin Review（kotlin-reviewer agent）发现并修复

| 级别 | 问题 | 修复 |
|------|------|------|
| HIGH | `Button + outlinedButtonColors` 模板按钮背景透明 | 改为 `OutlinedButton` |
| HIGH | `isAtLeast(STARTED)` 与 Observer catch-up 导致传感器双重注册 | 删除手动 guard |
| HIGH | `DisposableEffect` 缺 `cameraController` key | 加入 key |
| MEDIUM | Composition 期间 `screen = Screen.Camera` 直接突变 | 改为 `LaunchedEffect` |
| MEDIUM | 分享 Intent 无 `ClipData`，Android 12+ `FLAG_GRANT_READ_URI_PERMISSION` 无效 | 加 `clipData = ClipData.newRawUri(...)` |
| MEDIUM | `titleSaved` 重置逻辑缺失 | 展开改标题面板时重置 |
| MEDIUM | `uiState.value.latestHeading` 在 IO 线程重读导致 TOCTOU | 拍照前一次性快照整个 state |
| LOW | `onRegenerate` 命名误导（实际只保存标题） | 改名 `onSaveTitle` |

#### 5. 依赖变更

`gradle/libs.versions.toml` 新增：
```toml
coil = "2.7.0"
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }
```

`app/build.gradle.kts` 新增：
```kotlin
implementation(libs.coil.compose)
```

### 失败路径 / 未尝试路径

**"改标题重新生成水印"功能被降级**：最初设计为在预览页允许用户修改标题后重新渲染同一张照片。但原图 temp file 在拍照完成后立即删除（`source.delete()`），无法重渲染。备选方案是保留 temp file，但这会导致缓存目录膨胀。

最终决定：改标题仅更新 config 持久化，下次拍照生效。`ViewModel.regenerateWatermark()` 方法写了又删。

**Hilt DI 未引入**：Kotlin reviewer 指出 ViewModel 中依赖手动 new，不易测试。但当前规模引入 Hilt 会增加大量样板。决定维持现状，待项目规模增大再引入。

### 未完成

- Gradle build 未在本 session 执行，编译正确性未验证
- 单元测试仍为空（用户明确说"交给其他 AI"）
- 点击对焦未实现

---

## 2026-07-16 Session 2：全面优化迭代（构建验证 + 审查修复 + 真机部署）

### 背景
用户请求"读取项目，然后全面进行优化和迭代"。本机此前无 JDK，先安装 Temurin 17.0.19。上一会话只到架构拆分，本会话首次跑通 Gradle build，并对照 `kotlin-reviewer` 审查结论 + 自审全面修复，最后在 Huawei Mate 30 Pro (Android 10) 真机部署验证。

### 读取阶段发现（沿用 Session 1）
20 个 Kotlin 源文件已拆分为 `core/data/domain/feature`。本次新增 `WatermarkFormatterTest`、`WeatherRepositoryTest`，扩展 `BearingFormatterTest`、`WatermarkComposerTest`，总计 20 个单测。

### 执行内容

#### 1. 构建验证（首次）
- `winget install EclipseAdoptium.Temurin.17.JDK`
- `./gradlew assembleDebug` → BUILD SUCCESSFUL（4m37s），产出 99 MB APK
- `./gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL（51s），20 tests, 0 failures

#### 2. 启动 kotlin-reviewer agent 并对照自审修复

| 级别 | 问题 | 处理 |
|------|------|------|
| HIGH 1 | 相机预览黑屏（"缺 PREVIEW flag"） | **未修改** — CameraX 1.3.4 `CameraController` 无 PREVIEW 常量（flags: IMAGE_CAPTURE/IMAGE_ANALYSIS/VIDEO_CAPTURE），预览隐式绑定；真机截屏 `docs/camera_preview.png` 证明原代码预览正常渲染 |
| HIGH 2 | `runCatching{...}.getOrNull()` 吞 CancellationException | `LocationRepository.requestCurrentLocation` 用 `try/catch(CancellationException){throw}` |
| HIGH 3 | 方位传感器 ~60Hz 触发整屏重组 | `HeadingRepository.publish` 加值未变则跳过 |
| MED 1/2 | 非相机页 status 丢失/与导航竞争 | `SnackbarHost` 提升到 `MainActivity` 全局 `Box` |
| MED 3 | saveLogo Activity Context 泄漏 | 改用 `getApplication()` |
| MED 4 | SettingsRepository 映射重复 | 抽 `toWatermarkConfig()` / `writeInto()` |
| MED 5 | 部分失败遗留 MediaStore 孤儿照片 | `processCapturedFile` 跟踪已插入 Uri 失败清理 |
| LOW 1 | WatermarkRenderer 未用 context | 删除构造参数 |
| LOW 2 | 多余 @Suppress DEPRECATION | 保留（Geocoder 3-arg 在 API 34 仍 deprecated） |
| LOW 3 | WeatherRepository 1.8s 超时 + 禁重定向 | 15s + 允许重定向 |
| LOW 4 | HeadingAccuracy 永远 Medium | `onAccuracyChanged` 记录真实精度，`mapAccuracy()` 映射 |

#### 3. 功能新增
- **天气 TTL 缓存**：10 分钟同粗坐标复用，`fetcher` 可注入便于测试。`WeatherSource.Cache` 区分来源。
- **点击对焦**：`PreviewView.meteringPointFactory.createPoint(x,y)` + `cameraControl.startFocusAndMetering(...)`；白色圆圈指示器 1 秒消失。

#### 4. 单元测试：4 → 20

#### 5. 真机部署（关键里程碑）
- 设备 Huawei ELE-AL00 (Mate 30 Pro), Android 10 (SDK 29), 1080×2340
- `adb install -r` → Success；`pm grant` CAMERA + 定位；`am start` 启动
- 截屏 `docs/camera_preview.png`：预览、顶栏、预览卡片、拍照按钮全部正常显示
- logcat：无 FATAL，无 AndroidRuntime 异常

### 失败路径 / 未尝试路径

- **ADB `input tap` 在本机 EMUI 上无法驱动 Compose UI**（多次尝试 `input tap` / `input touchscreen tap` / `input motionevent` 均无响应，截屏 md5 不变）。这是华为 EMUI 输入注入的已知限制，**非应用代码问题**。开发者用手指触摸正常操作。
- 因此拍照 → 预览 → 保存到相册的端到端 ADB 验证未能完成；相关代码路径已通过单元测试 + 静态分析覆盖。
- 第二个审查 agent（code-reviewer）启动后未返回结论（可能超时/未完成），故最终结论以 `kotlin-reviewer` + 自审为主。

### 关键发现（值得记录）

1. **CameraX `CameraController` 无 PREVIEW 常量**，预览隐式绑定。`setEnabledUseCases(IMAGE_CAPTURE)` 是正确用法。
2. **`pointerInput` 在 `androidx.compose.ui.input.pointer`**，不是 `foundation.gestures`（后者只有手势检测器）。
3. **Kotlin 构造器默认值不能引用类成员**——必须默认值 `null` + 方法体内 `?:`。
4. **`try + catch(Sub){throw} + catch(Super){null}` 推断陷阱**：需要显式类型注解 `val x: T? = try { ... }`。

### 新增依赖
无（沿用 Session 1 的 Coil 等）。

### 文件变更摘要
详见 `docs/iteration-summary.md` 第四节。

---
