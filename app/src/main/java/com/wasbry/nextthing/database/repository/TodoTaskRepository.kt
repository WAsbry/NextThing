package com.wasbry.nextthing.database.repository

import android.util.Log
import com.wasbry.nextthing.database.dao.TodoTaskDao
import com.wasbry.nextthing.database.model.TodoTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

class TodoTaskRepository(private val todoTaskDao: TodoTaskDao) {

    // 获取所有待办任务的LiveData,用于观察数据变化
    val allTodoTasks: Flow<List<TodoTask>> = todoTaskDao.getAllTodoTasks()
        .onEach { tasks ->
            Log.d("TodoTaskRepository", "Received ${tasks.size} tasks: $tasks")
        }

    // 插入单个待办任务，使用挂起函数处理
    suspend fun insertTodoTask(todoTask: TodoTask){
        withContext(Dispatchers.IO) {
            todoTaskDao.insertTodoTask(todoTask)
        }
    }

    // 更新单个待办任务
    suspend fun updateTodoTask(todoTask: TodoTask) {
        withContext(Dispatchers.IO) {
            todoTaskDao.updateTodoTask(todoTask)
        }
    }

    // 删除单个待办任务
    suspend fun deleteTodoTask(todoTask: TodoTask) {
        withContext(Dispatchers.IO) {
            todoTaskDao.deleteTodoTask(todoTask)
        }
    }
}