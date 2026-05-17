package com.nextthing.app.presentation.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadMonthData(_uiState.value.currentMonth)
        loadTasksForDate(_uiState.value.selectedDate)
    }

    fun onMonthChanged(yearMonth: YearMonth) {
        _uiState.value = _uiState.value.copy(currentMonth = yearMonth)
        loadMonthData(yearMonth)
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        loadTasksForDate(date)
    }

    private fun loadMonthData(yearMonth: YearMonth) {
        viewModelScope.launch {
            try {
                val dates = taskRepository.getDatesWithTasksInMonth(yearMonth.year, yearMonth.monthValue)
                _uiState.value = _uiState.value.copy(datesWithTasks = dates.toSet())
            } catch (e: Exception) {
                timber.log.Timber.e(e, "加载月份数据失败")
            }
        }
    }

    private fun loadTasksForDate(date: LocalDate) {
        viewModelScope.launch {
            try {
                val startOfDay = date.atStartOfDay()
                taskRepository.getTasksByDueDate(startOfDay).collect { tasks ->
                    _uiState.value = _uiState.value.copy(tasksForSelectedDate = tasks)
                }
            } catch (e: Exception) {
                timber.log.Timber.e(e, "加载日期任务失败")
            }
        }
    }
}

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val tasksForSelectedDate: List<Task> = emptyList(),
    val datesWithTasks: Set<LocalDate> = emptySet()
)
