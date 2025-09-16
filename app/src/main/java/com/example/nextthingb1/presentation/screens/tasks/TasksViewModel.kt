package com.example.nextthingb1.presentation.screens.tasks

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextthingb1.domain.model.Task
import com.example.nextthingb1.domain.model.TaskStatus
import com.example.nextthingb1.domain.usecase.TaskUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class TaskView(val title: String) {
    LIST("流水"),
    CALENDAR("日历")
}

data class TaskGroup(
    val date: String,
    val completedCount: Int,
    val totalCount: Int,
    val tasks: List<Task>
)

data class CalendarDay(
    val date: String,
    val dayNumber: String,
    val hasTask: Boolean = false,
    val taskCount: Int = 0,
    val pendingCount: Int = 0,
    val completedCount: Int = 0,
    val overdueCount: Int = 0,
    val cancelledCount: Int = 0,
    val isCurrentWeek: Boolean = false
)

data class TasksUiState(
    val selectedView: TaskView = TaskView.LIST,
    val currentMonth: String = "",
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val pendingTasks: Int = 0,
    val overdueTasks: Int = 0,
    val completionRate: Float = 0f,
    val taskGroups: List<TaskGroup> = emptyList(),
    val calendarDays: List<CalendarDay> = emptyList(),
    val selectedDate: String? = null,
    val allTasks: List<Task> = emptyList(),
    val earliestTaskDate: LocalDate? = null,
    val currentWeekOffset: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskUseCases: TaskUseCases
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()
    
    private val currentDate = LocalDate.now()
    private var currentMonthDate = currentDate
    
    init {
        Log.d("TasksViewModel", "=== TasksViewModel 初始化 ===")
        Log.d("TasksViewModel", "初始化周偏移量: ${_uiState.value.currentWeekOffset}")
        updateCurrentMonth()
        loadTasks()
        loadEarliestTaskDate()
        generateCalendarDays()
        Log.d("TasksViewModel", "=== TasksViewModel 初始化完成 ===")
    }
    
    private fun updateCurrentMonth() {
        val formatter = DateTimeFormatter.ofPattern("yyyy年MM月")
        _uiState.value = _uiState.value.copy(
            currentMonth = currentMonthDate.format(formatter)
        )
    }
    
    private fun loadTasks() {
        Log.d("TasksViewModel", "=== loadTasks() 开始 ===")
        Log.d("TasksViewModel", "当前周偏移量: ${_uiState.value.currentWeekOffset}")

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                taskUseCases.getAllTasks().collect { tasks ->
                    Log.d("TasksViewModel", "从数据库获取到 ${tasks.size} 个任务")

                    // 计算当前周的任务统计
                    val currentWeekTasks = filterTasksByWeek(tasks, _uiState.value.currentWeekOffset)
                    Log.d("TasksViewModel", "当前周的任务数量: ${currentWeekTasks.size}")

                    val taskGroups = createTaskGroups(tasks)
                    Log.d("TasksViewModel", "创建的任务组数量: ${taskGroups.size}")

                    val completedTasks = tasks.count { it.status == TaskStatus.COMPLETED }
                    val pendingTasks = tasks.count {
                        it.status == TaskStatus.PENDING || it.status == TaskStatus.IN_PROGRESS
                    }
                    val overdueTasks = tasks.count { it.status == TaskStatus.OVERDUE }
                    val completionRate = if (tasks.isNotEmpty()) {
                        completedTasks.toFloat() / tasks.size
                    } else 0f

                    val weekCompletedTasks = currentWeekTasks.count { it.status == TaskStatus.COMPLETED }
                    val weekPendingTasks = currentWeekTasks.count {
                        it.status == TaskStatus.PENDING || it.status == TaskStatus.IN_PROGRESS
                    }
                    val weekOverdueTasks = currentWeekTasks.count { it.status == TaskStatus.OVERDUE }
                    val weekCompletionRate = if (currentWeekTasks.isNotEmpty()) {
                        weekCompletedTasks.toFloat() / currentWeekTasks.size
                    } else 0f

                    Log.d("TasksViewModel", "周统计 - 完成: $weekCompletedTasks, 待办: $weekPendingTasks, 逾期: $weekOverdueTasks")
                    Log.d("TasksViewModel", "周完成率: $weekCompletionRate")

                    val newState = _uiState.value.copy(
                        taskGroups = taskGroups,
                        totalTasks = currentWeekTasks.size,
                        completedTasks = weekCompletedTasks,
                        pendingTasks = weekPendingTasks,
                        overdueTasks = weekOverdueTasks,
                        completionRate = weekCompletionRate,
                        allTasks = tasks,
                        isLoading = false
                    )

                    Log.d("TasksViewModel", "更新UI状态 - 周偏移: ${newState.currentWeekOffset}")
                    _uiState.value = newState

                    Log.d("TasksViewModel", "UI状态更新完成")
                }
            } catch (e: Exception) {
                Log.e("TasksViewModel", "loadTasks() 异常: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message,
                    isLoading = false
                )
            }
        }

        Log.d("TasksViewModel", "=== loadTasks() 结束 ===")
    }
    
    private fun createTaskGroups(tasks: List<Task>): List<TaskGroup> {
        Log.d("TasksViewModel", "=== createTaskGroups() 开始 ===")
        Log.d("TasksViewModel", "当前周偏移量: ${_uiState.value.currentWeekOffset}")

        val currentWeekTasks = filterTasksByWeek(tasks, _uiState.value.currentWeekOffset)
        Log.d("TasksViewModel", "过滤后的当前周任务数量: ${currentWeekTasks.size}")

        val groups = currentWeekTasks.groupBy { task ->
            task.createdAt.toLocalDate().toString()
        }.map { (date, tasksForDate) ->
            val completedCount = tasksForDate.count { it.status == TaskStatus.COMPLETED }
            Log.d("TasksViewModel", "日期: $date, 任务数: ${tasksForDate.size}, 完成数: $completedCount")
            TaskGroup(
                date = date,
                completedCount = completedCount,
                totalCount = tasksForDate.size,
                tasks = tasksForDate
            )
        }.sortedByDescending { it.date }

        Log.d("TasksViewModel", "生成的任务组数量: ${groups.size}")
        Log.d("TasksViewModel", "=== createTaskGroups() 结束 ===")

        return groups
    }

    private fun filterTasksByWeek(tasks: List<Task>, weekOffset: Int): List<Task> {
        Log.d("TasksViewModel", "=== filterTasksByWeek() 开始 ===")
        Log.d("TasksViewModel", "传入的任务数量: ${tasks.size}")
        Log.d("TasksViewModel", "周偏移量: $weekOffset")

        val today = LocalDate.now()
        val currentWeekStart = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        val targetWeekStart = currentWeekStart.plusWeeks(weekOffset.toLong())
        val targetWeekEnd = targetWeekStart.plusDays(6)

        Log.d("TasksViewModel", "今天日期: $today")
        Log.d("TasksViewModel", "当前周开始: $currentWeekStart")
        Log.d("TasksViewModel", "目标周开始: $targetWeekStart")
        Log.d("TasksViewModel", "目标周结束: $targetWeekEnd")

        val filteredTasks = tasks.filter { task ->
            val taskDate = task.createdAt.toLocalDate()
            val inWeek = !taskDate.isBefore(targetWeekStart) && !taskDate.isAfter(targetWeekEnd)
            Log.d("TasksViewModel", "任务: ${task.title}, 创建日期: $taskDate, 在目标周内: $inWeek")
            inWeek
        }

        Log.d("TasksViewModel", "过滤后的任务数量: ${filteredTasks.size}")
        Log.d("TasksViewModel", "=== filterTasksByWeek() 结束 ===")

        return filteredTasks
    }
    
    private fun generateCalendarDays() {
        val days = mutableListOf<CalendarDay>()
        val firstDayOfMonth = currentMonthDate.withDayOfMonth(1)
        val lastDayOfMonth = currentMonthDate.withDayOfMonth(currentMonthDate.lengthOfMonth())

        // 修改为星期一为起点（1=周一，7=周日）
        val startDayOfWeek = (firstDayOfMonth.dayOfWeek.value - 1) % 7

        // 获取当前周的范围用于判断是否为当前周
        val today = LocalDate.now()
        val currentWeekStart = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        val currentWeekEnd = currentWeekStart.plusDays(6)

        // 填充上月天数
        val prevMonth = firstDayOfMonth.minusMonths(1)
        val prevMonthLastDay = prevMonth.lengthOfMonth()
        for (i in startDayOfWeek - 1 downTo 0) {
            val day = prevMonthLastDay - i
            val dayDate = prevMonth.withDayOfMonth(day)
            val isCurrentWeek = !dayDate.isBefore(currentWeekStart) && !dayDate.isAfter(currentWeekEnd)

            days.add(
                CalendarDay(
                    date = "${prevMonth.year}-${prevMonth.monthValue.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}",
                    dayNumber = day.toString(),
                    isCurrentWeek = isCurrentWeek
                )
            )
        }

        // 填充当月天数
        for (day in 1..lastDayOfMonth.dayOfMonth) {
            val dayDate = currentMonthDate.withDayOfMonth(day)
            val isCurrentWeek = !dayDate.isBefore(currentWeekStart) && !dayDate.isAfter(currentWeekEnd)

            // 获取当天的任务统计（这里先用空值，稍后会在另一个方法中更新）
            days.add(
                CalendarDay(
                    date = "${currentMonthDate.year}-${currentMonthDate.monthValue.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}",
                    dayNumber = day.toString(),
                    isCurrentWeek = isCurrentWeek
                )
            )
        }

        // 填充下月天数
        val remainingDays = 42 - days.size
        val nextMonth = currentMonthDate.plusMonths(1)
        for (day in 1..remainingDays) {
            val dayDate = nextMonth.withDayOfMonth(day)
            val isCurrentWeek = !dayDate.isBefore(currentWeekStart) && !dayDate.isAfter(currentWeekEnd)

            days.add(
                CalendarDay(
                    date = "${nextMonth.year}-${nextMonth.monthValue.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}",
                    dayNumber = day.toString(),
                    isCurrentWeek = isCurrentWeek
                )
            )
        }

        _uiState.value = _uiState.value.copy(calendarDays = days)

        // 更新日历天数的任务统计
        updateCalendarTasksStatistics()
    }
    
    fun selectView(view: TaskView) {
        Log.d("TasksViewModel", "selectView() 被调用: $view")
        _uiState.value = _uiState.value.copy(selectedView = view)
    }
    fun previousMonth() {
        currentMonthDate = currentMonthDate.minusMonths(1)
        updateCurrentMonth()
        generateCalendarDays()
    }

    fun nextMonth() {
        currentMonthDate = currentMonthDate.plusMonths(1)
        updateCurrentMonth()
        generateCalendarDays()
    }
    fun selectDate(date: String) { _uiState.value = _uiState.value.copy(selectedDate = date) }
    fun clearErrorMessage() { _uiState.value = _uiState.value.copy(errorMessage = null) }

    private fun updateCalendarTasksStatistics() {
        viewModelScope.launch {
            try {
                taskUseCases.getAllTasks().collect { allTasks ->
                    val updatedCalendarDays = _uiState.value.calendarDays.map { day ->
                        val dayTasks = allTasks.filter { task ->
                            val taskDate = task.createdAt.toLocalDate()
                            taskDate.toString() == day.date
                        }

                        val pendingCount = dayTasks.count {
                            it.status == TaskStatus.PENDING || it.status == TaskStatus.IN_PROGRESS
                        }
                        val completedCount = dayTasks.count { it.status == TaskStatus.COMPLETED }
                        val overdueCount = dayTasks.count { it.status == TaskStatus.OVERDUE }
                        val cancelledCount = dayTasks.count { it.status == TaskStatus.CANCELLED }
                        val totalTasks = dayTasks.size

                        day.copy(
                            hasTask = totalTasks > 0,
                            taskCount = totalTasks,
                            pendingCount = pendingCount,
                            completedCount = completedCount,
                            overdueCount = overdueCount,
                            cancelledCount = cancelledCount
                        )
                    }

                    _uiState.value = _uiState.value.copy(calendarDays = updatedCalendarDays)
                }
            } catch (e: Exception) {
                Log.e("TasksViewModel", "更新日历任务统计失败: ${e.message}", e)
            }
        }
    }

    fun changeWeek(weekOffset: Int) {
        Log.d("switchWeek", "=== changeWeek() 开始 ===")
        Log.d("switchWeek", "ViewModel接收到新的周偏移量: $weekOffset")
        Log.d("switchWeek", "当前状态中的周偏移量: ${_uiState.value.currentWeekOffset}")

        val oldState = _uiState.value
        _uiState.value = _uiState.value.copy(currentWeekOffset = weekOffset)

        Log.d("switchWeek", "状态更新后的周偏移量: ${_uiState.value.currentWeekOffset}")
        Log.d("switchWeek", "开始重新加载任务数据...")

        // 重新加载任务数据以应用新的周过滤
        loadTasks()

        Log.d("switchWeek", "=== changeWeek() 结束 ===")
    }

    private fun loadEarliestTaskDate() {
        viewModelScope.launch {
            try {
                Log.d("weekCount", "开始获取数据库最早任务日期...")
                val earliestDate = taskUseCases.getEarliestTaskDate()
                Log.d("weekCount", "获取到的最早任务日期: $earliestDate")
                _uiState.value = _uiState.value.copy(
                    earliestTaskDate = earliestDate
                )
                Log.d("weekCount", "已更新UI状态中的最早任务日期")
            } catch (e: Exception) {
                Log.e("weekCount", "获取最早任务日期失败: ${e.message}", e)
                // If no tasks exist, earliest date remains null
            }
        }
    }
} 