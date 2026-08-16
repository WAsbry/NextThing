# NextThing - 你的 AI 伙伴

**基于 Clean Architecture 的智能任务管理应用**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 应用概述

NextThing 是一款面向个人用户的智能任务管理应用，融合了 AI 解析、语音识别、地理围栏、天气主题、成就系统等前沿功能。采用 Clean Architecture + MVVM 架构，100% Jetpack Compose 构建 UI。

## 品牌标识

**NT 图标** — 白色 NT 字母的方形品牌图标，可独立用作 App Icon。图标资产保留历史紫蓝视觉，但不代表当前全局主题色。

**当前界面风格**：默认浅色主题采用蓝白现代卡片风格，应用级主色为 `#0A84FF`，背景使用白色与浅蓝灰。一级页面以 10dp 外边距、8dp 小圆角和细描边组织信息；紫色不是默认页面主色，仅保留在明确的局部语义或可选主题中。

## 当前开发状态（2026-08-16）

- 首页、任务、创建与统计一级页面正在按统一蓝白视觉基线收口。
- 任务页负责搜索、智能筛选、状态筛选、周/月浏览和任务处理；周/月共用顶部时间导航。
- 统计页负责执行结果、风险指标、完成趋势与专项分析入口，不重复承担任务处理职责。
- 地点能力使用高德定位、地图选点、POI 搜索与逆地理编码；从任务地理围栏入口创建地点后可自动绑定当前任务。
- 敏感配置从 `local.properties` 或服务端注入；APK、签名、私钥、模型、native runtime 和本地日志不进入 Git。

## 核心功能

### 任务管理
- 5 种任务状态：待办、已完成、延期、逾期、放弃
- 周视图/月视图、统一时间导航、状态筛选与普通关键词搜索
- 显式触发的 AI 智能筛选，包含加载、成功、空结果、失败和重试反馈
- 艾森豪威尔四象限分类（重要-紧急矩阵）
- 重复任务：每日/工作日/周末/法定节假日/每周（指定星期）/每月（指定日期）
- 子任务、标签、图片附件
- 数据导出（CSV / XLSX / Markdown）

### AI 智能解析
- 自然语言创建任务（接入 DeepSeek / 通义千问 LLM）
- AI 统计分析摘要
- 语音识别创建任务（Sherpa-ONNX 端侧 ASR，CPU + QNN HTP 双阶段链路）
- 端侧 AI 推理（SER 情绪识别 + Voice Pipeline）

### 地理围栏
- 高德定位 SDK 作为唯一定位来源，高德地图选点、POI 搜索与逆地理编码
- 任务关联地理位置，进入/离开围栏触发提醒
- 位置使用频率统计 + 月度统计历史

### 天气主题
- Android 通过登录鉴权后的 NextThing 后端访问和风天气；Ed25519 私钥与 JWT 签名只存在于服务端
- 天气驱动的动态主题：晴天/阴天/雨天自动切换 App 配色
- 天气进度卡片展示实时天气 + 任务完成率

### 成就系统
- 20 种成就，5 大类（任务大师/坚持达人/效率专家/全能型/里程碑）
- 4 个等级（铜/银/金/钻石），进度追踪

### 专注模式
- 内置番茄钟计时器
- SER 情绪守护，专注期间状态锁定

### 数据统计
- 今日/本周/本月执行概览与完成趋势
- 任务结构、趋势分析、效率诊断和 AI 周报专项入口
- 完成率热力图、四象限分布、分类效率对比

---

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Kotlin | 1.9.25 |
| UI | Jetpack Compose + Material 3 | BOM 2024.10.01 |
| 导航 | Navigation Compose | 2.7.6 |
| DI | Hilt + KSP | 2.48.1 |
| 数据库 | Room | 2.6.1 |
| 网络 | Retrofit + OkHttp | 2.9.0 / 4.12.0 |
| 序列化 | Gson + Kotlinx Serialization | 2.10.1 / 1.6.3 |
| 协程 | Kotlinx Coroutines | 1.7.3 |
| 图片 | Coil Compose | 2.5.0 |
| 后台任务 | WorkManager | 2.9.0 |
| 存储 | DataStore Preferences | 1.0.0 |
| 定位 | 高德定位 SDK | 6.4.3 |
| 地图 | 高德 2D Map + Search | 6.0.0 / 9.7.0 |
| 端侧 AI | Sherpa-ONNX + ONNX Runtime | - |
| 加密 | BouncyCastle (EdDSA) | 1.70 |
| 日志 | Timber | 5.0.1 |
| 注解处理 | KSP | 1.9.25-1.0.20 |

构建配置：`compileSdk=34`，`minSdk=24`，`targetSdk=34`，Java 17

---

## 架构设计

