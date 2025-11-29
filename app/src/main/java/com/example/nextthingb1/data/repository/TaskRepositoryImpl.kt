package com.example.nextthingb1.data.repository

import com.example.nextthingb1.data.local.dao.TaskDao
import com.example.nextthingb1.data.local.dao.CategoryDao
import com.example.nextthingb1.data.local.dao.CategoryTaskCount
import com.example.nextthingb1.data.mapper.toDomain
import com.example.nextthingb1.data.mapper.toDomainList
import com.example.nextthingb1.data.mapper.toEntity
import com.example.nextthingb1.data.mapper.CategoryMapper.toDomain
import com.example.nextthingb1.domain.model.Task
import com.example.nextthingb1.domain.model.Category
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
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao
) : TaskRepository {

    companion object {
        private const val TAG = "DataFlow"
    }

    override suspend fun insertTask(task: Task): String {
        timber.log.Timber.tag(TAG).d("━━━━━━ Repository.insertTask ━━━━━━")
        timber.log.Timber.tag(TAG).d("插入任务: ${task.title}, ID: ${task.id}")
        taskDao.insertTask(task.toEntity())
        timber.log.Timber.tag(TAG).d("✅ 任务已插入数据库")
        return task.id
    }

    override suspend fun updateTask(task: Task) {
        timber.log.Timber.tag(TAG).d("━━━━━━ Repository.updateTask ━━━━━━")
        timber.log.Timber.tag(TAG).d("更新任务: ${task.title}, ID: ${task.id}")
        timber.log.Timber.tag(TAG).d("  标题: ${task.title}")
        timber.log.Timber.tag(TAG).d("  描述: ${task.description}")
        timber.log.Timber.tag(TAG).d("  分类: ${task.category.name}")
        timber.log.Timber.tag(TAG).d("  重要程度: ${task.importanceUrgency?.displayName ?: "null"}")
        timber.log.Timber.tag(TAG).d("  截止时间: ${task.dueDate}")
        timber.log.Timber.tag(TAG).d("  位置: ${task.locationInfo?.locationName ?: "null"}")
        timber.log.Timber.tag(TAG).d("  状态: ${task.status}")

        val entity = task.toEntity()
        timber.log.Timber.tag(TAG).d("转换为Entity后:")
        timber.log.Timber.tag(TAG).d("  importanceUrgencyJson: ${entity.importanceUrgencyJson}")

        taskDao.updateTask(entity)
        timber.log.Timber.tag(TAG).d("✅ 任务已更新到数据库")
    }

    override suspend fun deleteTask(taskId: String) {
        timber.log.Timber.tag(TAG).d("━━━━━━ Repository.deleteTask ━━━━━━")
        timber.log.Timber.tag(TAG).d("删除任务: $taskId")
        taskDao.deleteTaskById(taskId)
        timber.log.Timber.tag(TAG).d("✅ 任务已删除")
    }

    override suspend fun deleteAllTasks() {
        timber.log.Timber.tag(TAG).d("━━━━━━ Repository.deleteAllTasks ━━━━━━")
        taskDao.deleteAllTasks()
        timber.log.Timber.tag(TAG).d("✅ 所有任务已删除")
    }

    override suspend fun getTaskById(taskId: String): Task? {
        timber.log.Timber.tag(TAG).d("━━━━━━ Repository.getTaskById ━━━━━━")
        timber.log.Timber.tag(TAG).d("查询任务ID: $taskId")
        val entity = taskDao.getTaskById(taskId)
        val task = entity?.toDomain()
        timber.log.Timber.tag(TAG).d("查询结果: ${if (task != null) "找到任务 ${task.title}" else "未找到"}")
        return task
    }

    override fun getAllTasks(): Flow<List<Task>> {
        timber.log.Timber.tag(TAG).d("━━━━━━ Repository.getAllTasks ━━━━━━")
        timber.log.Timber.tag(TAG).d("开始订阅所有任务的Flow")
        return taskDao.getAllTasks().map { taskWithCategories ->
            timber.log.Timber.tag(TAG).d("📊 DAO返回 ${taskWithCategories.size} 个TaskWithCategory")
            taskWithCategories.forEachIndexed { index, twc ->
                timber.log.Timber.tag(TAG).d("  [$index] Entity: id=${twc.task.id}, title=${twc.task.title}, status=${twc.task.status}")
            }
            val tasks = taskWithCategories.toDomainList()
            timber.log.Timber.tag(TAG).d("📊 转换后得到 ${tasks.size} 个Task对象")
            tasks.forEachIndexed { index, task ->
                timber.log.Timber.tag(TAG).d("  [$index] Task: id=${task.id}, title=${task.title}, status=${task.status}")
            }
            tasks
        }
    }
    
    override fun getTasksByStatus(status: TaskStatus): Flow<List<Task>> {
        timber.log.Timber.tag(TAG).d("━━━━━━ Repository.getTasksByStatus ━━━━━━")
        timber.log.Timber.tag(TAG).d("查询状态: $status")
        return taskDao.getTasksByStatus(status).map { taskWithCategories ->
            timber.log.Timber.tag(TAG).d("📊 状态[$status]返回 ${taskWithCategories.size} 个任务")
            taskWithCategories.toDomainList()
        }
    }

    override fun getTasksByCategory(category: Category): Flow<List<Task>> {
        timber.log.Timber.tag(TAG).d("━━━━━━ Repository.getTasksByCategory ━━━━━━")
        timber.log.Timber.tag(TAG).d("查询分类: ${category.name}")
        return taskDao.getTasksByCategoryId(category.id).map { taskWithCategories ->
            timber.log.Timber.tag(TAG).d("📊 分类[${category.name}]返回 ${taskWithCategories.size} 个任务")
            taskWithCategories.toDomainList()
        }
    }


    override fun getTasksByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<Task>> {
        timber.log.Timber.tag(TAG).d("━━━━━━ Repository.getTasksByDateRange ━━━━━━")
        timber.log.Timber.tag(TAG).d("查询日期范围: $startDate ~ $endDate")
        return taskDao.getTasksByDateRange(startDate, endDate).map { taskWithCategories ->
            timber.log.Timber.tag(TAG).d("📊 日期范围返回 ${taskWithCategories.size} 个任务")
            taskWithCategories.toDomainList()
        }
    }

    override fun getTodayTasks(): Flow<List<Task>> {
        timber.log.Timber.tag(TAG).d("━━━━━━ Repository.getTodayTasks ━━━━━━")
        timber.log.Timber.tag(TAG).d("查询今日任务")
        return taskDao.getTodayTasks().map { taskWithCategories ->
            timber.log.Timber.tag(TAG).d("📊 今日任务返回 ${taskWithCategories.size} 个任务")
            taskWithCategories.forEachIndexed { index, twc ->
                timber.log.Timber.tag(TAG).d("  [$index] 今日任务: ${twc.task.title}, dueDate=${twc.task.dueDate}")
            }
            taskWithCategories.toDomainList()
        }
    }
    
    override fun getOverdueTasks(): Flow<List<Task>> {
        return taskDao.getOverdueTasks().map { taskWithCategories -> taskWithCategories.toDomainList() }
    }

    override fun getUrgentTasks(): Flow<List<Task>> {
        return taskDao.getUrgentTasks().map { taskWithCategories -> taskWithCategories.toDomainList() }
    }

    override fun searchTasks(query: String): Flow<List<Task>> {
        return taskDao.searchTasks(query).map { taskWithCategories -> taskWithCategories.toDomainList() }
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

        // Get category statistics
        val categoryTaskCounts = taskDao.getCategoryTaskCounts()
        val allCategoriesEntities = categoryDao.getAllCategoriesList()
        val categoryStats = categoryTaskCounts.mapNotNull { count ->
            val categoryEntity = allCategoriesEntities.find { it.id == count.categoryId }
            categoryEntity?.let { it.toDomain() to count.count }
        }.toMap()

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
    
    override suspend fun getCategoryStatistics(): Map<Category, Int> {
        // Get category task counts from database
        val categoryTaskCounts = taskDao.getCategoryTaskCounts()
        val allCategoriesEntities = categoryDao.getAllCategoriesList()

        // Map category IDs to Category domain models with their counts
        return categoryTaskCounts.mapNotNull { count ->
            val categoryEntity = allCategoriesEntities.find { it.id == count.categoryId }
            categoryEntity?.let { it.toDomain() to count.count }
        }.toMap()
    }
    
    override suspend fun markTasksAsCompleted(taskIds: List<String>) {
        taskDao.markTasksAsCompleted(taskIds)
    }
    
    override suspend fun deleteCompletedTasks() {
        taskDao.deleteCompletedTasks()
    }
    
    override suspend fun bulkUpdateTaskCategory(taskIds: List<String>, category: Category) {
        taskDao.bulkUpdateTaskCategory(taskIds, category.id)
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

    override suspend fun getTemplateTasks(): List<Task> {
        timber.log.Timber.tag(TAG).d("━━━━━━ Repository.getTemplateTasks ━━━━━━")
        val templates = taskDao.getTemplateTasks().map { it.toDomain() }
        timber.log.Timber.tag(TAG).d("找到 ${templates.size} 个模板任务")
        return templates
    }

    override suspend fun hasInstanceForDate(templateId: String, date: LocalDateTime): Boolean {
        timber.log.Timber.tag(TAG).v("检查模板任务 $templateId 在 ${date.toLocalDate()} 是否已有实例")
        return taskDao.hasInstanceForDate(templateId, date)
    }
} 