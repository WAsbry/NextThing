package com.example.nextthingb1.presentation.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextthingb1.domain.usecase.TaskUseCases
import com.example.nextthingb1.domain.model.TaskCategory
import com.example.nextthingb1.domain.model.TaskStatus
import com.example.nextthingb1.domain.model.TaskImportanceUrgency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class StatsTab(val title: String) {
    OVERVIEW("概览"),
    CATEGORY("分类"),
    TREND("趋势"),
    EFFICIENCY("效率")
}

data class StatsUiState(
    val selectedTab: StatsTab = StatsTab.OVERVIEW,
    val currentMonth: String = "",
    // 概览数据
    val totalTasks: Int = 0,
    val pendingTasks: Int = 0,        // 未完成
    val completedTasks: Int = 0,      // 已完成
    val deferredTasks: Int = 0,       // 延期
    val overdueTasks: Int = 0,        // 逾期
    val cancelledTasks: Int = 0,      // 放弃
    val completionRate: Float = 0f,
    // 重要程度分布
    val importantUrgentCount: Int = 0,
    val importantNotUrgentCount: Int = 0,
    val notImportantUrgentCount: Int = 0,
    val notImportantNotUrgentCount: Int = 0,
    // 智能洞察
    val insights: List<InsightData> = emptyList(),
    // 任务健康度
    val healthScore: Int = 0,
    val healthLevel: HealthLevel = HealthLevel.GOOD,
    // 本周vs上周对比
    val weekComparison: WeekComparisonData? = null,
    // 分类统计
    val categoryStats: Map<TaskCategory, CategoryStatsData> = emptyMap(),
    // 新增：分类饼图选中状态
    val selectedCategory: TaskCategory? = null,
    // 新增：分类效率排行
    val categoryEfficiencyRanking: List<CategoryEfficiencyData> = emptyList(),
    // 新增：分类×星期热力图数据
    val categoryWeekdayHeatmap: Map<TaskCategory, Map<Int, Int>> = emptyMap(),
    // 趋势数据
    val weeklyTrend: List<DailyTrendData> = emptyList(),
    val monthlyTrend: List<WeeklyTrendData> = emptyList(),
    val trendViewMode: TrendViewMode = TrendViewMode.WEEK,
    // 新增：时间范围选择器
    val selectedTimeRange: TimeRange = TimeRange.WEEK_7,
    val customStartDate: LocalDate? = null,
    val customEndDate: LocalDate? = null,
    // 新增：月历热力图（GitHub风格）
    val calendarHeatmap: List<CalendarHeatmapData> = emptyList(),
    val calendarStats: CalendarHeatmapStats? = null,
    // 新增：任务积压趋势
    val backlogTrend: List<BacklogTrendData> = emptyList(),
    val backlogThreshold: Int = 20,  // 积压预警阈值
    // 新增：完成速度加速度
    val velocityAcceleration: List<VelocityAccelerationData> = emptyList(),
    // 效率数据
    val completionTimeByCategory: Map<TaskCategory, Double> = emptyMap(),
    val completionTimeByImportance: Map<TaskImportanceUrgency, Double> = emptyMap(),
    val onTimeCompletionRate: Float = 0f,
    val overdueCompletionRate: Float = 0f,
    val subtaskCompletionData: List<SubtaskStatsData> = emptyList(),
    // 新增：时间热力图
    val timeHeatmap: List<TimeHeatmapData> = emptyList(),
    val timeHeatmapStats: TimeHeatmapStats? = null,
    // 新增：拖延分析雷达图
    val procrastinationRadar: ProcrastinationRadarData? = null,
    // 新增：任务完成漏斗
    val taskFunnel: TaskFunnelData? = null,
    // 新增：延迟成本分析
    val delayAnalysis: DelayAnalysisData? = null,
    // UI 状态
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastUpdateTime: LocalDateTime = LocalDateTime.now()
)

data class CategoryStatsData(
    val category: TaskCategory,
    val totalCount: Int,
    val completedCount: Int,
    val completionRate: Float,
    val averageDuration: Double,
    // 新增：详细状态分布
    val pendingCount: Int = 0,
    val overdueCount: Int = 0,
    val cancelledCount: Int = 0,
    // 新增：效率分数
    val efficiencyScore: Int = 0
)

data class DailyTrendData(
    val date: LocalDate,
    val createdCount: Int,
    val completedCount: Int,
    val completionRate: Float
)

data class WeeklyTrendData(
    val weekLabel: String,
    val createdCount: Int,
    val completedCount: Int,
    val completionRate: Float
)

data class SubtaskStatsData(
    val taskTitle: String,
    val totalSubtasks: Int,
    val completedSubtasks: Int,
    val completionRate: Float
)

enum class TrendViewMode {
    WEEK,
    MONTH
}

// 洞察数据类型
enum class InsightType {
    POSITIVE,   // 积极洞察（绿色）
    WARNING,    // 警告（橙色）
    ALERT       // 警报（红色）
}

// 洞察数据
data class InsightData(
    val type: InsightType,
    val icon: String,
    val message: String
)

// 健康度等级
enum class HealthLevel(val displayName: String, val minScore: Int) {
    EXCELLENT("优秀", 85),
    GOOD("良好", 70),
    AVERAGE("一般", 50),
    POOR("待改进", 0)
}

// 本周vs上周对比数据
data class WeekComparisonData(
    val thisWeekCompleted: Int,
    val lastWeekCompleted: Int,
    val completedChange: Int,           // 正数=增长，负数=下降
    val thisWeekCompletionRate: Float,
    val lastWeekCompletionRate: Float,
    val completionRateChange: Float,
    val thisWeekAvgDuration: Double,
    val lastWeekAvgDuration: Double,
    val avgDurationChange: Double
)

// 分类效率排行数据
data class CategoryEfficiencyData(
    val category: TaskCategory,
    val efficiencyScore: Int,
    val rank: Int,
    val completionRate: Float,
    val avgDuration: Double,
    val overdueRate: Float
)

// 趋势Tab新增数据结构