```
com.nextthing.app/
├── domain/                    # 领域层（纯 Kotlin）
│   ├── model/                 #   领域模型（Task, Category, RepeatFrequency...）
│   ├── repository/            #   Repository 接口
│   ├── service/               #   服务接口（Location, Weather, ASR, Geofence...）
│   └── usecase/               #   业务用例（每个用例独立 @Inject constructor）
│
├── data/                      # 数据层
│   ├── local/                 #   本地数据源
│   │   ├── database/          #     Room Database (v13，显式增量迁移)
│   │   ├── dao/               #     10 个 DAO 接口
│   │   ├── entity/            #     10 个 Entity
│   │   └── converter/         #     TypeConverter
│   ├── remote/                #   远程数据源
│   │   ├── api/               #     Retrofit API (TaskApi, SyncApi, AIChatApi)
│   │   ├── dto/               #     同步 DTO + AI Chat DTO
│   │   └── interceptor/       #     Auth 拦截器
│   ├── repository/            #   Repository 实现（@Inject constructor + @Singleton）
│   ├── mapper/                #   Entity ↔ Domain 转换
│   ├── service/               #   服务实现（AI, ASR, Weather, Location, Achievement...）
│   ├── ai/                    #   端侧 AI（AudioPreprocessor, SER, OnDeviceEngine）
│   ├── asr/                   #   语音识别（Sherpa-ONNX）
│   ├── export/                #   数据导出（CSV/XLSX/Markdown）
│   └── preferences/           #   DataStore（AI 配置、主题、视图偏好）
│
├── presentation/              # 表现层
│   ├── screens/               #   18+ 功能页面（各含 Screen + ViewModel）
│   │   ├── splash/            #     启动页（NT Logo + 脉冲光晕）
│   │   ├── login/             #     登录/注册（NT 图标 + 紫蓝主题）
│   │   ├── today/             #     今日页（天气卡片 + 任务列表）
│   │   ├── create/            #     创建任务（语音 + AI 解析）
│   │   ├── tasks/             #     任务搜索、筛选与周/月视图
│   │   ├── stats/             #     执行概览、趋势与专项分析
│   │   ├── settings/          #     全局设置
│   │   └── ...                #     更多页面
│   ├── components/            #   可复用 Composable
│   ├── navigation/            #   NavHost + 路由定义
│   └── theme/                 #   Material 3 主题（蓝白主视觉 + 天气动态配色）
│
├── di/                        # 依赖注入
│   ├── DatabaseModule         #   Room DB + DAO 提供 + @Binds 绑定 Repository
│   ├── NetworkModule          #   Retrofit + OkHttpClient
│   ├── AIModule               #   AI HTTP Client + 解析服务
│   ├── ASRModule              #   语音识别服务
│   ├── LocationModule         #   定位 + 地理围栏
│   ├── UseCaseModule          #   UseCase 注入
│   └── WeatherModule          #   天气服务
│
├── work/                      # 后台任务
│   ├── TaskWorkScheduler      #   统一调度器
│   ├── CheckOverdueTasksWorker      # 每日 1:00 逾期检测
│   ├── ConvertDelayedTasksWorker    # 每日 0:00 延期转待办
│   ├── GenerateRecurringTasksWorker # 每日 0:00 重复任务生成
│   ├── TaskNotificationWorker       # 每 15 分钟通知检查
│   ├── CountdownNotificationWorker  # 每 15 分钟倒计时刷新
│   └── DailyBriefingWorker          # 每日简报
│
├── util/                      # 工具类
│   ├── TaskAlarmManager       #   AlarmManager 任务提醒
│   ├── NotificationHelper     #   通知渠道管理
│   ├── PermissionManager      #   权限状态管理
│   └── ...
│
└── receiver/
    └── GeofenceBroadcastReceiver    # 地理围栏进出事件
```

### 数据库设计

**Room Database v13** — 10 张表，使用显式增量迁移，无破坏性降级

| 表名 | 说明 |
|------|------|
| tasks | 任务（含重复模板/实例、同步状态） |
| categories | 分类（预置 + 自定义） |
| locations | 位置信息 |
| notification_strategies | 通知策略（声音/震动/提醒时间） |
| users | 用户信息 |
| geofence_config | 地理围栏全局配置 |
| geofence_locations | 围栏位置（含使用统计） |
| task_geofences | 任务-围栏关联 |
| geofence_location_statistics_history | 围栏月度统计 |
| achievements | 成就解锁记录 |

---

## 测试

### 单元测试

