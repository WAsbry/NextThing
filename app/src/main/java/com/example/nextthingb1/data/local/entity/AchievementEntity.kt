package com.example.nextthingb1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey
    val id: String,              // 对应 AchievementType.name，如 "TASK_10"
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null // 解锁时间戳（毫秒）
)
