package com.nextthing.app.domain.model

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class AchievementCategory(val displayName: String, val icon: String) {
    TASK_MASTER("任务达人", "📝"),
    PERSISTENCE("坚持系列", "🔥"),
    EFFICIENCY("效率系列", "⚡"),
    VERSATILE("多面手", "🎨"),
    MILESTONE("里程碑", "🎬")
}

enum class AchievementTier(val displayName: String) {
    BRONZE("铜"),
    SILVER("银"),
    GOLD("金"),
    DIAMOND("钻")
}

enum class AchievementType(
    val displayName: String,
    val description: String,
    val icon: String,
    val category: AchievementCategory,
    val tier: AchievementTier,
    val threshold: Int,
    val tip: String
) {
    // ── 任务达人（累计完成数）──
    TASK_10(
        "初露锋芒", "累计完成 10 个任务", "🌱",
        AchievementCategory.TASK_MASTER, AchievementTier.BRONZE, 10,
        "每天完成一个小任务，积少成多"
    ),
    TASK_50(
        "效率能手", "累计完成 50 个任务", "⚡",
        AchievementCategory.TASK_MASTER, AchievementTier.SILVER, 50,
        "试着把大任务拆解成小步骤，逐个击破"
    ),
    TASK_200(
        "任务达人", "累计完成 200 个任务", "🏆",
        AchievementCategory.TASK_MASTER, AchievementTier.GOLD, 200,
        "你已经是任务管理的老手了，继续保持！"
    ),
    TASK_500(
        "执行之王", "累计完成 500 个任务", "👑",
        AchievementCategory.TASK_MASTER, AchievementTier.DIAMOND, 500,
        "传说中的执行力大师，非你莫属"
    ),

    // ── 坚持系列（连续完成天数）──
    STREAK_3(
        "三天打鱼", "连续 3 天完成任务", "🔥",
        AchievementCategory.PERSISTENCE, AchievementTier.BRONZE, 3,
        "万事开头难，连续三天就是好的开始"
    ),
    STREAK_7(
        "周周坚持", "连续 7 天完成任务", "💪",
        AchievementCategory.PERSISTENCE, AchievementTier.SILVER, 7,
        "设置每日提醒，养成每天完成任务的习惯"
    ),
    STREAK_30(
        "月度坚持", "连续 30 天完成任务", "🌟",
        AchievementCategory.PERSISTENCE, AchievementTier.GOLD, 30,
        "21天养成习惯，30天巩固习惯，你做到了！"
    ),
    STREAK_100(
        "百日不辍", "连续 100 天完成任务", "💎",
        AchievementCategory.PERSISTENCE, AchievementTier.DIAMOND, 100,
        "百日坚持，是自律的最好证明"
    ),

    // ── 效率系列 ──
    PERFECT_DAY(
        "全勤一日", "单日完成 3 个以上任务且全部完成", "✨",
        AchievementCategory.EFFICIENCY, AchievementTier.BRONZE, 1,
        "合理规划当日任务数量，确保全部完成"
    ),
    PERFECT_WEEK(
        "全勤一周", "某一自然周完成 10 个以上任务且全部完成", "🎯",
        AchievementCategory.EFFICIENCY, AchievementTier.SILVER, 1,
        "周一规划好本周任务，逐日攻克"
    ),
    NO_OVERDUE_7(
        "零逾期周", "连续 7 天无逾期任务", "🛡️",
        AchievementCategory.EFFICIENCY, AchievementTier.GOLD, 7,
        "提前设置提醒，避免任务逾期"
    ),
    HIGH_RATE_30(
        "效率大师", "连续 30 天任务完成率 ≥ 80%", "🏅",
        AchievementCategory.EFFICIENCY, AchievementTier.DIAMOND, 30,
        "保持高完成率的秘诀：少承诺，多交付"
    ),

    // ── 多面手系列 ──
    CATEGORY_2(
        "初步涉猎", "在 2 个不同分类中完成任务", "📋",
        AchievementCategory.VERSATILE, AchievementTier.BRONZE, 2,
        "尝试在不同领域创建任务，拓宽视野"
    ),
    CATEGORY_4(
        "全面发展", "在 4 个不同分类中完成任务", "🎨",
        AchievementCategory.VERSATILE, AchievementTier.SILVER, 4,
        "生活不止一面，工作、学习、运动都兼顾"
    ),
    GEOFENCE_3(
        "地理达人", "设置 3 个地理围栏地点", "📍",
        AchievementCategory.VERSATILE, AchievementTier.GOLD, 3,
        "把常去的地方设为围栏，到达时自动提醒"
    ),
    CATEGORY_ALL_10(
        "全能王者", "在 5 个分类中各完成 10 个任务", "🌈",
        AchievementCategory.VERSATILE, AchievementTier.DIAMOND, 10,
        "真正的全能选手，各领域均衡发展"
    ),

    // ── 里程碑系列 ──
    FIRST_TASK(
        "新手上路", "创建第 1 个任务", "🎬",
        AchievementCategory.MILESTONE, AchievementTier.BRONZE, 1,
        "千里之行，始于足下"
    ),
    USAGE_30(
        "忠实用户", "使用 App 满 30 天", "📅",
        AchievementCategory.MILESTONE, AchievementTier.SILVER, 30,
        "感谢你的持续使用，我们会越来越好"
    ),
    REPEAT_5(
        "重复高手", "创建 5 个重复任务", "🔄",
        AchievementCategory.MILESTONE, AchievementTier.GOLD, 5,
        "重复任务适合日常习惯，如每日运动、阅读"
    ),
    USAGE_365(
        "老用户", "使用 App 满 365 天", "🏠",
        AchievementCategory.MILESTONE, AchievementTier.DIAMOND, 365,
        "一年的陪伴，你是我们最珍贵的用户"
    )
}

