package com.nextthing.app.data.local.dao

import androidx.room.*
import com.nextthing.app.data.local.entity.TaskEntity
import com.nextthing.app.data.local.entity.TaskWithCategory
import com.nextthing.app.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

import com.nextthing.app.data.local.entity.SyncStatus

@Dao
interface TaskDao {

    // ========== 联表查询方法（推荐使用） ==========

    @Transaction
    @Query("SELECT * FROM tasks WHERE isTemplate = 0 AND deleted = 0 ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskWithCategory>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId AND deleted = 0")
    suspend fun getTaskById(taskId: String): TaskWithCategory?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskEntityByIdIncludingDeleted(taskId: String): TaskEntity?

    @Transaction
    @Query("SELECT * FROM tasks WHERE status = :status AND isTemplate = 0 AND deleted = 0 ORDER BY createdAt DESC")
    fun getTasksByStatus(status: TaskStatus): Flow<List<TaskWithCategory>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE categoryId = :categoryId AND isTemplate = 0 AND deleted = 0 ORDER BY createdAt DESC")
    fun getTasksByCategoryId(categoryId: String): Flow<List<TaskWithCategory>>

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE strftime('%Y-%m-%d', dueDate) = strftime('%Y-%m-%d', 'now', 'localtime')
        AND isTemplate = 0 AND deleted = 0
        ORDER BY dueDate ASC
    """)
    fun getTodayTasks(): Flow<List<TaskWithCategory>>

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE (
            status = 'OVERDUE'
            OR (status IN ('PENDING', 'DELAYED') AND dueDate < :currentTime)
        )
        AND isTemplate = 0 AND deleted = 0
        ORDER BY dueDate ASC
    """)
    fun getOverdueTasks(currentTime: LocalDateTime = LocalDateTime.now()): Flow<List<TaskWithCategory>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE isUrgent = 1 AND status IN ('PENDING', 'DELAYED', 'OVERDUE') AND isTemplate = 0 AND deleted = 0 ORDER BY dueDate ASC")
    fun getUrgentTasks(): Flow<List<TaskWithCategory>>

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE createdAt BETWEEN :startDate AND :endDate AND isTemplate = 0 AND deleted = 0
        ORDER BY createdAt DESC
    """)
    fun getTasksByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<TaskWithCategory>>

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE createdAt BETWEEN :startDate AND :endDate AND isTemplate = 0 AND deleted = 0
        ORDER BY createdAt DESC
    """)
    suspend fun getTasksByDateRangeOnce(startDate: LocalDateTime, endDate: LocalDateTime): List<TaskWithCategory>

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') AND isTemplate = 0 AND deleted = 0
        ORDER BY createdAt DESC
    """)
    fun searchTasks(query: String): Flow<List<TaskWithCategory>>

    // ========== 同步相关查询 ==========

    @Query("SELECT * FROM tasks WHERE syncStatus = :status")
    suspend fun getTasksBySyncStatus(status: SyncStatus): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE syncStatus = 'CONFLICT'")
    suspend fun getConflictTasks(): List<TaskEntity>

    @Query("UPDATE tasks SET syncStatus = :status, serverUpdatedAt = :serverTime, syncError = NULL WHERE id = :taskId")
    suspend fun updateSyncStatus(taskId: String, status: SyncStatus, serverTime: Long? = null)

    @Query("UPDATE tasks SET syncStatus = :status, syncError = :error WHERE id = :taskId")
    suspend fun updateSyncError(taskId: String, status: SyncStatus, error: String?)

    @Query("SELECT COUNT(*) FROM tasks WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncCount(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE syncStatus = 'CONFLICT'")
    suspend fun getConflictCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    /**
     * Used by recurring generation. IGNORE makes the unique
     * (templateTaskId, instanceDate) index the atomic idempotency boundary.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaskIfAbsent(task: TaskEntity): Long
    
    @Update
    suspend fun updateTask(task: TaskEntity)
    
    @Delete
    suspend fun deleteTask(task: TaskEntity)
    
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("""
        UPDATE tasks
        SET deleted = 1, syncStatus = 'PENDING', syncError = NULL, updatedAt = :updatedAt
        WHERE id = :taskId
    """)
    suspend fun softDeleteTask(taskId: String, updatedAt: LocalDateTime)
    
    @Query("""
        UPDATE tasks
        SET deleted = 1, syncStatus = 'PENDING', syncError = NULL, updatedAt = :updatedAt
        WHERE status = 'COMPLETED' AND deleted = 0
    """)
    suspend fun softDeleteCompletedTasks(updatedAt: LocalDateTime)

    @Query("""
        UPDATE tasks
        SET deleted = 1, syncStatus = 'PENDING', syncError = NULL, updatedAt = :updatedAt
        WHERE deleted = 0
    """)
    suspend fun softDeleteAllTasks(updatedAt: LocalDateTime)
    
    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
    
    @Query("""
        UPDATE tasks
        SET status = 'COMPLETED', completedAt = :completedAt, updatedAt = :completedAt,
            syncStatus = 'PENDING', syncError = NULL
        WHERE id IN (:taskIds) AND deleted = 0
    """)
    suspend fun markTasksAsCompleted(taskIds: List<String>, completedAt: LocalDateTime = LocalDateTime.now())

    @Query("""
        UPDATE tasks
        SET categoryId = :categoryId, updatedAt = :updatedAt,
            syncStatus = 'PENDING', syncError = NULL
        WHERE id IN (:taskIds) AND deleted = 0
    """)
    suspend fun bulkUpdateTaskCategory(
        taskIds: List<String>,
        categoryId: String,
        updatedAt: LocalDateTime = LocalDateTime.now()
    )
    
    // 统计查询
    @Query("SELECT COUNT(*) FROM tasks WHERE isTemplate = 0 AND deleted = 0")
    suspend fun getTotalTasksCount(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'COMPLETED' AND isTemplate = 0 AND deleted = 0")
    suspend fun getCompletedTasksCount(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'PENDING' AND isTemplate = 0 AND deleted = 0")
    suspend fun getPendingTasksCount(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'OVERDUE' AND isTemplate = 0 AND deleted = 0")
    suspend fun getOverdueTasksCount(): Int
    
    @Query("""
        SELECT categoryId, COUNT(*) as count
        FROM tasks
        WHERE isTemplate = 0 AND deleted = 0
        GROUP BY categoryId
    """)
    suspend fun getCategoryTaskCounts(): List<CategoryTaskCount>
    
    @Query("""
        SELECT AVG(actualDuration)
        FROM tasks
        WHERE status = 'COMPLETED' AND actualDuration > 0 AND isTemplate = 0 AND deleted = 0
    """)
    suspend fun getAverageCompletionTime(): Double?

    @Query("SELECT MIN(createdAt) FROM tasks WHERE isTemplate = 0 AND deleted = 0")
    suspend fun getEarliestTaskDate(): LocalDateTime?

    @Query("""
        SELECT * FROM tasks
        WHERE createdAt >= :weekStart AND createdAt <= :weekEnd AND isTemplate = 0 AND deleted = 0
        ORDER BY createdAt ASC
    """)
    suspend fun getTasksInWeek(weekStart: LocalDateTime, weekEnd: LocalDateTime): List<TaskEntity>

    // ========== 成就系统查询 ==========

    // 统计每个分类的已完成任务数（全量，不限时间范围）
    @Query("""
        SELECT categoryId, COUNT(*) as count
        FROM tasks
        WHERE status = 'COMPLETED' AND isTemplate = 0 AND deleted = 0
        GROUP BY categoryId
    """)
    suspend fun getCompletedTaskCountByCategory(): List<CategoryTaskCount>

    // 重复任务模板数量（用于"重复高手"成就）
    @Query("SELECT COUNT(*) FROM tasks WHERE isTemplate = 1 AND deleted = 0")
    suspend fun getTemplateTasksCount(): Int

    // ========== 重复任务相关查询 ==========

    @Transaction
    @Query("SELECT * FROM tasks WHERE isTemplate = 1 AND deleted = 0")
    suspend fun getTemplateTasks(): List<TaskWithCategory>

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE templateTaskId = :templateId AND date(instanceDate) = date(:date)
        ORDER BY createdAt ASC
        LIMIT 1
    """)
    suspend fun getTaskInstance(templateId: String, date: LocalDateTime): TaskWithCategory?

    @Query("""
        SELECT COUNT(*) FROM tasks
        WHERE templateTaskId = :templateId AND date(instanceDate) = date(:date)
    """)
    suspend fun hasInstanceForDate(templateId: String, date: LocalDateTime): Boolean

    // 重复任务删除相关
    @Transaction
    @Query("SELECT * FROM tasks WHERE templateTaskId = :templateId")
    suspend fun getInstancesByTemplateId(templateId: String): List<TaskWithCategory>

    @Query("""
        UPDATE tasks
        SET deleted = 1, syncStatus = 'PENDING', syncError = NULL, updatedAt = :updatedAt
        WHERE templateTaskId = :templateId
    """)
    suspend fun softDeleteInstancesByTemplateId(templateId: String, updatedAt: LocalDateTime)

    @Query("""
        UPDATE tasks
        SET deleted = 1, syncStatus = 'PENDING', syncError = NULL, updatedAt = :updatedAt
        WHERE id = :templateId OR templateTaskId = :templateId
    """)
    suspend fun softDeleteTemplateAndAllInstances(templateId: String, updatedAt: LocalDateTime)

    // ========== 日历视图查询 ==========

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE date(dueDate) BETWEEN date(:startDate) AND date(:endDate)
        AND isTemplate = 0 AND deleted = 0
        ORDER BY dueDate ASC
    """)
    fun getTasksByDueDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<TaskWithCategory>>

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE date(dueDate) = date(:date)
        AND isTemplate = 0 AND deleted = 0
        ORDER BY dueDate ASC
    """)
    fun getTasksByDueDate(date: LocalDateTime): Flow<List<TaskWithCategory>>

    @Query("""
        SELECT DISTINCT date(dueDate) as dateStr
        FROM tasks
        WHERE dueDate BETWEEN :monthStart AND :monthEnd
        AND isTemplate = 0 AND deleted = 0 AND status != 'COMPLETED' AND status != 'CANCELLED'
    """)
    suspend fun getDatesWithTasksInMonth(monthStart: LocalDateTime, monthEnd: LocalDateTime): List<String>

    // Widget 用：一次性查询今日任务（非 Flow）
    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE strftime('%Y-%m-%d', dueDate) = strftime('%Y-%m-%d', 'now', 'localtime')
        AND isTemplate = 0 AND deleted = 0
        ORDER BY dueDate ASC
        LIMIT 5
    """)
    suspend fun getTodayTasksAsList(): List<TaskWithCategory>
}

/**
 * Data class to hold category task count statistics
 */
data class CategoryTaskCount(
    val categoryId: String,
    val count: Int
)
