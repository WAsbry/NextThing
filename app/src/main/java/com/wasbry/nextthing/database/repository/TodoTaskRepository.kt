package com.wasbry.nextthing.database.repository

import android.util.Log
import com.wasbry.nextthing.database.dao.TodoTaskDao
import com.wasbry.nextthing.database.model.TodoTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

class TodoTaskRepository(private val todoTaskDao: TodoTaskDao) {

    // 获取所有待办任务的Flow,用于观察数据变化
    val allTodoTasks: Flow<List<TodoTask>> = todoTaskDao.getAllTodoTasks()
        .onEach { tasks ->
            Log.d("TodoTaskRepository", "Received ${tasks.size} tasks: $tasks")
        }

    // 获取所有已完成的任务
    val getCompletedTodoTasks: Flow<List<TodoTask>> = todoTaskDao.getCompletedTodoTasks()

    // 获取所有未完成的任务
    val getIncompleteTodoTasks: Flow<List<TodoTask>> = todoTaskDao.getIncompleteTodoTasks()

    // 获取所有放弃的任务
    val getAbandonedTodoTasks: Flow<List<TodoTask>> = todoTaskDao.getAbandonedTodoTasks()

    // 获取所有延期的任务
    val getPostponedTodoTasks: Flow<List<TodoTask>> = todoTaskDao.getPostponedTodoTasks()


    // 插入单个待办任务，使用挂起函数处理
    suspend fun insertTodoTask(todoTask: TodoTask) {
        withContext(Dispatchers.IO) {
            todoTaskDao.insertTodoTask(todoTask)
        }
    }

    // 插入多个待办任务，使用挂起函数处理
    suspend fun insertTodoTasks(todoTasks: List<TodoTask>) {
        withContext(Dispatchers.IO) {
            todoTaskDao.insertTodoTasks(todoTasks)
        }
    }

    // 更新单个待办任务
    suspend fun updateTodoTask(todoTask: TodoTask) {
        withContext(Dispatchers.IO) {
            todoTaskDao.updateTodoTask(todoTask)
        }
    }

    // 更新多个待办任务
    suspend fun updateTodoTasks(todoTasks: List<TodoTask>) {
        withContext(Dispatchers.IO) {
            todoTaskDao.updateTodoTasks(todoTasks)
        }
    }

    // 删除单个待办任务
    suspend fun deleteTodoTask(todoTask: TodoTask) {
        withContext(Dispatchers.IO) {
            todoTaskDao.deleteTodoTask(todoTask)
        }
    }

    // 根据任务ID删除单个待办任务
    suspend fun deleteTodoTaskById(taskId: Long) {
        withContext(Dispatchers.IO) {
            todoTaskDao.deleteTodoTaskById(taskId)
        }
    }

    // 清空所有待办任务
    suspend fun deleteAllTodoTasks() {
        withContext(Dispatchers.IO) {
            todoTaskDao.deleteAllTodoTasks()
        }
    }

    // 根据任务ID获取单个待办任务
    suspend fun getTodoTaskById(taskId: Long): TodoTask? {
        return withContext(Dispatchers.IO) {
            todoTaskDao.getTodoTaskById(taskId)
        }
    }

    // 获取所有已完成的待办任务
    suspend fun getCompletedTodoTasks(): Flow<List<TodoTask>> {
        return withContext(Dispatchers.IO) {
            todoTaskDao.getCompletedTodoTasks()
        }
    }

    // 根据关联的 PersonalTime 的 ID 获取相关的 TodoTask
    val todoTasksByPersonalTimeId: (Long) -> Flow<List<TodoTask>> = { personalTimeId ->
        todoTaskDao.getTodoTasksByPersonalTimeId(personalTimeId)
    }

    // 根据关联的 PersonalTime 的 ID 获取相关的 TodoTask，并按截止日期升序排列
    val todoTasksByPersonalTimeIdSorted: (Long) -> Flow<List<TodoTask>> = { personalTimeId ->
        todoTaskDao.getTodoTasksByPersonalTimeIdSorted(personalTimeId)
    }
}