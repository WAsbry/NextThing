package com.example.nextthingb1.domain.repository

import com.example.nextthingb1.domain.model.Task
import com.example.nextthingb1.domain.model.TaskCategory
import com.example.nextthingb1.domain.model.TaskPriority
import com.example.nextthingb1.domain.model.TaskStatistics
import com.example.nextthingb1.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface TaskRepository {
    
    // 基础CRUD操作
    suspend fun insertTask(task: Task): String
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(taskId: String)
    suspend fun deleteAllTasks()
    suspend fun getTaskById(taskId: String): Task?
    
    // 查询操作
    fun getAllTasks(): Flow<List<Task>>
    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>>
    fun getTasksByCategory(category: TaskCategory): Flow<List<Task>>
    fun getTasksByPriority(priority: TaskPriority): Flow<List<Task>>
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
    suspend fun getCategoryStatistics(): Map<TaskCategory, Int>
    
    // 批量操作
    suspend fun markTasksAsCompleted(taskIds: List<String>)
    suspend fun deleteCompletedTasks()
    suspend fun bulkUpdateTaskCategory(taskIds: List<String>, category: TaskCategory)
    
    // 数据同步
    suspend fun syncTasks(): Result<Unit>
    suspend fun exportTasks(): Result<String> // 返回导出文件路径
    suspend fun importTasks(filePath: String): Result<Int> // 返回导入任务数量
} 