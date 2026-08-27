package com.nextthing.app.presentation.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextthing.app.data.service.AICompletionClient
import com.nextthing.app.data.service.AIRouteMode
import com.nextthing.app.data.service.AIRouteStatus
import com.nextthing.app.domain.usecase.TaskUseCases
import com.nextthing.app.domain.model.Category
import com.nextthing.app.domain.model.TaskStatus
import com.nextthing.app.domain.model.TaskImportanceUrgency
import com.nextthing.app.domain.service.AIStatsAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class StatsTab(val title: String) {
    OVERVIEW("概览"),
    CATEGORY("结构"),
    EFFICIENCY("效率")
}

// 概览页面时间维度
enum class OverviewTimeRange(val displayName: String) {
    TODAY("今日"),
    THIS_WEEK("本周"),
    THIS_MONTH("本月"),
    ALL("全部")
}

// 任务列表类型
enum class TaskListType(val title: String) {
    PENDING("待办任务"),
    IMPORTANT_URGENT("重要紧急"),
    OVERDUE("逾期任务"),
    COMPLETED("已完成")
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
    val selectedOverviewTimeRange: OverviewTimeRange = OverviewTimeRange.TODAY, // 概览时间维度
    // 核心指标（根据时间维度动态计算）
    val coreMetricPending: Int = 0,           // 待办任务数
    val coreMetricImportantUrgent: Int = 0,   // 重要紧急任务数
    val coreMetricOverdue: Int = 0,           // 逾期任务数
    val coreMetricProgress: String = "0",     // 进度指标（今日显示数量，其他显示百分比）
    val coreMetricProgressType: String = "count", // "count" 或 "rate"
    val overviewTotalTasks: Int = 0,
    val overviewCompletedTasks: Int = 0,
    val overviewCompletionRate: Float = 0f,
    // 任务列表弹窗相关
    val showTaskListSheet: Boolean = false,   // 是否显示任务列表弹窗
    val taskListType: TaskListType? = null,   // 任务列表类型
    val filteredTasks: List<com.nextthing.app.domain.model.Task> = emptyList(), // 筛选后的任务列表
    // 任务健康度
    val healthScore: Int = 0,
    val healthLevel: HealthLevel = HealthLevel.GOOD,
    // 本周vs上周对比
    val weekComparison: WeekComparisonData? = null,
    // 分类统计
    val categoryStats: Map<Category, CategoryStatsData> = emptyMap(),
    // 新增：分类饼图选中状态
    val selectedCategory: Category? = null,
    // 新增：分类效率排行
    val categoryEfficiencyRanking: List<CategoryEfficiencyData> = emptyList(),
    // 新增：分类×星期热力图数据
    val categoryWeekdayHeatmap: Map<Category, Map<Int, Int>> = emptyMap(),
    // 新增：分类页面时间维度选择
    val selectedCategoryTimeRange: OverviewTimeRange = OverviewTimeRange.ALL,
    // 趋势数据
    val weeklyTrend: List<DailyTrendData> = emptyList(),
    val allWeeklyTrend: List<DailyTrendData> = emptyList(), // 新增：保存完整未过滤的趋势数据
    val overviewTrend: List<DailyTrendData> = emptyList(),
    val overdueTrend: List<DailyOverdueTrendData> = emptyList(),
    val monthlyTrend: List<WeeklyTrendData> = emptyList(),
    val trendViewMode: TrendViewMode = TrendViewMode.WEEK,
    // 新增：时间范围选择器
    val selectedTimeRange: TimeRange = TimeRange.WEEK_7,
    val customStartDate: LocalDate? = null,
    val customEndDate: LocalDate? = null,
    // 新增：趋势页面时间范围选择
    val selectedTrendTimeRange: OverviewTimeRange = OverviewTimeRange.THIS_WEEK,
    // 新增：效率页面时间维度选择
    val selectedEfficiencyTimeRange: OverviewTimeRange = OverviewTimeRange.ALL,
    val efficiencySummary: EfficiencySummaryData = EfficiencySummaryData(),
    // 新增：月历热力图（GitHub风格）
    val calendarHeatmap: List<CalendarHeatmapData> = emptyList(),
    val allCalendarHeatmap: List<CalendarHeatmapData> = emptyList(), // 保存完整未过滤的热力图数据
    val calendarStats: CalendarHeatmapStats? = null,
    // 完成率走势
    val completionRateTrend: List<WeeklyCompletionRateData> = emptyList(),
    val allCompletionRateTrend: List<WeeklyCompletionRateData> = emptyList(),
    // 周期时间趋势
    val cycleTimeTrend: List<WeeklyCycleTimeData> = emptyList(),
    val allCycleTimeTrend: List<WeeklyCycleTimeData> = emptyList(),
    // 累积流图
    val cumulativeFlow: List<CumulativeFlowData> = emptyList(),
    val allCumulativeFlow: List<CumulativeFlowData> = emptyList(),
    // 新增：任务积压趋势
    val backlogTrend: List<BacklogTrendData> = emptyList(),
    val allBacklogTrend: List<BacklogTrendData> = emptyList(), // 保存完整未过滤的积压趋势数据
    val backlogThreshold: Int = 20,  // 积压预警阈值
    // 新增：完成速度加速度
    val velocityAcceleration: List<VelocityAccelerationData> = emptyList(),
    // 效率数据
    val completionTimeByCategory: Map<Category, Double> = emptyMap(),
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
    // AI 智能总结
    val aiSummary: String? = null,
    val isAISummaryLoading: Boolean = false,
    val aiSummaryError: String? = null,
    val aiSummaryTimeRange: OverviewTimeRange? = null,
    // AI 周报
    val weeklyReport: com.nextthing.app.domain.service.WeeklyReport? = null,
    val isGeneratingReport: Boolean = false,
    val reportExportText: String? = null,
    val aiRouteStatus: AIRouteStatus? = null,
    val aiReportContextLoaded: Boolean = false,
    val isAIReportContextLoading: Boolean = false,
    val aiReportWeekStart: LocalDate? = null,
    val aiReportWeekEnd: LocalDate? = null,
    val aiReportWeekTaskCount: Int = 0,
    val aiReportCompletedCount: Int = 0,
    val aiReportErrorMessage: String? = null,
    // UI 状态
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastUpdateTime: LocalDateTime = LocalDateTime.now()
)

