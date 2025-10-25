# NextThing - 最好的开源待办软件



**现代化的 Android 个人任务管理应用**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android)](https://www.android.com/)  
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin)](https://kotlinlang.org/)  
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)  
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)  
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)

[功能特性](#-功能特性) • [技术栈](#-技术栈) • [架构设计](#-架构设计) • [快速开始](#-快速开始) • [下载](#-下载)

## 📱 应用概述

**NextThing** 是一款面向个人用户的现代化任务管理应用，采用 Clean Architecture 架构模式和最新的 Android 开发技术栈构建。应用融合了艾森豪威尔矩阵（重要-紧急四象限）、位置提醒等多种效率管理方法，为用户提供科学、高效的任务管理解决方案。

### 🎯 应用定位

- **目标用户**：需要高效管理个人任务、提升工作效率的用户
- **核心价值**：通过智能化的任务分类、可视化的数据统计、位置感知提醒等功能，帮助用户更好地规划和执行任务
- **技术特色**：Clean Architecture + MVVM + Flow，确保代码可维护性、可测试性和可扩展性

---  

## 🌟 核心技术亮点总结

1. **Clean Architecture 架构**：Domain、Data、Presentation 三层分离，职责清晰
2. **响应式编程**：Kotlin Flow + StateFlow 实现数据流自动更新
3. **依赖注入**：Hilt 全局统一管理依赖，支持 ViewModel、WorkManager 注入
4. **数据库设计**：Room 数据库 + 复杂类型转换器 + 版本迁移
5. **位置服务双引擎**：Google Play Services + 高德 SDK 智能切换
6. **后台任务调度**：WorkManager 可靠的定时任务（逾期检测、延期转换）
7. **权限管理**：智能权限请求 + 状态监听 + 降级方案
8. **Compose 声明式 UI**：完全使用 Jetpack Compose 构建，Material 3 规范
9. **协程优化**：超时控制、缓存机制、静默更新策略
10. **可测试性**：Use Case 封装业务逻辑，便于单元测试和 Mock

---  

## ✨ 核心功能

### 1. 📅 今日视图（TodayScreen）
**智能的今日任务管理中心**

- **任务状态管理**：支持 5 种任务状态（待办、已完成、延期、逾期、放弃）
- **实时进度追踪**：动态展示今日任务完成率和剩余任务数
- **智能位置服务**：
  - 自动位置获取与缓存（5分钟缓存机制）
  - 支持 Google Play Services 和高德定位双引擎
  - 位置权限智能管理与引导
- **天气集成**：基于当前位置自动获取实时天气信息，**集成和风天气SDK**
- **手势操作**：
  - 左滑弹出任务快捷操作：标记任务完成、延期任务至次日、放弃当前任务
  - 点击查看任务详情

**技术亮点**：
- Flow 响应式数据流，自动同步任务状态变化
- 协程 + withTimeout 实现位置获取超时控制、智能缓存机制减少不必要的定位请求、权限状态实时监听与自动刷新

### 2. 📋 任务管理（TasksScreen）
**全面的任务组织与筛选系统**

- **多维度筛选**：
  - 按分类筛选：
    - 预置分类：工作、学习、生活、健康、个人
    - 新增分类：支持自定义
  - 按状态筛选：全部、待办、已完成、延期、逾期、放弃
  - 按重要紧急程度筛选：四象限矩阵
- **自定义分类**：
  - 动态创建/删除/置顶分类
  - 分类使用频率自动排序
  - DataStore 持久化分类偏好
- **批量操作**：支持批量标记完成、批量删除等操作
- **搜索功能**：实时搜索任务标题和描述

**技术亮点**：
- Room Database 流式查询，实时响应数据变化
- 自定义 TypeConverter 处理复杂数据类型（重复频次、位置信息等）
- Repository 模式统一数据访问层
- Use Case 封装业务逻辑，提高代码复用性

### 3. ➕ 任务创建（CreateTaskScreen）
**功能完善的任务创建流程**

- **基础信息**：标题、描述、分类、截止日期、精确时间
- **高级功能**：
  - **重要紧急矩阵**：四象限分类（IMPORTANT_URGENT, IMPORTANT_NOT_URGENT 等）
  - **重复任务**：支持每日、每周（指定星期）、每月（指定日期）重复
  - **位置提醒**：选择已保存位置或创建新位置
  - **图片附件**：支持拍照或从相册选择图片
  - **通知策略**：自定义提醒声音、震动模式、**地理围栏**
- **智能表单**：
  - 自动保存上次选择的分类
  - 分类按使用频率智能排序
  - 表单验证与友好提示

**技术亮点**：
- Coil 图片加载库集成
- FileProvider 安全的文件共享
- DataStore Preferences 存储用户偏好
- ViewModel 状态管理与表单验证

### 4. 📊 数据统计（StatsScreen）
**多维度的任务数据可视化分析**

- **概览统计**：
  - 任务总数、完成数、待办数统计
  - 完成率可视化展示
  - 5 种状态分布（待办、已完成、延期、逾期、放弃）
  - 四象限任务分布统计
- **分类统计**：
  - 各分类任务数量与完成率
  - 平均完成时长分析
  - 分类效率对比
- **趋势分析**：
  - 近 7 日、近30日、近90日、全部任务创建与完成趋势
  - 周/月视图切换
  - 完成率变化曲线
- **效率分析**：
  - 按分类统计平均完成时长
  - 按重要程度统计完成效率
  - 准时完成率 vs 逾期完成率
  - 子任务完成度分析

**技术亮点**：
- Flow collectLatest 实现响应式统计更新
- 自定义 Composable 图表组件
- LocalDate/LocalDateTime 时间处理
- 复杂数据聚合与计算优化

### 6. ⚙️ 设置与用户管理

#### 设置页面（SettingsScreen）
- **用户信息**：头像、昵称、使用天数展示
- **功能开关**：
  - 位置信息增强开关
  - 地理围栏开关
  - 主题皮肤设置（规划中）
- **一键导航**：快速跳转用户信息页

#### 用户信息页（UserInfoScreen）
- **基础信息管理**：
  - 头像选择（原生图片选择器）
  - 昵称编辑
  - 8 位随机用户 ID
- **账号绑定**：
  - 手机号绑定/换绑
  - 微信账号绑定
  - QQ 账号绑定
- **账号操作**：
  - 退出登录
  - 注销账号（带二次确认）

#### 登录系统（LoginScreen）
- **首次启动引导**：首次使用需创建用户
- **用户创建**：输入昵称，自动生成 8 位随机 ID
- **状态持久化**：Room 数据库存储用户信息
- **自动登录**：下次启动自动识别用户

**技术亮点**：
- Room 数据库迁移（版本 6 → 7）
- Flow 实现用户状态响应式同步
- Activity Result API 集成图片选择
- Navigation 条件路由（根据登录状态决定起始页）

### 7. 📍 位置管理

#### 位置创建（CreateLocationScreen）
- 手动输入位置信息
- 地图选点（集成高德地图）
- 位置搜索与地址解析
- 位置保存与管理

#### 位置权限管理
- **智能权限请求**：首次使用时引导授权
- **权限状态监听**：实时检测权限变化
- **降级方案**：无权限时提供手动输入选项
- **帮助引导**：多层次的帮助对话框与操作提示

**技术亮点**：
- Google Play Services Location API
- 高德定位 SDK 双引擎支持
- 权限状态 BroadcastReceiver 监听
- 位置缓存与智能刷新策略

### 8. 🔔 通知策略（CreateNotificationStrategyScreen）
- **提醒音设置**：系统预置音频 + 自定义音频
- **震动模式**：多种震动模式可选
- **提醒时间**：支持多个提醒时间点
- **策略保存**：可复用的通知策略模板

**技术亮点**：
- MediaPlayer 音频预览播放
- Vibrator 震动反馈
- Room 数据库存储策略
- 自定义音频文件选择与管理

---  

## 🏗️ 技术架构

### 架构模式

本项目采用 **Clean Architecture（清晰架构）** + **MVVM** 设计模式，确保代码的可维护性、可测试性和可扩展性。

```  
app/src/main/java/com/example/nextthingb1/  
│  
├── presentation/          # 表现层（UI Layer）  
│   ├── screens/          # 各功能页面  
│   │   ├── today/        # 今日视图  
│   │   ├── tasks/        # 任务管理  
│   │   ├── create/       # 任务创建  
│   │   ├── stats/        # 数据统计  
│   │   ├── focus/        # 专注模式  
│   │   ├── settings/     # 设置页面  
│   │   ├── login/        # 登录页面  
│   │   └── userinfo/     # 用户信息  
│   ├── components/       # 可复用 Composable 组件  
│   ├── navigation/       # 导航配置  
│   └── theme/            # Material 3 主题  
│  
├── domain/               # 领域层（Domain Layer）  
│   ├── model/           # 领域模型（纯 Kotlin 类）  
│   ├── repository/      # Repository 接口定义  
│   ├── usecase/         # 业务逻辑用例  
│   └── service/         # 服务接口（定位、天气等）  
│  
├── data/                # 数据层（Data Layer）  
│   ├── local/           # 本地数据源  
│   │   ├── database/    # Room 数据库  
│   │   ├── dao/         # 数据访问对象  
│   │   ├── entity/      # 数据库实体  
│   │   └── converter/   # 类型转换器  
│   ├── remote/          # 远程数据源  
│   │   ├── api/         # Retrofit API 定义  
│   │   └── dto/         # 数据传输对象  
│   ├── repository/      # Repository 实现  
│   ├── mapper/          # Entity ↔ Domain Model 映射  
│   └── service/         # 服务实现（定位、天气等）  
│  
├── di/                  # 依赖注入（Dependency Injection）  
│   ├── DatabaseModule   # 数据库模块  
│   ├── NetworkModule    # 网络模块  
│   ├── LocationModule   # 定位模块  
│   ├── UseCaseModule    # 用例模块  
│   └── WeatherModule    # 天气模块  
│  
├── util/                # 工具类  
│   ├── PermissionHelper        # 权限管理  
│   ├── SyncScheduler          # 同步调度  
│   ├── AudioFileHelper        # 音频文件处理  
│   └── ToastHelper            # Toast 提示  
│  
└── work/                # 后台任务（WorkManager）  
    ├── SyncTasksWorker            # 任务同步 Worker    ├── CheckOverdueTasksWorker    # 逾期检测 Worker    ├── ConvertDelayedTasksWorker  # 延期转待办 Worker    └── TaskWorkScheduler          # 任务调度器  
```  

### 核心技术栈

#### 🎨 UI 层
- **Jetpack Compose**：声明式 UI 框架，完全使用 Compose 构建
- **Material Design 3**：遵循最新 Material You 设计规范
- **Navigation Compose**：类型安全的导航组件
- **Coil Compose**：高性能图片加载库

#### 🧠 业务逻辑层
- **Kotlin Coroutines**：协程异步编程
- **Kotlin Flow**：响应式数据流
- **Hilt/Dagger**：依赖注入框架
- **Use Case Pattern**：用例模式封装业务逻辑

#### 💾 数据持久化
- **Room Database**：SQLite 数据库 ORM
  - 版本：2.6.1
  - 当前数据库版本：7
  - 支持 Flow 查询、TypeConverter、Migration
- **DataStore Preferences**：键值对存储，替代 SharedPreferences
- **类型转换器**：
  - `LocalDateTime` ↔ `Long` 时间戳
  - `List<String>` ↔ `String` (JSON)
  - `TaskCategory` ↔ `String`
  - `TaskStatus` ↔ `String`
  - `RepeatFrequency` ↔ `String` (JSON)
  - `LocationInfo` ↔ `String` (JSON)
  - `TaskImportanceUrgency` ↔ `String` (JSON)

#### 🌐 网络层
- **Retrofit 2**：RESTful API 客户端
- **OkHttp 3**：HTTP 客户端与拦截器
- **Gson**：JSON 序列化/反序列化
- **Kotlinx Serialization**：Kotlin 原生序列化

#### 📍 位置服务
- **Google Play Services Location**：Google 位置服务 API
- **高德定位 SDK**：国内定位解决方案
- **双引擎策略**：自动选择可用定位引擎
- **位置缓存**：5 分钟缓存机制优化性能

#### ⏰ 后台任务
- **WorkManager**：可靠的后台任务调度
  - 每日凌晨 1:00 逾期任务检测
  - 每日凌晨 0:00 延期任务转待办
  - 周期性数据同步（15 分钟间隔）
- **Hilt WorkManager Integration**：WorkManager 依赖注入支持

#### 🛠️ 开发工具
- **Timber**：日志记录库
- **LeakCanary**：内存泄漏检测（Debug 构建）
- **JUnit + Mockito**：单元测试
- **Espresso + Compose Test**：UI 自动化测试

#### 🔐 安全加密
- **BouncyCastle**：EdDSA 加密算法支持（用于 JWT）
- **FileProvider**：安全的文件共享

---  

## 📂 项目结构

### 数据库设计

**Room Database - Version 7**

#### 1. Tasks 表（TaskEntity）
```kotlin  
@Entity(tableName = "tasks")  
data class TaskEntity(    @PrimaryKey val id: String,  
    val title: String,  
    val description: String,  
    val category: String,                    // TaskCategory  
    val status: String,                      // TaskStatus (5种状态)  
    val createdAt: Long,                     // 时间戳  
    val updatedAt: Long,  
    val dueDate: Long?,  
    val completedAt: Long?,  
    val tags: String,                        // JSON List<String>  
    val isUrgent: Boolean,  
    val estimatedDuration: Int,              // 分钟  
    val actualDuration: Int,  
    val subtasks: String,                    // JSON List<Subtask>  
    val imageUri: String?,  
    val repeatFrequencyJson: String,         // JSON RepeatFrequency  
    val locationInfoJson: String?,           // JSON LocationInfo  
    val importanceUrgencyJson: String?       // JSON TaskImportanceUrgency  
)  
```  

**5 种任务状态**：
- `PENDING`：待办（当天需处理）
- `COMPLETED`：已完成（终态）
- `DELAYED`：延期（手动延期至次日，次日自动转 PENDING）
- `OVERDUE`：逾期（截止时间已过且未完成）
- `CANCELLED`：放弃（终态）

#### 2. Locations 表（LocationEntity）
```kotlin  
@Entity(tableName = "locations")  
data class LocationEntity(    @PrimaryKey val id: String,  
    val locationName: String,  
    val address: String,  
    val latitude: Double,  
    val longitude: Double,  
    val createdAt: Long  
)  
```  

#### 3. NotificationStrategies 表（NotificationStrategyEntity）
```kotlin  
@Entity(tableName = "notification_strategies")  
data class NotificationStrategyEntity(    @PrimaryKey val id: String,  
    val name: String,  
    val audioUri: String?,  
    val vibratePattern: String,              // JSON LongArray  
    val remindTimes: String,                 // JSON List<String>  
    val createdAt: Long  
)  
```  

#### 4. Users 表（UserEntity）
```kotlin  
@Entity(tableName = "users")  
data class UserEntity(    @PrimaryKey val id: String,              // 8位随机字符串  
    val nickname: String,  
    val avatarUri: String?,  
    val phoneNumber: String?,  
    val wechatId: String?,  
    val qqId: String?,  
    val createdAt: Long,  
    val updatedAt: Long  
)  
```  

### 核心数据模型

#### Task Domain Model
```kotlin  
data class Task(    val id: String = UUID.randomUUID().toString(),  
    val title: String,  
    val description: String = "",  
    val category: TaskCategory = TaskCategory.WORK,  
    val status: TaskStatus = TaskStatus.PENDING,  
    val createdAt: LocalDateTime = LocalDateTime.now(),  
    val updatedAt: LocalDateTime = LocalDateTime.now(),  
    val dueDate: LocalDateTime? = null,  
    val completedAt: LocalDateTime? = null,  
    val tags: List<String> = emptyList(),  
    val isUrgent: Boolean = false,  
    val estimatedDuration: Int = 0,          // 分钟  
    val actualDuration: Int = 0,             // 分钟  
    val subtasks: List<Subtask> = emptyList(),  
    val imageUri: String? = null,  
    val repeatFrequency: RepeatFrequency = RepeatFrequency(),  
    val locationInfo: LocationInfo? = null,  
    val importanceUrgency: TaskImportanceUrgency? = null  
)  
```  

#### 艾森豪威尔矩阵（重要-紧急四象限）
```kotlin  
enum class TaskImportanceUrgency(    val displayName: String,  
    val description: String,  
    val colorHex: String,  
    val importance: TaskImportance,  
    val urgency: TaskUrgency  
) {  
    IMPORTANT_URGENT(  
        "重要且紧急",  
        "需要立即处理的重要事项",  
        "#F44336",        TaskImportance.IMPORTANT,        TaskUrgency.URGENT    ),    IMPORTANT_NOT_URGENT(  
        "重要但不紧急",  
        "重要的长期目标和计划",  
        "#FF9800",        TaskImportance.IMPORTANT,        TaskUrgency.NOT_URGENT    ),    NOT_IMPORTANT_URGENT(  
        "不重要但紧急",  
        "需要快速处理的事务性工作",  
        "#2196F3",        TaskImportance.NOT_IMPORTANT,        TaskUrgency.URGENT    ),    NOT_IMPORTANT_NOT_URGENT(  
        "不重要且不紧急",  
        "可以暂缓或委托的事项",  
        "#4CAF50",        TaskImportance.NOT_IMPORTANT,        TaskUrgency.NOT_URGENT    )}  
```  

#### 重复频次
```kotlin  
data class RepeatFrequency(    val type: RepeatFrequencyType = RepeatFrequencyType.NONE,  
    val weekdays: Set<Int> = emptySet(),      // 1-7 (周一到周日)  
    val monthDays: Set<Int> = emptySet()      // 1-31  
)  
  
enum class RepeatFrequencyType {    NONE,     // 不重复  
    DAILY,    // 每天  
    WEEKLY,   // 每周（指定星期几）  
    MONTHLY   // 每月（指定日期）  
}  
```  
  
---  

## 🔧 构建与运行

### 环境要求

- **Android Studio**：Hedgehog (2023.1.1) 或更高版本
- **JDK**：Java 17
- **Gradle**：8.0+
- **Kotlin**：1.9.22
- **Min SDK**：24 (Android 7.0)
- **Target SDK**：34 (Android 14)
- **Compile SDK**：34

### 依赖镜像配置

项目已配置国内镜像加速（阿里云、腾讯云），加快依赖下载速度。

**settings.gradle.kts**：
```kotlin  
dependencyResolutionManagement {  
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)    repositories {        maven { url = uri("https://maven.aliyun.com/repository/google") }        maven { url = uri("https://maven.aliyun.com/repository/public") }        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-public/") }        google()        mavenCentral()    }}  
```  

### 构建步骤

#### 1. 克隆项目
```bash  
git clone https://github.com/your-repo/NextThingB1.gitcd NextThingB1
```

#### 2. 配置签名（Release 构建）
项目已配置 Release 签名，如需自定义：

**app/build.gradle.kts**：
```kotlin  
signingConfigs {  
    create("release") {        
	    storeFile = file("your-keystore.jks") 
	    storePassword = "your-store-password"    
	    keyAlias = "your-key-alias" 
        keyPassword = "your-key-password"    
    }}  
```  

#### 3. 构建 APK

**Debug 构建**：
```bash  
./gradlew assembleDebug
``` 

**Release 构建**：
```bash  
./gradlew assembleRelease
```  

生成的 APK 位置：
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

#### 4. 运行测试

**单元测试**：
```bash  
./gradlew testDebugUnitTest
```  

**UI 测试（需连接设备/模拟器）**：
```bash  
./gradlew connectedDebugAndroidTest
```  

#### 5. 代码质量检查
```bash  
./gradlew lint
```  
  
---  

## 🚀 开发指南

### 添加新功能的步骤

#### 1. 创建 Domain Model（领域模型）
在 `domain/model/` 创建纯 Kotlin 数据类：
```kotlin  
// domain/model/YourModel.kt  
data class YourModel(  
    val id: String,    val name: String,    // ...)  
```  

#### 2. 创建 Room Entity（数据库实体）
在 `data/local/entity/` 创建数据库实体：
```kotlin  
// data/local/entity/YourEntity.kt  
@Entity(tableName = "your_table")  
data class YourEntity(  
    @PrimaryKey val id: String,    val name: String,    // ...)  
```  

#### 3. 创建 DAO（数据访问对象）
在 `data/local/dao/` 创建 DAO 接口：
```kotlin  
// data/local/dao/YourDao.kt  
@Dao  
interface YourDao {  
    @Query("SELECT * FROM your_table")    
    fun getAll(): Flow<List<YourEntity>>  
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: YourEntity)  
    
    @Delete    
    suspend fun delete(entity: YourEntity)}  
```  

#### 4. 更新 Database 版本
在 `TaskDatabase.kt` 添加实体和迁移：
```kotlin  
@Database(  
    entities = [        
	    TaskEntity::class,
        LocationEntity::class,
        YourEntity::class  // 新增  
    ],    
    version = 8,  // 版本号+1  
    exportSchema = false)  
abstract class TaskDatabase : RoomDatabase() {
    abstract fun yourDao(): YourDao    
    companion object {
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {  
                database.execSQL("""CREATE TABLE IF NOT EXISTS your_table(
                                    id TEXT PRIMARY KEY NOT NULL,
                                    name TEXT NOT NULL)"""
                                .trimIndent())  
            }        }    }}  
```  

#### 5. 创建 Mapper（映射器）
在 `data/mapper/` 创建转换函数：
```kotlin  
// data/mapper/YourMapper.kt  
fun YourEntity.toDomainModel(): YourModel {  
    return YourModel(        
	    id = this.id,        
	    name = this.name    
	    )
	}  
  
fun YourModel.toEntity(): YourEntity {  
    return YourEntity(
        id = this.id,
        name = this.name
        )
    }  
```  

#### 6. 创建 Repository（仓库）
在 `domain/repository/` 定义接口：
```kotlin  
// domain/repository/YourRepository.kt  
interface YourRepository {  
    fun getAll(): Flow<List<YourModel>>    
    
    suspend fun insert(model: YourModel): Result<Unit>    
    
    suspend fun delete(id: String): Result<Unit>
}  
```  

在 `data/repository/` 实现接口：
```kotlin  
// data/repository/YourRepositoryImpl.kt  
class YourRepositoryImpl @Inject constructor(  
    private val dao: YourDao) : YourRepository {  
    
    override fun getAll(): Flow<List<YourModel>> {
        return dao.getAll().map { entities -> 
           entities.map { it.toDomainModel() }        
       }    
    }
      
    override suspend fun insert(model: YourModel): Result<Unit> {
        return try {
            dao.insert(model.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {    
	        Result.failure(e)   
        }
    }  
    
    override suspend fun delete(id: String): Result<Unit> {
        // ...    }
    }  
```  

#### 7. 创建 Use Case（用例）
在 `domain/usecase/` 创建业务逻辑：
```kotlin  
// domain/usecase/YourUseCases.kt  
data class YourUseCases(  
    val getAll: GetAllYourModelsUseCase,    
    val create: CreateYourModelUseCase,    
    val delete: DeleteYourModelUseCase
)  
  
class GetAllYourModelsUseCase @Inject constructor(  
    private val repository: YourRepository) {  
    operator fun invoke(): Flow<List<YourModel>> {
        return repository.getAll()    
    }
}  
  
class CreateYourModelUseCase @Inject constructor(  
    private val repository: YourRepository) {  
    suspend operator fun invoke(name: String): Result<Unit> {
        val model = YourModel(
            id = UUID.randomUUID().toString(),
            name = name
        )
    return repository.insert(model)
    }
}  
```  

#### 8. 配置 Hilt Module
在 `di/` 添加 DI 配置：
```kotlin  
// di/DatabaseModule.kt  
@Module  
@InstallIn(SingletonComponent::class)  
object DatabaseModule {  
    @Provides    
    @Singleton
    fun provideYourDao(database: TaskDatabase): YourDao {
        return database.yourDao()
    }  
    
    @Provides
    @Singleton
    fun provideYourRepository(dao: YourDao): YourRepository {
        return YourRepositoryImpl(dao)    
    }
}  
  
// di/UseCaseModule.kt  
@Module  
@InstallIn(ViewModelComponent::class)  
object UseCaseModule {  
    @Provides    
    fun provideYourUseCases(repository: YourRepository): YourUseCases {
        return YourUseCases(
            getAll = GetAllYourModelsUseCase(repository),
            create = CreateYourModelUseCase(repository),
            delete = DeleteYourModelUseCase(repository)
        )
    }
}  
```  

#### 9. 创建 ViewModel
在 `presentation/screens/yourfeature/` 创建 ViewModel：
```kotlin  
// presentation/screens/yourfeature/YourViewModel.kt  
data class YourUiState(  
    val items: List<YourModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)  
  
@HiltViewModel  
class YourViewModel @Inject constructor(  
    private val useCases: YourUseCases) : ViewModel() {  
	    private val _uiState = MutableStateFlow(YourUiState())
        val uiState: StateFlow<YourUiState> = _uiState.asStateFlow()  
	    init {
	        loadItems()
	    }  
	    private fun loadItems() {
	        viewModelScope.launch {
	            _uiState.value = _uiState.value.copy(isLoading = true)  
	            useCases.getAll().collect { items ->
	                _uiState.value = _uiState.value.copy(
	                    items = items,
	                    isLoading = false           
	                )
	            }
	        }
	    }
	      
    fun createItem(name: String) {
        viewModelScope.launch {
            useCases.create(name).fold(
                onSuccess = { /* 成功处理 */ },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message
                    )
                }
            )
        }
    }
}  
```  

#### 10. 创建 Composable Screen
在 `presentation/screens/yourfeature/` 创建 UI：
```kotlin  
// presentation/screens/yourfeature/YourScreen.kt  
@Composable  
fun YourScreen(  
    viewModel: YourViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit) {  
	    val uiState by viewModel.uiState.collectAsState()  
	    Scaffold(
	        topBar = {
	            TopAppBar(
	                title = { Text("Your Feature") },
	                navigationIcon = {
	                    IconButton(onClick = onNavigateBack) {
	                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
	                    }
	                }
	            )
	        }
	    ) {
		    paddingValues ->
	        if (uiState.isLoading) {
	            CircularProgressIndicator()
	        } else {
	            LazyColumn(
	                modifier = Modifier.padding(paddingValues)
	            ) {
	                items(uiState.items) { item ->
	                    Text(text = item.name)
	            }
            }
        }
    }
}  
```  

#### 11. 添加 Navigation 路由
在 `NextThingNavigation.kt` 添加路由：
```kotlin  
// presentation/navigation/NextThingNavigation.kt  
sealed class Screen(val route: String) {  
    // ...    object YourFeature : Screen("your_feature")}  
  
@Composable  
fun NextThingNavigation(/* ... */) {  
    NavHost(/* ... */) {
        // ...
        composable(Screen.YourFeature.route) {
            val viewModel: YourViewModel = hiltViewModel()
            YourScreen(
                viewModel = viewModel,
                onNavigateBack = { 
	                navController.popBackStack() 
	            }
            )
        }
    }
}  
```  
  
---  

## 📝 代码规范

### Kotlin 编码规范
- 遵循 [Kotlin 官方编码规范](https://kotlinlang.org/docs/coding-conventions.html)
- 使用 4 空格缩进
- 变量命名：驼峰命名法（camelCase）
- 常量命名：大写下划线（UPPER_SNAKE_CASE）
- 类名：帕斯卡命名法（PascalCase）

### Compose 最佳实践
- Composable 函数使用 PascalCase 命名
- 优先使用 `remember` 和 `derivedStateOf` 优化重组
- 使用 `LaunchedEffect` 和 `DisposableEffect` 管理副作用
- 提取可复用的 Composable 到 `presentation/components/`

### 依赖注入规范
- 所有 ViewModel 使用 `@HiltViewModel` 注解
- Repository 和 Service 使用 `@Inject` 构造函数注入
- 模块使用 `@Module` + `@InstallIn` 注解

### Git 提交规范
```  
feat: 新功能  
fix: 修复 Bugdocs: 文档更新  
style: 代码格式调整（不影响功能）  
refactor: 重构代码  
test: 测试相关  
chore: 构建/工具链相关  
```  
  
---  

## 🧪 测试策略

### 单元测试
- **覆盖范围**：Use Case、ViewModel、Repository
- **工具**：JUnit 4、Mockito、Kotlin Coroutines Test
- **位置**：`app/src/test/java/`

示例：
```kotlin  
@Test  
fun `createTask should save task to repository`() = runTest {  
    // Given    val repository = mockk<TaskRepository>()    val useCase = CreateTaskUseCase(repository)  
    // When    useCase(title = "Test Task", category = TaskCategory.WORK)  
    // Then    coVerify { repository.insert(any()) }  
}  
```  

### UI 测试
- **覆盖范围**：Composable UI、用户交互流程
- **工具**：Compose Testing、Espresso
- **位置**：`app/src/androidTest/java/`

示例：
```kotlin  
@Test  
fun taskList_displaysCorrectly() {  
    composeTestRule.setContent {  
        TaskListScreen(tasks = listOf(testTask))  
    }  
  
    composeTestRule.onNodeWithText("Test Task").assertIsDisplayed()}  
```  
  
---  

## 🔄 WorkManager 后台任务

### 已实现的 Worker

#### 1. CheckOverdueTasksWorker（逾期检测）
- **触发时间**：每日凌晨 1:00
- **功能**：检测截止时间在昨天及之前且状态为 PENDING 的任务，标记为 OVERDUE
- **约束**：无需低电量限制

#### 2. ConvertDelayedTasksWorker（延期转待办）
- **触发时间**：每日凌晨 0:00:01
- **功能**：将状态为 DELAYED 的任务自动转为 PENDING
- **约束**：无需低电量限制

#### 3. SyncTasksWorker（数据同步）
- **触发时间**：每 15 分钟
- **功能**：与远程服务器同步任务数据（预留接口）
- **约束**：需要网络连接

### 调度器配置
```kotlin  
// NextThingApplication.kt  
override fun onCreate() {  
    super.onCreate()  
    // 定时逾期检测  
    TaskWorkScheduler.scheduleOverdueCheck(this)    
    TaskWorkScheduler.triggerImmediateOverdueCheck(this)  // 启动立即检测  
  
    // 定时延期转待办  
    TaskWorkScheduler.scheduleDelayedConversion(this)    
    TaskWorkScheduler.triggerImmediateDelayedConversion(this)  
    // 数据同步  
    SyncScheduler.schedulePeriodicSync(this)}  
```  

## 📄 License

```  
MIT License  
  
Copyright (c) 2024 NextThing  
  
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
  
---  

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

---  

## 📬 联系方式

- [**项目地址**](https://github.com/WAsbry/NextThing)
- [**问题反馈**](https://github.com/WAsbry/NextThing/issues)

---  


**⭐ 如果这个项目对你有帮助，请给一个 Star！⭐**  
   
