package com.example.nextthingb1.presentation.screens.create

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
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class CreateTaskViewModel @Inject constructor(
    private val taskUseCases: TaskUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateTaskUiState())
    val uiState: StateFlow<CreateTaskUiState> = _uiState.asStateFlow()

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updatePriority(priority: TaskPriority) {
        _uiState.value = _uiState.value.copy(priority = priority)
    }

    fun updateCategory(category: TaskCategory) {
        _uiState.value = _uiState.value.copy(category = category)
    }

    fun updateDueDate(dueDate: String) {
        _uiState.value = _uiState.value.copy(dueDate = dueDate)
    }

    fun createTask() {
        val currentState = _uiState.value
        if (currentState.title.isBlank()) {
            Timber.w("Cannot create task with empty title")
            return
        }

        viewModelScope.launch {
            try {
                val result = taskUseCases.createTask(
                    title = currentState.title,
                    description = currentState.description,
                    priority = currentState.priority,
                    category = currentState.category,
                    dueDate = if (currentState.dueDate.isNotBlank()) LocalDateTime.now().plusDays(1) else null
                )
                
                if (result.isSuccess) {
                    Timber.d("Task created successfully: ${currentState.title}")
                    // 重置表单
                    _uiState.value = CreateTaskUiState()
                } else {
                    Timber.e("Failed to create task: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to create task")
            }
        }
    }
}

data class CreateTaskUiState(
    val title: String = "",
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: TaskCategory = TaskCategory.LIFE,
    val dueDate: String = "",
    val isLoading: Boolean = false
) 