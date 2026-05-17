# NextThing - 智能任务管理应用

**基于 Clean Architecture 的现代化 Android 任务管理应用**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 应用概述

NextThing 是一款面向个人用户的智能任务管理应用，融合了 AI 解析、语音识别、地理围栏、天气主题、成就系统等前沿功能。采用 Clean Architecture + MVVM 架构，100% Jetpack Compose 构建 UI，使用 KSP 替代 kapt 进行注解处理。

## 核心功能

### 任务管理
- 5 种任务状态：待办、已完成、延期、逾期、放弃
- 艾森豪威尔四象限分类（重要-紧急矩阵）
- 重复任务：每日/工作日/周末/法定节假日/每周（指定星期）/每月（指定日期）
- 子任务、标签、图片附件
- 数据导出（CSV / XLSX / Markdown）

### AI 智能解析
- 自然语言创建任务（接入 DeepSeek / 通义千问 LLM）
- AI 统计分析摘要
- 语音识别创建任务（科大讯飞 ASR）

### 地理围栏
- 高德地图选点 + Google Play Services 双引擎定位
- 任务关联地理位置，进入/离开围栏触发提醒
- 位置使用频率统计 + 月度统计历史

### 天气主题
- 和风天气 API（Ed25519 JWT 认证）
- 天气驱动的动态主题：晴天/阴天/雨天自动切换 App 配色

### 成就系统
- 20 种成就，5 大类（任务大师/坚持达人/效率专家/全能型/里程碑）
- 4 个等级（铜/银/金/钻石），进度追踪

### 专注模式
- 内置番茄钟计时器
- 专注期间任务状态锁定

### 数据统计
- 多维度趋势图表（7/30/90天/全部）
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
| 定位 | Google Play Services + 高德 SDK | 21.0.1 / 6.4.3 |
| 地图 | 高德 2D Map + Search | 6.0.0 / 9.7.0 |
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
│   │   ├── database/          #     Room Database (v10, 9 次增量迁移)
│   │   ├── dao/               #     10 个 DAO 接口
│   │   ├── entity/            #     10 个 Entity
│   │   └── converter/         #     TypeConverter
│   ├── remote/                #   远程数据源
│   │   ├── api/               #     Retrofit API (TaskApi, SyncApi)
│   │   ├── dto/               #     同步 DTO
│   │   └── interceptor/       #     Auth 拦截器
│   ├── repository/            #   Repository 实现（@Inject constructor + @Singleton）
│   ├── mapper/                #   Entity ↔ Domain 转换
│   ├── service/               #   服务实现（AI, ASR, Weather, Location, Achievement...）
│   ├── asr/                   #   科大讯飞语音识别
│   ├── export/                #   数据导出（CSV/XLSX/Markdown）
│   └── preferences/           #   DataStore（AI 配置、主题、视图偏好）
│
├── presentation/              # 表现层
│   ├── screens/               #   18+ 功能页面（各含 Screen + ViewModel）
│   ├── components/            #   可复用 Composable
│   ├── navigation/            #   NavHost + 路由定义
│   └── theme/                 #   Material 3 主题（支持天气动态配色）
│
├── di/                        # 依赖注入
│   ├── DatabaseModule         #   Room DB + DAO 提供 + @Binds 绑定 Repository
│   ├── NetworkModule          #   Retrofit + OkHttpClient
│   ├── AIModule               #   AI HTTP Client + 解析服务
│   ├── ASRModule              #   语音识别服务
│   ├── LocationModule         #   定位 + 地理围栏
│   └── WeatherModule          #   天气服务
│
├── work/                      # 后台任务
│   ├── TaskWorkScheduler      #   统一调度器
│   ├── CheckOverdueTasksWorker      # 每日 1:00 逾期检测
│   ├── ConvertDelayedTasksWorker    # 每日 0:00 延期转待办
│   ├── GenerateRecurringTasksWorker # 每日 0:00 重复任务生成
│   ├── TaskNotificationWorker       # 每 15 分钟通知检查
│   └── CountdownNotificationWorker  # 每 15 分钟倒计时刷新
│
├── util/                      # 工具类
│   ├── TaskAlarmManager       #   AlarmManager 任务提醒
│   ├── NotificationHelper     #   通知渠道管理
│   ├── PermissionManager      #   权限状态管理
│   ├── ChineseDateHelper      #   农历/节气
│   └── LegalHolidayHelper     #   法定节假日
│
└── receiver/
    └── GeofenceBroadcastReceiver    # 地理围栏进出事件
```

### DI 设计

- **Repository**：实现类使用 `@Inject constructor` + `@Singleton`，通过 `@Binds` 绑定到接口
- **UseCase**：每个用例独立 `@Inject constructor`，ViewModel 按需注入
- **Service**：接口通过 Module `@Binds` 绑定到实现

### 数据库设计

**Room Database v10** — 10 张表，9 次增量迁移，无破坏性降级

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
| TaskMapperTest | Task↔Entity 转换、JSON 序列化/反序列化、容错处理、round-trip | 15 |
| CategoryMapperTest | Entity↔Domain 转换、批量转换、round-trip | 12 |
| RepeatFrequencyTest | 验证逻辑、显示文本、WeekdayItem/MonthDayItem | 20 |
| TaskModelTest | 领域模型行为（状态、四象限、Subtask） | 10 |
| TaskRepositoryImplTest | CRUD、统计、模板任务（Mockito mock DAO） | 15 |

运行：
```bash
./gradlew testDebugUnitTest
```

---

## 构建与运行

### 环境要求

- Android Studio Hedgehog+
- JDK 17
- Kotlin 1.9.25

### 配置

1. 在 `local.properties` 中添加：
```properties
RELEASE_STORE_PASSWORD=your_password
RELEASE_KEY_PASSWORD=your_password
RELEASE_KEY_ALIAS=release
AMAP_API_KEY=your_amap_key
```

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

- **KSP 替代 kapt**：Hilt / Room 注解处理使用 KSP，构建速度提升 30-50%
- **@Binds 绑定模式**：Repository 接口通过 `@Binds` 绑定实现类，消除手动 `@Provides` 样板代码
- **安全实践**：签名密码和 API Key 从 `local.properties` 注入，私钥文件排除在版本控制外
- **R8 Full Mode**：开启完整代码优化和混淆
- **增量数据库迁移**：9 次迁移无破坏性降级，保障用户数据安全
- **无 Apache POI 的 XLSX 导出**：手写 OOXML 生成，零额外依赖
- **天气驱动动态主题**：整个 App 配色随天气实时变化
- **农历/节气/法定节假日**：支持中国节假日判断的重复任务

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
