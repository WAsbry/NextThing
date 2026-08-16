package com.nextthing.app.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.nextthing.app.data.local.dao.CategoryDao
import com.nextthing.app.data.local.dao.TaskDao
import com.nextthing.app.data.local.dao.LocationDao
import com.nextthing.app.data.local.dao.NotificationStrategyDao
import com.nextthing.app.data.local.dao.UserDao
import com.nextthing.app.data.local.dao.GeofenceConfigDao
import com.nextthing.app.data.local.dao.GeofenceLocationDao
import com.nextthing.app.data.local.dao.TaskGeofenceDao
import com.nextthing.app.data.local.dao.AchievementDao
import com.nextthing.app.data.local.dao.GeofenceLocationStatisticsHistoryDao
import com.nextthing.app.data.local.dao.StartupTraceDao
import com.nextthing.app.data.local.entity.AchievementEntity
import com.nextthing.app.data.local.entity.CategoryEntity
import com.nextthing.app.data.local.entity.TaskEntity
import com.nextthing.app.data.local.entity.LocationEntity
import com.nextthing.app.data.local.entity.NotificationStrategyEntity
import com.nextthing.app.data.local.entity.UserEntity
import com.nextthing.app.data.local.entity.GeofenceConfigEntity
import com.nextthing.app.data.local.entity.GeofenceLocationEntity
import com.nextthing.app.data.local.entity.TaskGeofenceEntity
import com.nextthing.app.data.local.entity.GeofenceLocationStatisticsHistoryEntity
import com.nextthing.app.performance.StartupTraceEntity
import com.nextthing.app.data.local.converter.Converters
import com.nextthing.app.domain.model.PresetCategories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Database(
    entities = [
        TaskEntity::class,
        CategoryEntity::class,
        LocationEntity::class,
        NotificationStrategyEntity::class,
        UserEntity::class,
        GeofenceConfigEntity::class,
        GeofenceLocationEntity::class,
        TaskGeofenceEntity::class,
        GeofenceLocationStatisticsHistoryEntity::class,
        AchievementEntity::class,
        StartupTraceEntity::class
    ],
    version = 13,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun locationDao(): LocationDao
    abstract fun notificationStrategyDao(): NotificationStrategyDao
    abstract fun userDao(): UserDao
    abstract fun geofenceConfigDao(): GeofenceConfigDao
    abstract fun geofenceLocationDao(): GeofenceLocationDao
    abstract fun taskGeofenceDao(): TaskGeofenceDao
    abstract fun achievementDao(): AchievementDao
    abstract fun geofenceLocationStatisticsHistoryDao(): GeofenceLocationStatisticsHistoryDao
    abstract fun startupTraceDao(): StartupTraceDao

    companion object {
        const val DATABASE_NAME = "tasks_database"

        @Volatile
        private var INSTANCE: TaskDatabase? = null

        // 数据库迁移：Version 12 -> Version 13
        // 补齐重复任务实例唯一索引，避免并发 Worker 为同一模板、同一时刻生成重复实例。
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                timber.log.Timber.tag("Migration").d("开始数据库迁移：Version 12 -> 13")
                database.execSQL("""
                    DELETE FROM tasks
                    WHERE templateTaskId IS NOT NULL
                      AND instanceDate IS NOT NULL
                      AND rowid NOT IN (
                          SELECT MIN(rowid)
                          FROM tasks
                          WHERE templateTaskId IS NOT NULL AND instanceDate IS NOT NULL
                          GROUP BY templateTaskId, instanceDate
                      )
                """)
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_tasks_templateTaskId_instanceDate
                    ON tasks(templateTaskId, instanceDate)
                """)
                timber.log.Timber.tag("Migration").d("数据库迁移完成：Version 12 -> 13")
            }
        }

        // 数据库迁移：Version 11 -> Version 12
        // 添加启动打点表
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                timber.log.Timber.tag("Migration").d("开始数据库迁移：Version 11 -> 12")
                try {
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS startup_traces (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            sessionId TEXT NOT NULL,
                            checkpoint TEXT NOT NULL,
                            timestampMs INTEGER NOT NULL,
                            elapsedFromProcessStart INTEGER NOT NULL,
                            createdAt INTEGER NOT NULL
                        )
                    """)
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_startup_traces_sessionId ON startup_traces(sessionId)")
                    timber.log.Timber.tag("Migration").d("数据库迁移完成：Version 11 -> 12")
                } catch (e: Exception) {
                    timber.log.Timber.tag("Migration").e(e, "数据库迁移失败")
                    throw e
                }
            }
        }

        // 数据库迁移：Version 10 -> Version 11
        // 添加软删除字段（deleted）用于同步
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                timber.log.Timber.tag("Migration").d("开始数据库迁移：Version 10 -> 11")
                try {
                    database.execSQL("ALTER TABLE tasks ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE categories ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
                    timber.log.Timber.tag("Migration").d("数据库迁移完成：Version 10 -> 11")
                } catch (e: Exception) {
                    timber.log.Timber.tag("Migration").e(e, "数据库迁移失败")
                    throw e
                }
            }
        }

        // 数据库迁移：Version 9 -> Version 10
        // 添加数据同步相关字段到 tasks 和 categories 表
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                timber.log.Timber.tag("Migration").d("开始数据库迁移：Version 9 -> 10")
                try {
                    // 为 tasks 表添加同步字段
                    database.execSQL("""
                        ALTER TABLE tasks
                        ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'
                    """)
                    database.execSQL("""
                        ALTER TABLE tasks
                        ADD COLUMN serverUpdatedAt INTEGER
                    """)
                    database.execSQL("""
                        ALTER TABLE tasks
                        ADD COLUMN syncError TEXT
                    """)
                    timber.log.Timber.tag("Migration").d("✅ 添加 tasks 同步字段成功")

                    // 为 tasks 表创建 syncStatus 索引
                    database.execSQL("""
                        CREATE INDEX IF NOT EXISTS index_tasks_syncStatus ON tasks(syncStatus)
                    """)
                    timber.log.Timber.tag("Migration").d("✅ 创建 tasks syncStatus 索引成功")

                    // 为 categories 表添加同步字段
                    database.execSQL("""
                        ALTER TABLE categories
                        ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'
                    """)
                    database.execSQL("""
                        ALTER TABLE categories
                        ADD COLUMN serverUpdatedAt INTEGER
                    """)
                    timber.log.Timber.tag("Migration").d("✅ 添加 categories 同步字段成功")

                    timber.log.Timber.tag("Migration").d("✅✅✅ 数据库迁移完成：Version 9 -> 10")
                } catch (e: Exception) {
                    timber.log.Timber.tag("Migration").e(e, "❌ 数据库迁移失败")
                    throw e
                }
            }
        }

        // 数据库迁移：Version 8 -> Version 9
        // 添加成就系统表
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                timber.log.Timber.tag("Migration").d("开始数据库迁移：Version 8 -> 9")
                try {
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS achievements (
                            id TEXT NOT NULL PRIMARY KEY,
                            isUnlocked INTEGER NOT NULL DEFAULT 0,
                            unlockedAt INTEGER
                        )
                    """)
                    timber.log.Timber.tag("Migration").d("✅✅✅ 数据库迁移完成：Version 8 -> 9")
                } catch (e: Exception) {
                    timber.log.Timber.tag("Migration").e(e, "❌ 数据库迁移失败")
                    throw e
                }
            }
        }

        // 数据库迁移：Version 7 -> Version 8
        // 为 task_geofences 表添加 geofenceDeferCount 字段（围栏外延期次数）
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                timber.log.Timber.tag("Migration").d("开始数据库迁移：Version 7 -> 8")
                try {
                    database.execSQL("""
                        ALTER TABLE task_geofences
                        ADD COLUMN geofenceDeferCount INTEGER NOT NULL DEFAULT 0
                    """)
                    timber.log.Timber.tag("Migration").d("✅✅✅ 数据库迁移完成：Version 7 -> 8")
                } catch (e: Exception) {
                    timber.log.Timber.tag("Migration").e(e, "❌ 数据库迁移失败")
                    throw e
                }
            }
        }

        // 数据库迁移：Version 6 -> Version 7
        // 为通知策略添加提前提醒时间字段
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                timber.log.Timber.tag("Migration").d("开始数据库迁移：Version 6 -> 7")
                try {
                    database.execSQL("""
                        ALTER TABLE notification_strategies
                        ADD COLUMN advanceReminderMinutes TEXT NOT NULL DEFAULT '[]'
                    """)
                    timber.log.Timber.tag("Migration").d("✅✅✅ 数据库迁移完成：Version 6 -> 7")
                } catch (e: Exception) {
                    timber.log.Timber.tag("Migration").e(e, "❌ 数据库迁移失败")
                    throw e
                }
            }
        }

        // 数据库迁移：Version 5 -> Version 6
        // 添加地理围栏月度统计历史表
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                timber.log.Timber.tag("Migration").d("开始数据库迁移：Version 5 -> 6")

                try {
                    // 创建地理围栏月度统计历史表
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS geofence_location_statistics_history (
                            id TEXT NOT NULL PRIMARY KEY,
                            geofenceLocationId TEXT NOT NULL,
                            month TEXT NOT NULL,
                            checkCount INTEGER NOT NULL,
                            hitCount INTEGER NOT NULL,
                            hitRate REAL NOT NULL,
                            createdAt TEXT NOT NULL,
                            FOREIGN KEY (geofenceLocationId) REFERENCES geofence_locations(id) ON DELETE CASCADE
                        )
                    """)
                    timber.log.Timber.tag("Migration").d("✅ 创建 geofence_location_statistics_history 表成功")

                    // 创建索引以加快查询
                    database.execSQL("""
                        CREATE INDEX IF NOT EXISTS index_geofence_location_statistics_history_locationId
                        ON geofence_location_statistics_history(geofenceLocationId)
                    """)
                    timber.log.Timber.tag("Migration").d("✅ 创建 geofenceLocationId 索引成功")

                    database.execSQL("""
                        CREATE INDEX IF NOT EXISTS index_geofence_location_statistics_history_month
                        ON geofence_location_statistics_history(month)
                    """)
                    timber.log.Timber.tag("Migration").d("✅ 创建 month 索引成功")

                    // 创建复合唯一索引，确保每个地点每个月只有一条记录
                    database.execSQL("""
                        CREATE UNIQUE INDEX IF NOT EXISTS index_geofence_location_statistics_history_unique
                        ON geofence_location_statistics_history(geofenceLocationId, month)
                    """)
                    timber.log.Timber.tag("Migration").d("✅ 创建唯一索引成功")

                    timber.log.Timber.tag("Migration").d("✅✅✅ 数据库迁移完成：Version 5 -> 6")
                } catch (e: Exception) {
                    timber.log.Timber.tag("Migration").e(e, "❌ 数据库迁移失败")
                    throw e
                }
            }
        }

        // 数据库迁移：Version 4 -> Version 5
        // 添加地理围栏统计字段
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                timber.log.Timber.tag("Migration").d("开始数据库迁移：Version 4 -> 5")

                try {
                    // 添加统计字段到 geofence_locations 表
                    database.execSQL("""
                        ALTER TABLE geofence_locations
                        ADD COLUMN monthlyCheckCount INTEGER NOT NULL DEFAULT 0
                    """)
                    timber.log.Timber.tag("Migration").d("✅ 添加 monthlyCheckCount 字段成功")

                    database.execSQL("""
                        ALTER TABLE geofence_locations
                        ADD COLUMN monthlyHitCount INTEGER NOT NULL DEFAULT 0
                    """)
                    timber.log.Timber.tag("Migration").d("✅ 添加 monthlyHitCount 字段成功")

                    database.execSQL("""
                        ALTER TABLE geofence_locations
                        ADD COLUMN lastStatisticsResetMonth TEXT
                    """)
                    timber.log.Timber.tag("Migration").d("✅ 添加 lastStatisticsResetMonth 字段成功")

                    timber.log.Timber.tag("Migration").d("✅✅✅ 数据库迁移完成：Version 4 -> 5")
                } catch (e: Exception) {
                    timber.log.Timber.tag("Migration").e(e, "❌ 数据库迁移失败")
                    throw e
                }
            }
        }

        // 数据库迁移：Version 3 -> Version 4
        // 添加地理围栏相关表
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                timber.log.Timber.tag("Migration").d("开始数据库迁移：Version 3 -> 4")

                try {
                    // 1. 创建地理围栏全局配置表（单例表）
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS geofence_config (
                            id TEXT NOT NULL PRIMARY KEY,
                            isGlobalEnabled INTEGER NOT NULL DEFAULT 0,
                            defaultRadius INTEGER NOT NULL DEFAULT 200,
                            locationAccuracyThreshold INTEGER NOT NULL DEFAULT 100,
                            autoRefreshInterval INTEGER NOT NULL DEFAULT 300,
                            batteryOptimization INTEGER NOT NULL DEFAULT 1,
                            notifyWhenOutside INTEGER NOT NULL DEFAULT 0,
                            createdAt TEXT NOT NULL,
                            updatedAt TEXT NOT NULL
                        )
                    """)
                    timber.log.Timber.tag("Migration").d("✅ 创建 geofence_config 表成功")

                    // 2. 插入默认配置记录
                    val currentTime = LocalDateTime.now().toString()
                    database.execSQL("""
                        INSERT INTO geofence_config (
                            id, isGlobalEnabled, defaultRadius, locationAccuracyThreshold,
                            autoRefreshInterval, batteryOptimization, notifyWhenOutside,
                            createdAt, updatedAt
                        )
                        VALUES (
                            'default', 0, 200, 100, 300, 1, 0,
                            '$currentTime', '$currentTime'
                        )
                    """)
                    timber.log.Timber.tag("Migration").d("✅ 插入默认配置成功")

                    // 3. 创建地理围栏地点表
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS geofence_locations (
                            id TEXT NOT NULL PRIMARY KEY,
                            locationId TEXT NOT NULL,
                            customRadius INTEGER,
                            isFrequent INTEGER NOT NULL DEFAULT 0,
                            usageCount INTEGER NOT NULL DEFAULT 0,
                            lastUsed TEXT,
                            createdAt TEXT NOT NULL,
                            updatedAt TEXT NOT NULL,
                            FOREIGN KEY (locationId) REFERENCES locations(id) ON DELETE CASCADE
                        )
                    """)
                    timber.log.Timber.tag("Migration").d("✅ 创建 geofence_locations 表成功")

                    // 4. 创建地理围栏地点表的索引
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_geofence_locations_locationId ON geofence_locations(locationId)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_geofence_locations_isFrequent ON geofence_locations(isFrequent)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_geofence_locations_usageCount ON geofence_locations(usageCount)")
                    timber.log.Timber.tag("Migration").d("✅ 创建 geofence_locations 索引成功")

                    // 5. 创建任务地理围栏关联表
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS task_geofences (
                            id TEXT NOT NULL PRIMARY KEY,
                            taskId TEXT NOT NULL,
                            geofenceLocationId TEXT NOT NULL,
                            radius INTEGER NOT NULL,
                            enabled INTEGER NOT NULL DEFAULT 1,
                            lastCheckTime TEXT,
                            lastCheckResult TEXT,
                            lastCheckDistance REAL,
                            lastCheckUserLatitude REAL,
                            lastCheckUserLongitude REAL,
                            createdAt TEXT NOT NULL,
                            updatedAt TEXT NOT NULL,
                            FOREIGN KEY (taskId) REFERENCES tasks(id) ON DELETE CASCADE,
                            FOREIGN KEY (geofenceLocationId) REFERENCES geofence_locations(id) ON DELETE CASCADE
                        )
                    """)
                    timber.log.Timber.tag("Migration").d("✅ 创建 task_geofences 表成功")

                    // 6. 创建任务地理围栏关联表的索引
                    database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_task_geofences_taskId ON task_geofences(taskId)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_task_geofences_geofenceLocationId ON task_geofences(geofenceLocationId)")
                    timber.log.Timber.tag("Migration").d("✅ 创建 task_geofences 索引成功")

                    timber.log.Timber.tag("Migration").d("✅✅✅ 数据库迁移完成：Version 3 -> 4")
                } catch (e: Exception) {
                    timber.log.Timber.tag("Migration").e(e, "❌ 数据库迁移失败")
                    throw e
                }
            }
        }

        // 数据库迁移：Version 2 -> Version 3
        // 添加重复任务相关字段
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                timber.log.Timber.tag("Migration").d("开始数据库迁移：Version 2 -> 3")

                try {
                    // 添加新字段
                    database.execSQL("ALTER TABLE tasks ADD COLUMN isTemplate INTEGER NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE tasks ADD COLUMN templateTaskId TEXT")
                    database.execSQL("ALTER TABLE tasks ADD COLUMN instanceDate TEXT")

                    // 创建索引
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_templateTaskId ON tasks(templateTaskId)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_instanceDate ON tasks(instanceDate)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_isTemplate ON tasks(isTemplate)")

                    timber.log.Timber.tag("Migration").d("✅✅✅ 数据库迁移完成：Version 2 -> 3")
                } catch (e: Exception) {
                    timber.log.Timber.tag("Migration").e(e, "❌ 数据库迁移失败")
                    throw e
                }
            }
        }

        // 数据库迁移：Version 1 -> Version 2
        // 添加分类表，并将任务表的 category 字段迁移到 categoryId
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                timber.log.Timber.tag("Migration").d("开始数据库迁移：Version 1 -> 2")

                try {
                    // 1. 创建新的分类表
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS categories (
                            id TEXT NOT NULL PRIMARY KEY,
                            name TEXT NOT NULL,
                            type INTEGER NOT NULL,
                            icon TEXT NOT NULL,
                            colorHex TEXT NOT NULL,
                            sortOrder INTEGER NOT NULL,
                            createdAt TEXT NOT NULL,
                            isEnabled INTEGER NOT NULL DEFAULT 1
                        )
                    """)
                    timber.log.Timber.tag("Migration").d("✅ 创建分类表成功")

                    // 2. 插入预置分类（工作、生活）
                    val currentTime = java.time.LocalDateTime.now().toString()
                    database.execSQL("""
                        INSERT INTO categories (id, name, type, icon, colorHex, sortOrder, createdAt, isEnabled)
                        VALUES ('preset_work', '工作', 0, 'laptop-code', '#42A5F5', 0, '$currentTime', 1)
                    """)
                    database.execSQL("""
                        INSERT INTO categories (id, name, type, icon, colorHex, sortOrder, createdAt, isEnabled)
                        VALUES ('preset_life', '生活', 1, 'dumbbell', '#66BB6A', 1, '$currentTime', 1)
                    """)
                    timber.log.Timber.tag("Migration").d("✅ 插入预置分类成功")

                    // 3. 创建临时任务表（新结构）
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS tasks_new (
                            id TEXT NOT NULL PRIMARY KEY,
                            title TEXT NOT NULL,
                            description TEXT NOT NULL,
                            categoryId TEXT NOT NULL,
                            status TEXT NOT NULL,
                            createdAt TEXT NOT NULL,
                            updatedAt TEXT NOT NULL,
                            dueDate TEXT,
                            completedAt TEXT,
                            tags TEXT NOT NULL,
                            isUrgent INTEGER NOT NULL,
                            estimatedDuration INTEGER NOT NULL,
                            actualDuration INTEGER NOT NULL,
                            subtasksJson TEXT NOT NULL,
                            imageUri TEXT,
                            repeatFrequencyJson TEXT NOT NULL DEFAULT '{}',
                            locationInfoJson TEXT,
                            importanceUrgencyJson TEXT,
                            notificationStrategyId TEXT,
                            FOREIGN KEY (categoryId) REFERENCES categories(id) ON DELETE RESTRICT
                        )
                    """)
                    timber.log.Timber.tag("Migration").d("✅ 创建临时任务表成功")

                    // 4. 创建索引
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_new_categoryId ON tasks_new(categoryId)")

                    // 5. 迁移数据：将旧的 category 枚举映射到新的 categoryId
                    // 映射关系：WORK -> preset_work, LIFE -> preset_life, 其他 -> preset_life（默认）
                    database.execSQL("""
                        INSERT INTO tasks_new (
                            id, title, description, categoryId, status, createdAt, updatedAt,
                            dueDate, completedAt, tags, isUrgent, estimatedDuration, actualDuration,
                            subtasksJson, imageUri, repeatFrequencyJson, locationInfoJson,
                            importanceUrgencyJson, notificationStrategyId
                        )
                        SELECT
                            id, title, description,
                            CASE
                                WHEN category = 'WORK' THEN 'preset_work'
                                WHEN category = 'LIFE' THEN 'preset_life'
                                ELSE 'preset_life'
                            END as categoryId,
                            status, createdAt, updatedAt, dueDate, completedAt, tags,
                            isUrgent, estimatedDuration, actualDuration, subtasksJson, imageUri,
                            repeatFrequencyJson, locationInfoJson, importanceUrgencyJson, notificationStrategyId
                        FROM tasks
                    """)
                    timber.log.Timber.tag("Migration").d("✅ 迁移任务数据成功")

                    // 6. 删除旧表
                    database.execSQL("DROP TABLE tasks")
                    timber.log.Timber.tag("Migration").d("✅ 删除旧任务表成功")

                    // 7. 重命名新表
                    database.execSQL("ALTER TABLE tasks_new RENAME TO tasks")
                    timber.log.Timber.tag("Migration").d("✅ 重命名任务表成功")

                    timber.log.Timber.tag("Migration").d("✅✅✅ 数据库迁移完成：Version 1 -> 2")
                } catch (e: Exception) {
                    timber.log.Timber.tag("Migration").e(e, "❌ 数据库迁移失败")
                    throw e
                }
            }
        }

        fun getDatabase(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                timber.log.Timber.tag("DataFlow").d("━━━━━━ 初始化数据库 ━━━━━━")
                timber.log.Timber.tag("DataFlow").d("数据库名称: $DATABASE_NAME")
                timber.log.Timber.tag("DataFlow").d("数据库版本: 12 (启动打点)")
                timber.log.Timber.tag("DataFlow").d("数据库路径: ${context.applicationContext.getDatabasePath(DATABASE_NAME).absolutePath}")

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            timber.log.Timber.tag("DataFlow").d("✅ 数据库首次创建完成 (Version 8)")

                            // 异步初始化预置分类
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    // 等待instance构建完成后再使用
                                    val db = INSTANCE ?: return@launch
                                    val categoryDao = db.categoryDao()
                                    val presetCategories = PresetCategories.getDefaultCategories()

                                    presetCategories.forEach { category ->
                                        val entity = CategoryEntity(
                                            id = category.id,
                                            name = category.name,
                                            type = category.type.value,
                                            icon = category.icon,
                                            colorHex = category.colorHex,
                                            sortOrder = category.sortOrder,
                                            createdAt = category.createdAt,
                                            isEnabled = category.isEnabled
                                        )
                                        categoryDao.insertCategory(entity)
                                    }

                                    timber.log.Timber.tag("DataFlow").d("✅ 预置分类初始化完成：${presetCategories.map { it.name }}")
                                } catch (e: Exception) {
                                    timber.log.Timber.tag("DataFlow").e(e, "❌ 预置分类初始化失败")
                                }
                            }
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            timber.log.Timber.tag("DataFlow").d("✅ 数据库已打开，版本: ${db.version}")

                            // 查询任务数量
                            val cursor = db.query("SELECT COUNT(*) FROM tasks")
                            if (cursor.moveToFirst()) {
                                val count = cursor.getInt(0)
                                timber.log.Timber.tag("DataFlow").d("📊 数据库中任务数量: $count")
                            }
                            cursor.close()

                            // 查询分类数量
                            val categoryCursor = db.query("SELECT COUNT(*) FROM categories")
                            if (categoryCursor.moveToFirst()) {
                                val count = categoryCursor.getInt(0)
                                timber.log.Timber.tag("DataFlow").d("📊 数据库中分类数量: $count")
                            }
                            categoryCursor.close()
                        }
                    })
                    // 注意：已移除 fallbackToDestructiveMigration，使用安全的迁移策略
                    .build()

                timber.log.Timber.tag("DataFlow").d("✅ 数据库实例创建完成")
                INSTANCE = instance
                instance
            }
        }
    }
}