| 测试类 | 覆盖内容 | 测试数 |
|--------|----------|--------|
| TodayViewModelTest | UiState 初始值、Tab 过滤、完成率计算、状态切换 | 12 |
| TaskMapperTest | Task↔Entity 转换、JSON 序列化/反序列化、容错处理 | 15 |
| CategoryMapperTest | Entity↔Domain 转换、批量转换、round-trip | 12 |
| RepeatFrequencyTest | 验证逻辑、显示文本、WeekdayItem/MonthDayItem | 20 |
| TaskModelTest | 领域模型行为（状态、四象限、Subtask） | 10 |
| TaskRepositoryImplTest | CRUD、统计、模板任务（Mockito mock DAO） | 15 |

### UI 测试

| 测试类 | 覆盖内容 | 测试数 |
|--------|----------|--------|
| TodayScreenUiTest | TopHeader、Tab 显示、任务列表渲染、统计数字 | 6 |

运行：
```bash
./gradlew testDebugUnitTest                          # 单元测试
./gradlew connectedDebugAndroidTest                  # UI 测试（需设备）
./gradlew assembleRelease                            # Release 工程门禁
```

没有配置正式签名时，Release 产物会明确命名为
`app/build/outputs/apk/release/NextThing-release-unsigned.apk`，只能用于构建验收，不能作为正式发布包。

天气能力依赖后端 `/api/weather/now`。Android 工程不应保存和风天气私钥，也不应自行生成供应商 JWT；服务端所需变量见 `nextthing-auth/.env.example`。

---

## 构建与运行

### 环境要求

- Android Studio Hedgehog+
- JDK 17
- Kotlin 1.9.25

### 运行硬件要求

NextThing 的普通任务管理、统计、同步、地理围栏等功能可以在 Android 7.0+ 设备上运行；但“语音创建任务”依赖 Sherpa-ONNX 端侧 ASR 和 SER 模型，完整体验需要更高的本地推理能力。

| 能力 | 最低要求 | 推荐配置 |
|------|----------|----------|
| Android 系统 | Android 7.0 / API 24 | Android 10+ |
| CPU 架构 | 支持项目内 native runtime 的 ABI | ARM64 / arm64-v8a |
| 内存 | 6 GB RAM | 8 GB RAM 及以上 |
| 可用存储 | 1 GB 以上 | 2 GB 以上 |
| 端侧 ASR | 可加载 ONNX Runtime + Sherpa-ONNX | 近年中高端 Android SoC |
| 麦克风 | 支持 16 kHz 单声道录音 | 低噪声麦克风环境 |

端侧 ASR 当前采用两阶段混合链路：Pass 1 使用 CPU Paraformer 流式出字，Pass 2 使用 Qualcomm QNN HTP 在 Hexagon NPU 上运行 10 秒 SenseVoice 模型并给出最终结果。QNN 模型为固定时长，不足 10 秒时补齐，超过 10 秒时截断。当前构建仅面向 `arm64-v8a` 设备。

### 必需模型与 Runtime 资源

语音创建任务不是纯云端功能，完整运行必须提供端侧模型和 native runtime。由于 GitHub 普通仓库不适合直接存放超过 100 MB 的文件，本仓库不提交模型、`.so`、APK、keystore 或私钥；这些文件通过单独资源包分发。

资源包建议命名：

```text
nextthing-asr-runtime-v1.zip
```

资源包解压到项目根目录后，应形成以下目录：

```text
app/src/main/assets/models/
app/src/main/jniLibs/
```

当前本地资源展开大小约为：

```text
models   486.06 MB
jniLibs  131.82 MB
total    617.88 MB
```

关键模型文件：

```text
app/src/main/assets/models/sherpa-onnx/paraformer/encoder.int8.onnx
app/src/main/assets/models/sherpa-onnx/paraformer/decoder.int8.onnx
app/src/main/assets/models/sherpa-onnx/paraformer/tokens.txt
app/src/main/assets/models/sherpa-onnx/sense-voice-qnn-10s/tokens.txt
app/src/main/assets/models/sherpa-onnx/sense-voice-qnn-10s/model-sm8550-v2.bin
app/src/main/assets/models/sherpa-onnx/silero_vad.onnx
app/src/main/assets/models/ser_model.tflite
app/src/main/jniLibs/arm64-v8a/libsherpa-onnx-jni.so
```

`model-sm8550-v2.bin` 是 SM8550 专用的预编译 QNN context，SHA-256 为 `67ed0e6f4544ee201156720792122fc54a3b52600af04c0450d501c64710f299`。不要换回便携 `libmodel.so`，否则首次运行会在手机上现场编译数十秒，并显著增加瞬时内存占用。

QNN HTP Runtime 由 `com.qualcomm.qti:qnn-runtime:2.34.0` 提供。真机验证时应同时检查 `SherpaASR` 的 `requested=QNN_HTP actual=QNN_HTP` 日志、`libQnnHtpV73Skel.so` 加载日志以及 CDSP domain 3 remote handle，不应只以“初始化成功”作为 NPU 证据。

