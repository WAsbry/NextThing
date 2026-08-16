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

enum class AISearchState {
    IDLE,
    LOADING,
    ACTIVE,
    EMPTY,
    ERROR
}

internal fun aiSearchStateForResultCount(resultCount: Int): AISearchState =
    if (resultCount > 0) AISearchState.ACTIVE else AISearchState.EMPTY

// ── 筛选枚举 ──

enum class StatusFilter(val label: String) {
    ALL("全部任务"),
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

/** 所有展示路径共用的任务筛选规则。 */
internal object TaskFilterPolicy {
    fun apply(
        tasks: List<Task>,
        statusFilter: StatusFilter,
        categoryId: String?,
        priorityFilter: PriorityFilter
    ): List<Task> = tasks
        .let { list ->
            when (statusFilter) {
                StatusFilter.ALL -> list
                StatusFilter.PENDING -> list.filter { it.status.isPendingLike() }
                StatusFilter.COMPLETED -> list.filter { it.status == TaskStatus.COMPLETED }
                StatusFilter.OVERDUE -> list.filter { it.status == TaskStatus.OVERDUE }
                StatusFilter.CANCELLED -> list.filter { it.status == TaskStatus.CANCELLED }
            }
        }
        .let { list -> if (categoryId == null) list else list.filter { it.category.id == categoryId } }
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

// ── 数据模型 ──

private fun TaskStatus.isPendingLike(): Boolean =
    this == TaskStatus.PENDING || this == TaskStatus.DELAYED

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
    val aiSearchState: AISearchState = AISearchState.IDLE,
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
    val selectedDate: String? = LocalDate.now().toString(),
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
            val matched = if (
                state.aiSearchState == AISearchState.ACTIVE ||
                state.aiSearchState == AISearchState.EMPTY
            ) {
                // 数据刷新时保留当前 AI 候选集合，并换成数据库中的最新任务对象。
                val aiResultIds = state.searchResults.map(Task::id).toSet()
                tasks.filter { it.id in aiResultIds }
            } else {
                val query = state.searchQuery.lowercase()
                tasks.filter { task ->
                    task.title.lowercase().contains(query) ||
                            task.description.lowercase().contains(query)
                }
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

            // 已进入“逾期”分组的任务不能再进入日期分组，否则逾期筛选会重复展示同一任务。
            val nonOverdueTasks = filtered.filter { it.status != TaskStatus.OVERDUE }

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
        updateCalendarDaysStats(tasks, _uiState.value)

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
    ): List<Task> = TaskFilterPolicy.apply(tasks, statusFilter, categoryId, priorityFilter)

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

    fun getMonthLabel(): String {
        val today = LocalDate.now()
        return when {
            currentMonthDate.year == today.year &&
                currentMonthDate.monthValue == today.monthValue -> "本月"
            currentMonthDate.year == today.year -> "${currentMonthDate.monthValue}月"
            else -> "${currentMonthDate.year}年${currentMonthDate.monthValue}月"
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
            updateCalendarDaysStats(_uiState.value.allTasks, _uiState.value)
        }
    }

