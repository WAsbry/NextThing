package com.example.nextthingb1.presentation.screens.tasks

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextthingb1.domain.model.Task
import com.example.nextthingb1.domain.model.TaskStatus
import com.example.nextthingb1.domain.model.TaskTab
import com.example.nextthingb1.domain.usecase.TaskUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class TaskView(val title: String) {
    LIST("周"),
    CALENDAR("月")
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
    val selectedTab: TaskTab = TaskTab.PENDING,
    val currentMonth: String = "",
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val pendingTasks: Int = 0,
    val overdueTasks: Int = 0,
    val completionRate: Float = 0f,
    val taskGroups: List<TaskGroup> = emptyList(),
    val calendarDays: List<CalendarDay> = emptyList(),
    val selectedDate: String? = null,
    val selectedDateTasks: List<Task> = emptyList(),
    val selectedDateCompletedCount: Int = 0,
    val selectedDatePendingCount: Int = 0,
    val selectedDateOverdueCount: Int = 0,
    val selectedDateCancelledCount: Int = 0,
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
        Timber.tag("DataFlow").d("━━━━━━ TasksViewModel.loadTasks ━━━━━━")
        Timber.tag("DataFlow").d("当前周偏移量: ${_uiState.value.currentWeekOffset}")

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            Timber.tag("DataFlow").d("开始加载所有任务，isLoading=true")

            try {
                Timber.tag("DataFlow").d("调用 taskUseCases.getAllTasks().collect")
                taskUseCases.getAllTasks().collect { tasks ->
                    Timber.tag("DataFlow").d("━━━━━━ Flow回调收到数据 ━━━━━━")
                    Timber.tag("DataFlow").d("📊 收到 ${tasks.size} 个任务")
                    tasks.take(5).forEachIndexed { index, task ->
                        Timber.tag("DataFlow").d("  [$index] ${task.title} (${task.status}, dueDate=${task.dueDate})")
                    }
                    if (tasks.size > 5) {
                        Timber.tag("DataFlow").d("  ... 还有 ${tasks.size - 5} 个任务")
                    }

                    // 计算当前周的任务统计
                    val currentWeekTasks = filterTasksByWeek(tasks, _uiState.value.currentWeekOffset)
                    Log.d("TasksViewModel", "当前周的任务数量: ${currentWeekTasks.size}")

                    val taskGroups = createTaskGroups(tasks)
                    Log.d("TasksViewModel", "创建的任务组数量: ${taskGroups.size}")

                    val completedTasks = tasks.count { it.status == TaskStatus.COMPLETED }
                    val pendingTasks = tasks.count {
                        it.status == TaskStatus.PENDING
                    }
                    val overdueTasks = tasks.count { it.status == TaskStatus.OVERDUE }
                    val completionRate = if (tasks.isNotEmpty()) {
                        completedTasks.toFloat() / tasks.size
                    } else 0f

                    val weekCompletedTasks = currentWeekTasks.count { it.status == TaskStatus.COMPLETED }
                    val weekPendingTasks = currentWeekTasks.count {
                        it.status == TaskStatus.PENDING
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
                    Log.d("clickEvent", "UI状态更新完成:")
                    Log.d("clickEvent", "  - 新的任务组数量: ${taskGroups.size}")
                    Log.d("clickEvent", "  - 新的总任务数: ${currentWeekTasks.size}")
                    Log.d("clickEvent", "  - 新的完成任务数: $weekCompletedTasks")
                    Log.d("clickEvent", "  - 新的周偏移量: ${newState.currentWeekOffset}")
                }
            } catch (e: Exception) {
                Log.e("TasksViewModel", "loadTasks() 异常: ${e.message}", e)
                Log.e("clickEvent", "loadTasks() 方法发生异常: ${e.message}")
                Log.e("clickEvent", "  - 异常类型: ${e.javaClass.simpleName}")
                Log.e("clickEvent", "  - 当前周偏移量: ${_uiState.value.currentWeekOffset}")
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
        Log.d("TasksViewModel", "当前选择的标签页: ${_uiState.value.selectedTab}")

        val currentWeekTasks = filterTasksByWeek(tasks, _uiState.value.currentWeekOffset)
        Log.d("TasksViewModel", "过滤后的当前周任务数量: ${currentWeekTasks.size}")

        // 根据selectedTab过滤任务
        val filteredTasks = when (_uiState.value.selectedTab) {
            TaskTab.PENDING -> currentWeekTasks.filter {
                it.status == TaskStatus.PENDING
            }
            TaskTab.COMPLETED -> currentWeekTasks.filter {
                it.status == TaskStatus.COMPLETED
            }
        }
        Log.d("TasksViewModel", "根据标签页过滤后的任务数量: ${filteredTasks.size}")

        val groups = filteredTasks.groupBy { task ->
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
        Log.d("clickEvent", "filterTasksByWeek() 过滤器开始工作:")
        Log.d("clickEvent", "  - 输入任务数量: ${tasks.size}")
        Log.d("clickEvent", "  - 使用的周偏移量: $weekOffset")

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
        Log.d("clickEvent", "filterTasksByWeek() 过滤器完成:")
        Log.d("clickEvent", "  - 输出任务数量: ${filteredTasks.size}")
        Log.d("clickEvent", "  - 过滤效率: ${if (tasks.isNotEmpty()) "${(filteredTasks.size.toFloat() / tasks.size * 100).toInt()}%" else "N/A"}")
        Log.d("clickEvent", "  - 目标周范围: $targetWeekStart ~ $targetWeekEnd")

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

    fun selectTab(tab: TaskTab) {
        Log.d("TasksViewModel", "selectTab() 被调用: $tab")
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        // 重新加载任务数据以应用新的过滤
        loadTasks()
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
    fun selectDate(date: String) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        loadSelectedDateTasks(date)
    }

    private fun loadSelectedDateTasks(date: String) {
        viewModelScope.launch {
            try {
                taskUseCases.getAllTasks().collect { allTasks ->
                    val selectedDateTasks = allTasks.filter { task ->
                        task.createdAt.toLocalDate().toString() == date
                    }

                    val completedCount = selectedDateTasks.count { it.status == TaskStatus.COMPLETED }
                    val pendingCount = selectedDateTasks.count { it.status == TaskStatus.PENDING }
                    val overdueCount = selectedDateTasks.count { it.status == TaskStatus.OVERDUE }
                    val cancelledCount = selectedDateTasks.count { it.status == TaskStatus.CANCELLED }

                    _uiState.value = _uiState.value.copy(
                        selectedDateTasks = selectedDateTasks,
                        selectedDateCompletedCount = completedCount,
                        selectedDatePendingCount = pendingCount,
                        selectedDateOverdueCount = overdueCount,
                        selectedDateCancelledCount = cancelledCount
                    )
                }
            } catch (e: Exception) {
                Log.e("TasksViewModel", "加载选定日期任务失败: ${e.message}", e)
            }
        }
    }
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
                            it.status == TaskStatus.PENDING
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
        Log.d("clickEvent", "=== ViewModel.changeWeek() 方法开始 ===")
        Log.d("clickEvent", "ViewModel接收到的参数:")
        Log.d("clickEvent", "  - 新的周偏移量: $weekOffset")
        Log.d("clickEvent", "  - 当前状态中的周偏移量: ${_uiState.value.currentWeekOffset}")
        Log.d("clickEvent", "  - 早期任务日期: ${_uiState.value.earliestTaskDate}")
        Log.d("clickEvent", "  - 当前任务总数: ${_uiState.value.allTasks.size}")

        val oldState = _uiState.value
        Log.d("clickEvent", "备份旧状态完成，准备更新UI状态...")

        _uiState.value = _uiState.value.copy(currentWeekOffset = weekOffset)

        Log.d("clickEvent", "UI状态更新完成:")
        Log.d("clickEvent", "  - 更新后的周偏移量: ${_uiState.value.currentWeekOffset}")
        Log.d("clickEvent", "  - 状态是否发生变化: ${oldState.currentWeekOffset != _uiState.value.currentWeekOffset}")

        Log.d("clickEvent", "准备重新加载任务数据以应用新的周过滤器...")

        // 重新加载任务数据以应用新的周过滤
        loadTasks()

        Log.d("clickEvent", "=== ViewModel.changeWeek() 方法结束 ===")
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

    // Task action functions for swipe gestures

    fun toggleTaskStatus(taskId: String) {
        viewModelScope.launch {
            try {
                taskUseCases.toggleTaskStatus(taskId).fold(
                    onSuccess = {
                        loadTasks()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = error.message
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
            }
        }
    }

    fun deferTask(taskId: String) {
        viewModelScope.launch {
            try {
                taskUseCases.deferTask(taskId).fold(
                    onSuccess = {
                        loadTasks()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = error.message
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
            }
        }
    }

    fun cancelTask(taskId: String) {
        viewModelScope.launch {
            try {
                val task = _uiState.value.allTasks.find { it.id == taskId }
                task?.let {
                    taskUseCases.updateTask(
                        it.copy(status = TaskStatus.CANCELLED)
                    ).fold(
                        onSuccess = {
                            loadTasks()
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                errorMessage = error.message
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                taskUseCases.deleteTask(taskId).fold(
                    onSuccess = {
                        loadTasks()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = error.message
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
            }
        }
    }
} 