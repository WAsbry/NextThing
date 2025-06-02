package com.wasbry.nextthing.viewmodel.todoTask

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wasbry.nextthing.database.model.TaskStatus
import com.wasbry.nextthing.database.model.TodoTask
import com.wasbry.nextthing.database.model.WeeklySummary
import com.wasbry.nextthing.database.repository.TodoTaskRepository
import com.wasbry.nextthing.tool.TimeTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class TodoTaskViewModel(private val todoTaskRepository: TodoTaskRepository) : ViewModel() {

    val TAG = "TodoTaskViewModel"

    // 单一Flow实例，监听所有任务的变化
    private val _allTasksFlow = todoTaskRepository.allTodoTasks
        .distinctUntilChanged()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            replay = 1
        )

    // 获取所有待办任务（使用单一Flow）
    val allTodoTasks: Flow<List<TodoTask>> = _allTasksFlow

    // 获取所有已完成的任务（使用单一Flow）
    val getCompletedTodoTask: Flow<List<TodoTask>> = _allTasksFlow
        .map { tasks -> tasks.filter { it.status == TaskStatus.COMPLETED } }
        .distinctUntilChanged()

    // 获取所有未完成的任务（使用单一Flow）
    val getIncompleteTodoTasks: Flow<List<TodoTask>> = _allTasksFlow
        .map { tasks -> tasks.filter { it.status == TaskStatus.INCOMPLETE } }
        .distinctUntilChanged()

    // 获取所有放弃的任务（使用单一Flow）
    val getAbandonedTodoTasks: Flow<List<TodoTask>> = _allTasksFlow
        .map { tasks -> tasks.filter { it.status == TaskStatus.ABANDONED } }
        .distinctUntilChanged()

    // 获取所有延期的任务（使用单一Flow）
    val getPostponedTodoTasks: Flow<List<TodoTask>> = _allTasksFlow
        .map { tasks -> tasks.filter { it.status == TaskStatus.POSTPONED } }
        .distinctUntilChanged()

    // 获取指定日期的任务列表（使用单一Flow）
    fun getTasksByDate(targetDate: String): Flow<List<TodoTask>> {
        return _allTasksFlow
            .map { tasks -> tasks.filter { it.madeDate == targetDate } }
            .distinctUntilChanged()
    }

    // 获取指定日期未完成的任务列表（使用单一Flow）
    fun getIncompleteTasksByDate(targetDate: String): Flow<List<TodoTask>> {
        return _allTasksFlow
            .map { tasks ->
                tasks.filter { it.madeDate == targetDate && it.status == TaskStatus.INCOMPLETE }
            }
            .distinctUntilChanged()
    }

    // 获取指定时间范围的任务列表（使用单一Flow）
    fun getTasksByDateRange(startTime: String, endTime: String): Flow<List<TodoTask>> {
        return _allTasksFlow
            .map { tasks ->
                tasks.filter { task ->
                    val taskDate = task.madeDate
                    taskDate >= startTime && taskDate <= endTime
                }
            }
            .distinctUntilChanged()
    }

    /**
     * 获取本周任务统计信息（直接返回 Repository 的 Flow）
     * @param date 基准日期（用于计算本周范围）
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getWeeklySummary(date: LocalDate): Flow<WeeklySummary> {
        val startDate = TimeTool.getStartOfWeek(date)
        val endDate = TimeTool.getEndOfWeek(date)
        return todoTaskRepository.getWeeklySummary(startDate, endDate)
            .flowOn(Dispatchers.IO) // 调度到 IO 线程
    }

    // 插入单个待办任务
    fun insertTodoTask(todoTask: TodoTask) = viewModelScope.launch {
        todoTaskRepository.insertTodoTask(todoTask)
    }

    // 更新单个待办任务
    fun updateTodoTask(todoTask: TodoTask) = viewModelScope.launch {
        todoTaskRepository.updateTodoTask(todoTask)
    }

    // 删除单个待办任务
    fun deleteTodoTask(todoTask: TodoTask) = viewModelScope.launch {
        todoTaskRepository.deleteTodoTask(todoTask)
    }

    // 标记任务为「已完成」
    fun markTaskAsCompleted(task: TodoTask) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedTask = task.copy(
                status = TaskStatus.COMPLETED,
                // 可额外更新其他字段（如完成时间）
                // completedDate = Date() （如果模型有该字段）
            )
            Log.d("task status change","完成任务，task.name = ${task.description}")
            todoTaskRepository.updateTodoTask(updatedTask)
        }
    }

    // 标记任务为「已放弃」
    fun markTaskAsAbandoned(task: TodoTask) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedTask = task.copy(
                status = TaskStatus.ABANDONED
            )
            Log.d("task status change","放弃任务，task.name = ${task.description}")
            todoTaskRepository.updateTodoTask(updatedTask)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun markTaskAsPostponed(task: TodoTask) {
        viewModelScope.launch(Dispatchers.IO) {
            val today = LocalDate.now()
            // 移除 coerceAtMost，允许延期到下月
            val newDate = today.plusDays(1) // 直接加一天，无需限制

            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val madeDateStr = newDate.format(dateFormatter) // 格式化为 "yyyy-MM-dd"

            val updatedTask = task.copy(
                status = TaskStatus.POSTPONED,
                madeDate = madeDateStr
            )
            Log.d("Postpone", "延期任务：${task.description}, 新日期：$madeDateStr")
            todoTaskRepository.updateTodoTask(updatedTask)
        }
    }

    // 新增任务（示例）
    fun addNewTask(todoTask: TodoTask) {
        viewModelScope.launch(Dispatchers.IO) {
            todoTaskRepository.insertTodoTask(todoTask)
        }
    }

    // 删除任务（示例）
    fun deleteTask(task: TodoTask) {
        viewModelScope.launch(Dispatchers.IO) {
            todoTaskRepository.deleteTodoTask(task)
        }
    }
}