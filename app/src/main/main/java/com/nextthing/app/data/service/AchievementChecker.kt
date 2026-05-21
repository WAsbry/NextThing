package com.nextthing.app.data.service

import com.nextthing.app.data.local.dao.GeofenceLocationDao
import com.nextthing.app.data.local.dao.TaskDao
import com.nextthing.app.data.local.dao.UserDao
import com.nextthing.app.domain.model.AchievementType
import com.nextthing.app.domain.model.TaskStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

class AchievementChecker @Inject constructor(
    private val taskDao: TaskDao,
    private val geofenceLocationDao: GeofenceLocationDao,
    private val userDao: UserDao
) {
    // 每日聚合数据（内部使用）
    private data class DayStat(
        val total: Int,
        val completed: Int,
        val hasOverdue: Boolean
    )

    /**
     * 计算所有 20 个成就的当前进度值
     * 返回 Map<AchievementType, currentValue>
     */
    suspend fun calculateAllProgress(): Map<AchievementType, Int> {
        val result = mutableMapOf<AchievementType, Int>()

        // ─── 批量查询，减少 SQL 次数 ───

        val completedCount = taskDao.getCompletedTasksCount()
        val totalCount = taskDao.getTotalTasksCount()
        val geofenceCount = geofenceLocationDao.getCount()
        val user = userDao.getCurrentUserOnce()
        val userCreatedAt = user?.createdAt ?: System.currentTimeMillis()
        val templateCount = taskDao.getTemplateTasksCount()
        val completedByCategory = taskDao.getCompletedTaskCountByCategory()

        // 最近 120 天任务（createdAt 范围，覆盖连续天数 + 效率计算所需数据）
        val today = LocalDate.now()
        val recentTasks = taskDao.getTasksByDateRangeOnce(
            startDate = today.minusDays(120).atStartOfDay(),
            endDate = today.atTime(LocalTime.MAX)
        )

        // ─── 按日期分组构建每日统计 ───
        // 使用 dueDate 优先（若为空则用 createdAt）作为任务所属日期
        val dailyStats = mutableMapOf<LocalDate, DayStat>()
        for (twc in recentTasks) {
            val task = twc.task
            val date = (task.dueDate ?: task.createdAt).toLocalDate()
            val prev = dailyStats.getOrDefault(date, DayStat(0, 0, false))
            dailyStats[date] = DayStat(
                total = prev.total + 1,
                completed = prev.completed + if (task.status == TaskStatus.COMPLETED) 1 else 0,
                hasOverdue = prev.hasOverdue || task.status == TaskStatus.OVERDUE
            )
        }

        // ─── 任务达人系列：直接用累计完成数 ───
        result[AchievementType.TASK_10] = completedCount
        result[AchievementType.TASK_50] = completedCount
        result[AchievementType.TASK_200] = completedCount
        result[AchievementType.TASK_500] = completedCount

        // ─── 坚持系列：从今天倒序计算连续完成天数 ───
        val streak = calcStreak(today, dailyStats)
        result[AchievementType.STREAK_3] = streak
        result[AchievementType.STREAK_7] = streak
        result[AchievementType.STREAK_30] = streak
        result[AchievementType.STREAK_100] = streak

        // ─── 效率系列 ───

        // PERFECT_DAY：某天 total>=3 且 completed==total
        val hasPerfectDay = dailyStats.any { (_, s) ->
            s.total >= 3 && s.completed == s.total
        }
        result[AchievementType.PERFECT_DAY] = if (hasPerfectDay) 1 else 0

        // PERFECT_WEEK：某自然周(周一~周日) total>=10 且 completed==total
        val hasPerfectWeek = calcPerfectWeek(today, dailyStats)
        result[AchievementType.PERFECT_WEEK] = if (hasPerfectWeek) 1 else 0

        // NO_OVERDUE_7：从今天倒序，连续无逾期天数（无任务日跳过，最多检查 30 天）
        var noOverdueStreak = 0
        var checkDate = today
        for (i in 0 until 30) {
            val stat = dailyStats[checkDate]
            when {
                stat == null || stat.total == 0 -> { /* 无任务日跳过，不中断也不计数 */ }
                stat.hasOverdue -> break  // 有逾期，中断
                else -> noOverdueStreak++  // 有任务且无逾期，计数
            }
            checkDate = checkDate.minusDays(1)
        }
        result[AchievementType.NO_OVERDUE_7] = noOverdueStreak

        // HIGH_RATE_30：连续完成率>=80% 天数（无任务日跳过，最多检查 60 天）
        var highRateStreak = 0
        checkDate = today
        for (i in 0 until 60) {
            val stat = dailyStats[checkDate]
            when {
                stat == null || stat.total == 0 -> { /* 无任务日跳过，不中断也不计数 */ }
                stat.completed.toFloat() / stat.total >= 0.8f -> highRateStreak++
                else -> break  // 完成率不足，中断
            }
            checkDate = checkDate.minusDays(1)
        }
        result[AchievementType.HIGH_RATE_30] = highRateStreak

        // ─── 多面手系列 ───

        // 有已完成任务的分类数（全量，不限时间）
        val completedCategoryCount = completedByCategory.count { it.count > 0 }
        result[AchievementType.CATEGORY_2] = completedCategoryCount
        result[AchievementType.CATEGORY_4] = completedCategoryCount

        // GEOFENCE_3
        result[AchievementType.GEOFENCE_3] = geofenceCount

        // CATEGORY_ALL_10：必须在 5 个分类中各完成 10 个任务才能解锁
        // 不足 5 个分类时直接返回 0（缺失分类的完成数视为 0）
        val minPerCategory = if (completedByCategory.size >= 5) {
            completedByCategory.minOf { it.count }
        } else {
            0
        }
        result[AchievementType.CATEGORY_ALL_10] = minPerCategory

        // ─── 里程碑系列 ───
        result[AchievementType.FIRST_TASK] = totalCount

        val usageDays = ((System.currentTimeMillis() - userCreatedAt) / 86_400_000L)
            .toInt().coerceAtLeast(if (user != null) 1 else 0)
        result[AchievementType.USAGE_30] = usageDays
        result[AchievementType.USAGE_365] = usageDays

        result[AchievementType.REPEAT_5] = templateCount

        return result
    }

    /**
     * 从今天倒序计算连续完成天数
     * 允许今天暂无完成任务（跳过今天从昨天开始）
     */
    private fun calcStreak(today: LocalDate, dailyStats: Map<LocalDate, DayStat>): Int {
        var streak = 0
        var checkDate = today
        var skippedToday = false

        while (streak < 100) {
            val stat = dailyStats[checkDate]
            when {
                stat != null && stat.completed > 0 -> {
                    streak++
                    checkDate = checkDate.minusDays(1)
                }
                !skippedToday && checkDate == today -> {
                    // 今天还没有完成任务，允许跳过今天从昨天开始算
                    skippedToday = true
                    checkDate = checkDate.minusDays(1)
                }
                else -> break
            }
        }
        return streak
    }

    /**
     * 检查最近 16 个自然周（周一~周日）中是否存在某周：
     * total >= 10 且 completed == total
     */
    private fun calcPerfectWeek(today: LocalDate, dailyStats: Map<LocalDate, DayStat>): Boolean {
        for (weekOffset in 0L until 16L) {
            val weekStart = today.minusWeeks(weekOffset).with(DayOfWeek.MONDAY)
            var weekTotal = 0
            var weekCompleted = 0
            for (dayOffset in 0L until 7L) {
                val stat = dailyStats[weekStart.plusDays(dayOffset)] ?: continue
                weekTotal += stat.total
                weekCompleted += stat.completed
            }
            if (weekTotal >= 10 && weekCompleted == weekTotal) return true
        }
        return false
    }
}
