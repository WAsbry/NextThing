# 三大 Feature 实现：云同步 + Widget + 日历视图

## 问题背景

NextThing 与商业待办软件（滴答清单/Todoist）对比后，发现三个致命短板：
1. 没有云同步 — 数据只存本地，换设备丢失
2. 没有 Widget — 桌面看不到今日待办
3. 没有日历视图 — 无法按日期查看任务分布

## 问题根源

### 云同步
- `SyncUseCasesStub.kt` 的 5 个 use case 全部返回硬编码值，完全忽略 `SyncRepository`
- `SyncRepositoryImpl` 虽然完整实现了同步逻辑，但 `lastSyncTimestamp` 是内存变量，进程重启后丢失
- `markTaskForSync()` 是空操作，CRUD 操作后不标记 PENDING
- 后端 `nextthing-auth` 只有 auth 端点，sync 端点全部缺失
- `TaskEntity`/`CategoryEntity` 没有 `deleted` 字段，无法做软删除（同步需要保留删除记录）

### Widget
- 项目中没有任何 Widget 代码
- `build.gradle.kts` 没有引入 Glance 依赖
- `AndroidManifest.xml` 没有 Widget receiver 声明

### 日历视图
- `TaskDao.getTasksByDateRange()` 按 `createdAt` 过滤，不是 `dueDate`，不适合日历场景
- 没有查询"某月哪些日期有任务"的方法（日历标点需要）
- 没有 CalendarScreen/CalendarViewModel

## 修改思路

### 1. 云同步

**客户端**：
- `SyncUseCasesStub.kt` → 改名去 stub，每个 use case 直接委托 `SyncRepository`
- `TaskRepositoryImpl`/`CategoryRepositoryImpl` 的 insert/update/delete → 加 `.copy(syncStatus = SyncStatus.PENDING)`
- 批量操作 SQL（`markTasksAsCompleted`、`bulkUpdateTaskCategory`）→ 加 `syncStatus='PENDING'`
- `SyncPreferences.kt` → 新建 DataStore 存储 `lastSync_timestamp`
- `SyncRepositoryImpl` → 注入 `SyncPreferences`，同步成功后持久化时间戳
- `TaskEntity`/`CategoryEntity` → 加 `deleted: Boolean = false`
- DB 迁移 v10→v11 → ALTER TABLE 加 deleted 列
- 所有 TaskDao/CategoryDao SELECT 查询 → 加 `AND deleted = 0`
- `SyncScreen.kt` → 新建同步状态页面，从 Settings 进入

**后端**：
- `SyncedTask.java` / `SyncedCategory.java` → JPA 实体，对齐客户端 DTO
- `SyncedTaskRepository` / `SyncedCategoryRepository` → Spring Data JPA
- `SyncService.java` → 增量同步、冲突检测、全量同步、冲突解决
- `SyncController.java` → `/sync/` 下 9 个端点，对齐客户端 `SyncApi`

### 2. Widget

- 添加 `androidx.glance:glance-appwidget:1.0.0` 和 `glance-material3:1.0.0`
- `TaskListWidget.kt` → GlanceAppWidget，显示今日待办前 5 条
- `TaskListWidgetReceiver.kt` → GlanceAppWidgetReceiver
- `WidgetUpdateWorker.kt` → WorkManager Worker，查询 Room 更新 Widget 数据
- `task_widget_info.xml` → Widget 元数据，4×3 大小，30 分钟刷新
- `TaskDao.getTodayTasksAsList()` → 新增一次性查询（非 Flow），Widget 用
- `AndroidManifest.xml` → 注册 Widget receiver

### 3. 日历月视图

- `TaskDao` 新增 3 个查询：`getTasksByDueDateRange`、`getTasksByDueDate`、`getDatesWithTasksInMonth`
- `TaskRepository`/`TaskRepositoryImpl` → 暴露新方法
- `CalendarViewModel.kt` → 管理月份、选中日期、任务列表、有任务日期集合
- `CalendarScreen.kt` → 月历网格（6×7）+ 日期标点 + 下方任务列表
- `Screen.Calendar` → 添加导航路由
- `TodayScreen` TopHeader → 加日历图标按钮

## 涉及文件

### 客户端

