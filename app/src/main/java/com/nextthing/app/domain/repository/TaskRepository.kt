package com.nextthing.app.domain.repository

import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.model.Category
import com.nextthing.app.domain.model.TaskStatistics
import com.nextthing.app.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

interface TaskRepository {

    // 基础CRUD操作
    suspend fun insertTask(task: Task): String
    suspend fun insertTaskIfAbsent(task: Task): Boolean
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(taskId: String)
    suspend fun deleteAllTasks()
    suspend fun getTaskById(taskId: String): Task?

    // 查询操作
    fun getAllTasks(): Flow<List<Task>>
    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>>
    fun getTasksByCategory(category: Category): Flow<List<Task>>
    fun getTasksByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<Task>>
    fun getTodayTasks(): Flow<List<Task>>
    fun getOverdueTasks(): Flow<List<Task>>
    fun getUrgentTasks(): Flow<List<Task>>
    
    // 搜索功能
    fun searchTasks(query: String): Flow<List<Task>>
    fun getTasksByTags(tags: List<String>): Flow<List<Task>>
    
    // 统计功能
    suspend fun getTaskStatistics(): TaskStatistics
    suspend fun getTaskStatisticsByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): TaskStatistics
    suspend fun getCategoryStatistics(): Map<Category, Int>
    suspend fun getEarliestTaskDate(): LocalDate?

    // 批量操作
    suspend fun markTasksAsCompleted(taskIds: List<String>)
    suspend fun deleteCompletedTasks()
    suspend fun bulkUpdateTaskCategory(taskIds: List<String>, category: Category)
    
    // 重复任务相关
    suspend fun getTemplateTasks(): List<Task>
    suspend fun hasInstanceForDate(templateId: String, date: LocalDateTime): Boolean
    suspend fun getInstancesByTemplateId(templateId: String): List<Task>
    suspend fun deleteInstancesByTemplateId(templateId: String)
    suspend fun deleteTemplateAndAllInstances(templateId: String)

    // 日历视图
    fun getTasksByDueDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<Task>>
    fun getTasksByDueDate(date: LocalDateTime): Flow<List<Task>>
    suspend fun getDatesWithTasksInMonth(year: Int, month: Int): List<LocalDate>
}
