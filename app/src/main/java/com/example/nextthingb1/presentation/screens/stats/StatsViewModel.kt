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
    // 分类统计
    val categoryStats: Map<TaskCategory, CategoryStatsData> = emptyMap(),
    // 趋势数据
    val weeklyTrend: List<DailyTrendData> = emptyList(),
    val monthlyTrend: List<WeeklyTrendData> = emptyList(),
    val trendViewMode: TrendViewMode = TrendViewMode.WEEK,
    // 效率数据
    val completionTimeByCategory: Map<TaskCategory, Double> = emptyMap(),
    val completionTimeByImportance: Map<TaskImportanceUrgency, Double> = emptyMap(),
    val onTimeCompletionRate: Float = 0f,
    val overdueCompletionRate: Float = 0f,
    val subtaskCompletionData: List<SubtaskStatsData> = emptyList(),
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
    val averageDuration: Double
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
                            weeklyTrend = weeklyTrend,
                            completionTimeByCategory = timeByCategory,
                            completionTimeByImportance = timeByImportance,
                            onTimeCompletionRate = onTimeRate,
                            overdueCompletionRate = overdueRate,
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
            val avgDuration = categoryTasks
                .filter { it.status == TaskStatus.COMPLETED && it.actualDuration > 0 }
                .map { it.actualDuration }
                .average()
                .takeIf { !it.isNaN() } ?: 0.0

            CategoryStatsData(
                category = category,
                totalCount = categoryTasks.size,
                completedCount = completed,
                completionRate = if (categoryTasks.isNotEmpty())
                    (completed.toFloat() / categoryTasks.size) * 100f else 0f,
                averageDuration = avgDuration
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
}
