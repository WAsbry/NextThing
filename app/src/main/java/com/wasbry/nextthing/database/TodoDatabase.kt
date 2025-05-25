package com.wasbry.nextthing.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wasbry.nextthing.database.dao.PersonalTimeDao
import com.wasbry.nextthing.database.dao.TimeTypeDao
import com.wasbry.nextthing.database.dao.TodoTaskDao
import com.wasbry.nextthing.database.model.PersonalTime
import com.wasbry.nextthing.database.model.TimeType
import com.wasbry.nextthing.database.model.TodoTask
import com.wasbry.nextthing.database.utils.DateConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 使用 @Database 注解将该类标记为 Room 数据库类
// entities 参数指定数据库包含的实体类，这里包含 TodoTask 和 Category
// version 参数指定数据库的版本号，当数据库结构发生变化时需要更新版本号
// exportSchema 参数指定是否导出数据库架构，这里设置为 false 不导出
@Database(entities = [TodoTask::class,PersonalTime::class,TimeType::class], version = 1, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class TodoDatabase : RoomDatabase() {

    // 抽象方法，用于获取 TodoTaskDao 实例，通过该实例可以进行 TodoTask 表的数据库操作
    abstract fun todoTaskDao(): TodoTaskDao

    abstract fun personalTimeDao(): PersonalTimeDao

    abstract fun timeTypeDao(): TimeTypeDao


    // 单例方法名应为 getInstance（符合常规命名习惯）
    companion object {
        @Volatile
        private var INSTANCE: TodoDatabase? = null

        // ✅ 修正方法名：将 getDatabase 改为 getInstance
        fun getInstance(context: Context): TodoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TodoDatabase::class.java,
                    "todoDatabase"
                )
                    // 如果需要数据库升级迁移，添加 .addMigrations(...)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