data class AchievementProgress(
    val type: AchievementType,
    val currentValue: Int,
    val isUnlocked: Boolean,
    val unlockedAt: Long? = null
) {
    val progress: Float
        get() = (currentValue.toFloat() / type.threshold).coerceIn(0f, 1f)

    /**
     * 格式化的解锁时间文本
     * @return "2026-03-15 14:30" 或 null
     */
    val formattedUnlockTime: String?
        get() {
            val ts = unlockedAt ?: return null
            return try {
                val dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(ts),
                    ZoneId.systemDefault()
                )
                dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            } catch (e: Exception) {
                null
            }
        }

    /**
     * 距离解锁还差多少的描述文本
     */
    val remainingText: String?
        get() {
            if (isUnlocked) return null
            val remaining = type.threshold - currentValue
            return when (type.category) {
                AchievementCategory.TASK_MASTER -> "还需完成 $remaining 个任务"
                AchievementCategory.PERSISTENCE -> "还需连续 $remaining 天"
                AchievementCategory.EFFICIENCY -> when (type) {
                    AchievementType.PERFECT_DAY -> "完成一次全勤日即可解锁"
                    AchievementType.PERFECT_WEEK -> "完成一次全勤周即可解锁"
                    AchievementType.NO_OVERDUE_7 -> "还需连续 $remaining 天无逾期"
                    AchievementType.HIGH_RATE_30 -> "还需连续 $remaining 天高完成率"
                    else -> "还需 $remaining"
                }
                AchievementCategory.VERSATILE -> when (type) {
                    AchievementType.GEOFENCE_3 -> "还需设置 $remaining 个地点"
                    AchievementType.CATEGORY_ALL_10 -> "还需在更多分类中完成任务"
                    else -> "还需覆盖 $remaining 个分类"
                }
                AchievementCategory.MILESTONE -> when (type) {
                    AchievementType.FIRST_TASK -> "创建你的第一个任务吧"
                    AchievementType.REPEAT_5 -> "还需创建 $remaining 个重复任务"
                    else -> "还需使用 $remaining 天"
                }
            }
        }
}
