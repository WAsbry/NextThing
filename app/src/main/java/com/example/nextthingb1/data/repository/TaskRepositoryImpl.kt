package com.example.nextthingb1.data.repository

import com.example.nextthingb1.data.local.dao.TaskDao
import com.example.nextthingb1.data.mapper.toDomain
import com.example.nextthingb1.data.mapper.toEntity
import com.example.nextthingb1.domain.model.Task
import com.example.nextthingb1.domain.model.TaskCategory
import com.example.nextthingb1.domain.model.TaskPriority
import com.example.nextthingb1.domain.model.TaskStatistics
import com.example.nextthingb1.domain.model.TaskStatus
import com.example.nextthingb1.domain.repository.TaskRepository
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {
    
    override suspend fun insertTask(task: Task): String {
        taskDao.insertTask(task.toEntity())
        return task.id
    }
    
    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
    }
    
    override suspend fun deleteTask(taskId: String) {
        taskDao.deleteTaskById(taskId)
    }
    
    override suspend fun deleteAllTasks() {
        taskDao.deleteAllTasks()
    }
    
    override suspend fun getTaskById(taskId: String): Task? {
        return taskDao.getTaskById(taskId)?.toDomain()
    }
    
    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { entities -> entities.toDomain() }
    }
    
    override fun getTasksByStatus(status: TaskStatus): Flow<List<Task>> {
        return taskDao.getTasksByStatus(status).map { entities -> entities.toDomain() }
    }
    
    override fun getTasksByCategory(category: TaskCategory): Flow<List<Task>> {
        return taskDao.getTasksByCategory(category).map { entities -> entities.toDomain() }
    }
    
    override fun getTasksByPriority(priority: TaskPriority): Flow<List<Task>> {
        return taskDao.getTasksByPriority(priority).map { entities -> entities.toDomain() }
    }
    
    override fun getTasksByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<Task>> {
        return taskDao.getTasksByDateRange(startDate, endDate).map { entities -> entities.toDomain() }
    }
    
    override fun getTodayTasks(): Flow<List<Task>> {
        return taskDao.getTodayTasks().map { entities -> entities.toDomain() }
    }
    
    override fun getOverdueTasks(): Flow<List<Task>> {
        return taskDao.getOverdueTasks().map { entities -> entities.toDomain() }
    }
    
    override fun getUrgentTasks(): Flow<List<Task>> {
        return taskDao.getUrgentTasks().map { entities -> entities.toDomain() }
    }
    
    override fun searchTasks(query: String): Flow<List<Task>> {
        return taskDao.searchTasks(query).map { entities -> entities.toDomain() }
    }
    
    override fun getTasksByTags(tags: List<String>): Flow<List<Task>> {
        return getAllTasks().map { tasks ->
            tasks.filter { task ->
                tags.any { tag -> task.tags.contains(tag) }
            }
        }
    }
    
    override suspend fun getTaskStatistics(): TaskStatistics {
        val totalTasks = taskDao.getTotalTasksCount()
        val completedTasks = taskDao.getCompletedTasksCount()
        val pendingTasks = taskDao.getPendingTasksCount()
        val overdueTasks = taskDao.getOverdueTasksCount()
        val completionRate = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
        val averageCompletionTime = taskDao.getAverageCompletionTime()?.toInt() ?: 0
        val categoryCounts = taskDao.getCategoryStatistics()
        val categoryStats = categoryCounts.associate { it.category to it.count }
        
        return TaskStatistics(
            totalTasks = totalTasks,
            completedTasks = completedTasks,
            pendingTasks = pendingTasks,
            overdueTasks = overdueTasks,
            completionRate = completionRate,
            averageCompletionTime = averageCompletionTime,
            mostProductiveHour = 9,
            categoryStats = categoryStats,
            weeklyStats = emptyList()
        )
    }
    
    override suspend fun getTaskStatisticsByDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): TaskStatistics {
        return getTaskStatistics()
    }
    
    override suspend fun getCategoryStatistics(): Map<TaskCategory, Int> {
        return taskDao.getCategoryStatistics().associate { it.category to it.count }
    }
    
    override suspend fun markTasksAsCompleted(taskIds: List<String>) {
        taskDao.markTasksAsCompleted(taskIds)
    }
    
    override suspend fun deleteCompletedTasks() {
        taskDao.deleteCompletedTasks()
    }
    
    override suspend fun bulkUpdateTaskCategory(taskIds: List<String>, category: TaskCategory) {
        taskDao.bulkUpdateTaskCategory(taskIds, category)
    }
    
    override suspend fun syncTasks(): Result<Unit> {
        // TODO: 实现网络同步逻辑
        return Result.success(Unit)
    }
    
    override suspend fun exportTasks(): Result<String> {
        // TODO: 实现任务导出逻辑
        return Result.success("")
    }
    
    override suspend fun importTasks(filePath: String): Result<Int> {
        // TODO: 实现任务导入逻辑
        return Result.success(0)
    }

    override suspend fun getEarliestTaskDate(): LocalDate? {
        Log.d("weekCount", "Repository: 开始查询数据库最早任务日期...")
        val earliestDateTime = taskDao.getEarliestTaskDate()
        Log.d("weekCount", "Repository: 数据库返回的最早任务DateTime: $earliestDateTime")
        val earliestDate = earliestDateTime?.toLocalDate()
        Log.d("weekCount", "Repository: 转换后的最早任务LocalDate: $earliestDate")
        return earliestDate
    }
} 