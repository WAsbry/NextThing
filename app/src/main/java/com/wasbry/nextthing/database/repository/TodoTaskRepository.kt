package com.wasbry.nextthing.database.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.wasbry.nextthing.database.dao.TodoTaskDao
import com.wasbry.nextthing.database.model.TaskStatus
import com.wasbry.nextthing.database.model.TodoTask
import com.wasbry.nextthing.database.model.WeeklySummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class TodoTaskRepository(private val todoTaskDao: TodoTaskDao) {

    val TAG = "TodoTaskRepository"

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

    // 获取指定日期的任务列表（按创建时间逆序排列）
    fun getTasksByDate(targetDate: String): Flow<List<TodoTask>> {
        return todoTaskDao.getTasksByDate(targetDate)
    }

    // 获取指定日期未完成的任务列表（按创建时间逆序排列）
    fun getIncompleteTasksByDate(targetDate: String): Flow<List<TodoTask>> {
        return todoTaskDao.getIncompleteTasksByDate(targetDate)
    }

    // 获取指定时间范围的所有任务列表
    fun getTasksByDateRange(startTime: String, endTime: String): Flow<List<TodoTask>> {
        return todoTaskDao.getTasksByDateRange(startTime,endTime)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getWeeklySummary(startDate: LocalDate, endDate: LocalDate): Flow<WeeklySummary> {
        val startDateAsDate = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val endDateAsDate = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())

        // 获取各状态的 Flow（Room 自动响应数据库变更）
        val taskTotalFlow = todoTaskDao.getTaskCountByDateRange(startDateAsDate, endDateAsDate)
        val incompleteFlow = todoTaskDao.getTaskCountByStatusAndDateRange(
            TaskStatus.INCOMPLETE.name,
            startDateAsDate, endDateAsDate
        )
        val completedFlow = todoTaskDao.getTaskCountByStatusAndDateRange(
            TaskStatus.COMPLETED.name,
            startDateAsDate, endDateAsDate
        )
        val abandonedFlow = todoTaskDao.getTaskCountByStatusAndDateRange(
            TaskStatus.ABANDONED.name,
            startDateAsDate, endDateAsDate
        )
        val postponedFlow = todoTaskDao.getTaskCountByStatusAndDateRange(
            TaskStatus.POSTPONED.name,
            startDateAsDate, endDateAsDate
        )

        // 合并所有 Flow，数据变更时自动发射新的 WeeklySummary
        return combine(
            taskTotalFlow,
            incompleteFlow,
            completedFlow,
            abandonedFlow,
            postponedFlow
        ) { total, incomplete, completed, abandoned, postponed ->
            WeeklySummary(
                startDate = startDate,
                endDate = endDate,
                taskTotalCount = total,
                taskIncompleteTotalCount = incomplete,
                taskCompletedTotalCount = completed,
                taskAbandonedTotalCount = abandoned,
                taskPostponedTotalCount = postponed,
                expectedTaskCount = 0 // 按需计算
            )
        }
    }


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