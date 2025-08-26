package com.example.nextthingb1.presentation.screens.taskdetail

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
import timber.log.Timber
import javax.inject.Inject

data class TaskDetailUiState(
    val task: Task? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskUseCases: TaskUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    private var currentTaskId: String? = null

    fun loadTask(taskId: String) {
        currentTaskId = taskId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                taskUseCases.getAllTasks().collect { tasks ->
                    val task = tasks.find { it.id == taskId }
                    _uiState.value = _uiState.value.copy(
                        task = task,
                        isLoading = false,
                        errorMessage = if (task == null) "任务不存在" else null
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load task: $taskId")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "加载任务失败"
                )
            }
        }
    }

    fun toggleTaskStatus() {
        val taskId = currentTaskId ?: return
        viewModelScope.launch {
            try {
                taskUseCases.toggleTaskStatus(taskId).fold(
                    onSuccess = {
                        Timber.d("Task status toggled successfully")
                        // 重新加载任务以获取最新状态
                        loadTask(taskId)
                    },
                    onFailure = { error ->
                        Timber.e("Failed to toggle task status: ${error.message}")
                        _uiState.value = _uiState.value.copy(
                            errorMessage = error.message
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception while toggling task status")
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
            }
        }
    }

    fun toggleSubtaskStatus(subtaskId: String) {
        val task = _uiState.value.task ?: return
        viewModelScope.launch {
            try {
                val updatedSubtasks = task.subtasks.map { subtask ->
                    if (subtask.id == subtaskId) {
                        subtask.copy(isCompleted = !subtask.isCompleted)
                    } else {
                        subtask
                    }
                }
                
                val updatedTask = task.copy(subtasks = updatedSubtasks)
                
                taskUseCases.updateTask(updatedTask).fold(
                    onSuccess = {
                        Timber.d("Subtask status toggled successfully")
                        _uiState.value = _uiState.value.copy(task = updatedTask)
                    },
                    onFailure = { error ->
                        Timber.e("Failed to toggle subtask status: ${error.message}")
                        _uiState.value = _uiState.value.copy(
                            errorMessage = error.message
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception while toggling subtask status")
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
            }
        }
    }

    fun deleteTask() {
        val taskId = currentTaskId ?: return
        viewModelScope.launch {
            try {
                taskUseCases.deleteTask(taskId).fold(
                    onSuccess = {
                        Timber.d("Task deleted successfully")
                        _uiState.value = _uiState.value.copy(
                            task = null,
                            errorMessage = "任务已删除"
                        )
                    },
                    onFailure = { error ->
                        Timber.e("Failed to delete task: ${error.message}")
                        _uiState.value = _uiState.value.copy(
                            errorMessage = error.message
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception while deleting task")
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
} 