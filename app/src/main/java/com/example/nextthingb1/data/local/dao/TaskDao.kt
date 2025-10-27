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
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskWithCategory>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskWithCategory?

    @Transaction
    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY createdAt DESC")
    fun getTasksByStatus(status: TaskStatus): Flow<List<TaskWithCategory>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE categoryId = :categoryId ORDER BY createdAt DESC")
    fun getTasksByCategoryId(categoryId: String): Flow<List<TaskWithCategory>>

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE date(dueDate) = date('now', 'localtime')
        ORDER BY dueDate ASC
    """)
    fun getTodayTasks(): Flow<List<TaskWithCategory>>

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE dueDate < :currentTime AND status != 'COMPLETED'
        ORDER BY dueDate ASC
    """)
    fun getOverdueTasks(currentTime: LocalDateTime = LocalDateTime.now()): Flow<List<TaskWithCategory>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE isUrgent = 1 AND status != 'COMPLETED' ORDER BY dueDate ASC")
    fun getUrgentTasks(): Flow<List<TaskWithCategory>>

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE createdAt BETWEEN :startDate AND :endDate
        ORDER BY createdAt DESC
    """)
    fun getTasksByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<TaskWithCategory>>

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'
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
    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTotalTasksCount(): Int
    
    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'COMPLETED'")
    suspend fun getCompletedTasksCount(): Int
    
    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'PENDING'")
    suspend fun getPendingTasksCount(): Int
    
    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'OVERDUE'")
    suspend fun getOverdueTasksCount(): Int
    
    @Query("""
        SELECT categoryId, COUNT(*) as count
        FROM tasks
        GROUP BY categoryId
    """)
    suspend fun getCategoryTaskCounts(): List<CategoryTaskCount>
    
    @Query("""
        SELECT AVG(actualDuration)
        FROM tasks
        WHERE status = 'COMPLETED' AND actualDuration > 0
    """)
    suspend fun getAverageCompletionTime(): Double?

    @Query("SELECT MIN(createdAt) FROM tasks")
    suspend fun getEarliestTaskDate(): LocalDateTime?

    @Query("""
        SELECT * FROM tasks
        WHERE createdAt >= :weekStart AND createdAt <= :weekEnd
        ORDER BY createdAt ASC
    """)
    suspend fun getTasksInWeek(weekStart: LocalDateTime, weekEnd: LocalDateTime): List<TaskEntity>
}

/**
 * Data class to hold category task count statistics
 */
data class CategoryTaskCount(
    val categoryId: String,
    val count: Int
) 