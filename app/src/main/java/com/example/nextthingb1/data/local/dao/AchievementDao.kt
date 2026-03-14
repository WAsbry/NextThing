package com.example.nextthingb1.data.local.dao

import androidx.room.*
import com.example.nextthingb1.data.local.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun getAllAchievementsOnce(): List<AchievementEntity>

    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getAchievementById(id: String): AchievementEntity?

    @Query("SELECT COUNT(*) FROM achievements WHERE isUnlocked = 1")
    suspend fun getUnlockedCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(achievements: List<AchievementEntity>)

    // 返回受影响的行数：>0 表示首次解锁（WHERE isUnlocked = 0 防重复）
    @Query("UPDATE achievements SET isUnlocked = 1, unlockedAt = :unlockedAt WHERE id = :id AND isUnlocked = 0")
    suspend fun unlockAchievement(id: String, unlockedAt: Long = System.currentTimeMillis()): Int
}
