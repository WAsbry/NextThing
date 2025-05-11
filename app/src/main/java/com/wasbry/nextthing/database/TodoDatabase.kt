package com.wasbry.nextthing.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wasbry.nextthing.database.dao.PersonalTimeDao
import com.wasbry.nextthing.database.dao.TodoTaskDao
import com.wasbry.nextthing.database.model.PersonalTime
import com.wasbry.nextthing.database.model.TodoTask
import com.wasbry.nextthing.database.utils.DateConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 使用 @Database 注解将该类标记为 Room 数据库类
// entities 参数指定数据库包含的实体类，这里包含 TodoTask 和 Category
// version 参数指定数据库的版本号，当数据库结构发生变化时需要更新版本号
// exportSchema 参数指定是否导出数据库架构，这里设置为 false 不导出
@Database(entities = [TodoTask::class,PersonalTime::class], version = 2, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class TodoDatabase : RoomDatabase() {

    // 抽象方法，用于获取 TodoTaskDao 实例，通过该实例可以进行 TodoTask 表的数据库操作
    abstract fun todoTaskDao(): TodoTaskDao

    abstract fun personalTimeDao(): PersonalTimeDao

    // 单例模式，保证只有一个数据库实例
    companion object {
        // 使用 @Volatile 注解确保变量的可见性，保证多线程环境下的一致性
        @Volatile
        private var INSTANCE: TodoDatabase? = null

        // 获取数据库实例的方法，传入上下文对象
        fun getDatabase(context: Context): TodoDatabase {
            // 使用双检锁机制确保只有一个数据库实例被创建
            return INSTANCE ?: synchronized(this) {
                // 如果 INSTANCE 为空，则创建一个新的数据库实例
                val instance = Room.databaseBuilder(
                    // 传入应用程序上下文
                    context.applicationContext,
                    // 指定数据库类
                    TodoDatabase::class.java,
                    // 指定数据库名称
                    "todoDatabase"
                ).build()
                // 将新创建的实例赋值给 INSTANCE
                INSTANCE = instance

                // 返回数据库实例
                instance
            }
        }
    }
}
