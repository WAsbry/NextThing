package com.example.nextthingb1.data.local.dao

import androidx.room.*
import com.example.nextthingb1.data.local.entity.TaskEntity
import com.example.nextthingb1.domain.model.TaskCategory
import com.example.nextthingb1.domain.model.TaskPriority
import com.example.nextthingb1.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

// 分类统计承载类
data class CategoryCount(
    val category: TaskCategory,
    val count: Int
)

@Dao
interface TaskDao {
    
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?
    
    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY createdAt DESC")
    fun getTasksByStatus(status: TaskStatus): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE category = :category ORDER BY createdAt DESC")
    fun getTasksByCategory(category: TaskCategory): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE priority = :priority ORDER BY createdAt DESC")
    fun getTasksByPriority(priority: TaskPriority): Flow<List<TaskEntity>>
    
    @Query("""
        SELECT * FROM tasks 
        WHERE date(createdAt) = date('now', 'localtime') 
        ORDER BY priority DESC, createdAt ASC
    """)
    fun getTodayTasks(): Flow<List<TaskEntity>>
    
    @Query("""
        SELECT * FROM tasks 
        WHERE dueDate < :currentTime AND status != 'COMPLETED' 
        ORDER BY dueDate ASC
    """)
    fun getOverdueTasks(currentTime: LocalDateTime = LocalDateTime.now()): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE isUrgent = 1 AND status != 'COMPLETED' ORDER BY dueDate ASC")
    fun getUrgentTasks(): Flow<List<TaskEntity>>
    
    @Query("""
        SELECT * FROM tasks 
        WHERE createdAt BETWEEN :startDate AND :endDate 
        ORDER BY createdAt DESC
    """)
    fun getTasksByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<TaskEntity>>
    
    @Query("""
        SELECT * FROM tasks 
        WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchTasks(query: String): Flow<List<TaskEntity>>
    
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
    
    @Query("UPDATE tasks SET category = :category WHERE id IN (:taskIds)")
    suspend fun bulkUpdateTaskCategory(taskIds: List<String>, category: TaskCategory)
    
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
        SELECT category as category, COUNT(*) as count 
        FROM tasks 
        GROUP BY category
    """)
    suspend fun getCategoryStatistics(): List<CategoryCount>
    
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