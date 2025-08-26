package com.example.nextthingb1.presentation.screens.tasks

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
    val taskCount: Int = 0
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
        updateCurrentMonth()
        loadTasks()
        generateCalendarDays()
    }
    
    private fun updateCurrentMonth() {
        val formatter = DateTimeFormatter.ofPattern("yyyy年MM月")
        _uiState.value = _uiState.value.copy(
            currentMonth = currentMonthDate.format(formatter)
        )
    }
    
    private fun loadTasks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                taskUseCases.getAllTasks().collect { tasks ->
                    val taskGroups = createTaskGroups(tasks)
                    val completedTasks = tasks.count { it.status == TaskStatus.COMPLETED }
                    val pendingTasks = tasks.count { 
                        it.status == TaskStatus.PENDING || it.status == TaskStatus.IN_PROGRESS 
                    }
                    val overdueTasks = tasks.count { it.status == TaskStatus.OVERDUE }
                    val completionRate = if (tasks.isNotEmpty()) {
                        completedTasks.toFloat() / tasks.size
                    } else 0f
                    
                    _uiState.value = _uiState.value.copy(
                        taskGroups = taskGroups,
                        totalTasks = tasks.size,
                        completedTasks = completedTasks,
                        pendingTasks = pendingTasks,
                        overdueTasks = overdueTasks,
                        completionRate = completionRate,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    private fun createTaskGroups(tasks: List<Task>): List<TaskGroup> {
        return tasks.groupBy { task ->
            task.createdAt.toLocalDate().toString()
        }.map { (date, tasksForDate) ->
            val completedCount = tasksForDate.count { it.status == TaskStatus.COMPLETED }
            TaskGroup(
                date = date,
                completedCount = completedCount,
                totalCount = tasksForDate.size,
                tasks = tasksForDate
            )
        }.sortedByDescending { it.date }
    }
    
    private fun generateCalendarDays() {
        val days = mutableListOf<CalendarDay>()
        val firstDayOfMonth = currentMonthDate.withDayOfMonth(1)
        val lastDayOfMonth = currentMonthDate.withDayOfMonth(currentMonthDate.lengthOfMonth())
        val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
        val prevMonth = firstDayOfMonth.minusMonths(1)
        val prevMonthLastDay = prevMonth.lengthOfMonth()
        for (i in startDayOfWeek - 1 downTo 0) {
            val day = prevMonthLastDay - i
            days.add(
                CalendarDay(
                    date = "${prevMonth.year}.${prevMonth.monthValue.toString().padStart(2, '0')}.${day.toString().padStart(2, '0')}",
                    dayNumber = day.toString()
                )
            )
        }
        for (day in 1..lastDayOfMonth.dayOfMonth) {
            val hasTask = day <= 7
            val taskCount = when (day) {
                1 -> 5
                2 -> 1
                3 -> 3
                4 -> 2
                5 -> 4
                6 -> 3
                7 -> 1
                else -> 0
            }
            days.add(
                CalendarDay(
                    date = "${currentMonthDate.year}.${currentMonthDate.monthValue.toString().padStart(2, '0')}.${day.toString().padStart(2, '0')}",
                    dayNumber = if (day == 1) "八月" else day.toString().padStart(2, '0'),
                    hasTask = hasTask,
                    taskCount = taskCount
                )
            )
        }
        val remainingDays = 42 - days.size
        val nextMonth = currentMonthDate.plusMonths(1)
        for (day in 1..remainingDays) {
            days.add(
                CalendarDay(
                    date = "${nextMonth.year}.${nextMonth.monthValue.toString().padStart(2, '0')}.${day.toString().padStart(2, '0')}",
                    dayNumber = day.toString().padStart(2, '0')
                )
            )
        }
        _uiState.value = _uiState.value.copy(calendarDays = days)
    }
    
    fun selectView(view: TaskView) { _uiState.value = _uiState.value.copy(selectedView = view) }
    fun previousMonth() { currentMonthDate = currentMonthDate.minusMonths(1); updateCurrentMonth(); generateCalendarDays() }
    fun nextMonth() { currentMonthDate = currentMonthDate.plusMonths(1); updateCurrentMonth(); generateCalendarDays() }
    fun selectDate(date: String) { _uiState.value = _uiState.value.copy(selectedDate = date) }
    fun clearErrorMessage() { _uiState.value = _uiState.value.copy(errorMessage = null) }
} 