### ASR CPU / NPU 对照构建

默认构建使用 NPU Pass 2：

```powershell
.\gradlew.bat clean :app:assembleDebug -PasrBackend=npu --no-daemon
```

CPU 基线构建使用本地备份的原 SenseVoice ONNX 模型：

```powershell
.\gradlew.bat clean :app:assembleDebug -PasrBackend=cpu --no-daemon
```

CPU 资源位置为 `local-artifacts/original-cpu-sensevoice/`，只用于本地 A/B 测试，不进入 Git。两种构建都会通过 `ASR-Benchmark` 输出初始化墙钟时间、进程 CPU 时间、Pass 2 墙钟时间、线程/进程 CPU 时间以及 Native Heap、PSS、RSS。比较时必须使用同一设备、相同录音内容和相近音频长度。

如果缺少上述资源，项目可以编译，键盘创建任务等普通功能仍可使用；进入创建页时，语音入口会提示“语音资源未安装”。发布到 GitHub 时，建议将 `nextthing-asr-runtime-v1.zip` 放在 GitHub Releases、Hugging Face 或其他稳定下载位置，并在 Release Notes 中注明版本和校验值。

本地生成资源包：

```powershell
.\scripts\package-asr-runtime.ps1 -Version v1
```

如果 Windows PowerShell 阻止脚本执行，可以使用当前进程临时绕过执行策略：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\package-asr-runtime.ps1 -Version v1
```

脚本会生成：

```text
local-artifacts/nextthing-asr-runtime-v1.zip
local-artifacts/nextthing-asr-runtime-v1.zip.sha256
```

当前 `v1` 资源包压缩后约 `431 MB`。

`local-artifacts/` 和 `*.zip` 已被 `.gitignore` 忽略，不会进入代码仓库。

### 配置

1. 在 `local.properties` 中添加：
```properties
RELEASE_STORE_PASSWORD=your_password
RELEASE_KEY_PASSWORD=your_password
RELEASE_KEY_ALIAS=release
AMAP_API_KEY=your_amap_key
BACKEND_BASE_URL=https://api.qianqian.chat/
```

`BACKEND_BASE_URL` 必须以 `/` 结尾。未配置时默认使用 `https://api.qianqian.chat/`；Smoke 构建仍使用本机测试地址。

2. 将签名文件 `release.keystore` 放至 `app/` 目录

### 构建

```bash
./gradlew assembleDebug      # Debug 构建
./gradlew assembleRelease    # Release 构建（输出至项目根目录）
```

### 国内镜像

项目已配置阿里云 + 腾讯云 Maven 镜像加速（`settings.gradle.kts`）。

---

## 项目特色

- **蓝白主视觉**：默认主题以蓝色、白色和浅蓝灰为主，卡片、导航及操作反馈保持统一视觉语言
- **局部语义强调**：AI、个人中心、Toast、统计指标和部分滑动操作保留紫色强调，但紫色不是全局主色
- **一级页面约束**：首页、任务、创建和统计默认使用蓝色主强调；紫色不得作为普通一级页面的大面积主视觉
- **KSP 替代 kapt**：Hilt / Room 注解处理使用 KSP，构建速度提升 30-50%
- **@Binds 绑定模式**：Repository 接口通过 `@Binds` 绑定实现类，消除手动 `@Provides` 样板代码
- **安全实践**：签名密码和 Android 平台 Key 从本地配置注入；天气 Ed25519 私钥仅由鉴权后端持有，不进入 APK
- **R8 Full Mode**：开启完整代码优化和混淆
- **增量数据库迁移**：Room Database v13 使用显式增量迁移，保障用户数据安全
- **无 Apache PoI 的 XLSX 导出**：手写 OOXML 生成，零额外依赖
- **天气驱动动态主题**：整个 App 配色随天气实时变化
- **端侧 AI**：Sherpa-ONNX 端侧语音识别 + SER 情绪识别，隐私优先
- **农历/节气/法定节假日**：支持中国节假日判断的重复任务

---

## 版本历史

| 版本 | 主要功能 |
|------|---------|
| v0.5.0 | AI 智能秘书 10 大功能 + 后端同步部署 |
| v0.6.0 | UI 品牌升级 — NT Logo + 紫蓝主题 + 端侧 AI + 权限弹窗重构 |
| 当前开发版（未发布） | 蓝白主主题与天气动态配色、AI 助手与配置页、端侧 ASR 双阶段链路、同步/鉴权/提醒重构及测试补强；以当前工作区源码为准 |

---

## License

```
MIT License

Copyright (c) 2025 NextThing

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

**如果这个项目对你有帮助，请给一个 Star！**
