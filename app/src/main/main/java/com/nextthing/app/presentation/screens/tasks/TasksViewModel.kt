package com.nextthing.app.presentation.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextthing.app.domain.model.Category
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.model.TaskImportanceUrgency
import com.nextthing.app.domain.model.TaskStatus
import com.nextthing.app.domain.repository.CategoryRepository
import com.nextthing.app.domain.usecase.TaskUseCases
import com.nextthing.app.domain.service.AITaskSearcher
import com.nextthing.app.util.DailyQuotes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

// ── 视图类型 ──

enum class TaskView(val title: String) {
    LIST("周视图"),
    CALENDAR("月视图")
}

// ── 筛选枚举 ──

enum class StatusFilter(val label: String) {
    ALL("全部"),
    PENDING("待办"),
    COMPLETED("已完成"),
    OVERDUE("逾期"),
    CANCELLED("已取消")
}

enum class PriorityFilter(val label: String) {
    ALL("全部"),
    IMPORTANT_URGENT("重要且紧急"),
    IMPORTANT_NOT_URGENT("重要但不紧急"),
    NOT_IMPORTANT_URGENT("不重要但紧急"),
    NOT_IMPORTANT_NOT_URGENT("不重要且不紧急")
}

// ── 数据模型 ──

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
    val isToday: Boolean = false,
    val isCurrentMonth: Boolean = true,
    val isCurrentWeek: Boolean = false
)

// ── UI 状态 ──