data class CategoryStatsData(
    val category: Category,
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

data class DailyOverdueTrendData(
    val date: LocalDate,
    val overdueCount: Int
)

data class WeeklyTrendData(
    val weekLabel: String,
    val createdCount: Int,
    val completedCount: Int,
    val completionRate: Float
)

// 周完成率走势数据点
data class WeeklyCompletionRateData(
    val weekLabel: String,
    val weekStartDate: LocalDate,
    val completionRate: Float,  // 0~1
    val totalTasks: Int,
    val completedTasks: Int
)

// 周期时间趋势数据点
data class WeeklyCycleTimeData(
    val weekLabel: String,
    val weekStartDate: LocalDate,
    val avgDays: Float,         // 平均完成天数
    val taskCount: Int          // 该周完成的任务数
)

// 累积流图数据点
data class CumulativeFlowData(
    val date: LocalDate,
    val completed: Int,         // 累计已完成
    val overdue: Int,           // 当日逾期中
    val pending: Int            // 当日待办中
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
    val avgDurationChange: Double,
    // 新增：任务总数对比
    val thisWeekTotalTasks: Int,
    val lastWeekTotalTasks: Int,
    val totalTasksChange: Int,
    // 新增：延期任务对比
    val thisWeekDelayedTasks: Int,
    val lastWeekDelayedTasks: Int,
    val delayedTasksChange: Int,
    // 新增：放弃任务对比
    val thisWeekCancelledTasks: Int,
    val lastWeekCancelledTasks: Int,
    val cancelledTasksChange: Int,
    // 新增：逾期任务对比
    val thisWeekOverdueTasks: Int,
    val lastWeekOverdueTasks: Int,
    val overdueTasksChange: Int
)

// 分类效率排行数据
data class CategoryEfficiencyData(
    val category: Category,
    val efficiencyScore: Int,
    val rank: Int,
    val completionRate: Float, // 0..1，供进度条和百分比文案统一使用
    val avgDuration: Double,
    val overdueRate: Float // 0..1
)

// 任务状态分布数据（基于时间维度）
data class StatusDistributionData(
    val total: Int,
    val pending: Int,
    val completed: Int,
    val deferred: Int,
    val overdue: Int,
    val cancelled: Int,
    val completionRate: Float
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

data class EfficiencySummaryData(
    val completedCount: Int = 0,
    val completedWithDeadlineCount: Int = 0,
    val onTimeCompletedCount: Int = 0,
    val overdueCompletedCount: Int = 0,
    val averageCompletionMinutes: Double = 0.0,
    val activeDayCount: Int = 0
) {
    val onTimeRate: Float
        get() = if (completedWithDeadlineCount > 0) {
            onTimeCompletedCount.toFloat() / completedWithDeadlineCount * 100f
        } else {
            0f
        }
}

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

private data class AIReportWeekContext(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val tasks: List<com.nextthing.app.domain.model.Task>,
    val completedTasks: List<com.nextthing.app.domain.model.Task>
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val taskUseCases: TaskUseCases,
    private val aiStatsAnalyzer: AIStatsAnalyzer,
    private val aiWeeklyReporter: com.nextthing.app.domain.service.AIWeeklyReporter,
    private val aiCompletionClient: AICompletionClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val currentDate = LocalDate.now()
    private var currentMonthDate = currentDate

    // 创建单独的时间维度 Flow，用于监听变化
    private val _selectedTimeRange = MutableStateFlow(OverviewTimeRange.TODAY)

    // 创建分类页面时间维度 Flow
    private val _selectedCategoryTimeRange = MutableStateFlow(OverviewTimeRange.ALL)

    // 创建趋势页面时间维度 Flow（加入 combine，确保任务数据更新时使用正确的趋势范围）
    private val _selectedTrendTimeRange = MutableStateFlow(OverviewTimeRange.THIS_WEEK)

    // 创建效率页面时间维度 Flow
    private val _selectedEfficiencyTimeRange = MutableStateFlow(OverviewTimeRange.ALL)

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
     * 关键改进：使用 combine 监听任务和时间维度变化，实现响应式更新
     */
    private fun observeTaskChanges() {
        viewModelScope.launch {
            // 同时监听任务变化、概览时间维度变化、分类时间维度变化、趋势时间维度变化
            combine(
                taskUseCases.getAllTasks(),
                _selectedTimeRange,
                _selectedCategoryTimeRange,
                _selectedTrendTimeRange,
                _selectedEfficiencyTimeRange
            ) { tasks, timeRange, categoryTimeRange, trendTimeRange, efficiencyTimeRange ->
                quintuple(tasks, timeRange, categoryTimeRange, trendTimeRange, efficiencyTimeRange)
            }
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = e.message,
                        isLoading = false
                    )
                }
                .collectLatest { (tasks, timeRange, categoryTimeRange, trendTimeRange, efficiencyTimeRange) ->
                    // 任务数据或时间维度变化时自动重新计算统计
                    _uiState.value = _uiState.value.copy(isLoading = true)

                    try {
                        val overviewStats = calculateOverviewBasicStats(
                            tasks = tasks,
                            timeRange = timeRange
                        )

                        // 健康度仍使用全量完成率，基础概览则使用当前时间范围。
                        val total = tasks.size
                        val completed = tasks.count { it.status == TaskStatus.COMPLETED }
                        val completionRate = safePercentage(completed, total)

                        // 重要程度分布与基础概览共用同一时间范围。
                        val importantUrgent = overviewStats.importantUrgent
                        val importantNotUrgent = overviewStats.importantNotUrgent
                        val notImportantUrgent = overviewStats.notImportantUrgent
                        val notImportantNotUrgent = overviewStats.notImportantNotUrgent

                        // 分类统计（根据分类时间维度过滤）
                        val categoryStatsMap = calculateCategoryStats(tasks, categoryTimeRange)

                        // 趋势数据
                        val weeklyTrend = calculateWeeklyTrend(tasks)

                        // 效率数据
                        val timeByCategory = calculateTimeByCategory(tasks)
                        val timeByImportance = calculateTimeByImportance(tasks)
                        val (onTimeRate, overdueRate) = calculateOnTimeRate(tasks)

                        // 新增：本周vs上周对比（使用传入的 timeRange）
                        val weekComparison = calculateWeekComparison(tasks, timeRange)

                        // 新增：重要紧急任务统计
                        val importantUrgentCompletionRate = safePercentage(
                            overviewStats.importantUrgentCompleted,
                            importantUrgent
                        )

                        // 新增：健康度计算
                        val (healthScore, healthLevel) = calculateHealthScore(
                            completionRate = completionRate,
                            onTimeRate = onTimeRate,
                            importantUrgentCompletionRate = importantUrgentCompletionRate
                        )

                        // 新增：智能洞察生成（使用传入的 timeRange）
                        val insights = generateInsights(
                            tasks = tasks,
                            timeRange = timeRange
                        )

                        // 新增：分类效率排行和热力图
                        val categoryEfficiencyRanking = calculateCategoryEfficiencyRanking(categoryStatsMap)
                        val categoryWeekdayHeatmap = calculateCategoryWeekdayHeatmap(tasks)

                        // 新增：趋势Tab数据计算
                        val calendarHeatmap = calculateCalendarHeatmap(tasks)
                        val calendarStats = calculateCalendarStats(calendarHeatmap)
                        val backlogTrend = calculateBacklogTrend(tasks)
                        val velocityAcceleration = calculateVelocityAcceleration(tasks)
                        val completionRateTrend = calculateCompletionRateTrend(tasks)
                        val cycleTimeTrend = calculateCycleTimeTrend(tasks)
                        val cumulativeFlow = calculateCumulativeFlow(tasks)
                        val overdueTrend = calculateOverdueTrend(tasks)

                        // 根据时间范围过滤趋势数据（使用 combine 参数，避免读取 stale uiState）
                        val filteredWeeklyTrend = filterTrendByOverviewTimeRange(
                            weeklyTrend,
                            trendTimeRange
                        )
                        val overviewTrend = filterTrendByOverviewTimeRange(weeklyTrend, timeRange)

                        val filteredBacklogTrend = filterBacklogByOverviewTimeRange(
                            backlogTrend,
                            trendTimeRange
                        )

                        val filteredCompletionRateTrend = filterCompletionRateTrendByOverviewTimeRange(
                            completionRateTrend,
                            trendTimeRange
                        )

                        val filteredCycleTimeTrend = filterCycleTimeTrendByOverviewTimeRange(
                            cycleTimeTrend,
                            trendTimeRange
                        )

                        val filteredCumulativeFlow = filterCumulativeFlowByOverviewTimeRange(
                            cumulativeFlow,
                            trendTimeRange
                        )

                        // 热力图固定显示最近3个月（约90天）
                        val threeMonthsHeatmap = calendarHeatmap.filter {
                            it.date >= LocalDate.now().minusDays(89)
                        }

                        // 新增：效率Tab数据计算（按效率时间维度过滤）
                        val efficiencyTasks = filterTasksByEfficiencyTimeRange(tasks, efficiencyTimeRange)
                        val efficiencySummary = calculateEfficiencySummary(efficiencyTasks)
                        val timeHeatmap = calculateTimeHeatmap(efficiencyTasks)
                        val timeHeatmapStats = calculateTimeHeatmapStats(timeHeatmap)
                        val procrastinationRadar = calculateProcrastinationRadar(efficiencyTasks)
                        val taskFunnel = calculateTaskFunnel(tasks)
                        val delayAnalysis = calculateDelayAnalysis(tasks)

                        _uiState.value = _uiState.value.copy(
                            totalTasks = overviewStats.total,
                            pendingTasks = overviewStats.pending,
                            completedTasks = overviewStats.completed,
                            deferredTasks = overviewStats.deferred,
                            overdueTasks = overviewStats.overdue,
                            cancelledTasks = overviewStats.cancelled,
                            completionRate = overviewStats.completionRate,
                            importantUrgentCount = importantUrgent,
                            importantNotUrgentCount = importantNotUrgent,
                            notImportantUrgentCount = notImportantUrgent,
                            notImportantNotUrgentCount = notImportantNotUrgent,
                            categoryStats = categoryStatsMap,
                            categoryEfficiencyRanking = categoryEfficiencyRanking,
                            categoryWeekdayHeatmap = categoryWeekdayHeatmap,
                            selectedCategoryTimeRange = categoryTimeRange,
                            weeklyTrend = filteredWeeklyTrend,
                            allWeeklyTrend = weeklyTrend, // 保存完整未过滤的数据
                            overviewTrend = overviewTrend,
                            overdueTrend = overdueTrend,
                            calendarHeatmap = threeMonthsHeatmap,
                            allCalendarHeatmap = calendarHeatmap, // 保存完整未过滤的热力图数据
                            calendarStats = calendarStats,
                            backlogTrend = filteredBacklogTrend,
                            allBacklogTrend = backlogTrend,
                            velocityAcceleration = velocityAcceleration,
                            completionRateTrend = filteredCompletionRateTrend,
                            allCompletionRateTrend = completionRateTrend,
                            cycleTimeTrend = filteredCycleTimeTrend,
                            allCycleTimeTrend = cycleTimeTrend,
                            cumulativeFlow = filteredCumulativeFlow,
                            allCumulativeFlow = cumulativeFlow,
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
                            selectedOverviewTimeRange = timeRange,
                            // 核心指标
                            coreMetricPending = overviewStats.corePending,
                            coreMetricImportantUrgent = overviewStats.coreImportantUrgent,
                            coreMetricOverdue = overviewStats.coreOverdue,
                            coreMetricProgress = overviewStats.coreProgress,
                            coreMetricProgressType = overviewStats.coreProgressType,
                            overviewTotalTasks = overviewStats.total,
                            overviewCompletedTasks = overviewStats.completed,
                            overviewCompletionRate = overviewStats.completionRate,
                            selectedEfficiencyTimeRange = efficiencyTimeRange,
                            efficiencySummary = efficiencySummary,
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

    private fun calculateCategoryStats(
        tasks: List<com.nextthing.app.domain.model.Task>,
        timeRange: OverviewTimeRange
    ): Map<Category, CategoryStatsData> {
        val today = LocalDate.now()

        // 获取时间范围的起止日期
        val (rangeStart, rangeEnd) = when (timeRange) {
            OverviewTimeRange.TODAY -> Pair(today, today)
            OverviewTimeRange.THIS_WEEK -> {
                val weekStart = today.with(java.time.DayOfWeek.MONDAY)
                val weekEnd = today.with(java.time.DayOfWeek.SUNDAY)
                Pair(weekStart, weekEnd)
            }
            OverviewTimeRange.THIS_MONTH -> {
                val monthStart = today.withDayOfMonth(1)
                val monthEnd = today.withDayOfMonth(today.lengthOfMonth())
                Pair(monthStart, monthEnd)
            }
            OverviewTimeRange.ALL -> {
                val earliest = tasks.minByOrNull { it.createdAt }?.createdAt?.toLocalDate() ?: today
                Pair(earliest, today)
            }
        }

        // 筛选该时间范围内创建的任务
        val filteredTasks = tasks.filter { task ->
            val taskDate = task.createdAt.toLocalDate()
            taskDate in rangeStart..rangeEnd
        }

        // 获取所有唯一的分类
        val categories = filteredTasks.map { it.category }.distinctBy { it.id }
        return categories.associateWith { category ->
            val categoryTasks = filteredTasks.filter { it.category.id == category.id }
            val completed = categoryTasks.count { it.status == TaskStatus.COMPLETED }
            val pending = categoryTasks.count { it.status == TaskStatus.PENDING }
            val deferred = categoryTasks.count { it.status == TaskStatus.DELAYED }
            val overdue = categoryTasks.count { it.status == TaskStatus.OVERDUE }
            val cancelled = categoryTasks.count { it.status == TaskStatus.CANCELLED }

            val completionRate = if (categoryTasks.isNotEmpty())
                (completed.toFloat() / categoryTasks.size) * 100f else 0f

            val avgDuration = categoryTasks
                .filter { it.status == TaskStatus.COMPLETED && it.actualDuration > 0 }
                .map { it.actualDuration }
                .average()
                .takeIf { !it.isNaN() } ?: 0.0

            // 计算效率分数（5维度）
            val efficiencyScore = calculateCategoryEfficiency(
                total = categoryTasks.size,
                completed = completed,
                pending = pending,
                overdue = overdue,
                cancelled = cancelled,
                categoryTasks = categoryTasks
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

    /**
     * 计算每日趋势数据（最近90天）
     * 生成足够多的数据以支持所有时间范围选择
     */
    private fun calculateWeeklyTrend(tasks: List<com.nextthing.app.domain.model.Task>): List<DailyTrendData> {
        return (0..89).map { daysAgo ->
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

    private fun calculateTimeByCategory(tasks: List<com.nextthing.app.domain.model.Task>): Map<Category, Double> {
        val completedTasks = tasks.filter { it.status == TaskStatus.COMPLETED }
        val categories = tasks.map { it.category }.distinctBy { it.id }
        return categories.associateWith { category ->
            completedTasks
                .filter { it.category.id == category.id && it.actualDuration > 0 }
                .map { it.actualDuration }
                .average()
                .takeIf { !it.isNaN() } ?: 0.0
        }.filter { it.value > 0 }
    }

    private fun calculateTimeByImportance(tasks: List<com.nextthing.app.domain.model.Task>): Map<TaskImportanceUrgency, Double> {
        val completedTasks = tasks.filter { it.status == TaskStatus.COMPLETED }
        return TaskImportanceUrgency.values().associateWith { importance ->
            completedTasks
                .filter { it.importanceUrgency == importance && it.actualDuration > 0 }
                .map { it.actualDuration }
                .average()
                .takeIf { !it.isNaN() } ?: 0.0
        }.filter { it.value > 0 }
    }

    private fun calculateOnTimeRate(tasks: List<com.nextthing.app.domain.model.Task>): Pair<Float, Float> {
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

    /**
     * 计算基于时间维度的任务状态分布
     */
    private fun calculateStatusDistribution(
        tasks: List<com.nextthing.app.domain.model.Task>,
        timeRange: OverviewTimeRange
    ): StatusDistributionData {
        val today = LocalDate.now()

        // 获取时间范围的起止日期
        val (rangeStart, rangeEnd) = when (timeRange) {
            OverviewTimeRange.TODAY -> Pair(today, today)
            OverviewTimeRange.THIS_WEEK -> {
                val weekStart = today.with(java.time.DayOfWeek.MONDAY)
                val weekEnd = today.with(java.time.DayOfWeek.SUNDAY)
                Pair(weekStart, weekEnd)
            }
            OverviewTimeRange.THIS_MONTH -> {
                val monthStart = today.withDayOfMonth(1)
                val monthEnd = today.withDayOfMonth(today.lengthOfMonth())
                Pair(monthStart, monthEnd)
            }
            OverviewTimeRange.ALL -> {
                val earliest = tasks.minByOrNull { it.createdAt }?.createdAt?.toLocalDate() ?: today
                Pair(earliest, today)
            }
        }

        // 筛选该时间范围内创建的任务
        val filteredTasks = tasks.filter { task ->
            val taskDate = task.createdAt.toLocalDate()
            taskDate in rangeStart..rangeEnd
        }

        // 统计各状态数量
        val total = filteredTasks.size
        val pending = filteredTasks.count { it.status == TaskStatus.PENDING }
        val completed = filteredTasks.count { it.status == TaskStatus.COMPLETED }
        val deferred = filteredTasks.count { it.status == TaskStatus.DELAYED }
        val overdue = filteredTasks.count { it.status == TaskStatus.OVERDUE }
        val cancelled = filteredTasks.count { it.status == TaskStatus.CANCELLED }

        val completionRate = if (total > 0) (completed.toFloat() / total) * 100f else 0f

        return StatusDistributionData(
            total = total,
            pending = pending,
            completed = completed,
            deferred = deferred,
            overdue = overdue,
            cancelled = cancelled,
            completionRate = completionRate
        )
    }

    fun selectTab(tab: StatsTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun selectOverviewTimeRange(timeRange: OverviewTimeRange) {
        // 时间维度变化时清除 AI 总结，规则洞察会立即更新
        if (_selectedTimeRange.value != timeRange) {
            _uiState.value = _uiState.value.copy(
                aiSummary = null,
                aiSummaryError = null,
                aiSummaryTimeRange = null
            )
        }
        _selectedTimeRange.value = timeRange
    }

    // 显示任务列表弹窗
    fun showTaskList(taskListType: TaskListType) {
        viewModelScope.launch {
            try {
                // 获取当前所有任务
                val allTasks = taskUseCases.getAllTasks().first()

                // 根据类型和时间范围筛选任务
                val filtered = filterTasksByType(allTasks, taskListType, _selectedTimeRange.value)

                _uiState.value = _uiState.value.copy(
                    showTaskListSheet = true,
                    taskListType = taskListType,
                    filteredTasks = filtered
                )
            } catch (e: Exception) {
                // 处理错误
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    // 关闭任务列表弹窗
    fun hideTaskList() {
        _uiState.value = _uiState.value.copy(
            showTaskListSheet = false,
            taskListType = null,
            filteredTasks = emptyList()
        )
    }

    // 根据类型和时间范围筛选任务
    private fun filterTasksByType(
        tasks: List<com.nextthing.app.domain.model.Task>,
        type: TaskListType,
        timeRange: OverviewTimeRange
    ): List<com.nextthing.app.domain.model.Task> {
        val today = LocalDate.now()

        // 获取时间范围
        val (rangeStart, rangeEnd) = when (timeRange) {
            OverviewTimeRange.TODAY -> today to today
            OverviewTimeRange.THIS_WEEK -> {
                val weekStart = today.with(java.time.DayOfWeek.MONDAY)
                val weekEnd = today.with(java.time.DayOfWeek.SUNDAY)
                weekStart to weekEnd
            }
            OverviewTimeRange.THIS_MONTH -> {
                val monthStart = today.withDayOfMonth(1)
                val monthEnd = today.withDayOfMonth(today.lengthOfMonth())
                monthStart to monthEnd
            }
            OverviewTimeRange.ALL -> {
                val earliest = tasks.minByOrNull { it.createdAt }?.createdAt?.toLocalDate() ?: today
                earliest to today
            }
        }

        // 该时间范围内创建的任务
        val createdInRange = tasks.filter { task ->
            val taskDate = task.createdAt.toLocalDate()
            taskDate in rangeStart..rangeEnd
        }

        // 根据类型筛选
        return when (type) {
            TaskListType.PENDING -> {
                // 待办任务：该时间范围内创建且未完成
                createdInRange.filter { task ->
                    task.status != TaskStatus.COMPLETED && task.status != TaskStatus.CANCELLED
                }
            }
            TaskListType.IMPORTANT_URGENT -> {
                // 重要紧急：该时间范围内创建的重要紧急且未完成
                createdInRange.filter { task ->
                    task.importanceUrgency == TaskImportanceUrgency.IMPORTANT_URGENT &&
                    task.status != TaskStatus.COMPLETED &&
                    task.status != TaskStatus.CANCELLED
                }
            }
            TaskListType.OVERDUE -> {
                // 逾期任务：截止日期在该时间范围内且未完成
                tasks.filter { task ->
                    isStatsTaskOverdueInRange(task, rangeStart, rangeEnd, LocalDateTime.now())
                }
            }
            TaskListType.COMPLETED -> {
                // 已完成：该时间范围内创建且已完成
                createdInRange.filter { task ->
                    task.status == TaskStatus.COMPLETED
                }
            }
        }
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

    // ==================== AI 智能总结 ====================

    fun generateAISummary() {
        val state = _uiState.value
        if (state.isAISummaryLoading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAISummaryLoading = true,
                aiSummaryError = null
            )

            val statsData = buildStatsDataText(state)
            val result = aiStatsAnalyzer.generateSummary(statsData)

            result.fold(
                onSuccess = { summary ->
                    _uiState.value = _uiState.value.copy(
                        aiSummary = summary,
                        isAISummaryLoading = false,
                        aiSummaryError = null,
                        aiSummaryTimeRange = state.selectedOverviewTimeRange
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isAISummaryLoading = false,
                        aiSummaryError = error.message ?: "AI 分析失败，请重试"
                    )
                }
            )
        }
    }

    fun clearAISummary() {
        _uiState.value = _uiState.value.copy(
            aiSummary = null,
            aiSummaryError = null,
            aiSummaryTimeRange = null
        )
    }

    fun clearAISummaryError() {
        _uiState.value = _uiState.value.copy(aiSummaryError = null)
    }

    private fun buildStatsDataText(state: StatsUiState): String {
        val sb = StringBuilder()
        sb.appendLine("时间范围：${state.selectedOverviewTimeRange.displayName}")
        sb.appendLine("任务总数：${state.totalTasks}，已完成：${state.completedTasks}，待办：${state.pendingTasks}，逾期：${state.overdueTasks}，延期：${state.deferredTasks}，放弃：${state.cancelledTasks}")
        sb.appendLine("完成率：${"%.1f".format(state.completionRate)}%")
        sb.appendLine("健康度：${state.healthScore}分（${state.healthLevel.displayName}）")
        sb.appendLine("四象限分布：重要紧急 ${state.importantUrgentCount}个，重要不紧急 ${state.importantNotUrgentCount}个，不重要紧急 ${state.notImportantUrgentCount}个，不重要不紧急 ${state.notImportantNotUrgentCount}个")

        state.weekComparison?.let { wc ->
            val completedSign = if (wc.completedChange >= 0) "+" else ""
            val rateSign = if (wc.completionRateChange >= 0) "+" else ""
            val overdueSign = if (wc.overdueTasksChange >= 0) "+" else ""
            sb.appendLine("与上周对比：完成数 $completedSign${wc.completedChange}，完成率 $rateSign${"%.1f".format(wc.completionRateChange)}%，逾期数 $overdueSign${wc.overdueTasksChange}")
        }

        if (state.categoryStats.isNotEmpty()) {
            val catSummary = state.categoryStats.entries.take(5).joinToString("、") { (cat, data) ->
                "${cat.name}(${data.totalCount}个,完成率${"%.0f".format(data.completionRate * 100)}%)"
            }
            sb.appendLine("分类统计：$catSummary")
        }

        return sb.toString().trim()
    }

    // ==================== 智能洞察生成（精准统计 + 多级提示）====================
    private fun generateInsights(
        tasks: List<com.nextthing.app.domain.model.Task>,
        timeRange: OverviewTimeRange = OverviewTimeRange.TODAY
    ): List<InsightData> {
        val allInsights = mutableListOf<InsightData>()
        val today = LocalDate.now()

        // 获取时间范围的起止日期
        val (rangeStart, rangeEnd, timeRangeLabel, previousRangeStart, previousRangeEnd) = when (timeRange) {
            OverviewTimeRange.TODAY -> {
                val yesterday = today.minusDays(1)
                quintuple(today, today, "今日", yesterday, yesterday)
            }
            OverviewTimeRange.THIS_WEEK -> {
                val weekStart = today.with(java.time.DayOfWeek.MONDAY)
                val weekEnd = today.with(java.time.DayOfWeek.SUNDAY)
                val lastWeekStart = weekStart.minusWeeks(1)
                val lastWeekEnd = weekStart.minusDays(1)
                quintuple(weekStart, weekEnd, "本周", lastWeekStart, lastWeekEnd)
            }
            OverviewTimeRange.THIS_MONTH -> {
                val monthStart = today.withDayOfMonth(1)
                val monthEnd = today.withDayOfMonth(today.lengthOfMonth())
                val lastMonthStart = monthStart.minusMonths(1)
                val lastMonthEnd = lastMonthStart.withDayOfMonth(lastMonthStart.lengthOfMonth())
                quintuple(monthStart, monthEnd, "本月", lastMonthStart, lastMonthEnd)
            }
            OverviewTimeRange.ALL -> {
                val earliest = tasks.minByOrNull { it.createdAt }?.createdAt?.toLocalDate() ?: today
                quintuple(earliest, today, "总体", earliest, today)
            }
        }

        // ==================== 精准数据统计 ====================

        // 1. 该时间范围内创建的任务
        val createdTasks = tasks.filter { task ->
            val taskDate = task.createdAt.toLocalDate()
            taskDate in rangeStart..rangeEnd
        }

        // 2. 该时间范围内完成的任务（从创建的任务中筛选）
        val completedInRange = createdTasks.count { it.status == TaskStatus.COMPLETED }

        // 3. 完成率 = 完成的任务 / 创建的任务
        val createdTotal = createdTasks.size
        val completionRate = safePercentage(completedInRange, createdTotal)

        // 4. 在该时间范围内变成逾期状态的任务（截止日期在范围内且未完成）
        val insightNow = LocalDateTime.now()
        val overdueInRange = tasks.count { task ->
            isStatsTaskOverdueInRange(task, rangeStart, rangeEnd, insightNow)
        }

        // 5. 该时间范围内创建的重要紧急任务
        val importantUrgentTasks = createdTasks.filter {
            it.importanceUrgency == TaskImportanceUrgency.IMPORTANT_URGENT
        }
        val importantUrgentTotal = importantUrgentTasks.size
        val importantUrgentCompleted = importantUrgentTasks.count { it.status == TaskStatus.COMPLETED }
        val importantUrgentRate = safePercentage(importantUrgentCompleted, importantUrgentTotal)

        // ==================== 维度1：完成率洞察（6档）====================
        if (createdTotal > 0) {
            val completionInsight = when {
                completionRate >= 90f -> InsightData(
                    type = InsightType.POSITIVE,
                    icon = "🎉",
                    message = "${timeRangeLabel}完成率${String.format("%.0f", completionRate)}%，表现卓越！"
                )
                completionRate >= 80f -> InsightData(
                    type = InsightType.POSITIVE,
                    icon = "🎉",
                    message = "${timeRangeLabel}完成率${String.format("%.0f", completionRate)}%，表现优秀！"
                )
                completionRate >= 70f -> InsightData(
                    type = InsightType.POSITIVE,
                    icon = "👍",
                    message = "${timeRangeLabel}完成率${String.format("%.0f", completionRate)}%，表现良好"
                )
                completionRate >= 60f -> InsightData(
                    type = InsightType.POSITIVE,
                    icon = "📊",
                    message = "${timeRangeLabel}完成率${String.format("%.0f", completionRate)}%，继续保持"
                )
                completionRate >= 40f -> InsightData(
                    type = InsightType.WARNING,
                    icon = "⚠️",
                    message = "${timeRangeLabel}完成率${String.format("%.0f", completionRate)}%，需要努力"
                )
                else -> InsightData(
                    type = InsightType.ALERT,
                    icon = "🔴",
                    message = "${timeRangeLabel}完成率仅${String.format("%.0f", completionRate)}%，需重点关注"
                )
            }
            allInsights.add(completionInsight)
        }

        // ==================== 维度2：逾期任务洞察（5档）====================
        val overdueInsight = when {
            overdueInRange == 0 -> InsightData(
                type = InsightType.POSITIVE,
                icon = "✅",
                message = "${timeRangeLabel}无逾期任务，时间管理优秀"
            )
            overdueInRange in 1..2 -> InsightData(
                type = InsightType.WARNING,
                icon = "⏰",
                message = "${timeRangeLabel}有${overdueInRange}个逾期任务，需适当关注"
            )
            overdueInRange in 3..5 -> InsightData(
                type = InsightType.WARNING,
                icon = "⚠️",
                message = "${timeRangeLabel}有${overdueInRange}个逾期任务，建议优先处理"
            )
            overdueInRange in 6..10 -> InsightData(
                type = InsightType.ALERT,
                icon = "🔴",
                message = "${timeRangeLabel}有${overdueInRange}个逾期任务，需重点关注"
            )
            else -> InsightData(
                type = InsightType.ALERT,
                icon = "🚨",
                message = "${timeRangeLabel}有${overdueInRange}个逾期任务，任务积压严重！"
            )
        }
        allInsights.add(overdueInsight)

        // ==================== 维度3：重要紧急任务洞察（5档）====================
        if (importantUrgentTotal > 0) {
            val urgentInsight = when {
                importantUrgentRate >= 90f -> InsightData(
                    type = InsightType.POSITIVE,
                    icon = "🔥",
                    message = "${timeRangeLabel}重要紧急任务完成率${String.format("%.0f", importantUrgentRate)}%，执行力强"
                )
                importantUrgentRate >= 80f -> InsightData(
                    type = InsightType.POSITIVE,
                    icon = "🔥",
                    message = "${timeRangeLabel}重要紧急任务完成率${String.format("%.0f", importantUrgentRate)}%，处理及时"
                )
                importantUrgentRate >= 60f -> InsightData(
                    type = InsightType.POSITIVE,
                    icon = "💼",
                    message = "${timeRangeLabel}重要紧急任务完成${importantUrgentCompleted}/${importantUrgentTotal}个，继续加油"
                )
                importantUrgentRate >= 40f -> InsightData(
                    type = InsightType.WARNING,
                    icon = "⚠️",
                    message = "${timeRangeLabel}重要紧急任务完成率${String.format("%.0f", importantUrgentRate)}%，需加快进度"
                )
                else -> InsightData(
                    type = InsightType.ALERT,
                    icon = "⚡",
                    message = "${timeRangeLabel}重要紧急任务完成率仅${String.format("%.0f", importantUrgentRate)}%，需重点关注"
                )
            }
            allInsights.add(urgentInsight)
        } else {
            allInsights.add(InsightData(
                type = InsightType.POSITIVE,
                icon = "😌",
                message = "${timeRangeLabel}暂无重要紧急任务"
            ))
        }

        // ==================== 维度4：任务创建速度分析 ====================
        if (timeRange != OverviewTimeRange.ALL) {
            // 计算前一时间范围内创建的任务数
            val previousCreatedTasks = tasks.filter { task ->
                val taskDate = task.createdAt.toLocalDate()
                taskDate in previousRangeStart..previousRangeEnd
            }.size

            // 计算平均值
            val avgCreated = (createdTotal + previousCreatedTasks) / 2.0

            if (avgCreated > 0) {
                if (createdTotal > avgCreated * 1.5) {
                    allInsights.add(InsightData(
                        type = InsightType.WARNING,
                        icon = "📈",
                        message = "${timeRangeLabel}创建了${createdTotal}个任务，任务量较大"
                    ))
                } else if (createdTotal < avgCreated * 0.5 && createdTotal > 0) {
                    allInsights.add(InsightData(
                        type = InsightType.POSITIVE,
                        icon = "📉",
                        message = "${timeRangeLabel}创建了${createdTotal}个任务，任务量较少"
                    ))
                }
            }
        }

        // ==================== 维度5：完成速度趋势分析 ====================
        if (timeRange != OverviewTimeRange.ALL && createdTotal > 0) {
            val previousCreatedTasks = tasks.filter { task ->
                val taskDate = task.createdAt.toLocalDate()
                taskDate in previousRangeStart..previousRangeEnd
            }

            val previousCompleted = previousCreatedTasks.count { it.status == TaskStatus.COMPLETED }
            val previousTotal = previousCreatedTasks.size

            val previousCompletionRate = if (previousTotal > 0) {
                (previousCompleted.toFloat() / previousTotal) * 100f
            } else 0f

            val rateChange = completionRate - previousCompletionRate

            if (previousTotal > 0) {
                if (rateChange > 10f) {
                    allInsights.add(InsightData(
                        type = InsightType.POSITIVE,
                        icon = "🚀",
                        message = "完成速度比上期提升${String.format("%.0f", rateChange)}%，状态良好"
                    ))
                } else if (rateChange < -10f) {
                    allInsights.add(InsightData(
                        type = InsightType.WARNING,
                        icon = "🐌",
                        message = "完成速度比上期下降${String.format("%.0f", kotlin.math.abs(rateChange))}%，需调整状态"
                    ))
                }
            }
        }

        // 返回最重要的3条洞察（优先级：ALERT > WARNING > POSITIVE）
        return allInsights
            .sortedWith(compareByDescending<InsightData> {
                when (it.type) {
                    InsightType.ALERT -> 3
                    InsightType.WARNING -> 2
                    InsightType.POSITIVE -> 1
                }
            }.thenByDescending { it.message.length })
            .take(3)
    }

    // 辅助函数：创建五元组
    private fun <A, B, C, D, E> quintuple(first: A, second: B, third: C, fourth: D, fifth: E): Quintuple<A, B, C, D, E> {
        return Quintuple(first, second, third, fourth, fifth)
    }

    private data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

    // ==================== 核心指标计算（动态维度）====================
    private fun calculateCoreMetrics(
        tasks: List<com.nextthing.app.domain.model.Task>,
        timeRange: OverviewTimeRange
    ): CoreMetricsData {
        val today = LocalDate.now()

        // 获取时间范围的起止日期
        val (rangeStart, rangeEnd) = when (timeRange) {
            OverviewTimeRange.TODAY -> today to today
            OverviewTimeRange.THIS_WEEK -> {
                val weekStart = today.with(java.time.DayOfWeek.MONDAY)
                val weekEnd = today.with(java.time.DayOfWeek.SUNDAY)
                weekStart to weekEnd
            }
            OverviewTimeRange.THIS_MONTH -> {
                val monthStart = today.withDayOfMonth(1)
                val monthEnd = today.withDayOfMonth(today.lengthOfMonth())
                monthStart to monthEnd
            }
            OverviewTimeRange.ALL -> {
                val earliest = tasks.minByOrNull { it.createdAt }?.createdAt?.toLocalDate() ?: today
                earliest to today
            }
        }

        // 1. 该时间范围内创建的任务
        val createdTasks = tasks.filter { task ->
            val taskDate = task.createdAt.toLocalDate()
            taskDate in rangeStart..rangeEnd
        }

        // 2. 待办任务（该时间范围内创建且未完成）
        val pendingCount = createdTasks.count { task ->
            task.status != TaskStatus.COMPLETED && task.status != TaskStatus.CANCELLED
        }

        // 3. 重要紧急任务（该时间范围内创建的重要紧急且未完成）
        val importantUrgentCount = createdTasks.count { task ->
            task.importanceUrgency == TaskImportanceUrgency.IMPORTANT_URGENT &&
            task.status != TaskStatus.COMPLETED &&
            task.status != TaskStatus.CANCELLED
        }

        // 4. 逾期任务（截止日期在该时间范围内且未完成）
        val overdueCount = tasks.count { task ->
            task.dueDate != null &&
            task.dueDate!!.toLocalDate() in rangeStart..rangeEnd &&
            task.status != TaskStatus.COMPLETED &&
            task.status != TaskStatus.CANCELLED
        }

        // 5. 进度指标
        val (progressValue, progressType) = if (timeRange == OverviewTimeRange.TODAY) {
            // 今日：显示已完成数量
            val completedCount = createdTasks.count { it.status == TaskStatus.COMPLETED }
            completedCount.toString() to "count"
        } else {
            // 其他：显示完成率
            val completedCount = createdTasks.count { it.status == TaskStatus.COMPLETED }
            val total = createdTasks.size
            val rate = if (total > 0) {
                ((completedCount.toFloat() / total) * 100).toInt()
            } else 0
            "${rate}%" to "rate"
        }

        return CoreMetricsData(
            pending = pendingCount,
            importantUrgent = importantUrgentCount,
            overdue = overdueCount,
            progress = progressValue,
            progressType = progressType
        )
    }

    // 核心指标数据类
    private data class CoreMetricsData(
        val pending: Int,
        val importantUrgent: Int,
        val overdue: Int,
        val progress: String,
        val progressType: String
    )


    // ==================== 新增：健康度计算 ====================
    private fun calculateHealthScore(
        completionRate: Float,
        onTimeRate: Float,
        importantUrgentCompletionRate: Float
    ): Pair<Int, HealthLevel> {
        // 三维度加权计算：完成率40%，准时率35%，重要任务完成率25%
        val score = (
            completionRate.coerceIn(0f, 100f) * 0.4f +
                onTimeRate.coerceIn(0f, 100f) * 0.35f +
                importantUrgentCompletionRate.coerceIn(0f, 100f) * 0.25f
            ).toInt().coerceIn(0, 100)

        val level = when {
            score >= HealthLevel.EXCELLENT.minScore -> HealthLevel.EXCELLENT
            score >= HealthLevel.GOOD.minScore -> HealthLevel.GOOD
            score >= HealthLevel.AVERAGE.minScore -> HealthLevel.AVERAGE
            else -> HealthLevel.POOR
        }

        return Pair(score, level)
    }

    // ==================== 新增：时间范围对比（支持多维度）====================
    private fun calculateWeekComparison(
        tasks: List<com.nextthing.app.domain.model.Task>,
        timeRange: OverviewTimeRange = OverviewTimeRange.TODAY
    ): WeekComparisonData? {
        val now = LocalDate.now()

        // 全部维度不显示对比
        if (timeRange == OverviewTimeRange.ALL) {
            return null
        }

        // 根据时间维度计算当前期间和上一期间的日期范围
        val (currentStart, currentEnd, previousStart, previousEnd) = when (timeRange) {
            OverviewTimeRange.TODAY -> {
                // 今日 VS 昨日
                val today = now
                val yesterday = now.minusDays(1)
                Triple(today, today, yesterday to yesterday)
            }
            OverviewTimeRange.THIS_WEEK -> {
                // 本周 VS 上周
                val thisWeekStart = now.with(java.time.DayOfWeek.MONDAY)
                val thisWeekEnd = now.with(java.time.DayOfWeek.SUNDAY)
                val lastWeekStart = thisWeekStart.minusWeeks(1)
                val lastWeekEnd = thisWeekStart.minusDays(1)
                Triple(thisWeekStart, thisWeekEnd, lastWeekStart to lastWeekEnd)
            }
            OverviewTimeRange.THIS_MONTH -> {
                // 本月 VS 上月
                val thisMonthStart = now.withDayOfMonth(1)
                val thisMonthEnd = now.withDayOfMonth(now.lengthOfMonth())
                val lastMonthStart = thisMonthStart.minusMonths(1)
                val lastMonthEnd = lastMonthStart.withDayOfMonth(lastMonthStart.lengthOfMonth())
                Triple(thisMonthStart, thisMonthEnd, lastMonthStart to lastMonthEnd)
            }
            OverviewTimeRange.ALL -> {
                return null // 不应该到这里
            }
        }.let { (start, end, previous) ->
            quadruple(start, end, previous.first, previous.second)
        }

        // 当前期间任务统计
        val currentTasks = tasks.filter {
            val date = it.createdAt.toLocalDate()
            date in currentStart..currentEnd
        }
        // 使用创建在该时间段内且已完成的任务数，避免 completedAt 跨期导致比率超过100%
        val currentCompleted = currentTasks.count { it.status == TaskStatus.COMPLETED }
        val currentCompletionRate = if (currentTasks.isNotEmpty())
            (currentCompleted.toFloat() / currentTasks.size) * 100f else 0f

        val currentAvgDuration = currentTasks
            .filter { it.status == TaskStatus.COMPLETED && it.actualDuration > 0 }
            .map { it.actualDuration }
            .average()
            .takeIf { !it.isNaN() } ?: 0.0

        // 当前期间延期任务数（DELAYED 状态）
        val currentDelayed = currentTasks.count {
            it.status == com.nextthing.app.domain.model.TaskStatus.DELAYED
        }

        // 当前期间放弃任务数（CANCELLED 状态）
        val currentCancelled = currentTasks.count {
            it.status == com.nextthing.app.domain.model.TaskStatus.CANCELLED
        }

        // 当前期间逾期任务数（OVERDUE 状态）
        val currentOverdue = currentTasks.count {
            it.status == com.nextthing.app.domain.model.TaskStatus.OVERDUE
        }

        // 上一期间任务统计
        val previousTasks = tasks.filter {
            val date = it.createdAt.toLocalDate()
            date in previousStart..previousEnd
        }
        // 同样使用创建在该时间段内且已完成的任务数，保持与当前期间计算方式一致
        val previousCompleted = previousTasks.count { it.status == TaskStatus.COMPLETED }
        val previousCompletionRate = if (previousTasks.isNotEmpty())
            (previousCompleted.toFloat() / previousTasks.size) * 100f else 0f

        val previousAvgDuration = previousTasks
            .filter { it.status == TaskStatus.COMPLETED && it.actualDuration > 0 }
            .map { it.actualDuration }
            .average()
            .takeIf { !it.isNaN() } ?: 0.0

        // 上一期间延期任务数
        val previousDelayed = previousTasks.count {
            it.status == com.nextthing.app.domain.model.TaskStatus.DELAYED
        }

        // 上一期间放弃任务数
        val previousCancelled = previousTasks.count {
            it.status == com.nextthing.app.domain.model.TaskStatus.CANCELLED
        }

        // 上一期间逾期任务数
        val previousOverdue = previousTasks.count {
            it.status == com.nextthing.app.domain.model.TaskStatus.OVERDUE
        }

        return WeekComparisonData(
            thisWeekCompleted = currentCompleted,
            lastWeekCompleted = previousCompleted,
            completedChange = currentCompleted - previousCompleted,
            thisWeekCompletionRate = currentCompletionRate,
            lastWeekCompletionRate = previousCompletionRate,
            completionRateChange = currentCompletionRate - previousCompletionRate,
            thisWeekAvgDuration = currentAvgDuration,
            lastWeekAvgDuration = previousAvgDuration,
            avgDurationChange = currentAvgDuration - previousAvgDuration,
            thisWeekTotalTasks = currentTasks.size,
            lastWeekTotalTasks = previousTasks.size,
            totalTasksChange = currentTasks.size - previousTasks.size,
            thisWeekDelayedTasks = currentDelayed,
            lastWeekDelayedTasks = previousDelayed,
            delayedTasksChange = currentDelayed - previousDelayed,
            thisWeekCancelledTasks = currentCancelled,
            lastWeekCancelledTasks = previousCancelled,
            cancelledTasksChange = currentCancelled - previousCancelled,
            thisWeekOverdueTasks = currentOverdue,
            lastWeekOverdueTasks = previousOverdue,
            overdueTasksChange = currentOverdue - previousOverdue
        )
    }

    // 辅助函数：创建四元组
    private fun <A, B, C, D> quadruple(first: A, second: B, third: C, fourth: D): Quadruple<A, B, C, D> {
        return Quadruple(first, second, third, fourth)
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)


    // ==================== 新增：分类效率计算 ====================
    /**
     * 计算分类效率分数（满分100分）
     *
     * 计算规则：
     * - 效率分 = 100 - (延期率 × 60 + 放弃率 × 40)
     * - 延期率权重60%：反映计划能力和时间管理
     * - 放弃率权重40%：反映任务筛选和优先级能力
     * - 分数范围：0-100分
     */
    /**
     * 5 维度效率分计算：
     * 完成率(35%) + 准时率(25%) + 执行率(15%) + 响应速度(15%) + 积压控制(10%)
     */
    private fun calculateCategoryEfficiency(
        total: Int,
        completed: Int,
        pending: Int,
        overdue: Int,
        cancelled: Int,
        categoryTasks: List<com.nextthing.app.domain.model.Task>
    ): Int {
        if (total == 0) return 0

        // 1. 完成率 (35%) — 已完成 / 总数
        val completionRatio = completed.toFloat() / total

        // 2. 准时率 (25%) — 按时完成 / 已完成
        val completedTasks = categoryTasks.filter { it.status == TaskStatus.COMPLETED }
        val onTimeCount = completedTasks.count { task ->
            val due = task.dueDate
            val done = task.completedAt
            if (due == null || done == null) true // 无截止日期视为准时
            else !done.isAfter(due)
        }
        val onTimeRatio = if (completedTasks.isNotEmpty())
            onTimeCount.toFloat() / completedTasks.size else 0.5f

        // 3. 执行率 (15%) — 1 - 放弃数/总数
        val executionRatio = 1f - (cancelled.toFloat() / total)

        // 4. 响应速度 (15%) — 基于平均完成时长（分钟）标准化
        val avgMinutes = completedTasks
            .filter { it.actualDuration > 0 }
            .map { it.actualDuration }
            .average()
            .takeIf { !it.isNaN() } ?: -1.0
        val speedRatio = when {
            avgMinutes < 0 -> 0.5f               // 无数据，中性分
            avgMinutes <= 24 * 60 -> 1.0f         // ≤1天
            avgMinutes <= 3 * 24 * 60 -> 0.8f     // 1~3天
            avgMinutes <= 7 * 24 * 60 -> 0.6f     // 3~7天
            avgMinutes <= 14 * 24 * 60 -> 0.4f    // 7~14天
            else -> 0.2f                           // 14天+
        }

        // 5. 积压控制 (10%) — 1 - 待办数/总数
        val backlogRatio = 1f - (pending.toFloat() / total)

        val score = (completionRatio * 35f +
                     onTimeRatio * 25f +
                     executionRatio * 15f +
                     speedRatio * 15f +
                     backlogRatio * 10f)

        return score.toInt().coerceIn(0, 100)
    }

    // ==================== 新增：分类效率排行榜 ====================
    private fun calculateCategoryEfficiencyRanking(
        categoryStats: Map<Category, CategoryStatsData>
    ): List<CategoryEfficiencyData> {
        return categoryStats.values
            .sortedByDescending { it.efficiencyScore }
            .mapIndexed { index, stats ->
                CategoryEfficiencyData(
                    category = stats.category,
                    efficiencyScore = stats.efficiencyScore,
                    rank = index + 1,
                    completionRate = percentageToRatio(stats.completionRate),
                    avgDuration = stats.averageDuration,
                    overdueRate = if (stats.totalCount > 0) {
                        (stats.overdueCount.toFloat() / stats.totalCount).coerceIn(0f, 1f)
                    } else 0f
                )
            }
    }

    // ==================== 新增：分类×星期热力图 ====================
    private fun calculateCategoryWeekdayHeatmap(
        tasks: List<com.nextthing.app.domain.model.Task>
    ): Map<Category, Map<Int, Int>> {
        val completedTasks = tasks.filter {
            it.status == TaskStatus.COMPLETED && it.completedAt != null
        }

        val categories = tasks.map { it.category }.distinctBy { it.id }
        return categories.associateWith { category ->
            val categoryTasks = completedTasks.filter { it.category.id == category.id }

            // 星期1-7的完成数量统计
            (1..7).associateWith { dayOfWeek ->
                categoryTasks.count {
                    it.completedAt!!.dayOfWeek.value == dayOfWeek
                }
            }
        }.filter { (_, weekdayMap) -> weekdayMap.values.sum() > 0 } // 过滤没有数据的分类
    }

    // ==================== 新增：选择分类 ====================
    fun selectCategory(category: Category?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    // 选择分类页面时间维度
    fun selectCategoryTimeRange(timeRange: OverviewTimeRange) {
        _selectedCategoryTimeRange.value = timeRange
    }

    // 选择效率页面时间维度
    fun selectEfficiencyTimeRange(timeRange: OverviewTimeRange) {
        _selectedEfficiencyTimeRange.value = timeRange
    }

    // ==================== 趋势Tab新增功能 ====================

    /**
     * 计算月历热力图数据（GitHub风格）
     * 最近90天的每日完成情况
     */
    private fun calculateCalendarHeatmap(tasks: List<com.nextthing.app.domain.model.Task>): List<CalendarHeatmapData> {
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
    private fun calculateBacklogTrend(tasks: List<com.nextthing.app.domain.model.Task>): List<BacklogTrendData> {
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
     * 计算最近90天每天结束时仍处于逾期状态的任务数量。
     * 该序列用于趋势页比较当前周期与上一同期的逾期风险。
     */
    private fun calculateOverdueTrend(
        tasks: List<com.nextthing.app.domain.model.Task>
    ): List<DailyOverdueTrendData> {
        val today = LocalDate.now()

        return (0..89).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val endOfDay = date.atTime(23, 59, 59)
            val overdueCount = tasks.count { task ->
                val dueAt = task.dueDate
                val createdInTime = !task.createdAt.isAfter(endOfDay)
                val dueInTime = dueAt != null && !dueAt.isAfter(endOfDay)
                val unfinishedAtEndOfDay = task.completedAt?.isAfter(endOfDay) != false
                val isCountable = task.status != TaskStatus.CANCELLED

                createdInTime && dueInTime && unfinishedAtEndOfDay && isCountable
            }

            DailyOverdueTrendData(date = date, overdueCount = overdueCount)
        }.reversed()
    }

    /**
     * 计算完成速度加速度数据（按周）
     * 展示最近12周的数据
     */
    private fun calculateVelocityAcceleration(tasks: List<com.nextthing.app.domain.model.Task>): List<VelocityAccelerationData> {
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
     * 周完成率走势（最近12周）
     */
    private fun calculateCompletionRateTrend(tasks: List<com.nextthing.app.domain.model.Task>): List<WeeklyCompletionRateData> {
        val today = LocalDate.now()
        return (0..11).map { weeksAgo ->
            val weekEnd = today.minusWeeks(weeksAgo.toLong())
            val weekStart = weekEnd.minusDays(6)
            val weekTasks = tasks.filter { it.createdAt.toLocalDate() in weekStart..weekEnd }
            val completed = weekTasks.count { it.status == TaskStatus.COMPLETED }
            val actionable = weekTasks.count {
                it.status == TaskStatus.COMPLETED || it.status == TaskStatus.OVERDUE || it.status == TaskStatus.CANCELLED
            }.coerceAtLeast(1)
            WeeklyCompletionRateData(
                weekLabel = "${weekStart.monthValue}/${weekStart.dayOfMonth}",
                weekStartDate = weekStart,
                completionRate = completed.toFloat() / actionable,
                totalTasks = weekTasks.size,
                completedTasks = completed
            )
        }.reversed()
    }

    /**
     * 周期时间趋势（最近12周，Cycle Time：创建到完成的天数）
     */
    private fun calculateCycleTimeTrend(tasks: List<com.nextthing.app.domain.model.Task>): List<WeeklyCycleTimeData> {
        val today = LocalDate.now()
        return (0..11).map { weeksAgo ->
            val weekEnd = today.minusWeeks(weeksAgo.toLong())
            val weekStart = weekEnd.minusDays(6)
            val completedInWeek = tasks.filter { task ->
                task.status == TaskStatus.COMPLETED &&
                task.completedAt != null &&
                task.completedAt.toLocalDate() in weekStart..weekEnd
            }
            val avgDays = if (completedInWeek.isNotEmpty()) {
                completedInWeek.map { task ->
                    java.time.temporal.ChronoUnit.HOURS.between(task.createdAt, task.completedAt!!).toFloat() / 24f
                }.average().toFloat()
            } else 0f
            WeeklyCycleTimeData(
                weekLabel = "${weekStart.monthValue}/${weekStart.dayOfMonth}",
                weekStartDate = weekStart,
                avgDays = avgDays,
                taskCount = completedInWeek.size
            )
        }.reversed()
    }

    /**
     * 累积流图（最近30天，每日各状态任务数）
     */
    private fun calculateCumulativeFlow(tasks: List<com.nextthing.app.domain.model.Task>): List<CumulativeFlowData> {
        val today = LocalDate.now()
        return (0..29).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val dateEnd = date.atTime(23, 59, 59)
            // 截止该日期的累计完成数
            val completed = tasks.count {
                it.status == TaskStatus.COMPLETED &&
                it.completedAt != null &&
                !it.completedAt.isAfter(dateEnd)
            }
            // 该日期仍处于逾期状态的任务
            val overdue = tasks.count { task ->
                val due = task.dueDate
                due != null &&
                !due.toLocalDate().isAfter(date) &&
                task.status != TaskStatus.COMPLETED &&
                task.status != TaskStatus.CANCELLED &&
                task.createdAt.toLocalDate() <= date
            }
            // 该日期的待办任务（已创建但未完成/未取消/未逾期）
            val pending = tasks.count { task ->
                task.createdAt.toLocalDate() <= date &&
                task.status != TaskStatus.COMPLETED &&
                task.status != TaskStatus.CANCELLED &&
                (task.dueDate == null || task.dueDate.toLocalDate().isAfter(date))
            }
            CumulativeFlowData(date = date, completed = completed, overdue = overdue, pending = pending)
        }.reversed()
    }

    /**
     * 时间范围选择器
     */
    fun selectTimeRange(timeRange: TimeRange, startDate: LocalDate? = null, endDate: LocalDate? = null) {
        val customStart = if (timeRange == TimeRange.CUSTOM) startDate else null
        val customEnd = if (timeRange == TimeRange.CUSTOM) endDate else null

        val filteredTrend = filterTrendByTimeRange(
            _uiState.value.allWeeklyTrend,
            timeRange,
            customStart,
            customEnd
        )

        val filteredHeatmap = filterHeatmapByTimeRange(
            _uiState.value.allCalendarHeatmap,
            timeRange,
            customStart,
            customEnd
        )

        val filteredBacklog = filterBacklogByTimeRange(
            _uiState.value.allBacklogTrend,
            timeRange,
            customStart,
            customEnd
        )

        // 重新计算热力图统计数据
        val newCalendarStats = if (filteredHeatmap.isNotEmpty()) {
            calculateCalendarStats(filteredHeatmap)
        } else {
            null
        }

        _uiState.value = _uiState.value.copy(
            selectedTimeRange = timeRange,
            customStartDate = customStart,
            customEndDate = customEnd,
            weeklyTrend = filteredTrend,
            calendarHeatmap = filteredHeatmap,
            calendarStats = newCalendarStats,
            backlogTrend = filteredBacklog
        )
    }

    /**
     * 选择趋势页面的时间范围（本周/本月/全部）
     */
    fun selectTrendTimeRange(timeRange: OverviewTimeRange) {
        // 更新 Flow，触发 combine 重新计算（确保任务数据+趋势范围同步）
        _selectedTrendTimeRange.value = timeRange
        // 同时立即过滤已有数据，提升 UI 响应速度
        val filteredTrend = filterTrendByOverviewTimeRange(
            _uiState.value.allWeeklyTrend,
            timeRange
        )
        val filteredBacklog = filterBacklogByOverviewTimeRange(
            _uiState.value.allBacklogTrend,
            timeRange
        )
        val filteredCompletionRate = filterCompletionRateTrendByOverviewTimeRange(
            _uiState.value.allCompletionRateTrend,
            timeRange
        )
        val filteredCycleTime = filterCycleTimeTrendByOverviewTimeRange(
            _uiState.value.allCycleTimeTrend,
            timeRange
        )
        val filteredCumulativeFlow = filterCumulativeFlowByOverviewTimeRange(
            _uiState.value.allCumulativeFlow,
            timeRange
        )

        _uiState.value = _uiState.value.copy(
            selectedTrendTimeRange = timeRange,
            weeklyTrend = filteredTrend,
            backlogTrend = filteredBacklog,
            completionRateTrend = filteredCompletionRate,
            cycleTimeTrend = filteredCycleTime,
            cumulativeFlow = filteredCumulativeFlow
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

    /**
     * 根据时间范围过滤热力图数据
     */
    private fun filterHeatmapByTimeRange(
        allHeatmap: List<CalendarHeatmapData>,
        timeRange: TimeRange,
        customStart: LocalDate?,
        customEnd: LocalDate?
    ): List<CalendarHeatmapData> {
        val today = LocalDate.now()

        return when (timeRange) {
            TimeRange.WEEK_7 -> allHeatmap.filter { it.date >= today.minusDays(6) }
            TimeRange.DAYS_30 -> allHeatmap.filter { it.date >= today.minusDays(29) }
            TimeRange.DAYS_90 -> allHeatmap.filter { it.date >= today.minusDays(89) }
            TimeRange.ALL -> allHeatmap
            TimeRange.CUSTOM -> {
                if (customStart != null && customEnd != null) {
                    allHeatmap.filter { it.date in customStart..customEnd }
                } else {
                    allHeatmap
                }
            }
        }
    }

    /**
     * 根据时间范围过滤积压趋势数据
     */
    private fun filterBacklogByTimeRange(
        allBacklog: List<BacklogTrendData>,
        timeRange: TimeRange,
        customStart: LocalDate?,
        customEnd: LocalDate?
    ): List<BacklogTrendData> {
        val today = LocalDate.now()

        return when (timeRange) {
            TimeRange.WEEK_7 -> allBacklog.filter { it.date >= today.minusDays(6) }
            TimeRange.DAYS_30 -> allBacklog.filter { it.date >= today.minusDays(29) }
            TimeRange.DAYS_90 -> allBacklog.filter { it.date >= today.minusDays(89) }
            TimeRange.ALL -> allBacklog
            TimeRange.CUSTOM -> {
                if (customStart != null && customEnd != null) {
                    allBacklog.filter { it.date in customStart..customEnd }
                } else {
                    allBacklog
                }
            }
        }
    }

    /**
     * 根据OverviewTimeRange过滤趋势数据（本周/本月/全部）
     */
    private fun filterTrendByOverviewTimeRange(
        allTrend: List<DailyTrendData>,
        timeRange: OverviewTimeRange
    ): List<DailyTrendData> {
        val today = LocalDate.now()

        return when (timeRange) {
            OverviewTimeRange.TODAY -> allTrend.filter { it.date == today }
            OverviewTimeRange.THIS_WEEK -> {
                val startOfWeek = today.with(java.time.DayOfWeek.MONDAY)
                allTrend.filter { it.date >= startOfWeek }
            }
            OverviewTimeRange.THIS_MONTH -> {
                val startOfMonth = today.withDayOfMonth(1)
                allTrend.filter { it.date >= startOfMonth }
            }
            OverviewTimeRange.ALL -> allTrend
        }
    }

    /**
     * 根据OverviewTimeRange过滤积压趋势数据（本周/本月/全部）
     */
    private fun filterBacklogByOverviewTimeRange(
        allBacklog: List<BacklogTrendData>,
        timeRange: OverviewTimeRange
    ): List<BacklogTrendData> {
        val today = LocalDate.now()

        return when (timeRange) {
            OverviewTimeRange.TODAY -> allBacklog.filter { it.date == today }
            OverviewTimeRange.THIS_WEEK -> {
                val startOfWeek = today.with(java.time.DayOfWeek.MONDAY)
                allBacklog.filter { it.date >= startOfWeek }
            }
            OverviewTimeRange.THIS_MONTH -> {
                val startOfMonth = today.withDayOfMonth(1)
                allBacklog.filter { it.date >= startOfMonth }
            }
            OverviewTimeRange.ALL -> allBacklog
        }
    }

    /**
     * 根据OverviewTimeRange过滤完成率走势数据
     */
    private fun filterCompletionRateTrendByOverviewTimeRange(
        all: List<WeeklyCompletionRateData>,
        timeRange: OverviewTimeRange
    ): List<WeeklyCompletionRateData> {
        val today = LocalDate.now()
        return when (timeRange) {
            OverviewTimeRange.TODAY -> all.filter { it.weekStartDate == today }
            OverviewTimeRange.THIS_WEEK -> {
                val startOfWeek = today.with(java.time.DayOfWeek.MONDAY)
                all.filter { it.weekStartDate >= startOfWeek }
            }
            OverviewTimeRange.THIS_MONTH -> {
                val startOfMonth = today.withDayOfMonth(1)
                all.filter { it.weekStartDate >= startOfMonth }
            }
            OverviewTimeRange.ALL -> all
        }
    }

    /**
     * 根据OverviewTimeRange过滤周期时间趋势数据
     */
    private fun filterCycleTimeTrendByOverviewTimeRange(
        all: List<WeeklyCycleTimeData>,
        timeRange: OverviewTimeRange
    ): List<WeeklyCycleTimeData> {
        val today = LocalDate.now()
        return when (timeRange) {
            OverviewTimeRange.TODAY -> all.filter { it.weekStartDate == today }
            OverviewTimeRange.THIS_WEEK -> {
                val startOfWeek = today.with(java.time.DayOfWeek.MONDAY)
                all.filter { it.weekStartDate >= startOfWeek }
            }
            OverviewTimeRange.THIS_MONTH -> {
                val startOfMonth = today.withDayOfMonth(1)
                all.filter { it.weekStartDate >= startOfMonth }
            }
            OverviewTimeRange.ALL -> all
        }
    }

    /**
     * 根据OverviewTimeRange过滤累积流图数据
     */
    private fun filterCumulativeFlowByOverviewTimeRange(
        all: List<CumulativeFlowData>,
        timeRange: OverviewTimeRange
    ): List<CumulativeFlowData> {
        val today = LocalDate.now()
        return when (timeRange) {
            OverviewTimeRange.TODAY -> all.filter { it.date == today }
            OverviewTimeRange.THIS_WEEK -> {
                val startOfWeek = today.with(java.time.DayOfWeek.MONDAY)
                all.filter { it.date >= startOfWeek }
            }
            OverviewTimeRange.THIS_MONTH -> {
                val startOfMonth = today.withDayOfMonth(1)
                all.filter { it.date >= startOfMonth }
            }
            OverviewTimeRange.ALL -> all
        }
    }

    /**
     * 按效率时间维度过滤任务（基于 completedAt 或 createdAt）
     */
    private fun filterTasksByEfficiencyTimeRange(
        tasks: List<com.nextthing.app.domain.model.Task>,
        timeRange: OverviewTimeRange
    ): List<com.nextthing.app.domain.model.Task> {
        if (timeRange == OverviewTimeRange.ALL) return tasks
        val today = LocalDate.now()
        val (start, end) = when (timeRange) {
            OverviewTimeRange.TODAY -> today to today
            OverviewTimeRange.THIS_WEEK -> {
                val ws = today.with(java.time.DayOfWeek.MONDAY)
                ws to today.with(java.time.DayOfWeek.SUNDAY)
            }
            OverviewTimeRange.THIS_MONTH -> {
                val ms = today.withDayOfMonth(1)
                ms to today.withDayOfMonth(today.lengthOfMonth())
            }
            OverviewTimeRange.ALL -> return tasks
        }
        return tasks.filter { task ->
            val date = task.completedAt?.toLocalDate() ?: task.createdAt.toLocalDate()
            date in start..end
        }
    }

    // ==================== 效率Tab新增功能 ====================

    private fun calculateEfficiencySummary(
        tasks: List<com.nextthing.app.domain.model.Task>
    ): EfficiencySummaryData {
        val completedTasks = tasks.filter {
            it.status == TaskStatus.COMPLETED && it.completedAt != null
        }
        val completedWithDeadline = completedTasks.filter { it.dueDate != null }
        val onTimeCompleted = completedWithDeadline.count { task ->
            val completedAt = task.completedAt ?: return@count false
            val dueDate = task.dueDate ?: return@count false
            !completedAt.isAfter(dueDate)
        }
        val completionMinutes = completedTasks.mapNotNull { task ->
            val completedAt = task.completedAt ?: return@mapNotNull null
            java.time.Duration.between(task.createdAt, completedAt)
                .toMinutes()
                .takeIf { it >= 0 }
                ?.toDouble()
        }

        return EfficiencySummaryData(
            completedCount = completedTasks.size,
            completedWithDeadlineCount = completedWithDeadline.size,
            onTimeCompletedCount = onTimeCompleted,
            overdueCompletedCount = completedWithDeadline.size - onTimeCompleted,
            averageCompletionMinutes = completionMinutes.average().takeIf { !it.isNaN() } ?: 0.0,
            activeDayCount = completedTasks.mapNotNull { it.completedAt?.toLocalDate() }.distinct().size
        )
    }

    /**
     * 计算时间热力图数据（7×6矩阵）
     * 横轴：周一到周日，纵轴：6个时间段
     */
    private fun calculateTimeHeatmap(tasks: List<com.nextthing.app.domain.model.Task>): List<TimeHeatmapData> {
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
    private fun calculateProcrastinationRadar(tasks: List<com.nextthing.app.domain.model.Task>): ProcrastinationRadarData {
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
    private fun calculateTaskFunnel(tasks: List<com.nextthing.app.domain.model.Task>): TaskFunnelData {
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
    private fun calculateDelayAnalysis(tasks: List<com.nextthing.app.domain.model.Task>): DelayAnalysisData {
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

    // ── AI 周报生成 ──

    fun prepareAIReportContext() {
        if (_uiState.value.isAIReportContextLoading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAIReportContextLoading = true,
                aiReportErrorMessage = null
            )
            try {
                val allTasks = taskUseCases.getAllTasks().first()
                val context = buildAIReportWeekContext(allTasks)
                val routeStatus = aiCompletionClient.routeStatus()
                _uiState.value = _uiState.value.copy(
                    aiRouteStatus = routeStatus,
                    aiReportContextLoaded = true,
                    isAIReportContextLoading = false,
                    aiReportWeekStart = context.weekStart,
                    aiReportWeekEnd = context.weekEnd,
                    aiReportWeekTaskCount = context.tasks.size,
                    aiReportCompletedCount = context.completedTasks.size
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    aiReportContextLoaded = true,
                    isAIReportContextLoading = false,
                    aiReportErrorMessage = error.message ?: "本周任务数据加载失败"
                )
            }
        }
    }

    fun generateWeeklyReport() {
        if (_uiState.value.isGeneratingReport) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGeneratingReport = true,
                aiReportErrorMessage = null
            )
            try {
                val allTasks = taskUseCases.getAllTasks().first()
                val context = buildAIReportWeekContext(allTasks)
                val routeStatus = aiCompletionClient.routeStatus()

                if (context.tasks.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isGeneratingReport = false,
                        aiRouteStatus = routeStatus,
                        aiReportContextLoaded = true,
                        aiReportWeekStart = context.weekStart,
                        aiReportWeekEnd = context.weekEnd,
                        aiReportWeekTaskCount = 0,
                        aiReportCompletedCount = 0
                    )
                    return@launch
                }

                if (routeStatus.mode == AIRouteMode.Unavailable) {
                    _uiState.value = _uiState.value.copy(
                        isGeneratingReport = false,
                        aiRouteStatus = routeStatus,
                        aiReportErrorMessage = "AI 服务尚未配置，请先完成 AI 设置"
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    aiRouteStatus = routeStatus,
                    aiReportContextLoaded = true,
                    aiReportWeekStart = context.weekStart,
                    aiReportWeekEnd = context.weekEnd,
                    aiReportWeekTaskCount = context.tasks.size,
                    aiReportCompletedCount = context.completedTasks.size
                )

                aiWeeklyReporter.generateReport(context.tasks, context.completedTasks)
                    .onSuccess { report ->
                        val exportText = buildExportText(report)
                        _uiState.value = _uiState.value.copy(
                            isGeneratingReport = false,
                            weeklyReport = report,
                            reportExportText = exportText,
                            aiReportErrorMessage = null
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isGeneratingReport = false,
                            aiReportErrorMessage = error.message ?: "周报生成失败"
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGeneratingReport = false,
                    aiReportErrorMessage = e.message ?: "周报生成失败"
                )
            }
        }
    }

    private fun buildAIReportWeekContext(
        allTasks: List<com.nextthing.app.domain.model.Task>
    ): AIReportWeekContext {
        val today = LocalDate.now()
        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val weekEnd = weekStart.plusDays(6)
        val startAt = weekStart.atStartOfDay()
        val endExclusive = weekStart.plusDays(7).atStartOfDay()
        fun inWeek(dateTime: LocalDateTime?): Boolean =
            dateTime != null && dateTime >= startAt && dateTime < endExclusive

        val weekTasks = allTasks.filter { task ->
            if (task.status == TaskStatus.CANCELLED) return@filter false
            inWeek(task.dueDate) ||
                inWeek(task.completedAt) ||
                (task.dueDate == null && inWeek(task.createdAt))
        }
        val completedTasks = weekTasks.filter { task ->
            task.status == TaskStatus.COMPLETED && inWeek(task.completedAt)
        }

        return AIReportWeekContext(
            weekStart = weekStart,
            weekEnd = weekEnd,
            tasks = weekTasks,
            completedTasks = completedTasks
        )
    }

    private fun buildExportText(report: com.nextthing.app.domain.service.WeeklyReport): String {
        val sb = StringBuilder()
        sb.appendLine("# ${report.title}")
        sb.appendLine()
        sb.appendLine(report.summary)
        sb.appendLine()
        if (report.highlights.isNotEmpty()) {
            sb.appendLine("## 亮点")
            report.highlights.forEach { sb.appendLine("- $it") }
            sb.appendLine()
        }
        if (report.behaviorInsights.isNotEmpty()) {
            sb.appendLine("## 行为洞察")
            report.behaviorInsights.forEach { sb.appendLine("- $it") }
            sb.appendLine()
        }
        if (report.improvements.isNotEmpty()) {
            sb.appendLine("## 改进")
            report.improvements.forEach { sb.appendLine("- $it") }
            sb.appendLine()
        }
        if (report.nextWeekSuggestions.isNotEmpty()) {
            sb.appendLine("## 下周建议")
            report.nextWeekSuggestions.forEach { sb.appendLine("- $it") }
        }
        return sb.toString()
    }

    fun exportWeeklyReport(): String? = _uiState.value.reportExportText
}
