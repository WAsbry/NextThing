# Hilt 编译错误修复（kapt → KSP 迁移遗留问题）

## 问题背景

项目从 kapt 迁移到 KSP 后，编译报多个错误：`Unresolved reference`、`MissingBinding`、`ASM transform failed`，涉及多个文件。

## 问题根源

### 1. Kotlin 扩展函数调用语法错误

```kotlin
// 错误：Kotlin 不支持用全限定名调用扩展函数
kotlinx.coroutines.flow.firstOrNull(tokenManager.accessToken)

// 正确：import 后用 receiver.method() 语法
import kotlinx.coroutines.flow.firstOrNull
tokenManager.accessToken.firstOrNull()
```

这个错误是迁移过程中手动改写时引入的，kapt 时代不存在这个问题。

### 2. Hilt MissingBinding：Context 缺少 @ApplicationContext

```kotlin
// 错误：Hilt 不知道注入哪个 Context（Application? Activity?）
class CategoryPreferencesManagerImpl @Inject constructor(
    private val context: Context
)

// 正确：用 @ApplicationContext 限定符明确注入 Application Context
class CategoryPreferencesManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
)
```

### 3. Hilt MissingBinding：data class 缺少 @Inject constructor

```kotlin
// 错误：Hilt 无法构造 UserUseCases
data class UserUseCases(
    val createUser: CreateUser,
    // ...
)

// 正确：添加 @Inject 让 Hilt 知道如何注入依赖
data class UserUseCases @Inject constructor(
    val createUser: CreateUser,
    // ...
)
```

### 4. BroadcastReceiver 的 Hilt ASM transform 失败

Hilt 2.48.1 + KSP 存在已知 Bug：不会为 `BroadcastReceiver` 生成 `Hilt_BroadcastReceiver` 基类，导致 ASM transform 阶段失败。

```kotlin
// 错误：@AndroidEntryPoint 在 KSP 模式下不生成基类
@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() { ... }

// 正确：用 @EntryPoint + EntryPointAccessors 手动获取依赖
@EntryPoint
@InstallIn(SingletonComponent::class)
interface GeofenceEntryPoint {
    fun taskGeofenceRepository(): TaskGeofenceRepository
    fun geofenceLocationRepository(): GeofenceLocationRepository
    // ...
}

// 在 onReceive() 中手动获取
val entryPoint = EntryPointAccessors.fromApplication(context, GeofenceEntryPoint::class.java)
val repository = entryPoint.taskGeofenceRepository()
```

### 5. Gradle 仓库顺序导致 KSP 插件 502

阿里云镜像对 KSP 插件返回 502，需要把官方仓库排在镜像前面。

```kotlin
// settings.gradle.kts 仓库顺序
repositories {
    gradlePluginPortal()  // 官方优先
    google()
    mavenCentral()
    maven { url = uri("https://maven.aliyun.com/...") }  // 镜像放后面
}
```

## 修改思路

逐个修复编译错误，原则是 **找到根本原因而不是绕过**：
- 语法错误 → 修正调用方式
- Hilt 绑定缺失 → 添加正确的注解/限定符
- Hilt + KSP 已知 Bug → 用 @EntryPoint 模式替代 @AndroidEntryPoint
- 依赖下载失败 → 调整仓库顺序

## 经验总结

1. **kapt → KSP 迁移不是无缝的**：Hilt 2.48.1 + KSP 对 BroadcastReceiver、ContentProvider 等少用组件有兼容性问题，迁移后需要逐个验证。

2. **@EntryPoint 模式**：当 `@AndroidEntryPoint` 不生效时的标准替代方案。定义一个 `@EntryPoint @InstallIn` 接口，通过 `EntryPointAccessors` 手动获取依赖。适用于 BroadcastReceiver、Worker 等不受 Hilt 管理生命周期的组件。

3. **Hilt 依赖注入的 Context 限定符**：Hilt 可注入的 Context 有两种——Application Context 和 Activity Context，必须用 `@ApplicationContext` 或 `@ActivityContext` 限定符明确指定，否则编译报 MissingBinding。

4. **Gradle 仓库顺序很重要**：Gradle 按声明顺序依次尝试仓库，排在前面的优先。镜像站可能对某些依赖不完整（返回 502），官方源应排在前面作为主力。

5. **面试话术角度**：可以讲"我们项目从 kapt 迁移到 KSP 的过程中遇到了 Hilt 兼容性问题，我是如何定位和解决的"——展示迁移经验和问题排查能力。
