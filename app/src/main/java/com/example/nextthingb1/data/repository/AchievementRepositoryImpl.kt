package com.example.nextthingb1.data.repository

import com.example.nextthingb1.data.local.dao.AchievementDao
import com.example.nextthingb1.data.local.entity.AchievementEntity
import com.example.nextthingb1.domain.model.AchievementType
import com.example.nextthingb1.domain.repository.AchievementRepository
import javax.inject.Inject

class AchievementRepositoryImpl @Inject constructor(
    private val achievementDao: AchievementDao
) : AchievementRepository {

    override suspend fun getAllUnlockStatesOnce(): Map<String, Long?> {
        return achievementDao.getAllAchievementsOnce()
            .associate { entity ->
                entity.id to if (entity.isUnlocked) entity.unlockedAt else null
            }
    }

    override suspend fun getUnlockedCount(): Int {
        return achievementDao.getUnlockedCount()
    }

    override suspend fun unlockAchievement(type: AchievementType): Boolean {
        val affected = achievementDao.unlockAchievement(
            id = type.name,
            unlockedAt = System.currentTimeMillis()
        )
        return affected > 0
    }

    override suspend fun initializeIfNeeded() {
        val existing = achievementDao.getAchievementById(AchievementType.FIRST_TASK.name)
        if (existing != null) return

        val entities = AchievementType.entries.map { type ->
            AchievementEntity(id = type.name, isUnlocked = false, unlockedAt = null)
        }
        achievementDao.insertAll(entities)
    }
}
