package com.wasbry.nextthing.viewmodel.todoTask

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wasbry.nextthing.database.model.TaskStatus
import com.wasbry.nextthing.database.model.TodoTask
import com.wasbry.nextthing.database.model.WeeklySummary
import com.wasbry.nextthing.database.repository.TodoTaskRepository
import com.wasbry.nextthing.tool.TimeTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Date


class TodoTaskViewModel(private val todoTaskRepository: TodoTaskRepository) : ViewModel() {

    // 获取所有待办任务
    // 直接暴露 Flow，不在 ViewModel 中收集！
    val allTodoTasks: Flow<List<TodoTask>> = todoTaskRepository.allTodoTasks

    // 获取所有已完成的任务
    val getCompletedTodoTask: Flow<List<TodoTask>> = todoTaskRepository.getCompletedTodoTasks

    // 获取所有未完成的任务
    val getIncompleteTodoTasks: Flow<List<TodoTask>> = todoTaskRepository.getIncompleteTodoTasks


    // 获取所有放弃的任务
    val getAbandonedTodoTasks: Flow<List<TodoTask>> = todoTaskRepository.getAbandonedTodoTasks

    // 获取所有延期的任务
    val getPostponedTodoTasks: Flow<List<TodoTask>> = todoTaskRepository.getPostponedTodoTasks

    /** 获取本周任务汇总（协程版本，非Flow） */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getWeeklySummaryByDate(date: LocalDate): WeeklySummary {
        return withContext(Dispatchers.IO) { // 在IO线程执行同步操作
            val startDate = TimeTool.getStartOfWeek(date)
            val endDate = TimeTool.getEndOfWeek(date)
            todoTaskRepository.getWeeklySummary(startDate, endDate)
        }
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

    // 标记任务为「已延期」（顺延）
    fun markTaskAsPostponed(task: TodoTask) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedTask = task.copy(
                status = TaskStatus.POSTPONED,
                // 可根据需求更新制定日期或其他时间字段
                 madeDate = Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000) // （延期一天）
            )
            Log.d("task status change","延期任务，task.name = ${task.description}")
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