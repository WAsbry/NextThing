package com.example.nextthingb1.data.local.dao

import androidx.room.*
import com.example.nextthingb1.data.local.entity.NotificationStrategyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationStrategyDao {

    @Query("SELECT * FROM notification_strategies ORDER BY usageCount DESC, lastUsedAt DESC, createdAt DESC")
    fun getAllStrategies(): Flow<List<NotificationStrategyEntity>>

    @Query("SELECT * FROM notification_strategies WHERE id = :id")
    suspend fun getStrategyById(id: String): NotificationStrategyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStrategy(strategy: NotificationStrategyEntity): Long

    @Update
    suspend fun updateStrategy(strategy: NotificationStrategyEntity)

    @Delete
    suspend fun deleteStrategy(strategy: NotificationStrategyEntity)

    @Query("DELETE FROM notification_strategies WHERE id = :id")
    suspend fun deleteStrategyById(id: String)

    @Query("DELETE FROM notification_strategies")
    suspend fun deleteAllStrategies()

    @Query("UPDATE notification_strategies SET usageCount = usageCount + 1, lastUsedAt = :timestamp WHERE id = :id")
    suspend fun incrementUsageCount(id: String, timestamp: java.time.LocalDateTime)

    @Query("SELECT COUNT(*) FROM notification_strategies")
    suspend fun getStrategyCount(): Int
}