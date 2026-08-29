# NextThing

> **中文**：NextThing 是一款面向个人的 Android 智能任务管理应用，帮助你把事情记录下来，在合适的时间或地点提醒你，并用统计与复盘看清执行变化。
> **English**: NextThing is an Android smart task manager for capturing work, acting at the right time or place, and reviewing execution results.

[![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android)](https://www.android.com/)
[![Language](https://img.shields.io/badge/language-Kotlin-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/license-MIT-0A84FF)](LICENSE)

## 中文

### 这是什么

NextThing 把“任务、提醒、地点、复盘”放进一个个人执行流程：

`创建任务 → 设置时间/地点规则 → 执行与提醒 → 完成、延期或取消 → 查看结果与改进方向`

它适合希望管理日常、学习、工作和重复习惯的个人用户。任务页负责找到并处理任务；统计页只回答“完成了多少、有没有风险、是否变好”。

### 核心功能

- **任务管理**：创建、编辑、完成、延期、取消任务；支持分类、优先级、标签、附件与子任务。
- **重复计划**：支持每日、工作日、周末、节假日、指定星期及指定日期的重复任务。
- **任务视图**：在周视图和月视图中浏览任务；支持关键词搜索、AI 搜索和状态筛选。
- **智能创建**：可用自然语言解析任务信息，并提供语音创建入口。
- **提醒能力**：可配置通知、声音、震动、提前提醒与智能早晚报。
- **地点提醒**：使用当前位置、地图选点或 POI 搜索创建地点；任务可在到达或离开地点时触发提醒。
- **执行统计**：按今日、本周、本月查看完成数、完成率、待办、逾期、重要紧急任务与变化趋势。
- **专项复盘**：提供任务结构、趋势分析、效率诊断和 AI 周报。
- **个人与数据**：支持成就、主题、视图偏好、数据同步、冲突处理、云端恢复及 CSV/XLSX/Markdown 导出。

### 主要页面

| 页面 | 用户可以做什么 |
|---|---|
| 首页 | 查看今日进度、今日任务、当前位置与日历入口 |
| 任务 | 搜索、筛选、按周/月查看并处理任务 |
| 创建 | 把一件事变成包含时间、分类、提醒和地点的可执行任务 |
| 统计 | 查看结果、风险和变化，并进入专项复盘 |
| 我的 | 管理个人资料、AI、地点、提醒、主题、同步和导出 |

### 下载与体验

请在 [GitHub Releases](https://github.com/WAsbry/NextThing/releases) 下载最新 APK。
首次使用地点、通知或语音能力时，Android 会请求对应权限；拒绝权限不会影响基础任务管理，但会限制相关增强功能。

### 当前状态

- 项目处于持续完善阶段，基础任务、地点、提醒、统计、同步和导出流程均已具备入口。
- 地图定位、通知、后台提醒与语音识别需要在真实 Android 设备上授权和验证。
- 语音能力需要额外下载模型资源；未安装资源时，普通键盘创建和任务管理仍可使用。

### 截图

正式展示截图将随 Release 持续更新：建议依次查看首页、任务、创建、地点选点、统计和个人中心。

---

## English

### Overview

NextThing is a personal execution companion. It turns an intention into an actionable task, supports time- and location-based reminders, and helps users review outcomes instead of merely accumulating lists.

`Create → plan → receive reminders → complete/postpone/cancel → review`

### Highlights

- Task creation, editing, completion, postponement, cancellation, priorities, tags, attachments, and subtasks.
- Recurrence rules for daily, workday, weekend, holiday, weekly, and monthly schedules.
- Weekly/monthly task browsing, keyword search, AI-assisted search, and status filtering.
- Natural-language task parsing and an optional voice-entry flow.
- Reusable notification strategies and smart morning/evening briefings.
- Reusable places from current location, map selection, or POI search for arrival/departure reminders.
- Today/week/month results, risk signals, trends, structure, efficiency analysis, and AI weekly reports.
- Profile, achievements, theme preferences, sync, conflict handling, recovery, and CSV/XLSX/Markdown export.

### Get the APK

Download the latest build from [GitHub Releases](https://github.com/WAsbry/NextThing/releases). Device permissions are requested only when a feature needs them. Core task management remains available without optional voice-model assets.

---

## For developers / 面向开发者

This README is intentionally limited to setup and running the project. Read [项目技术说明.md](项目技术说明.md) for architecture and design details, and [项目说明.md](项目说明.md) for the user-facing product description.

### Requirements / 环境要求

| Item | Requirement |
|---|---|
| IDE | Android Studio Hedgehog or newer |
| JDK | 17 |
| Android SDK | compileSdk 34; minSdk 24 |
| Device | Android 7.0+; Android 10+ ARM64 device recommended for optional voice features |
| Network | Required for remote AI, weather, map search, and cloud synchronization |

### Clone and configure / 下载并配置

```bash
git clone https://github.com/WAsbry/NextThing.git
cd NextThing
```

Create a local `local.properties` file. Never commit this file or any real credential.

```properties
sdk.dir=/path/to/Android/Sdk
AMAP_API_KEY=your_amap_key
BACKEND_BASE_URL=https://your-backend-base-url/

# Required only for signed release builds
RELEASE_STORE_PASSWORD=your_password
RELEASE_KEY_PASSWORD=your_password
RELEASE_KEY_ALIAS=your_alias
```

`AMAP_API_KEY` is required for current location, map selection, POI search, reverse geocoding, and geofence-related flows. `BACKEND_BASE_URL` is required for the remote services that your deployment enables.

### Optional voice-model assets / 可选语音模型资源

The repository does not include large model files or native runtime binaries. Basic task management works without them; voice-entry and emotion-related functions require a separately prepared local asset package.

- SenseVoice official repository: [FunAudioLLM/SenseVoice](https://github.com/FunAudioLLM/SenseVoice)
- SenseVoiceSmall model card: [Hugging Face](https://huggingface.co/FunAudioLLM/SenseVoiceSmall)
- Android deployment reference: [k2-fsa/sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx)

After obtaining compatible assets, place them in the project paths expected by the application:

```text
app/src/main/assets/models/
app/src/main/jniLibs/arm64-v8a/
```

Verify the model licence and the compatibility of every converted/runtime artifact before redistribution. The repository does not distribute APKs, keystores, private keys, models, or native runtime packages in source control.

### Build and run / 构建与运行

```powershell
# Compile Kotlin only
.\gradlew.bat :app:compileDebugKotlin --console=plain

# Build Debug APK
.\gradlew.bat :app:assembleDebug --console=plain
```

Output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Open the project in Android Studio, connect a real device, select the `app` configuration, and click Run. Do not commit generated APKs to Git; publish distributable APKs through GitHub Releases.

### Validation / 验证建议

Before publishing a build, verify on a real device:

1. Create, edit, complete, postpone, cancel, and repeat a task.
2. Grant/deny notification and location permissions and verify recovery guidance.
3. Create a place using current location and map selection; bind it to a task.
4. Switch task and statistics time ranges; verify empty and populated states.
5. Run sync, export, and destructive cloud-recovery confirmation flows.

### Documentation / 文档

Only these repository-root documents are maintained:

- [项目说明.md](项目说明.md): product and user-facing capability guide.
- `README.md`: setup and running guide.
- [项目技术说明.md](项目技术说明.md): technical and design reference.
- [更新日志.md](更新日志.md): meaningful product changes and fixes.

## License

MIT. See [LICENSE](LICENSE).
