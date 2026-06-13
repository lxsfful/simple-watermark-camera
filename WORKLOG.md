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

_下次追加记录请在此行下方添加，格式为 `## YYYY-MM-DD Session N：...`_