// 时间范围枚举
enum class TimeRange(val displayName: String, val days: Int) {
    WEEK_7("最近7天", 7),
    DAYS_30("最近30天", 30),
    DAYS_90("最近90天", 90),
    ALL("全部", Int.MAX_VALUE),
    CUSTOM("自定义", 0)
}

// 月历热力图数据（GitHub风格）
data class CalendarHeatmapData(
    val date: LocalDate,
    val completedCount: Int,
    val level: Int  // 0-4，对应白色到深绿色
)

// 月历热力图统计
data class CalendarHeatmapStats(
    val maxStreakDays: Int,          // 🔥 最长连续完成天数
    val currentStreakDays: Int,       // 当前连续天数
    val maxGapDays: Int,              // ❄️ 最长中断天数
    val currentMonthCompleted: Int    // 📅 本月累计完成
)

// 任务积压趋势数据点
data class BacklogTrendData(
    val date: LocalDate,
    val backlogCount: Int,        // 未完成任务总数（积压量）
    val newTasksCount: Int,       // 当日新增任务数量
    val isOverThreshold: Boolean  // 是否超过预警阈值
)

// 完成速度加速度数据（按周）
data class VelocityAccelerationData(
    val weekLabel: String,        // "第X周"
    val weekStartDate: LocalDate,
    val completedCount: Int,      // 本周完成数量
    val acceleration: Int,        // 加速度（本周 - 上周）
    val isAcceleration: Boolean   // true=加速，false=减速
)

// 效率Tab新增数据结构

// 时间段枚举
enum class TimeSlot(val displayName: String, val emoji: String, val hourRange: IntRange) {
    MIDNIGHT("凌晨", "🌙", 0..3),
    DAWN("早晨", "🌅", 4..7),
    MORNING("上午", "☀️", 8..11),
    AFTERNOON("下午", "🌤️", 12..15),
    EVENING("傍晚", "🌆", 16..19),
    NIGHT("深夜", "🌃", 20..23)
}

// 时间热力图数据
data class TimeHeatmapData(
    val dayOfWeek: Int,      // 1-7（周一到周日）
    val timeSlot: TimeSlot,
    val completedCount: Int,
    val level: Int           // 0-4，颜色等级
)

// 时间热力图统计
data class TimeHeatmapStats(
    val mostProductiveSlot: Pair<Int, TimeSlot>?,  // 🏆 最高效时段（周X，时间段）
    val mostProductiveCount: Int,
    val leastProductiveSlot: Pair<Int, TimeSlot>?,  // 😴 最低效时段
    val leastProductiveCount: Int
)

// 拖延分析雷达图数据
data class ProcrastinationRadarData(
    val onTimeRate: Float,              // ⏰ 准时完成率 0-100
    val responseSpeed: Float,           // 🚀 任务响应速度 0-100（标准化）
    val completionSpeed: Float,         // ⚡ 平均完成时长 0-100（反向标准化，越快越高）
    val importantPriority: Float,       // 🔥 重要任务优先度 0-100
    val completionStability: Float,     // 📊 完成稳定性 0-100（标准差越小越高）
    val goalAchievementRate: Float,     // 🎯 目标达成率 0-100
    val totalScore: Int,                // 总分 0-100
    val efficiencyGrade: String,        // A+/A/B/C/D
    val strongestDimension: String,     // 最强项
    val strongestScore: Float,
    val weakestDimension: String,       // 待提升
    val weakestScore: Float
)

// 任务完成漏斗数据
data class TaskFunnelData(
    val totalCreated: Int,              // 创建任务
    val withDeadline: Int,              // 设置截止日期
    val started: Int,                   // 开始执行
    val firstDeferred: Int,             // 首次延期
    val overdue: Int,                   // 逾期
    val finalCompleted: Int,            // 最终完成
    val abandoned: Int,                 // 放弃任务
    val maxLossStage: String,           // 最大流失环节
    val maxLossRate: Float              // 最大流失率
)

