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
import com.example.nextthingb1.data.local.entity.TaskEntity
import com.example.nextthingb1.data.local.entity.LocationEntity
import com.example.nextthingb1.data.local.entity.NotificationStrategyEntity
import com.example.nextthingb1.data.local.converter.Converters

@Database(
    entities = [TaskEntity::class, LocationEntity::class, NotificationStrategyEntity::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {
    
    abstract fun taskDao(): TaskDao
    abstract fun locationDao(): LocationDao
    abstract fun notificationStrategyDao(): NotificationStrategyDao
    
    companion object {
        const val DATABASE_NAME = "next_thing_database"

        @Volatile
        private var INSTANCE: TaskDatabase? = null

        // 数据库迁移脚本：从版本5到版本6
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 为tasks表添加新字段
                database.execSQL("ALTER TABLE tasks ADD COLUMN repeatFrequencyJson TEXT NOT NULL DEFAULT '{}'")
                database.execSQL("ALTER TABLE tasks ADD COLUMN locationInfoJson TEXT")
                database.execSQL("ALTER TABLE tasks ADD COLUMN importanceUrgencyJson TEXT")
            }
        }

        fun getDatabase(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 