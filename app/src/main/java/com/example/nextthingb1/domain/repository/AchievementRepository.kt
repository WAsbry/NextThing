package com.example.nextthingb1.domain.repository

import com.example.nextthingb1.domain.model.AchievementType

interface AchievementRepository {

    /** 获取所有成就的解锁状态快照（一次性读取） */
    suspend fun getAllUnlockStatesOnce(): Map<String, Long?>

    /** 获取已解锁数量 */
    suspend fun getUnlockedCount(): Int

    /**
     * 解锁指定成就
     * @return true = 首次解锁（可触发庆祝动画）；false = 已解锁，跳过
     */
    suspend fun unlockAchievement(type: AchievementType): Boolean

    /** 首次启动时初始化 20 条初始记录（已初始化则跳过） */
    suspend fun initializeIfNeeded()
}