| 文件 | 改动 |
|------|------|
| `domain/usecase/SyncUseCasesStub.kt` | 5 个 use case 从硬编码改为委托 SyncRepository |
| `data/repository/TaskRepositoryImpl.kt` | insert/update 加 PENDING，新增 3 个日历查询方法 |
| `data/repository/CategoryRepositoryImpl.kt` | CRUD 操作加 PENDING |
| `data/repository/SyncRepositoryImpl.kt` | 注入 SyncPreferences，实现 markTaskForSync |
| `data/preferences/SyncPreferences.kt` | 新建，DataStore 存储 lastSyncTimestamp |
| `data/local/entity/TaskEntity.kt` | 加 `deleted: Boolean = false` |
| `data/local/entity/CategoryEntity.kt` | 加 `deleted: Boolean = false` |
| `data/local/database/TaskDatabase.kt` | 迁移 v10→v11，版本号升到 11 |
| `data/local/dao/TaskDao.kt` | 所有 SELECT 加 `deleted = 0`，新增日历查询和 Widget 查询 |
| `data/local/dao/CategoryDao.kt` | 所有 SELECT 加 `deleted = 0` |
| `domain/repository/TaskRepository.kt` | 新增 3 个日历方法 |
| `presentation/navigation/NextThingNavigation.kt` | 加 Calendar 和 Sync 路由 |
| `presentation/screens/today/TodayScreen.kt` | 加日历图标入口 |
| `presentation/screens/settings/SettingsScreen.kt` | 加"数据同步"入口 |
| `presentation/screens/calendar/CalendarViewModel.kt` | 新建 |
| `presentation/screens/calendar/CalendarScreen.kt` | 新建 |
| `presentation/screens/sync/SyncScreen.kt` | 新建 |
| `widget/TaskListWidget.kt` | 新建 |
| `widget/TaskListWidgetReceiver.kt` | 新建 |
| `widget/WidgetUpdateWorker.kt` | 新建 |
| `res/xml/task_widget_info.xml` | 新建 |
| `AndroidManifest.xml` | 注册 Widget receiver |
| `build.gradle.kts` | 加 Glance 依赖 |

### 后端

| 文件 | 改动 |
|------|------|
| `entity/SyncedTask.java` | 新建，JPA 实体 |
| `entity/SyncedCategory.java` | 新建，JPA 实体 |
| `repository/SyncedTaskRepository.java` | 新建，Spring Data JPA |
| `repository/SyncedCategoryRepository.java` | 新建，Spring Data JPA |
| `service/SyncService.java` | 新建，同步核心逻辑 |
| `controller/SyncController.java` | 新建，9 个 REST 端点 |

## 经验总结

1. **同步架构设计先行**：DTO、Repository、API 接口先设计好，客户端的 SyncRepositoryImpl 甚至先写好了。实现时只需补上 use case 的委托逻辑和后端接口。这种"设计先行、分步实现"的方式避免了大规模返工。

2. **软删除与硬删除的选择**：同步场景必须用软删除（`deleted=true`），因为删除操作需要同步到服务端。硬删除后本地记录消失，无法告知服务端"这条数据被删了"。Room 迁移加一个 `deleted` 字段并给默认值 0 是安全的。

3. **DAO 查询过滤**：加 `deleted = 0` 过滤要覆盖所有 SELECT 查询。统计查询、成就查询、同步状态查询都不能漏。建议用全局搜索确保无遗漏。

4. **Glance Widget 限制**：Glance 不能直接用 Room Flow，只能通过 `PreferencesGlanceStateDefinition` 或 `GlanceStateDefinition` 传递数据。最佳实践是 WorkManager 查询数据库后更新 Glance State。

5. **日历视图从零手写**：月历网格本质是 6×7 的二维数组，计算首日偏移（星期几）和月份天数即可。比引入第三方日历库更轻量，且完全控制样式。

6. **面试话术角度**：可以讲"同步系统设计"——增量同步（lastSyncTimestamp）、冲突检测（双方都修改时生成冲突）、软删除（deleted 标记）、持久化时间戳（DataStore vs 内存变量）。这是客户端+服务端全栈能力的展示。

---

## 编译和运行时问题修复（05.17 补充）

实现三大 Feature 后，编译和运行阶段遇到了多个阻断性问题，详见 → `doc/第01周_0511-0517/Glance和ANR问题修复.md`

### Glance Widget 编译问题（3 轮修复）

1. **1.1.0 → 1.0.0**：Aliyun 镜像缓存不完整，`GlanceId` unresolved，降级解决
2. **子包导入**：`clickable`、`actionStartActivity` 在子包中，通配符导入不覆盖
3. **Preferences → 直查数据库**：`currentState<Preferences>()` 等 API 一直 unresolved，最终在 `provideGlance` 里直接查 Room

### 启动 ANR（runBlocking + 无限 Flow）

`SyncRepositoryImpl.init` 用 `runBlocking` + `collect` 读 DataStore 的无限 Flow，导致主线程永久阻塞。删除 init 块，改用 `.first()` 懒加载。
