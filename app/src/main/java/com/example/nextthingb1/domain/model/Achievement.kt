package com.example.nextthingb1.domain.model

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
    val threshold: Int
) {
    // ── 任务达人（累计完成数）──
    TASK_10(
        "初露锋芒", "累计完成 10 个任务", "🌱",
        AchievementCategory.TASK_MASTER, AchievementTier.BRONZE, 10
    ),
    TASK_50(
        "效率能手", "累计完成 50 个任务", "⚡",
        AchievementCategory.TASK_MASTER, AchievementTier.SILVER, 50
    ),
    TASK_200(
        "任务达人", "累计完成 200 个任务", "🏆",
        AchievementCategory.TASK_MASTER, AchievementTier.GOLD, 200
    ),
    TASK_500(
        "执行之王", "累计完成 500 个任务", "👑",
        AchievementCategory.TASK_MASTER, AchievementTier.DIAMOND, 500
    ),

    // ── 坚持系列（连续完成天数）──
    STREAK_3(
        "三天打鱼", "连续 3 天完成任务", "🔥",
        AchievementCategory.PERSISTENCE, AchievementTier.BRONZE, 3
    ),
    STREAK_7(
        "周周坚持", "连续 7 天完成任务", "💪",
        AchievementCategory.PERSISTENCE, AchievementTier.SILVER, 7
    ),
    STREAK_30(
        "月度坚持", "连续 30 天完成任务", "🌟",
        AchievementCategory.PERSISTENCE, AchievementTier.GOLD, 30
    ),
    STREAK_100(
        "百日不辍", "连续 100 天完成任务", "💎",
        AchievementCategory.PERSISTENCE, AchievementTier.DIAMOND, 100
    ),

    // ── 效率系列 ──
    PERFECT_DAY(
        "全勤一日", "单日完成 3 个以上任务且全部完成", "✨",
        AchievementCategory.EFFICIENCY, AchievementTier.BRONZE, 1
    ),
    PERFECT_WEEK(
        "全勤一周", "某一自然周完成 10 个以上任务且全部完成", "🎯",
        AchievementCategory.EFFICIENCY, AchievementTier.SILVER, 1
    ),
    NO_OVERDUE_7(
        "零逾期周", "连续 7 天无逾期任务", "🛡️",
        AchievementCategory.EFFICIENCY, AchievementTier.GOLD, 7
    ),
    HIGH_RATE_30(
        "效率大师", "连续 30 天任务完成率 ≥ 80%", "🏅",
        AchievementCategory.EFFICIENCY, AchievementTier.DIAMOND, 30
    ),

    // ── 多面手系列 ──
    CATEGORY_2(
        "初步涉猎", "在 2 个不同分类中完成任务", "📋",
        AchievementCategory.VERSATILE, AchievementTier.BRONZE, 2
    ),
    CATEGORY_4(
        "全面发展", "在 4 个不同分类中完成任务", "🎨",
        AchievementCategory.VERSATILE, AchievementTier.SILVER, 4
    ),
    GEOFENCE_3(
        "地理达人", "设置 3 个地理围栏地点", "📍",
        AchievementCategory.VERSATILE, AchievementTier.GOLD, 3
    ),
    CATEGORY_ALL_10(
        "全能王者", "在 5 个分类中各完成 10 个任务", "🌈",
        AchievementCategory.VERSATILE, AchievementTier.DIAMOND, 10
    ),

    // ── 里程碑系列 ──
    FIRST_TASK(
        "新手上路", "创建第 1 个任务", "🎬",
        AchievementCategory.MILESTONE, AchievementTier.BRONZE, 1
    ),
    USAGE_30(
        "忠实用户", "使用 App 满 30 天", "📅",
        AchievementCategory.MILESTONE, AchievementTier.SILVER, 30
    ),
    REPEAT_5(
        "重复高手", "创建 5 个重复任务", "🔄",
        AchievementCategory.MILESTONE, AchievementTier.GOLD, 5
    ),
    USAGE_365(
        "老用户", "使用 App 满 365 天", "🏠",
        AchievementCategory.MILESTONE, AchievementTier.DIAMOND, 365
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
}