data class TasksUiState(
    val selectedView: TaskView = TaskView.LIST,
    val currentWeekOffset: Int = 0,
    // 搜索
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val isAISearching: Boolean = false,
    val aiSearchUsed: Boolean = false,
    // 筛选
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val selectedCategoryId: String? = null,
    val priorityFilter: PriorityFilter = PriorityFilter.ALL,
    val availableCategories: List<Category> = emptyList(),
    val activeFilterCount: Int = 0,
    // 列表数据
    val overdueGroup: List<Task> = emptyList(),
    val isOverdueSectionExpanded: Boolean = true,
    val taskGroups: List<TaskGroup> = emptyList(),
    val searchResults: List<Task> = emptyList(),
    // 日历数据
    val currentMonth: String = "",
    val calendarDays: List<CalendarDay> = emptyList(),
    val selectedDate: String? = null,
    val selectedDateTasks: List<Task> = emptyList(),
    val selectedDateCompletedCount: Int = 0,
    val selectedDatePendingCount: Int = 0,
    val selectedDateOverdueCount: Int = 0,
    val selectedDateCancelledCount: Int = 0,
    // 原始数据
    val allTasks: List<Task> = emptyList(),
    // 名句
    val dailyQuote: String = "",
    // 通用
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

// ── ViewModel ──

@OptIn(FlowPreview::class)
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskUseCases: TaskUseCases,
    private val categoryRepository: CategoryRepository,
    private val aiTaskSearcher: AITaskSearcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    private var currentMonthDate: LocalDate = LocalDate.now()
    private var loadDataJob: Job? = null
    private val searchQueryFlow = MutableStateFlow("")

    init {
        _uiState.value = _uiState.value.copy(dailyQuote = DailyQuotes.todayQuote())
        updateCurrentMonth()
        loadAllData()
        generateCalendarDays()

        // 搜索防抖
        viewModelScope.launch {
            searchQueryFlow.debounce(300L).collect { query ->
                _uiState.value = _uiState.value.copy(searchQuery = query)
                recomputeFromCurrent()
            }
        }
    }

    // ── 数据加载 ──

    private fun loadAllData() {
        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                combine(
                    taskUseCases.getAllTasks(),
                    categoryRepository.getAllCategories()
                ) { tasks, categories ->
                    Pair(tasks, categories)
                }.collect { (tasks, categories) ->
                    recomputeState(tasks, categories)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message,
                    isLoading = false
                )
            }
        }
    }

    private fun recomputeState(tasks: List<Task>, categories: List<Category>) {
        val state = _uiState.value
        val activeFilterCount = listOf(
            state.statusFilter != StatusFilter.ALL,
            state.selectedCategoryId != null,
            state.priorityFilter != PriorityFilter.ALL
        ).count { it }

        if (state.isSearchActive && state.searchQuery.isNotBlank()) {
            // 搜索模式
            val query = state.searchQuery.lowercase()
            val matched = tasks.filter { task ->
                task.title.lowercase().contains(query) ||
                        task.description.lowercase().contains(query)
            }
            val filtered = applyFilters(matched, state.statusFilter, state.selectedCategoryId, state.priorityFilter)
            _uiState.value = state.copy(
                allTasks = tasks,
                availableCategories = categories,
                activeFilterCount = activeFilterCount,
                searchResults = filtered,
                overdueGroup = emptyList(),
                taskGroups = emptyList(),
                isLoading = false
            )
        } else {
            // 列表模式
            val weekTasks = filterTasksByWeek(tasks, state.currentWeekOffset)
            val filtered = applyFilters(weekTasks, state.statusFilter, state.selectedCategoryId, state.priorityFilter)

            // 分离逾期任务（仅当状态筛选为全部或逾期时显示）
            val overdueGroup = if (state.statusFilter == StatusFilter.ALL || state.statusFilter == StatusFilter.OVERDUE) {
                filtered.filter { it.status == TaskStatus.OVERDUE }
            } else emptyList()

            val nonOverdueTasks = if (state.statusFilter == StatusFilter.ALL) {
                filtered.filter { it.status != TaskStatus.OVERDUE }
            } else filtered

            val taskGroups = createTaskGroups(nonOverdueTasks)

            _uiState.value = state.copy(
                allTasks = tasks,
                availableCategories = categories,
                activeFilterCount = activeFilterCount,
                overdueGroup = overdueGroup,
                taskGroups = taskGroups,
                searchResults = emptyList(),
                isLoading = false
            )
        }

        // 更新日历统计
        updateCalendarDaysStats(tasks)

        // 更新选中日期数据
        val currentState = _uiState.value
        currentState.selectedDate?.let { date ->
            updateSelectedDateData(tasks, date, currentState)
        }
    }

    private fun recomputeFromCurrent() {
        val state = _uiState.value
        if (state.allTasks.isNotEmpty()) {
            recomputeState(state.allTasks, state.availableCategories)
        }
    }

    // ── 过滤工具 ──

    private fun applyFilters(
        tasks: List<Task>,
        statusFilter: StatusFilter,
        categoryId: String?,
        priorityFilter: PriorityFilter
    ): List<Task> {
        return tasks
            .let { list ->
                when (statusFilter) {
                    StatusFilter.ALL -> list
                    StatusFilter.PENDING -> list.filter { it.status == TaskStatus.PENDING }
                    StatusFilter.COMPLETED -> list.filter { it.status == TaskStatus.COMPLETED }
                    StatusFilter.OVERDUE -> list.filter { it.status == TaskStatus.OVERDUE }
                    StatusFilter.CANCELLED -> list.filter { it.status == TaskStatus.CANCELLED }
                }
            }
            .let { list ->
                if (categoryId == null) list
                else list.filter { it.category.id == categoryId }
            }
            .let { list ->
                when (priorityFilter) {
                    PriorityFilter.ALL -> list
                    PriorityFilter.IMPORTANT_URGENT -> list.filter { it.importanceUrgency == TaskImportanceUrgency.IMPORTANT_URGENT }
                    PriorityFilter.IMPORTANT_NOT_URGENT -> list.filter { it.importanceUrgency == TaskImportanceUrgency.IMPORTANT_NOT_URGENT }
                    PriorityFilter.NOT_IMPORTANT_URGENT -> list.filter { it.importanceUrgency == TaskImportanceUrgency.NOT_IMPORTANT_URGENT }
                    PriorityFilter.NOT_IMPORTANT_NOT_URGENT -> list.filter { it.importanceUrgency == TaskImportanceUrgency.NOT_IMPORTANT_NOT_URGENT }
                }
            }
    }

    private fun filterTasksByWeek(tasks: List<Task>, weekOffset: Int): List<Task> {
        val today = LocalDate.now()
        val currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val targetWeekStart = currentWeekStart.plusWeeks(weekOffset.toLong())
        val targetWeekEnd = targetWeekStart.plusDays(6)
        return tasks.filter { task ->
            val taskDate = (task.dueDate?.toLocalDate() ?: task.createdAt.toLocalDate())
            !taskDate.isBefore(targetWeekStart) && !taskDate.isAfter(targetWeekEnd)
        }
    }

    private fun createTaskGroups(tasks: List<Task>): List<TaskGroup> {
        return tasks.groupBy { task ->
            (task.dueDate?.toLocalDate() ?: task.createdAt.toLocalDate()).toString()
        }.map { (date, tasksForDate) ->
            TaskGroup(
                date = date,
                completedCount = tasksForDate.count { it.status == TaskStatus.COMPLETED },
                totalCount = tasksForDate.size,
                tasks = tasksForDate
            )
        }.sortedBy { it.date }
    }

    // ── 周导航标签 ──

    fun getWeekLabel(offset: Int): String {
        return when (offset) {
            0 -> "本周"
            -1 -> "上周"
            1 -> "下周"
            else -> {
                val today = LocalDate.now()
                val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .plusWeeks(offset.toLong())
                val formatter = DateTimeFormatter.ofPattern("M.d")
                weekStart.format(formatter) + " 周"
            }
        }
    }

    // ── 日历 ──

    private fun updateCurrentMonth() {
        val formatter = DateTimeFormatter.ofPattern("yyyy年MM月")
        _uiState.value = _uiState.value.copy(
            currentMonth = currentMonthDate.format(formatter)
        )
    }

    private fun generateCalendarDays() {
        val days = mutableListOf<CalendarDay>()
        val firstDayOfMonth = currentMonthDate.withDayOfMonth(1)
        val lastDayOfMonth = currentMonthDate.withDayOfMonth(currentMonthDate.lengthOfMonth())
        val startDayOfWeek = (firstDayOfMonth.dayOfWeek.value - 1) % 7

        val today = LocalDate.now()
        val currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val currentWeekEnd = currentWeekStart.plusDays(6)

        // 上月填充
        val prevMonth = firstDayOfMonth.minusMonths(1)
        val prevMonthLastDay = prevMonth.lengthOfMonth()
        for (i in startDayOfWeek - 1 downTo 0) {
            val day = prevMonthLastDay - i
            val dayDate = prevMonth.withDayOfMonth(day)
            val isCurrentWeek = !dayDate.isBefore(currentWeekStart) && !dayDate.isAfter(currentWeekEnd)
            days.add(CalendarDay(
                date = "${prevMonth.year}-${prevMonth.monthValue.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}",
                dayNumber = day.toString(),
                isToday = dayDate == today,
                isCurrentMonth = false,
                isCurrentWeek = isCurrentWeek
            ))
        }

        // 当月
        for (day in 1..lastDayOfMonth.dayOfMonth) {
            val dayDate = currentMonthDate.withDayOfMonth(day)
            val isCurrentWeek = !dayDate.isBefore(currentWeekStart) && !dayDate.isAfter(currentWeekEnd)
            days.add(CalendarDay(
                date = "${currentMonthDate.year}-${currentMonthDate.monthValue.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}",
                dayNumber = day.toString(),
                isToday = dayDate == today,
                isCurrentMonth = true,
                isCurrentWeek = isCurrentWeek
            ))
        }

        // 下月填充
        val remainingDays = 42 - days.size
        val nextMonth = currentMonthDate.plusMonths(1)
        for (day in 1..remainingDays) {
            val dayDate = nextMonth.withDayOfMonth(day)
            val isCurrentWeek = !dayDate.isBefore(currentWeekStart) && !dayDate.isAfter(currentWeekEnd)
            days.add(CalendarDay(
                date = "${nextMonth.year}-${nextMonth.monthValue.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}",
                dayNumber = day.toString(),
                isToday = dayDate == today,
                isCurrentMonth = false,
                isCurrentWeek = isCurrentWeek
            ))
        }

        _uiState.value = _uiState.value.copy(calendarDays = days)

        // 如果有已加载的任务数据，立即更新统计
        if (_uiState.value.allTasks.isNotEmpty()) {
            updateCalendarDaysStats(_uiState.value.allTasks)
        }
    }

    private fun updateCalendarDaysStats(allTasks: List<Task>) {
        val updatedDays = _uiState.value.calendarDays.map { day ->
            val dayTasks = allTasks.filter { task ->
                val taskDate = task.dueDate?.toLocalDate() ?: task.createdAt.toLocalDate()
                taskDate.toString() == day.date
            }
            day.copy(
                hasTask = dayTasks.isNotEmpty(),
                taskCount = dayTasks.size,
                pendingCount = dayTasks.count { it.status == TaskStatus.PENDING },
                completedCount = dayTasks.count { it.status == TaskStatus.COMPLETED },
                overdueCount = dayTasks.count { it.status == TaskStatus.OVERDUE },
                cancelledCount = dayTasks.count { it.status == TaskStatus.CANCELLED }
            )
        }
        _uiState.value = _uiState.value.copy(calendarDays = updatedDays)
    }

    private fun updateSelectedDateData(allTasks: List<Task>, date: String, state: TasksUiState) {
        val dateTasks = allTasks.filter { task ->
            (task.dueDate?.toLocalDate() ?: task.createdAt.toLocalDate()).toString() == date
        }
        val filtered = applyFilters(dateTasks, state.statusFilter, state.selectedCategoryId, state.priorityFilter)
        _uiState.value = _uiState.value.copy(
            selectedDateTasks = filtered,
            selectedDateCompletedCount = filtered.count { it.status == TaskStatus.COMPLETED },
            selectedDatePendingCount = filtered.count { it.status == TaskStatus.PENDING },
            selectedDateOverdueCount = filtered.count { it.status == TaskStatus.OVERDUE },
            selectedDateCancelledCount = filtered.count { it.status == TaskStatus.CANCELLED }
        )
    }

    // ── 公开操作 ──

    fun setSearchActive(active: Boolean) {
        _uiState.value = _uiState.value.copy(isSearchActive = active)
        if (!active) {
            searchQueryFlow.value = ""
            _uiState.value = _uiState.value.copy(searchQuery = "", searchResults = emptyList())
            recomputeFromCurrent()
        }
    }

    fun setSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    // ── AI 搜索 ──

    private var aiSearchJob: Job? = null

    fun searchWithAI() {
        val query = _uiState.value.searchQuery
        if (query.isBlank()) return
        val allTasks = _uiState.value.allTasks
        if (allTasks.isEmpty()) return

        aiSearchJob?.cancel()
        aiSearchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAISearching = true)
            aiTaskSearcher.searchByNaturalLanguage(query, allTasks)
                .onSuccess { results ->
                    _uiState.value = _uiState.value.copy(
                        isAISearching = false,
                        aiSearchUsed = true,
                        searchResults = results
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isAISearching = false)
                }
        }
    }

    fun clearAISearch() {
        aiSearchJob?.cancel()
        _uiState.value = _uiState.value.copy(aiSearchUsed = false)
        recomputeFromCurrent()
    }

    fun setStatusFilter(filter: StatusFilter) {
        _uiState.value = _uiState.value.copy(statusFilter = filter)
        recomputeFromCurrent()
    }

    fun setCategoryFilter(categoryId: String?) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
        recomputeFromCurrent()
    }

    fun setPriorityFilter(filter: PriorityFilter) {
        _uiState.value = _uiState.value.copy(priorityFilter = filter)
        recomputeFromCurrent()
    }

    fun clearAllFilters() {
        _uiState.value = _uiState.value.copy(
            statusFilter = StatusFilter.ALL,
            selectedCategoryId = null,
            priorityFilter = PriorityFilter.ALL,
            isSearchActive = false,
            searchQuery = ""
        )
        searchQueryFlow.value = ""
        recomputeFromCurrent()
    }

    fun toggleOverdueSection() {
        _uiState.value = _uiState.value.copy(
            isOverdueSectionExpanded = !_uiState.value.isOverdueSectionExpanded
        )
    }

    fun selectView(view: TaskView) {
        _uiState.value = _uiState.value.copy(selectedView = view)
    }

    fun changeWeek(newOffset: Int) {
        _uiState.value = _uiState.value.copy(currentWeekOffset = newOffset)
        recomputeFromCurrent()
    }

    fun selectDate(date: String) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        updateSelectedDateData(_uiState.value.allTasks, date, _uiState.value)
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

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // ── 任务操作 ──

    fun toggleTaskStatus(taskId: String) {
        viewModelScope.launch {
            try {
                taskUseCases.toggleTaskStatus(taskId)
            } catch (_: Exception) {}
        }
    }

    fun deferTask(taskId: String) {
        viewModelScope.launch {
            try {
                taskUseCases.deferTask(taskId)
            } catch (_: Exception) {}
        }
    }

    fun cancelTask(taskId: String) {
        viewModelScope.launch {
            try {
                val task = _uiState.value.allTasks.find { it.id == taskId }
                task?.let {
                    taskUseCases.updateTask(it.copy(status = TaskStatus.CANCELLED))
                }
            } catch (_: Exception) {}
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                taskUseCases.deleteTask(taskId)
            } catch (_: Exception) {}
        }
    }
}
