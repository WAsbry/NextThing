package com.example.nextthingb1.domain.usecase

import com.example.nextthingb1.data.service.AchievementChecker
import com.example.nextthingb1.domain.model.AchievementProgress
import com.example.nextthingb1.domain.model.AchievementType
import com.example.nextthingb1.domain.repository.AchievementRepository
import javax.inject.Inject

data class AchievementUseCases @Inject constructor(
    val getAchievements: GetAchievementsUseCase,
    val checkAndUnlock: CheckAndUnlockUseCase,
    val getUnlockedCount: GetUnlockedCountUseCase
)

/**
 * 获取所有成就及当前进度（一次性快照，在 ViewModel 协程中调用）
 */
class GetAchievementsUseCase @Inject constructor(
    private val repository: AchievementRepository,
    private val checker: AchievementChecker
) {
    suspend operator fun invoke(): List<AchievementProgress> {
        repository.initializeIfNeeded()

        val progressMap = checker.calculateAllProgress()
        val unlockStates = repository.getAllUnlockStatesOnce()

        return AchievementType.entries.map { type ->
            AchievementProgress(
                type = type,
                currentValue = progressMap[type] ?: 0,
                isUnlocked = unlockStates[type.name] != null,
                unlockedAt = unlockStates[type.name]
            )
        }
    }
}

/**
 * 检查所有成就进度并自动解锁达标成就
 * @return Pair：全量进度列表 + 本次新解锁的成就列表（用于触发庆祝动画）
 * 进度计算只执行一次，避免重复计算
 */
class CheckAndUnlockUseCase @Inject constructor(
    private val repository: AchievementRepository,
    private val checker: AchievementChecker
) {
    suspend operator fun invoke(): Pair<List<AchievementProgress>, List<AchievementType>> {
        repository.initializeIfNeeded()

        val progressMap = checker.calculateAllProgress()
        val newlyUnlocked = mutableListOf<AchievementType>()

        for (type in AchievementType.entries) {
            val currentValue = progressMap[type] ?: 0
            if (currentValue >= type.threshold) {
                val isNew = repository.unlockAchievement(type)
                if (isNew) newlyUnlocked.add(type)
            }
        }

        val unlockStates = repository.getAllUnlockStatesOnce()
        val achievements = AchievementType.entries.map { type ->
            AchievementProgress(
                type = type,
                currentValue = progressMap[type] ?: 0,
                isUnlocked = unlockStates[type.name] != null,
                unlockedAt = unlockStates[type.name]
            )
        }

        return achievements to newlyUnlocked
    }
}

/**
 * 获取已解锁成就数量（Settings 页概览用）
 */
class GetUnlockedCountUseCase @Inject constructor(
    private val repository: AchievementRepository
) {
    suspend operator fun invoke(): Int {
        repository.initializeIfNeeded()
        return repository.getUnlockedCount()
    }
}
