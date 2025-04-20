package com.wasbry.nextthing.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wasbry.nextthing.database.dao.CategoryDao
import com.wasbry.nextthing.database.dao.TodoTaskDao
import com.wasbry.nextthing.database.model.Category
import com.wasbry.nextthing.database.model.TodoTask

@Database(entities = [TodoTask::class,Category::class], version = 1, exportSchema = false)
abstract class TodoDatabase: RoomDatabase() {

    // 获取待办任务
    abstract fun todoTaskDao(): TodoTaskDao
    abstract fun categoryDao(): CategoryDao

    // 单例模式，保证只有一个数据库实例
    companion object {
        @Volatile
        private var INSTANCE: TodoDatabase ?= null

        fun getDatabase(context: Context): TodoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TodoDatabase::class.java,
                    "todoDatabase"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}