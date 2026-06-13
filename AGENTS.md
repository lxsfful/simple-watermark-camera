# AGENTS.md — 简易水印相机

> 长期约束规则，非重大架构变更不修改。所有 Agent 在开始工作前必须读完。

---

## 项目定性

单 Gradle module 的 Android Kotlin + Compose 水印相机 App。本地处理、无后端、无账号。核心价值：照片私密性 + 离线可用。

---

## 不可违反的约束

### 存储与隐私

- **照片必须本地处理**，不得上传任何图片到远程服务器
- **天气请求只发坐标**（约 100 米精度），且只在拍照时发，不做持续上报
- **GPS EXIF 默认关闭**，`writeGpsExif` 字段已在 `WatermarkConfig` 中保留，但当前 `WatermarkRenderer` 未实际写入——未来实现时必须默认 false
- **不申请后台定位**，`ACCESS_BACKGROUND_LOCATION` 不得出现在 manifest

### 拍照主流程不可阻塞

- 定位失败 → 仍可拍照，水印隐藏定位字段
- 天气请求失败 → 仍可拍照，水印隐藏天气字段
- Logo 加载失败 → 仍可拍照，水印无 Logo
- 任何非相机核心的异常 → 降级，不崩溃，不阻断拍照

### Android 版本

- `minSdk = 29`（Android 10）
- 不得使用需要 API 30+ 的特性（`LocationManager.getCurrentLocation` 已做版本判断）
- MediaStore scoped storage 已在 `PhotoStorageRepository` 中正确处理 `IS_PENDING`

---

## 架构规则

### 包结构

```
feature/camera/    ← CameraScreen + CameraViewModel
feature/settings/  ← SettingsScreen
feature/preview/   ← PhotoPreviewScreen
domain/            ← 纯 Kotlin 模型和业务逻辑，无 Android 依赖
data/              ← Repository 实现，依赖 Android SDK
core/              ← 通用工具（OperationResult 等）
```

- **domain 层不得 import Android SDK**（`android.*`）
- **feature 层可以 import domain 和 data**，但不得反向依赖
- **不拆 Gradle module**，包边界已足够，module 化带来的成本超过收益

### ViewModel

- 使用 `AndroidViewModel`，因为 `LocationRepository`、`WatermarkRenderer` 等需要 `Context`
- **不引入 Hilt**，当前规模不需要
- `SettingsRepository` 的 DataStore 实例由 `Context.settingsDataStore` 委托持有，全局单例，不得多次创建

### Compose 规则

- **副作用不发生在 composition 阶段**（直接在 `@Composable` 函数体中写 state 是错的），必须用 `LaunchedEffect`
- `DisposableEffect` 的 key 必须包含所有影响 effect 行为的变量
- `LifecycleEventObserver` 注册时会自动 catch-up 补发事件，**不要再手动判断 `isAtLeast(STARTED)`**，否则造成双重注册

### 错误处理

- 所有 IO 操作通过 `runOperation(message) { ... }` 包裹，返回 `OperationResult`
- **不得 swallow `CancellationException`**（`runOperation` 已正确 rethrow）
- 错误信息通过 `CameraUiState.status` → Snackbar 展示，不弹 Dialog

---

## 代码风格

- **不写注释**，除非 WHY 非常不明显（如 catch-up 事件问题）
- **不写 TODO**，未完成功能记录到 `docs/planning/04-task-list.md`
- 函数 < 50 行，文件 < 800 行
- 不可变数据：`data class` + `copy()`，不直接 mutate
- StateFlow 更新用 `mutable.update { ... }`，不用 `mutable.value = ...`

---

## 依赖管控

- **新增依赖必须记录在 `WORKLOG.md`**，说明原因
- **不引入 Retrofit / OkHttp**：天气请求用原生 `HttpURLConnection`，足够简单
- **不引入 Room**：当前只有 DataStore Preferences，没有结构化数据需求
- **不引入 Navigation Compose**：当前只有 3 个页面，手写 `enum Screen` 足够，引入 Navigation 增加样板

---

## 测试策略

- 单元测试目标：`domain/` 层所有 object（`WatermarkFormatter`、`BearingFormatter`、`WatermarkComposer`、`WatermarkLayoutCalculator`、`PhotoFileNameGenerator`）
- 单元测试**不 mock `data/` 层**，domain 层无 Android 依赖，直接 JVM 测试
- Android 测试（Instrumentation）：`DataStore` / `MediaStore` / `WatermarkRenderer` 输出验证
- **不写 E2E 测试**，真机手动验收即可（见 `04-task-list.md` 真机验收清单）

---

## 已知技术债

| 项 | 风险 | 优先级 |
|----|------|--------|
| EXIF GPS 写入 key 存在但未实现 | 低（默认关闭，用户无感知） | P2 |
| 天气无缓存，每次拍照都请求网络 | 中（频繁拍照时可能超时积压） | P2 |
| `LocationRepository` API 29 以下降级为空 | 低（minSdk=29，永远不触发） | 已接受 |
| `saveLogo` 传 Activity Context 给 ViewModel | 低（IO 操作短暂，不泄漏） | P3 |
| ViewModel 依赖无法注入，单元测试需 Robolectric | 低（domain 层纯 JVM 已覆盖主要逻辑） | P3 |
