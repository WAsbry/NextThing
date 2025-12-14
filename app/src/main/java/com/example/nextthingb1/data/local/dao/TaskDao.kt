package com.example.nextthingb1.data.local.dao

import androidx.room.*
import com.example.nextthingb1.data.local.entity.TaskEntity
import com.example.nextthingb1.data.local.entity.TaskWithCategory
import com.example.nextthingb1.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface TaskDao {

    // ========== 联表查询方法（推荐使用） ==========

    @Transaction
    @Query("SELECT * FROM tasks WHERE isTemplate = 0 ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskWithCategory>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskWithCategory?

    @Transaction
    @Query("SELECT * FROM tasks WHERE status = :status AND isTemplate = 0 ORDER BY createdAt DESC")
    fun getTasksByStatus(status: TaskStatus): Flow<List<TaskWithCategory>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE categoryId = :categoryId AND isTemplate = 0 ORDER BY createdAt DESC")
    fun getTasksByCategoryId(categoryId: String): Flow<List<TaskWithCategory>>

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE date(dueDate) = date('now', 'localtime')
        AND isTemplate = 0
        ORDER BY dueDate ASC
    """)
    fun getTodayTasks(): Flow<List<TaskWithCategory>>

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE dueDate < :currentTime AND status != 'COMPLETED' AND isTemplate = 0
        ORDER BY dueDate ASC
    """)
    fun getOverdueTasks(currentTime: LocalDateTime = LocalDateTime.now()): Flow<List<TaskWithCategory>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE isUrgent = 1 AND status != 'COMPLETED' AND isTemplate = 0 ORDER BY dueDate ASC")
    fun getUrgentTasks(): Flow<List<TaskWithCategory>>

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE createdAt BETWEEN :startDate AND :endDate AND isTemplate = 0
        ORDER BY createdAt DESC
    """)
    fun getTasksByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<TaskWithCategory>>

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') AND isTemplate = 0
        ORDER BY createdAt DESC
    """)
    fun searchTasks(query: String): Flow<List<TaskWithCategory>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long
    
    @Update
    suspend fun updateTask(task: TaskEntity)
    
    @Delete
    suspend fun deleteTask(task: TaskEntity)
    
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)
    
    @Query("DELETE FROM tasks WHERE status = 'COMPLETED'")
    suspend fun deleteCompletedTasks()
    
    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
    
    @Query("UPDATE tasks SET status = 'COMPLETED', completedAt = :completedAt WHERE id IN (:taskIds)")
    suspend fun markTasksAsCompleted(taskIds: List<String>, completedAt: LocalDateTime = LocalDateTime.now())
    
    @Query("UPDATE tasks SET categoryId = :categoryId WHERE id IN (:taskIds)")
    suspend fun bulkUpdateTaskCategory(taskIds: List<String>, categoryId: String)
    
    // 统计查询
    @Query("SELECT COUNT(*) FROM tasks WHERE isTemplate = 0")
    suspend fun getTotalTasksCount(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'COMPLETED' AND isTemplate = 0")
    suspend fun getCompletedTasksCount(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'PENDING' AND isTemplate = 0")
    suspend fun getPendingTasksCount(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'OVERDUE' AND isTemplate = 0")
    suspend fun getOverdueTasksCount(): Int
    
    @Query("""
        SELECT categoryId, COUNT(*) as count
        FROM tasks
        WHERE isTemplate = 0
        GROUP BY categoryId
    """)
    suspend fun getCategoryTaskCounts(): List<CategoryTaskCount>
    
    @Query("""
        SELECT AVG(actualDuration)
        FROM tasks
        WHERE status = 'COMPLETED' AND actualDuration > 0 AND isTemplate = 0
    """)
    suspend fun getAverageCompletionTime(): Double?

    @Query("SELECT MIN(createdAt) FROM tasks WHERE isTemplate = 0")
    suspend fun getEarliestTaskDate(): LocalDateTime?

    @Query("""
        SELECT * FROM tasks
        WHERE createdAt >= :weekStart AND createdAt <= :weekEnd AND isTemplate = 0
        ORDER BY createdAt ASC
    """)
    suspend fun getTasksInWeek(weekStart: LocalDateTime, weekEnd: LocalDateTime): List<TaskEntity>

    // ========== 重复任务相关查询 ==========

    @Transaction
    @Query("SELECT * FROM tasks WHERE isTemplate = 1")
    suspend fun getTemplateTasks(): List<TaskWithCategory>

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE templateTaskId = :templateId AND date(instanceDate) = date(:date)
        LIMIT 1
    """)
    suspend fun getTaskInstance(templateId: String, date: LocalDateTime): TaskWithCategory?

    @Query("""
        SELECT COUNT(*) FROM tasks
        WHERE templateTaskId = :templateId AND date(instanceDate) = date(:date)
    """)
    suspend fun hasInstanceForDate(templateId: String, date: LocalDateTime): Boolean
}

/**
 * Data class to hold category task count statistics
 */
data class CategoryTaskCount(
    val categoryId: String,
    val count: Int
) 