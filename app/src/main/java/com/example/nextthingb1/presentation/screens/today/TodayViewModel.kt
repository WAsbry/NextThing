package com.example.nextthingb1.presentation.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextthingb1.domain.model.Task
import com.example.nextthingb1.domain.model.TaskCategory
import com.example.nextthingb1.domain.model.TaskPriority
import com.example.nextthingb1.domain.model.TaskStatus
import com.example.nextthingb1.domain.usecase.TaskUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TaskTab(val title: String) {
    PENDING("待办"),
    COMPLETED("已完成")
}

data class TodayUiState(
    val allTasks: List<Task> = emptyList(),
    val displayTasks: List<Task> = emptyList(),
    val selectedTab: TaskTab = TaskTab.PENDING,
    val completionRate: Float = 0f,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val remainingTasks: Int = 0,
    val currentLocationName: String = "",
    val isLocationLoading: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val taskUseCases: TaskUseCases
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()
    
    init {
        loadTodayTasks()
    }
    
    private fun loadTodayTasks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                taskUseCases.getTodayTasks().collect { tasks ->
                    val completed = tasks.filter { it.status == TaskStatus.COMPLETED }
                    val pending = tasks.filter { 
                    it.status == TaskStatus.PENDING || 
                    it.status == TaskStatus.IN_PROGRESS || 
                    it.status == TaskStatus.OVERDUE 
                }
                    
                    _uiState.value = _uiState.value.copy(
                        allTasks = tasks,
                        displayTasks = if (_uiState.value.selectedTab == TaskTab.PENDING) pending else completed,
                        totalTasks = tasks.size,
                        completedTasks = completed.size,
                        remainingTasks = pending.size,
                        completionRate = if (tasks.isNotEmpty()) completed.size.toFloat() / tasks.size else 0f,
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
    
    fun selectTab(tab: TaskTab) {
        val displayTasks = when (tab) {
            TaskTab.PENDING -> _uiState.value.allTasks.filter { 
                it.status == TaskStatus.PENDING || 
                it.status == TaskStatus.IN_PROGRESS || 
                it.status == TaskStatus.OVERDUE 
            }
            TaskTab.COMPLETED -> _uiState.value.allTasks.filter { 
                it.status == TaskStatus.COMPLETED 
            }
        }
        
        _uiState.value = _uiState.value.copy(
            selectedTab = tab,
            displayTasks = displayTasks
        )
    }
    
    fun toggleTaskStatus(taskId: String) {
        viewModelScope.launch {
            try {
                taskUseCases.toggleTaskStatus(taskId).fold(
                    onSuccess = {
                        // 重新加载任务列表
                        loadTodayTasks()
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
    

    fun postponeTask(taskId: String) {
        viewModelScope.launch {
            try {
                // 延期任务：将截止日期推迟一天
                val task = _uiState.value.allTasks.find { it.id == taskId }
                task?.let {
                    val newDueDate = it.dueDate?.plusDays(1) 
                        ?: java.time.LocalDateTime.now().plusDays(1)
                    
                    taskUseCases.updateTask(
                        it.copy(dueDate = newDueDate)
                    ).fold(
                        onSuccess = {
                            loadTodayTasks()
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
    
    fun cancelTask(taskId: String) {
        viewModelScope.launch {
            try {
                val task = _uiState.value.allTasks.find { it.id == taskId }
                task?.let {
                    taskUseCases.updateTask(
                        it.copy(status = TaskStatus.CANCELLED)
                    ).fold(
                        onSuccess = {
                            loadTodayTasks()
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
    
    fun showCreateTaskDialog() {
        // TODO: 实现创建任务对话框
    }
    
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun requestCurrentLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLocationLoading = true)
            try {
                // TODO: 实现获取当前位置的逻辑
                // 暂时设置一个模拟位置
                _uiState.value = _uiState.value.copy(
                    currentLocationName = "北京市朝阳区",
                    isLocationLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "获取位置失败：${e.message}",
                    isLocationLoading = false
                )
            }
        }
    }
} 