package com.example.nextthingb1.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.nextthingb1.data.local.dao.TaskDao
import com.example.nextthingb1.data.local.dao.LocationDao
import com.example.nextthingb1.data.local.dao.NotificationStrategyDao
import com.example.nextthingb1.data.local.dao.UserDao
import com.example.nextthingb1.data.local.entity.TaskEntity
import com.example.nextthingb1.data.local.entity.LocationEntity
import com.example.nextthingb1.data.local.entity.NotificationStrategyEntity
import com.example.nextthingb1.data.local.entity.UserEntity
import com.example.nextthingb1.data.local.converter.Converters

@Database(
    entities = [TaskEntity::class, LocationEntity::class, NotificationStrategyEntity::class, UserEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun locationDao(): LocationDao
    abstract fun notificationStrategyDao(): NotificationStrategyDao
    abstract fun userDao(): UserDao
    
    companion object {
        const val DATABASE_NAME = "tasks_database"

        @Volatile
        private var INSTANCE: TaskDatabase? = null

        fun getDatabase(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                timber.log.Timber.tag("DataFlow").d("━━━━━━ 初始化数据库 ━━━━━━")
                timber.log.Timber.tag("DataFlow").d("数据库名称: $DATABASE_NAME")
                timber.log.Timber.tag("DataFlow").d("数据库版本: 1 (全新数据库)")
                timber.log.Timber.tag("DataFlow").d("数据库路径: ${context.applicationContext.getDatabasePath(DATABASE_NAME).absolutePath}")

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            timber.log.Timber.tag("DataFlow").d("✅ 数据库首次创建完成 (Version 1)")
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
                        }
                    })
                    .fallbackToDestructiveMigration() // 版本不匹配时清空数据库重建
                    .build()

                timber.log.Timber.tag("DataFlow").d("✅ 数据库实例创建完成")
                INSTANCE = instance
                instance
            }
        }
    }
} 