// 延迟成本分析数据
data class DelayAnalysisData(
    val totalDelayDays: Int,            // 📅 累计拖延天数
    val avgDelayPerTask: Float,         // ⏰ 平均每个逾期任务延迟天数
    val mostDelayedTask: String,        // 💸 拖延最严重的任务名称
    val mostDelayedDays: Int            // 拖延最严重的任务延迟天数
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val taskUseCases: TaskUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val currentDate = LocalDate.now()
    private var currentMonthDate = currentDate

    init {
        updateCurrentMonth()
        observeTaskChanges()
    }

    private fun updateCurrentMonth() {
        val formatter = DateTimeFormatter.ofPattern("yyyy年MM月")
        _uiState.value = _uiState.value.copy(
            currentMonth = currentMonthDate.format(formatter)
        )
    }

    /**
     * 关键改进：使用 Flow 监听任务变化，实现响应式更新
     */
    private fun observeTaskChanges() {
        viewModelScope.launch {
            // 监听所有任务变化
            taskUseCases.getAllTasks()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = e.message,
                        isLoading = false
                    )
                }
                .collectLatest { tasks ->
                    // 任务数据变化时自动重新计算统计
                    _uiState.value = _uiState.value.copy(isLoading = true)

                    try {
                        // 基础统计 - 5种状态
                        val total = tasks.size
                        val pending = tasks.count { it.status == TaskStatus.PENDING }
                        val completed = tasks.count { it.status == TaskStatus.COMPLETED }
                        val deferred = tasks.count { it.status == TaskStatus.DELAYED }
                        val overdue = tasks.count { it.status == TaskStatus.OVERDUE }
                        val cancelled = tasks.count { it.status == TaskStatus.CANCELLED }

                        val completionRate = if (total > 0) (completed.toFloat() / total) * 100f else 0f

                        // 重要程度分布
                        val importantUrgent = tasks.count { it.importanceUrgency == TaskImportanceUrgency.IMPORTANT_URGENT }
                        val importantNotUrgent = tasks.count { it.importanceUrgency == TaskImportanceUrgency.IMPORTANT_NOT_URGENT }
                        val notImportantUrgent = tasks.count { it.importanceUrgency == TaskImportanceUrgency.NOT_IMPORTANT_URGENT }
                        val notImportantNotUrgent = tasks.count { it.importanceUrgency == TaskImportanceUrgency.NOT_IMPORTANT_NOT_URGENT }

                        // 分类统计
                        val categoryStatsMap = calculateCategoryStats(tasks)

                        // 趋势数据
                        val weeklyTrend = calculateWeeklyTrend(tasks)

                        // 效率数据
                        val timeByCategory = calculateTimeByCategory(tasks)
                        val timeByImportance = calculateTimeByImportance(tasks)
                        val (onTimeRate, overdueRate) = calculateOnTimeRate(tasks)

                        // 新增：本周vs上周对比
                        val weekComparison = calculateWeekComparison(tasks)

                        // 新增：重要紧急任务统计
                        val importantUrgentCompleted = tasks.count {
                            it.importanceUrgency == TaskImportanceUrgency.IMPORTANT_URGENT &&
                                    it.status == TaskStatus.COMPLETED
                        }
                        val importantUrgentCompletionRate = if (importantUrgent > 0)
                            (importantUrgentCompleted.toFloat() / importantUrgent) * 100f else 0f

                        // 新增：健康度计算
                        val (healthScore, healthLevel) = calculateHealthScore(
                            completionRate = completionRate,
                            onTimeRate = onTimeRate,
                            importantUrgentCompletionRate = importantUrgentCompletionRate
                        )

                        // 新增：智能洞察生成
                        val insights = generateInsights(
                            tasks = tasks,
                            completionRate = completionRate,
                            overdueTasks = overdue,
                            importantUrgentCompleted = importantUrgentCompleted,
                            importantUrgentTotal = importantUrgent,
                            thisWeekCompleted = weekComparison.thisWeekCompleted,
                            lastWeekCompleted = weekComparison.lastWeekCompleted
                        )

                        // 新增：分类效率排行和热力图
                        val categoryEfficiencyRanking = calculateCategoryEfficiencyRanking(categoryStatsMap)
                        val categoryWeekdayHeatmap = calculateCategoryWeekdayHeatmap(tasks)

                        // 新增：趋势Tab数据计算
                        val calendarHeatmap = calculateCalendarHeatmap(tasks)
                        val calendarStats = calculateCalendarStats(calendarHeatmap)
                        val backlogTrend = calculateBacklogTrend(tasks)
                        val velocityAcceleration = calculateVelocityAcceleration(tasks)

                        // 根据时间范围过滤趋势数据
                        val filteredWeeklyTrend = filterTrendByTimeRange(
                            weeklyTrend,
                            _uiState.value.selectedTimeRange,
                            _uiState.value.customStartDate,
                            _uiState.value.customEndDate
                        )

                        // 新增：效率Tab数据计算
                        val timeHeatmap = calculateTimeHeatmap(tasks)
                        val timeHeatmapStats = calculateTimeHeatmapStats(timeHeatmap)
                        val procrastinationRadar = calculateProcrastinationRadar(tasks)
                        val taskFunnel = calculateTaskFunnel(tasks)
                        val delayAnalysis = calculateDelayAnalysis(tasks)

                        _uiState.value = _uiState.value.copy(
                            totalTasks = total,
                            pendingTasks = pending,
                            completedTasks = completed,
                            deferredTasks = deferred,
                            overdueTasks = overdue,
                            cancelledTasks = cancelled,
                            completionRate = completionRate,
                            importantUrgentCount = importantUrgent,
                            importantNotUrgentCount = importantNotUrgent,
                            notImportantUrgentCount = notImportantUrgent,
                            notImportantNotUrgentCount = notImportantNotUrgent,
                            categoryStats = categoryStatsMap,
                            categoryEfficiencyRanking = categoryEfficiencyRanking,
                            categoryWeekdayHeatmap = categoryWeekdayHeatmap,
                            weeklyTrend = filteredWeeklyTrend,
                            calendarHeatmap = calendarHeatmap,
                            calendarStats = calendarStats,
                            backlogTrend = backlogTrend,
                            velocityAcceleration = velocityAcceleration,
                            completionTimeByCategory = timeByCategory,
                            completionTimeByImportance = timeByImportance,
                            onTimeCompletionRate = onTimeRate,
                            overdueCompletionRate = overdueRate,
                            timeHeatmap = timeHeatmap,
                            timeHeatmapStats = timeHeatmapStats,
                            procrastinationRadar = procrastinationRadar,
                            taskFunnel = taskFunnel,
                            delayAnalysis = delayAnalysis,
                            // 新增字段
                            insights = insights,
                            healthScore = healthScore,
                            healthLevel = healthLevel,
                            weekComparison = weekComparison,
                            isLoading = false,
                            lastUpdateTime = LocalDateTime.now()
                        )
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = e.message,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun calculateCategoryStats(tasks: List<com.example.nextthingb1.domain.model.Task>): Map<TaskCategory, CategoryStatsData> {
        return TaskCategory.values().associateWith { category ->
            val categoryTasks = tasks.filter { it.category == category }
            val completed = categoryTasks.count { it.status == TaskStatus.COMPLETED }
            val pending = categoryTasks.count { it.status == TaskStatus.PENDING }
            val overdue = categoryTasks.count { it.status == TaskStatus.OVERDUE }
            val cancelled = categoryTasks.count { it.status == TaskStatus.CANCELLED }

            val completionRate = if (categoryTasks.isNotEmpty())
                (completed.toFloat() / categoryTasks.size) * 100f else 0f

            val avgDuration = categoryTasks
                .filter { it.status == TaskStatus.COMPLETED && it.actualDuration > 0 }
                .map { it.actualDuration }
                .average()
                .takeIf { !it.isNaN() } ?: 0.0

            val overdueRate = if (categoryTasks.isNotEmpty())
                (overdue.toFloat() / categoryTasks.size) * 100f else 0f

            // 计算效率分数（0-100）
            val efficiencyScore = calculateCategoryEfficiency(
                completionRate = completionRate,
                avgDuration = avgDuration,
                overdueRate = overdueRate
            )

            CategoryStatsData(
                category = category,
                totalCount = categoryTasks.size,
                completedCount = completed,
                pendingCount = pending,
                overdueCount = overdue,
                cancelledCount = cancelled,
                completionRate = completionRate,
                averageDuration = avgDuration,
                efficiencyScore = efficiencyScore
            )
        }.filter { it.value.totalCount > 0 } // 只返回有任务的分类
    }

    private fun calculateWeeklyTrend(tasks: List<com.example.nextthingb1.domain.model.Task>): List<DailyTrendData> {
        return (0..6).map { daysAgo ->
            val targetDate = LocalDate.now().minusDays(daysAgo.toLong())
            val dayTasks = tasks.filter {
                it.createdAt.toLocalDate() == targetDate
            }
            val completedCount = tasks.count {
                it.completedAt?.toLocalDate() == targetDate && it.status == TaskStatus.COMPLETED
            }

            DailyTrendData(
                date = targetDate,
                createdCount = dayTasks.size,
                completedCount = completedCount,
                completionRate = if (dayTasks.isNotEmpty())
                    (completedCount.toFloat() / dayTasks.size) * 100f else 0f
            )
        }.reversed()
    }

    private fun calculateTimeByCategory(tasks: List<com.example.nextthingb1.domain.model.Task>): Map<TaskCategory, Double> {
        val completedTasks = tasks.filter { it.status == TaskStatus.COMPLETED }
        return TaskCategory.values().associateWith { category ->
            completedTasks
                .filter { it.category == category && it.actualDuration > 0 }
                .map { it.actualDuration }
                .average()
                .takeIf { !it.isNaN() } ?: 0.0
        }.filter { it.value > 0 }
    }

    private fun calculateTimeByImportance(tasks: List<com.example.nextthingb1.domain.model.Task>): Map<TaskImportanceUrgency, Double> {
        val completedTasks = tasks.filter { it.status == TaskStatus.COMPLETED }
        return TaskImportanceUrgency.values().associateWith { importance ->
            completedTasks
                .filter { it.importanceUrgency == importance && it.actualDuration > 0 }
                .map { it.actualDuration }
                .average()
                .takeIf { !it.isNaN() } ?: 0.0
        }.filter { it.value > 0 }
    }

    private fun calculateOnTimeRate(tasks: List<com.example.nextthingb1.domain.model.Task>): Pair<Float, Float> {
        val completedTasksWithDueDate = tasks.filter {
            it.status == TaskStatus.COMPLETED && it.dueDate != null && it.completedAt != null
        }

        if (completedTasksWithDueDate.isEmpty()) {
            return Pair(0f, 0f)
        }

        val onTimeCount = completedTasksWithDueDate.count {
            it.completedAt!! <= it.dueDate!!
        }

        val onTimeRate = (onTimeCount.toFloat() / completedTasksWithDueDate.size) * 100f
        val overdueRate = 100f - onTimeRate

        return Pair(onTimeRate, overdueRate)
    }

    fun selectTab(tab: StatsTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun toggleTrendViewMode() {
        val newMode = if (_uiState.value.trendViewMode == TrendViewMode.WEEK) {
            TrendViewMode.MONTH
        } else {
            TrendViewMode.WEEK
        }
        _uiState.value = _uiState.value.copy(trendViewMode = newMode)
    }

    fun previousMonth() {
        currentMonthDate = currentMonthDate.minusMonths(1)
        updateCurrentMonth()
    }

    fun nextMonth() {
        currentMonthDate = currentMonthDate.plusMonths(1)
        updateCurrentMonth()
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // ==================== 新增：智能洞察生成 ====================
    private fun generateInsights(
        tasks: List<com.example.nextthingb1.domain.model.Task>,
        completionRate: Float,
        overdueTasks: Int,
        importantUrgentCompleted: Int,
        importantUrgentTotal: Int,
        thisWeekCompleted: Int,
        lastWeekCompleted: Int
    ): List<InsightData> {
        val insights = mutableListOf<InsightData>()

        // 1. 完成率洞察
        if (completionRate >= 80f) {
            insights.add(InsightData(
                type = InsightType.POSITIVE,
                icon = "🎉",
                message = "完成率${String.format("%.0f", completionRate)}%，表现优秀！"
            ))
        } else if (completionRate < 50f && tasks.isNotEmpty()) {
            insights.add(InsightData(
                type = InsightType.WARNING,
                icon = "⚠️",
                message = "完成率仅${String.format("%.0f", completionRate)}%，需要加油"
            ))
        }

        // 2. 逾期任务警告
        if (overdueTasks > 5) {
            insights.add(InsightData(
                type = InsightType.ALERT,
                icon = "🔴",
                message = "有${overdueTasks}个任务已逾期，建议优先处理"
            ))
        } else if (overdueTasks in 1..5) {
            insights.add(InsightData(
                type = InsightType.WARNING,
                icon = "⏰",
                message = "有${overdueTasks}个任务即将逾期"
            ))
        }

        // 3. 重要紧急任务洞察
        if (importantUrgentTotal > 0) {
            val urgentCompletionRate = (importantUrgentCompleted.toFloat() / importantUrgentTotal) * 100
            if (urgentCompletionRate < 50f) {
                insights.add(InsightData(
                    type = InsightType.ALERT,
                    icon = "🔥",
                    message = "重要紧急任务完成率仅${String.format("%.0f", urgentCompletionRate)}%"
                ))
            }
        }

        // 4. 本周vs上周对比洞察
        if (lastWeekCompleted > 0) {
            val weekChange = ((thisWeekCompleted - lastWeekCompleted).toFloat() / lastWeekCompleted) * 100
            if (weekChange > 20f) {
                insights.add(InsightData(
                    type = InsightType.POSITIVE,
                    icon = "📈",
                    message = "本周完成量提升${String.format("%.0f", weekChange)}%，保持节奏"
                ))
            } else if (weekChange < -20f) {
                insights.add(InsightData(
                    type = InsightType.WARNING,
                    icon = "📉",
                    message = "本周完成量下降${String.format("%.0f", -weekChange)}%"
                ))
            }
        }

        // 5. 连续完成激励
        val recentCompletedDays = tasks
            .filter { it.status == TaskStatus.COMPLETED && it.completedAt != null }
            .groupBy { it.completedAt!!.toLocalDate() }
            .keys
            .sortedDescending()
            .takeWhile { date ->
                val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(date, LocalDate.now())
                daysBetween <= 7
            }
            .size

        if (recentCompletedDays >= 7) {
            insights.add(InsightData(
                type = InsightType.POSITIVE,
                icon = "🔥",
                message = "已连续${recentCompletedDays}天完成任务，继续保持！"
            ))
        }

        // 限制最多3条洞察
        return insights.take(3)
    }

    // ==================== 新增：健康度计算 ====================
    private fun calculateHealthScore(
        completionRate: Float,
        onTimeRate: Float,
        importantUrgentCompletionRate: Float
    ): Pair<Int, HealthLevel> {
        // 三维度加权计算：完成率40%，准时率35%，重要任务完成率25%
        val score = (completionRate * 0.4f + onTimeRate * 0.35f + importantUrgentCompletionRate * 0.25f).toInt()

        val level = when {
            score >= HealthLevel.EXCELLENT.minScore -> HealthLevel.EXCELLENT
            score >= HealthLevel.GOOD.minScore -> HealthLevel.GOOD
            score >= HealthLevel.AVERAGE.minScore -> HealthLevel.AVERAGE
            else -> HealthLevel.POOR
        }

        return Pair(score, level)
    }

    // ==================== 新增：本周vs上周对比 ====================
    private fun calculateWeekComparison(
        tasks: List<com.example.nextthingb1.domain.model.Task>
    ): WeekComparisonData {
        val now = LocalDate.now()
        val thisWeekStart = now.minusDays(now.dayOfWeek.value.toLong() - 1) // 本周一
        val lastWeekStart = thisWeekStart.minusWeeks(1)
        val lastWeekEnd = thisWeekStart.minusDays(1)

        // 本周任务
        val thisWeekTasks = tasks.filter {
            it.createdAt.toLocalDate() >= thisWeekStart
        }
        val thisWeekCompleted = tasks.count {
            it.completedAt?.toLocalDate()?.let { date ->
                date >= thisWeekStart
            } ?: false
        }
        val thisWeekCompletionRate = if (thisWeekTasks.isNotEmpty())
            (thisWeekCompleted.toFloat() / thisWeekTasks.size) * 100f else 0f

        val thisWeekAvgDuration = tasks
            .filter {
                it.completedAt?.toLocalDate()?.let { date -> date >= thisWeekStart } ?: false
                        && it.actualDuration > 0
            }
            .map { it.actualDuration }
            .average()
            .takeIf { !it.isNaN() } ?: 0.0

        // 上周任务
        val lastWeekTasks = tasks.filter {
            val date = it.createdAt.toLocalDate()
            date >= lastWeekStart && date <= lastWeekEnd
        }
        val lastWeekCompleted = tasks.count {
            it.completedAt?.toLocalDate()?.let { date ->
                date >= lastWeekStart && date <= lastWeekEnd
            } ?: false
        }
        val lastWeekCompletionRate = if (lastWeekTasks.isNotEmpty())
            (lastWeekCompleted.toFloat() / lastWeekTasks.size) * 100f else 0f

        val lastWeekAvgDuration = tasks
            .filter {
                it.completedAt?.toLocalDate()?.let { date ->
                    date >= lastWeekStart && date <= lastWeekEnd
                } ?: false && it.actualDuration > 0
            }
            .map { it.actualDuration }
            .average()
            .takeIf { !it.isNaN() } ?: 0.0

        return WeekComparisonData(
            thisWeekCompleted = thisWeekCompleted,
            lastWeekCompleted = lastWeekCompleted,
            completedChange = thisWeekCompleted - lastWeekCompleted,
            thisWeekCompletionRate = thisWeekCompletionRate,
            lastWeekCompletionRate = lastWeekCompletionRate,
            completionRateChange = thisWeekCompletionRate - lastWeekCompletionRate,
            thisWeekAvgDuration = thisWeekAvgDuration,
            lastWeekAvgDuration = lastWeekAvgDuration,
            avgDurationChange = thisWeekAvgDuration - lastWeekAvgDuration
        )
    }

    // ==================== 新增：分类效率计算 ====================
    private fun calculateCategoryEfficiency(
        completionRate: Float,
        avgDuration: Double,
        overdueRate: Float
    ): Int {
        // 完成率权重50%，时长权重30%（越短越好），逾期率权重20%（越低越好）
        val completionScore = completionRate * 0.5f

        // 时长标准化：假设60分钟为标准，超过扣分
        val durationScore = if (avgDuration > 0) {
            val normalized = (60.0 / (avgDuration + 10)).coerceIn(0.0, 1.0) * 100
            normalized * 0.3
        } else {
            0.0
        }

        val overdueScore = (100f - overdueRate) * 0.2f

        return (completionScore + durationScore + overdueScore).toInt().coerceIn(0, 100)
    }

    // ==================== 新增：分类效率排行榜 ====================
    private fun calculateCategoryEfficiencyRanking(
        categoryStats: Map<TaskCategory, CategoryStatsData>
    ): List<CategoryEfficiencyData> {
        return categoryStats.values
            .sortedByDescending { it.efficiencyScore }
            .mapIndexed { index, stats ->
                CategoryEfficiencyData(
                    category = stats.category,
                    efficiencyScore = stats.efficiencyScore,
                    rank = index + 1,
                    completionRate = stats.completionRate,
                    avgDuration = stats.averageDuration,
                    overdueRate = if (stats.totalCount > 0)
                        (stats.overdueCount.toFloat() / stats.totalCount) * 100f else 0f
                )
            }
    }

    // ==================== 新增：分类×星期热力图 ====================
    private fun calculateCategoryWeekdayHeatmap(
        tasks: List<com.example.nextthingb1.domain.model.Task>
    ): Map<TaskCategory, Map<Int, Int>> {
        val completedTasks = tasks.filter {
            it.status == TaskStatus.COMPLETED && it.completedAt != null
        }

        return TaskCategory.values().associateWith { category ->
            val categoryTasks = completedTasks.filter { it.category == category }

            // 星期1-7的完成数量统计
            (1..7).associateWith { dayOfWeek ->
                categoryTasks.count {
                    it.completedAt!!.dayOfWeek.value == dayOfWeek
                }
            }
        }.filter { (_, weekdayMap) -> weekdayMap.values.sum() > 0 } // 过滤没有数据的分类
    }

    // ==================== 新增：选择分类 ====================
    fun selectCategory(category: TaskCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    // ==================== 趋势Tab新增功能 ====================

    /**
     * 计算月历热力图数据（GitHub风格）
     * 最近90天的每日完成情况
     */
    private fun calculateCalendarHeatmap(tasks: List<com.example.nextthingb1.domain.model.Task>): List<CalendarHeatmapData> {
        val today = LocalDate.now()
        val startDate = today.minusDays(89) // 90天

        // 统计每天的完成数量
        val completedByDate = tasks
            .filter { it.status == TaskStatus.COMPLETED && it.completedAt != null }
            .groupBy { it.completedAt!!.toLocalDate() }
            .mapValues { it.value.size }

        // 生成90天的数据
        return (0..89).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val count = completedByDate[date] ?: 0

            // 计算颜色等级：0个=0，1-2个=1，3-4个=2，5-6个=3，6+个=4
            val level = when {
                count == 0 -> 0
                count <= 2 -> 1
                count <= 4 -> 2
                count <= 6 -> 3
                else -> 4
            }

            CalendarHeatmapData(
                date = date,
                completedCount = count,
                level = level
            )
        }.reversed() // 从早到晚排序
    }

    /**
     * 计算月历热力图统计数据
     */
    private fun calculateCalendarStats(heatmapData: List<CalendarHeatmapData>): CalendarHeatmapStats {
        val today = LocalDate.now()

        // 🔥 计算最长连续完成天数
        var maxStreak = 0
        var currentStreak = 0
        var currentStreakDays = 0

        heatmapData.reversed().forEach { data ->
            if (data.completedCount > 0) {
                currentStreak++
                if (currentStreak > maxStreak) {
                    maxStreak = currentStreak
                }
            } else {
                currentStreak = 0
            }
        }

        // 计算当前连续天数（从今天开始往前）
        for (data in heatmapData.reversed()) {
            if (data.completedCount > 0) {
                currentStreakDays++
            } else {
                break
            }
        }

        // ❄️ 计算最长中断天数
        var maxGap = 0
        var currentGap = 0

        heatmapData.reversed().forEach { data ->
            if (data.completedCount == 0) {
                currentGap++
                if (currentGap > maxGap) {
                    maxGap = currentGap
                }
            } else {
                currentGap = 0
            }
        }

        // 📅 本月累计完成
        val currentMonthCompleted = heatmapData
            .filter { it.date.year == today.year && it.date.month == today.month }
            .sumOf { it.completedCount }

        return CalendarHeatmapStats(
            maxStreakDays = maxStreak,
            currentStreakDays = currentStreakDays,
            maxGapDays = maxGap,
            currentMonthCompleted = currentMonthCompleted
        )
    }

    /**
     * 计算任务积压趋势数据（最近30天）
     */
    private fun calculateBacklogTrend(tasks: List<com.example.nextthingb1.domain.model.Task>): List<BacklogTrendData> {
        val today = LocalDate.now()
        val threshold = _uiState.value.backlogThreshold

        return (0..29).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val endOfDay = date.atTime(23, 59, 59)

            // 计算该日期未完成任务总数（积压量）
            // 未完成 = 创建时间 <= 该日期 且 (未完成 或 完成时间 > 该日期)
            val backlogCount = tasks.count { task ->
                val createdDate = task.createdAt.toLocalDate()
                val isCreatedBefore = createdDate <= date

                val isNotCompleted = when (task.status) {
                    TaskStatus.PENDING -> true
                    TaskStatus.COMPLETED -> {
                        task.completedAt?.let { it.toLocalDate() > date } ?: false
                    }
                    TaskStatus.DELAYED -> true
                    TaskStatus.OVERDUE -> true
                    TaskStatus.CANCELLED -> false
                }

                isCreatedBefore && isNotCompleted
            }

            // 计算当日新增任务数量
            val newTasksCount = tasks.count { task ->
                task.createdAt.toLocalDate() == date
            }

            BacklogTrendData(
                date = date,
                backlogCount = backlogCount,
                newTasksCount = newTasksCount,
                isOverThreshold = backlogCount > threshold
            )
        }.reversed()
    }

    /**
     * 计算完成速度加速度数据（按周）
     * 展示最近12周的数据
     */
    private fun calculateVelocityAcceleration(tasks: List<com.example.nextthingb1.domain.model.Task>): List<VelocityAccelerationData> {
        val today = LocalDate.now()
        val completedTasks = tasks.filter { it.status == TaskStatus.COMPLETED && it.completedAt != null }

        // 计算最近12周的数据
        val weeklyData = (0..11).map { weeksAgo ->
            val weekEndDate = today.minusWeeks(weeksAgo.toLong())
            val weekStartDate = weekEndDate.minusDays(6)

            val weekCompleted = completedTasks.count { task ->
                val completedDate = task.completedAt!!.toLocalDate()
                completedDate in weekStartDate..weekEndDate
            }

            Triple(weekStartDate, weekEndDate, weekCompleted)
        }.reversed()

        // 计算加速度（本周 - 上周）
        return weeklyData.mapIndexed { index, (startDate, _, completedCount) ->
            val prevWeekCount = if (index > 0) weeklyData[index - 1].third else completedCount
            val acceleration = completedCount - prevWeekCount

            VelocityAccelerationData(
                weekLabel = "第${index + 1}周",
                weekStartDate = startDate,
                completedCount = completedCount,
                acceleration = acceleration,
                isAcceleration = acceleration >= 0
            )
        }
    }

    /**
     * 时间范围选择器
     */
    fun selectTimeRange(timeRange: TimeRange, startDate: LocalDate? = null, endDate: LocalDate? = null) {
        _uiState.value = _uiState.value.copy(
            selectedTimeRange = timeRange,
            customStartDate = if (timeRange == TimeRange.CUSTOM) startDate else null,
            customEndDate = if (timeRange == TimeRange.CUSTOM) endDate else null
        )
    }

    /**
     * 根据时间范围过滤趋势数据
     */
    private fun filterTrendByTimeRange(
        allTrend: List<DailyTrendData>,
        timeRange: TimeRange,
        customStart: LocalDate?,
        customEnd: LocalDate?
    ): List<DailyTrendData> {
        val today = LocalDate.now()

        return when (timeRange) {
            TimeRange.WEEK_7 -> allTrend.filter { it.date >= today.minusDays(6) }
            TimeRange.DAYS_30 -> allTrend.filter { it.date >= today.minusDays(29) }
            TimeRange.DAYS_90 -> allTrend.filter { it.date >= today.minusDays(89) }
            TimeRange.ALL -> allTrend
            TimeRange.CUSTOM -> {
                if (customStart != null && customEnd != null) {
                    allTrend.filter { it.date in customStart..customEnd }
                } else {
                    allTrend
                }
            }
        }
    }

    // ==================== 效率Tab新增功能 ====================

    /**
     * 计算时间热力图数据（7×6矩阵）
     * 横轴：周一到周日，纵轴：6个时间段
     */
    private fun calculateTimeHeatmap(tasks: List<com.example.nextthingb1.domain.model.Task>): List<TimeHeatmapData> {
        val completedTasks = tasks.filter { it.status == TaskStatus.COMPLETED && it.completedAt != null }

        // 统计每个时段的完成数量
        val heatmapMap = mutableMapOf<Pair<Int, TimeSlot>, Int>()

        completedTasks.forEach { task ->
            val completedTime = task.completedAt!!
            val dayOfWeek = completedTime.dayOfWeek.value  // 1-7
            val hour = completedTime.hour

            // 确定时间段
            val timeSlot = TimeSlot.values().find { hour in it.hourRange }

            if (timeSlot != null) {
                val key = Pair(dayOfWeek, timeSlot)
                heatmapMap[key] = heatmapMap.getOrDefault(key, 0) + 1
            }
        }

        // 生成7×6的完整矩阵
        return (1..7).flatMap { dayOfWeek ->
            TimeSlot.values().map { timeSlot ->
                val count = heatmapMap[Pair(dayOfWeek, timeSlot)] ?: 0

                // 计算颜色等级：0个=0，1-2个=1，3-4个=2，5-6个=3，7+个=4
                val level = when {
                    count == 0 -> 0
                    count <= 2 -> 1
                    count <= 4 -> 2
                    count <= 6 -> 3
                    else -> 4
                }

                TimeHeatmapData(
                    dayOfWeek = dayOfWeek,
                    timeSlot = timeSlot,
                    completedCount = count,
                    level = level
                )
            }
        }
    }

    /**
     * 计算时间热力图统计
     */
    private fun calculateTimeHeatmapStats(heatmapData: List<TimeHeatmapData>): TimeHeatmapStats {
        val maxData = heatmapData.maxByOrNull { it.completedCount }
        val minDataExcludingZero = heatmapData.filter { it.completedCount > 0 }.minByOrNull { it.completedCount }

        return TimeHeatmapStats(
            mostProductiveSlot = maxData?.let { Pair(it.dayOfWeek, it.timeSlot) },
            mostProductiveCount = maxData?.completedCount ?: 0,
            leastProductiveSlot = minDataExcludingZero?.let { Pair(it.dayOfWeek, it.timeSlot) },
            leastProductiveCount = minDataExcludingZero?.completedCount ?: 0
        )
    }

    /**
     * 计算拖延分析雷达图数据（6维）
     */
    private fun calculateProcrastinationRadar(tasks: List<com.example.nextthingb1.domain.model.Task>): ProcrastinationRadarData {
        val completedTasks = tasks.filter { it.status == TaskStatus.COMPLETED && it.completedAt != null }

        if (completedTasks.isEmpty()) {
            return ProcrastinationRadarData(
                onTimeRate = 0f,
                responseSpeed = 0f,
                completionSpeed = 0f,
                importantPriority = 0f,
                completionStability = 0f,
                goalAchievementRate = 0f,
                totalScore = 0,
                efficiencyGrade = "D",
                strongestDimension = "暂无数据",
                strongestScore = 0f,
                weakestDimension = "暂无数据",
                weakestScore = 0f
            )
        }

        // 1. ⏰ 准时完成率
        val tasksWithDeadline = completedTasks.filter { it.dueDate != null }
        val onTimeCount = tasksWithDeadline.count { task ->
            task.completedAt!!.toLocalDate() <= task.dueDate!!.toLocalDate()
        }
        val onTimeRate = if (tasksWithDeadline.isNotEmpty()) {
            (onTimeCount.toFloat() / tasksWithDeadline.size) * 100f
        } else 50f

        // 2. 🚀 任务响应速度（创建后多久开始，假设完成时间-创建时间越短越好）
        val avgResponseHours = completedTasks.map { task ->
            java.time.Duration.between(task.createdAt, task.completedAt).toHours()
        }.average()
        // 标准化：假设24小时内响应为满分，超过240小时为0分
        val responseSpeed = ((240 - avgResponseHours.coerceIn(0.0, 240.0)) / 240 * 100).toFloat()

        // 3. ⚡ 平均完成时长（越短越好，反向标准化）
        val avgCompletionHours = completedTasks.mapNotNull { task ->
            if (task.actualDuration > 0) task.actualDuration.toDouble() else null
        }.average()
        // 假设60分钟内完成为满分，超过600分钟为0分
        val completionSpeed = ((600 - avgCompletionHours.coerceIn(0.0, 600.0)) / 600 * 100).toFloat()

        // 4. 🔥 重要任务优先度（重要紧急类的平均完成时长 vs 其他类）
        val importantUrgentTasks = completedTasks.filter {
            it.importanceUrgency == TaskImportanceUrgency.IMPORTANT_URGENT
        }
        val importantPriority = if (importantUrgentTasks.isNotEmpty()) {
            val importantAvgHours = importantUrgentTasks.mapNotNull {
                if (it.actualDuration > 0) it.actualDuration.toDouble() else null
            }.average()
            // 重要任务完成得越快，分数越高
            ((300 - importantAvgHours.coerceIn(0.0, 300.0)) / 300 * 100).toFloat()
        } else 50f

        // 5. 📊 完成稳定性（每日完成数量的标准差，越小越好）
        val dailyCompletionCounts = completedTasks
            .groupBy { it.completedAt!!.toLocalDate() }
            .mapValues { it.value.size }
            .values.toList()

        val completionStability = if (dailyCompletionCounts.isNotEmpty()) {
            val mean = dailyCompletionCounts.average()
            val variance = dailyCompletionCounts.map { (it - mean) * (it - mean) }.average()
            val stdDev = kotlin.math.sqrt(variance)
            // 标准差越小，稳定性越高；假设stdDev=0为满分，stdDev>=5为0分
            ((5 - stdDev.coerceIn(0.0, 5.0)) / 5 * 100).toFloat()
        } else 50f

        // 6. 🎯 目标达成率（有截止日期的任务准时率）
        val goalAchievementRate = onTimeRate  // 与准时完成率相同

        // 计算总分（六维平均）
        val dimensions = listOf(
            "准时完成率" to onTimeRate,
            "任务响应速度" to responseSpeed,
            "平均完成时长" to completionSpeed,
            "重要任务优先度" to importantPriority,
            "完成稳定性" to completionStability,
            "目标达成率" to goalAchievementRate
        )

        val totalScore = dimensions.map { it.second }.average().toInt()

        // 效率等级
        val efficiencyGrade = when {
            totalScore >= 90 -> "A+"
            totalScore >= 80 -> "A"
            totalScore >= 70 -> "B"
            totalScore >= 60 -> "C"
            else -> "D"
        }

        // 最强项和待提升
        val strongest = dimensions.maxByOrNull { it.second }!!
        val weakest = dimensions.minByOrNull { it.second }!!

        return ProcrastinationRadarData(
            onTimeRate = onTimeRate,
            responseSpeed = responseSpeed,
            completionSpeed = completionSpeed,
            importantPriority = importantPriority,
            completionStability = completionStability,
            goalAchievementRate = goalAchievementRate,
            totalScore = totalScore,
            efficiencyGrade = efficiencyGrade,
            strongestDimension = strongest.first,
            strongestScore = strongest.second,
            weakestDimension = weakest.first,
            weakestScore = weakest.second
        )
    }

    /**
     * 计算任务完成漏斗数据
     */
    private fun calculateTaskFunnel(tasks: List<com.example.nextthingb1.domain.model.Task>): TaskFunnelData {
        val totalCreated = tasks.size
        val withDeadline = tasks.count { it.dueDate != null }
        val started = tasks.count { it.status != TaskStatus.PENDING || it.completedAt != null }
        val firstDeferred = tasks.count { it.status == TaskStatus.DELAYED }
        val overdue = tasks.count { it.status == TaskStatus.OVERDUE }
        val finalCompleted = tasks.count { it.status == TaskStatus.COMPLETED }
        val abandoned = tasks.count { it.status == TaskStatus.CANCELLED }

        // 计算每个阶段的流失率
        val lossRates = listOf(
            "设置截止日期" to if (totalCreated > 0) (totalCreated - withDeadline).toFloat() / totalCreated else 0f,
            "开始执行" to if (withDeadline > 0) (withDeadline - started).toFloat() / withDeadline else 0f,
            "首次延期" to if (started > 0) (started - firstDeferred).toFloat() / started else 0f,
            "逾期" to if (firstDeferred > 0) (firstDeferred - overdue).toFloat() / firstDeferred else 0f,
            "最终完成" to if (overdue > 0) (overdue - finalCompleted).toFloat() / overdue else 0f
        )

        val maxLoss = lossRates.maxByOrNull { it.second }

        return TaskFunnelData(
            totalCreated = totalCreated,
            withDeadline = withDeadline,
            started = started,
            firstDeferred = firstDeferred,
            overdue = overdue,
            finalCompleted = finalCompleted,
            abandoned = abandoned,
            maxLossStage = maxLoss?.first ?: "无",
            maxLossRate = (maxLoss?.second ?: 0f) * 100f
        )
    }

    /**
     * 计算延迟成本分析数据
     */
    private fun calculateDelayAnalysis(tasks: List<com.example.nextthingb1.domain.model.Task>): DelayAnalysisData {
        // 筛选有截止日期且已完成的逾期任务
        val overdueTasks = tasks.filter { task ->
            task.status == TaskStatus.COMPLETED &&
            task.dueDate != null &&
            task.completedAt != null &&
            task.completedAt!!.toLocalDate() > task.dueDate!!.toLocalDate()
        }

        if (overdueTasks.isEmpty()) {
            return DelayAnalysisData(
                totalDelayDays = 0,
                avgDelayPerTask = 0f,
                mostDelayedTask = "无",
                mostDelayedDays = 0
            )
        }

        // 计算每个任务的延迟天数
        val delayDaysMap = overdueTasks.map { task ->
            val delayDays = java.time.temporal.ChronoUnit.DAYS.between(
                task.dueDate,
                task.completedAt!!.toLocalDate()
            ).toInt()
            task to delayDays
        }

        val totalDelayDays = delayDaysMap.sumOf { it.second }
        val avgDelayPerTask = totalDelayDays.toFloat() / overdueTasks.size

        val mostDelayed = delayDaysMap.maxByOrNull { it.second }

        return DelayAnalysisData(
            totalDelayDays = totalDelayDays,
            avgDelayPerTask = avgDelayPerTask,
            mostDelayedTask = mostDelayed?.first?.title ?: "无",
            mostDelayedDays = mostDelayed?.second ?: 0
        )
    }
}
