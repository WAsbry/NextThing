package com.nextthing.app.data.repository

import com.nextthing.app.data.local.dao.AchievementDao
import com.nextthing.app.data.local.entity.AchievementEntity
import com.nextthing.app.domain.model.AchievementType
import com.nextthing.app.domain.repository.AchievementRepository
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