    private fun updateCalendarDaysStats(allTasks: List<Task>, state: TasksUiState) {
        val filteredTasks = applyFilters(
            tasks = allTasks,
            statusFilter = state.statusFilter,
            categoryId = state.selectedCategoryId,
            priorityFilter = state.priorityFilter
        )
        val updatedDays = _uiState.value.calendarDays.map { day ->
            val dayTasks = filteredTasks.filter { task ->
                val taskDate = task.dueDate?.toLocalDate() ?: task.createdAt.toLocalDate()
                taskDate.toString() == day.date
            }
            day.copy(
                hasTask = dayTasks.isNotEmpty(),
                taskCount = dayTasks.size,
                pendingCount = dayTasks.count { it.status.isPendingLike() },
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
        val filtered = applyFilters(
            tasks = dateTasks,
            statusFilter = state.statusFilter,
            categoryId = state.selectedCategoryId,
            priorityFilter = state.priorityFilter
        )
        _uiState.value = _uiState.value.copy(
            selectedDateTasks = filtered,
            selectedDateCompletedCount = filtered.count { it.status == TaskStatus.COMPLETED },
            selectedDatePendingCount = filtered.count { it.status.isPendingLike() },
            selectedDateOverdueCount = filtered.count { it.status == TaskStatus.OVERDUE },
            selectedDateCancelledCount = filtered.count { it.status == TaskStatus.CANCELLED }
        )
    }

    // ── 公开操作 ──

    fun setSearchActive(active: Boolean) {
        _uiState.value = _uiState.value.copy(isSearchActive = active)
        if (!active) {
            aiSearchJob?.cancel()
            searchQueryFlow.value = ""
            _uiState.value = _uiState.value.copy(
                searchQuery = "",
                searchResults = emptyList(),
                aiSearchState = AISearchState.IDLE
            )
            recomputeFromCurrent()
        }
    }

    fun setSearchQuery(query: String) {
        aiSearchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            aiSearchState = AISearchState.IDLE
        )
        searchQueryFlow.value = query
    }

    // ── AI 搜索 ──

    private var aiSearchJob: Job? = null

    fun searchWithAI() {
        val query = _uiState.value.searchQuery
        if (query.isBlank()) return
        val allTasks = _uiState.value.allTasks
        if (allTasks.isEmpty()) {
            _uiState.value = _uiState.value.copy(aiSearchState = AISearchState.EMPTY)
            return
        }

        aiSearchJob?.cancel()
        aiSearchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(aiSearchState = AISearchState.LOADING)
            aiTaskSearcher.searchByNaturalLanguage(query, allTasks)
                .onSuccess { results ->
                    val currentState = _uiState.value
                    if (
                        currentState.searchQuery != query ||
                        currentState.aiSearchState != AISearchState.LOADING
                    ) return@onSuccess
                    val filteredResults = applyFilters(
                        tasks = results,
                        statusFilter = currentState.statusFilter,
                        categoryId = currentState.selectedCategoryId,
                        priorityFilter = currentState.priorityFilter
                    )
                    _uiState.value = currentState.copy(
                        aiSearchState = aiSearchStateForResultCount(filteredResults.size),
                        searchResults = filteredResults
                    )
                }
                .onFailure {
                    val currentState = _uiState.value
                    if (
                        currentState.searchQuery != query ||
                        currentState.aiSearchState != AISearchState.LOADING
                    ) return@onFailure
                    // 保留点击前的普通搜索结果，仅展示可恢复、可重试的失败状态。
                    _uiState.value = currentState.copy(aiSearchState = AISearchState.ERROR)
                    recomputeFromCurrent()
                }
        }
    }

    fun clearAISearch() {
        aiSearchJob?.cancel()
        _uiState.value = _uiState.value.copy(aiSearchState = AISearchState.IDLE)
        recomputeFromCurrent()
    }

    fun setStatusFilter(filter: StatusFilter) {
        _uiState.value = _uiState.value.copy(
            statusFilter = filter,
            selectedCategoryId = null,
            priorityFilter = PriorityFilter.ALL,
            aiSearchState = AISearchState.IDLE
        )
        recomputeFromCurrent()
    }

    /** 一级任务页仅展示周范围与状态筛选，清除没有入口的历史筛选条件。 */
    fun clearHiddenFilters() {
        val state = _uiState.value
        if (state.selectedCategoryId == null && state.priorityFilter == PriorityFilter.ALL) return
        _uiState.value = state.copy(
            selectedCategoryId = null,
            priorityFilter = PriorityFilter.ALL
        )
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

    fun resetWeek() {
        if (_uiState.value.currentWeekOffset != 0) {
            changeWeek(0)
        }
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

    fun resetMonth() {
        val today = LocalDate.now()
        val alreadyCurrentMonth = currentMonthDate.year == today.year &&
            currentMonthDate.monthValue == today.monthValue
        if (!alreadyCurrentMonth) {
            currentMonthDate = today
            updateCurrentMonth()
            generateCalendarDays()
        }
        selectDate(today.toString())
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
