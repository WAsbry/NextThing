# Glance Widget 编译问题 + 启动 ANR 修复

## 问题背景

三大 Feature 代码写完后，编译和运行阶段连续遇到两个阻断性问题：
1. **Glance Widget 编译失败**：`GlanceId`、`clickable`、`actionStartActivity` 等 import 全部 unresolved
2. **启动 ANR**：App 启动后白屏转圈，MIUIScout 报 `APP_SCOUT_HANG`，主线程卡死 5s+

## 问题根源

### Glance 编译问题

**根因**：三层问题叠加。

1. **Glance 版本**：最初用了 `1.1.0`，但 Aliyun Maven 镜像可能缓存了不完整的 artifact。`GlanceAppWidget` 等基础类能解析，但 `GlanceId` 类找不到。
2. **子包导入遗漏**：降级到 `1.0.0` 后 `GlanceId` 解决了，但 `clickable` 和 `actionStartActivity` 分别在 `androidx.glance.action` 和 `androidx.glance.appwidget.action` 子包中。Kotlin 的通配符 `import androidx.glance.appwidget.*` 不会递归导入子包。
3. **GlanceState/Preferences 方案不可行**：最初用 `currentState<Preferences>()` + `intPreferencesKey` 读取 Widget 数据，但这些 API 在当前版本下一直有 unresolved 问题。

**编译错误链**：
```
GlanceId unresolved → 降级 1.0.0 → 解决
clickable unresolved → 补 import androidx.glance.action.clickable → 解决
actionStartActivity unresolved → 补 import androidx.glance.appwidget.action.actionStartActivity → 解决
Preferences/currentState unresolved → 放弃 Preferences 方案，改为直查数据库 → 解决
```

### 启动 ANR

**堆栈**：
```
dagger.internal.DoubleCheck.get(DoubleCheck.java:44)
→ createTaskUseCase → taskUseCases → ViewModelCImpl.SwitchingProvider
→ HiltViewModelFactory → viewModel() → NavHost composable
```

**根因**：`SyncRepositoryImpl` 的 `init` 块：
```kotlin
init {
    kotlinx.coroutines.runBlocking {
        syncPreferences.lastSyncTimestamp.collect { timestamp ->
            lastSyncTimestamp = timestamp
        }
    }
}
```

两个致命错误叠加：
1. **`runBlocking` 在主线程**：`SyncRepositoryImpl` 是 `@Singleton`，首次解析时在主线程执行构造函数。`runBlocking` 阻塞主线程。
2. **`collect` 永不返回**：DataStore 的 `Flow<Long?>` 是无限流，`collect` 永远不会结束。两个叠加 = 主线程永久阻塞。

**依赖链**：`ViewModel` 创建 → 需要 `TaskUseCases` → 需要 `CreateTaskUseCase` → 需要 `SyncRepository` → 触发 `SyncRepositoryImpl` 构造 → `runBlocking` + `collect` → 永久阻塞。

## 修改思路

### Glance Widget

1. **降级到 1.0.0**：`glance:1.0.0` + `glance-appwidget:1.0.0` + `glance-material3:1.0.0`
2. **补全子包导入**：
   ```kotlin
   import androidx.glance.action.clickable
   import androidx.glance.appwidget.action.actionStartActivity
   ```
3. **去掉 Preferences，改直查数据库**：
   ```kotlin
   override suspend fun provideGlance(context: Context, id: GlanceId) {
       val tasks = withContext(Dispatchers.IO) {
           val db = TaskDatabase.getDatabase(context)
           db.taskDao().getTodayTasksAsList()
       }
       provideContent { WidgetContent(tasks) }
   }
   ```
   直接在 `provideGlance`（suspend 函数）里查 Room，数据通过参数传给 composable，完全绕开 GlanceState/Preferences 机制。

4. **简化 WidgetUpdateWorker**：去掉数据库查询逻辑，只调 `widget.updateAll()`，让 Widget 自己查。

### ANR 修复

1. **删除 `init` 块**：`lastSyncTimestamp` 默认 `null` 就行，null 代表"首次同步"。
2. **懒加载**：在 `sync()` 和 `fullSync()` 开始时才从 DataStore 读取：
   ```kotlin
   if (lastSyncTimestamp == null) {
       lastSyncTimestamp = syncPreferences.lastSyncTimestamp.first()
   }
   ```
   `.first()` 是 suspend 函数，只取第一个值就返回，不会无限挂起。

## 涉及文件

| 文件 | 改动 |
|------|------|
| `widget/TaskListWidget.kt` | 重写：去掉 Preferences，直查数据库，补全子包导入 |
| `widget/WidgetUpdateWorker.kt` | 简化：只调 updateAll() |
| `data/repository/SyncRepositoryImpl.kt` | 删除 init 块，sync/fullSync 内懒加载 lastSyncTimestamp |
| `build.gradle.kts` | Glance 版本从 1.1.0 降级到 1.0.0，加 glance 基础依赖 |

## 经验总结

### 1. `runBlocking` + 无限 Flow = 永久 ANR

这是最严重的坑。DataStore 的 `.data` 返回的是 `SharedFlow`，永不结束。用 `collect` 收集它放在 `runBlocking` 里，等于永久阻塞当前线程。

**正确做法**：
- 一次性读取用 `.first()`（suspend，取第一个值后返回）
- 持续观察用 `lifecycleScope.launch { flow.collect {} }`
- 构造函数/init 块里**绝对不能**有 `runBlocking`

**面试话术**：我在同步系统里踩了一个 ANR 的坑——`SyncRepository` 是 `@Singleton`，首次解析在主线程。它的 `init` 块用了 `runBlocking` + `collect` 去读 DataStore 的 Flow，但 DataStore Flow 是无限流永远不会结束，导致主线程永久阻塞。修复方式是删掉 `init` 块，改成在首次 `sync()` 调用时用 `.first()` 懒加载。这体现了对协程生命周期的理解——`collect` 是挂起函数，不是阻塞函数，但包在 `runBlocking` 里就变成了主线程阻塞。

### 2. Glance 子包导入

Kotlin 的 `import pkg.*` 不会递归导入子包。`clickable` 在 `androidx.glance.action`，`actionStartActivity` 在 `androidx.glance.appwidget.action`，必须显式导入。

### 3. Glance 版本选择

国内 Aliyun 镜像可能缓存不完整的 AndroidX artifact。遇到 unresolved reference 时，先尝试：
1. 降级到上一个稳定版
2. 加基础模块依赖（如 `glance:1.0.0`）
3. 清缓存 rebuild（`./gradlew clean`）

### 4. Widget 数据获取策略

Glance Widget 获取数据有两种方式：
- **GlanceStateDefinition + Preferences**：官方推荐，但配置复杂，版本兼容性问题多
- **直查数据库**：在 `provideGlance`（suspend 函数）里直接查 Room，简单可靠

对于简单 Widget（只读展示），直查数据库更稳定。`provideGlance` 本身就是 suspend 函数，可以安全地做 IO 操作。
