package com.example.nextthingb1.presentation.screens.taskdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextthingb1.domain.model.Task
import com.example.nextthingb1.domain.model.TaskStatus
import com.example.nextthingb1.domain.model.Category
import com.example.nextthingb1.domain.model.CategoryType
import com.example.nextthingb1.domain.model.LocationInfo
import com.example.nextthingb1.domain.model.TaskImportanceUrgency
import com.example.nextthingb1.domain.model.RepeatFrequency
import com.example.nextthingb1.domain.model.Subtask
import com.example.nextthingb1.domain.model.CategoryItem
import com.example.nextthingb1.domain.usecase.TaskUseCases
import com.example.nextthingb1.domain.repository.CategoryRepository
import com.example.nextthingb1.domain.repository.NotificationStrategyRepository
import com.example.nextthingb1.domain.service.CategoryPreferencesManager
import com.example.nextthingb1.domain.usecase.LocationUseCases
import com.example.nextthingb1.domain.model.NotificationStrategy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class TaskDetailUiState(
    val task: Task? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isEditMode: Boolean = false,

    // 基础信息编辑
    val editedTitle: String = "",
    val editedDescription: String = "",

    // 时间信息编辑
    val editedDueDate: LocalDateTime? = null,
    val editedRepeatFrequency: RepeatFrequency = RepeatFrequency(),
    val editedEstimatedDuration: Int = 0,
    val editedActualDuration: Int = 0,

    // 分类和重要性编辑
    val editedCategoryItem: CategoryItem? = null,
    val editedImportanceUrgency: TaskImportanceUrgency? = null,

    // 位置和附件编辑
    val editedLocation: LocationInfo? = null,
    val editedImageUri: String? = null,

    // 标签和子任务编辑
    val editedTags: List<String> = emptyList(),
    val editedSubtasks: List<Subtask> = emptyList(),

    // 通知策略编辑
    val editedNotificationStrategyId: String? = null,

    // 状态编辑
    val editedStatus: TaskStatus = TaskStatus.PENDING,

    // UI 状态
    val showDeleteConfirmDialog: Boolean = false,
    val showDatePicker: Boolean = false,
    val showRepeatFrequencyDialog: Boolean = false,

    // Toast 消息
    val successMessage: String? = null
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskUseCases: TaskUseCases,
    private val categoryRepository: CategoryRepository,
    private val categoryPreferencesManager: CategoryPreferencesManager,
    private val locationUseCases: LocationUseCases,
    private val notificationStrategyRepository: NotificationStrategyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryItem>>(emptyList())
    val categories: StateFlow<List<CategoryItem>> = _categories.asStateFlow()

    private val _savedLocations = MutableStateFlow<List<LocationInfo>>(emptyList())
    val savedLocations: StateFlow<List<LocationInfo>> = _savedLocations.asStateFlow()

    private val _availableNotificationStrategies = MutableStateFlow<List<NotificationStrategy>>(emptyList())
    val availableNotificationStrategies: StateFlow<List<NotificationStrategy>> = _availableNotificationStrategies.asStateFlow()

    private val _showCreateCategoryDialog = MutableStateFlow(false)
    val showCreateCategoryDialog: StateFlow<Boolean> = _showCreateCategoryDialog.asStateFlow()

    private var currentTaskId: String? = null

    init {
        loadCategories()
        loadSavedLocations()
        loadNotificationStrategies()
    }

    private fun loadNotificationStrategies() {
        viewModelScope.launch {
            try {
                notificationStrategyRepository.getAllStrategies().collect { strategies ->
                    _availableNotificationStrategies.value = strategies
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load notification strategies")
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.initializeSystemCategories()
                categoryRepository.getAllCategories().collect { categories ->
                    // 将Category转换为CategoryItem
                    val categoryItems = categories.map { category ->
                        CategoryItem(
                            id = category.id,
                            displayName = category.name,
                            colorHex = category.colorHex,
                            icon = category.icon,
                            isPinned = false,
                            order = category.sortOrder,
                            isSystemDefault = (category.type == CategoryType.PRESET)
                        )
                    }
                    val sortedCategories = categoryPreferencesManager.sortCategoriesByUsage(categoryItems)
                    _categories.value = sortedCategories
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load categories")
            }
        }
    }

    private fun loadSavedLocations() {
        viewModelScope.launch {
            try {
                locationUseCases.getAllSavedLocations().collect { locations ->
                    _savedLocations.value = locations
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load saved locations")
            }
        }
    }

    fun loadTask(taskId: String) {
        Timber.tag("NotificationTask").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Timber.tag("NotificationTask").d("【详情页】loadTask 被调用")
        Timber.tag("NotificationTask").d("  taskId: $taskId")

        currentTaskId = taskId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                taskUseCases.getAllTasks().collect { tasks ->
                    val task = tasks.find { it.id == taskId }

                    if (task != null) {
                        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        Timber.tag("NotificationTask").d("✅ 从数据库加载到的任务信息:")
                        Timber.tag("NotificationTask").d("  ID: ${task.id}")
                        Timber.tag("NotificationTask").d("  标题: ${task.title}")
                        Timber.tag("NotificationTask").d("  描述: ${task.description}")
                        Timber.tag("NotificationTask").d("  分类: ${task.category.displayName}")
                        Timber.tag("NotificationTask").d("  截止时间: ${task.dueDate?.format(formatter) ?: "null"}")
                        Timber.tag("NotificationTask").d("  通知策略ID: ${task.notificationStrategyId ?: "null"}")
                        Timber.tag("NotificationTask").d("  状态: ${task.status}")

                        if (task.dueDate != null) {
                            Timber.tag("NotificationTask").d("  精确时间: ${task.dueDate.hour}:${String.format("%02d", task.dueDate.minute)}")
                        }
                        Timber.tag("NotificationTask").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    } else {
                        Timber.tag("NotificationTask").w("❌ 未找到任务")
                        Timber.tag("NotificationTask").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    }

                    _uiState.value = _uiState.value.copy(
                        task = task,
                        isLoading = false,
                        errorMessage = if (task == null) "任务不存在" else null
                    )
                }
            } catch (e: Exception) {
                Timber.tag("NotificationTask").e("❌ 加载任务失败: ${e.message}")
                Timber.tag("NotificationTask").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
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

    fun enterEditMode() {
        val task = _uiState.value.task ?: return

        // 将任务的category转换为CategoryItem
        val categoryItem = _categories.value.find { it.id == task.category.id }
            ?: CategoryItem(
                id = task.category.id,
                displayName = task.category.name,
                colorHex = task.category.colorHex,
                icon = task.category.icon,
                isPinned = false,
                order = task.category.sortOrder,
                isSystemDefault = (task.category.type == CategoryType.PRESET)
            )

        _uiState.value = _uiState.value.copy(
            isEditMode = true,
            editedTitle = task.title,
            editedDescription = task.description,
            editedDueDate = task.dueDate,
            editedRepeatFrequency = task.repeatFrequency,
            editedEstimatedDuration = task.estimatedDuration,
            editedActualDuration = task.actualDuration,
            editedCategoryItem = categoryItem,
            editedImportanceUrgency = task.importanceUrgency,
            editedLocation = task.locationInfo,
            editedImageUri = task.imageUri,
            editedTags = task.tags,
            editedSubtasks = task.subtasks,
            editedNotificationStrategyId = task.notificationStrategyId,
            editedStatus = task.status
        )
    }

    fun exitEditMode() {
        _uiState.value = _uiState.value.copy(
            isEditMode = false,
            editedTitle = "",
            editedDescription = "",
            editedDueDate = null,
            editedRepeatFrequency = RepeatFrequency(),
            editedEstimatedDuration = 0,
            editedActualDuration = 0,
            editedCategoryItem = null,
            editedImportanceUrgency = null,
            editedLocation = null,
            editedImageUri = null,
            editedTags = emptyList(),
            editedSubtasks = emptyList(),
            editedNotificationStrategyId = null,
            editedStatus = TaskStatus.PENDING
        )
    }

    fun updateEditedTitle(title: String) {
        _uiState.value = _uiState.value.copy(editedTitle = title)
    }

    fun updateEditedDescription(description: String) {
        _uiState.value = _uiState.value.copy(editedDescription = description)
    }

    fun updateEditedDueDate(dueDate: LocalDateTime?) {
        _uiState.value = _uiState.value.copy(editedDueDate = dueDate)
    }

    fun updateSelectedCategory(categoryItem: CategoryItem) {
        viewModelScope.launch {
            try {
                categoryPreferencesManager.recordCategoryUsage(categoryItem.id)
            } catch (e: Exception) {
                Timber.e(e, "Failed to record category usage")
            }
        }
        _uiState.value = _uiState.value.copy(editedCategoryItem = categoryItem)
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            try {
                categoryRepository.deleteCategory(categoryId)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete category")
            }
        }
    }

    fun pinCategory(categoryId: String, isPinned: Boolean) {
        viewModelScope.launch {
            try {
                categoryRepository.pinCategory(categoryId, isPinned)
            } catch (e: Exception) {
                Timber.e(e, "Failed to pin category")
            }
        }
    }

    fun deleteLocation(locationId: String) {
        viewModelScope.launch {
            try {
                locationUseCases.deleteLocation(locationId).fold(
                    onSuccess = {
                        if (_uiState.value.editedLocation?.id == locationId) {
                            _uiState.value = _uiState.value.copy(editedLocation = null)
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(errorMessage = "删除地点失败: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "删除地点时发生错误: ${e.message}")
            }
        }
    }

    fun updateEditedLocation(location: LocationInfo?) {
        _uiState.value = _uiState.value.copy(editedLocation = location)
    }

    fun updateEditedImagePath(imagePath: String?) {
        _uiState.value = _uiState.value.copy(editedImageUri = imagePath)
    }

    // 新增的更新函数
    fun updateEditedRepeatFrequency(repeatFrequency: RepeatFrequency) {
        _uiState.value = _uiState.value.copy(editedRepeatFrequency = repeatFrequency)
    }

    fun updateEditedEstimatedDuration(duration: Int) {
        _uiState.value = _uiState.value.copy(editedEstimatedDuration = duration)
    }

    fun updateEditedActualDuration(duration: Int) {
        _uiState.value = _uiState.value.copy(editedActualDuration = duration)
    }

    fun updateEditedImportanceUrgency(importanceUrgency: TaskImportanceUrgency?) {
        _uiState.value = _uiState.value.copy(editedImportanceUrgency = importanceUrgency)
    }

    fun updateEditedTags(tags: List<String>) {
        _uiState.value = _uiState.value.copy(editedTags = tags)
    }

    fun updateEditedSubtasks(subtasks: List<Subtask>) {
        _uiState.value = _uiState.value.copy(editedSubtasks = subtasks)
    }

    fun updateEditedStatus(status: TaskStatus) {
        _uiState.value = _uiState.value.copy(editedStatus = status)
    }

    fun showDeleteConfirmDialog() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = true)
    }

    fun hideDeleteConfirmDialog() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = false)
    }

    fun showDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = true)
    }

    fun hideDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = false)
    }

    fun showRepeatFrequencyDialog() {
        _uiState.value = _uiState.value.copy(showRepeatFrequencyDialog = true)
    }

    fun hideRepeatFrequencyDialog() {
        _uiState.value = _uiState.value.copy(showRepeatFrequencyDialog = false)
    }

    fun updateNotificationStrategy(strategyId: String?) {
        _uiState.value = _uiState.value.copy(editedNotificationStrategyId = strategyId)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            errorMessage = null
        )
    }

    fun showCreateCategoryDialog() {
        _showCreateCategoryDialog.value = true
    }

    fun hideCreateCategoryDialog() {
        _showCreateCategoryDialog.value = false
    }

    fun createCategory(name: String, colorHex: String = "#9E9E9E") {
        viewModelScope.launch {
            try {
                val result = categoryRepository.createCategory(name, colorHex)
                if (result.isSuccess) {
                    Timber.d("Category created successfully: $name")
                    hideCreateCategoryDialog()
                } else {
                    Timber.e("Failed to create category: ${result.exceptionOrNull()?.message}")
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "创建分类失败: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to create category")
                _uiState.value = _uiState.value.copy(
                    errorMessage = "创建分类失败: ${e.message}"
                )
            }
        }
    }

    fun saveChanges() {
        val task = _uiState.value.task ?: return
        val state = _uiState.value

        if (state.editedTitle.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "任务标题不能为空")
            return
        }

        // 将CategoryItem转换为Category
        val category = state.editedCategoryItem?.let { categoryItem ->
            Category(
                id = categoryItem.id,
                name = categoryItem.displayName,
                type = if (categoryItem.isSystemDefault) CategoryType.PRESET else CategoryType.CUSTOM,
                icon = categoryItem.icon,
                colorHex = categoryItem.colorHex
            )
        } ?: task.category // 如果没有编辑分类，保持原分类

        // 计算任务状态：如果截止时间改变，需要重新计算是否逾期
        val now = LocalDateTime.now()
        val computedStatus = when {
            // 保持已完成和已取消状态不变
            state.editedStatus == TaskStatus.COMPLETED || state.editedStatus == TaskStatus.CANCELLED -> {
                state.editedStatus
            }
            // 如果截止时间已过（超过5分钟），标记为逾期
            state.editedDueDate != null && now.isAfter(state.editedDueDate.plusMinutes(5)) -> {
                Timber.tag("TaskDetailSave").d("⚠️ 截止时间已过，自动标记为 OVERDUE")
                TaskStatus.OVERDUE
            }
            // 否则标记为待办
            else -> {
                if (state.editedStatus == TaskStatus.OVERDUE) {
                    Timber.tag("TaskDetailSave").d("✅ 截止时间在未来，从 OVERDUE 恢复为 PENDING")
                }
                TaskStatus.PENDING
            }
        }

        val updatedTask = task.copy(
            title = state.editedTitle,
            description = state.editedDescription,
            dueDate = state.editedDueDate,
            repeatFrequency = state.editedRepeatFrequency,
            estimatedDuration = state.editedEstimatedDuration,
            actualDuration = state.editedActualDuration,
            category = category,
            importanceUrgency = state.editedImportanceUrgency,
            locationInfo = state.editedLocation,
            imageUri = state.editedImageUri,
            tags = state.editedTags,
            subtasks = state.editedSubtasks,
            notificationStrategyId = state.editedNotificationStrategyId,
            status = computedStatus,
            updatedAt = LocalDateTime.now()
        )

        Timber.tag("TaskDetailSave").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Timber.tag("TaskDetailSave").d("【详情页】准备保存任务修改")
        Timber.tag("TaskDetailSave").d("  任务ID: ${updatedTask.id}")
        Timber.tag("TaskDetailSave").d("  标题: ${updatedTask.title}")
        Timber.tag("TaskDetailSave").d("  描述: ${updatedTask.description}")
        Timber.tag("TaskDetailSave").d("  分类: ${updatedTask.category.name}")
        Timber.tag("TaskDetailSave").d("  重要程度: ${updatedTask.importanceUrgency?.displayName ?: "null"}")
        Timber.tag("TaskDetailSave").d("  截止时间: ${updatedTask.dueDate}")
        Timber.tag("TaskDetailSave").d("  位置: ${updatedTask.locationInfo?.locationName ?: "null"}")
        Timber.tag("TaskDetailSave").d("  图片: ${updatedTask.imageUri ?: "null"}")
        Timber.tag("TaskDetailSave").d("  状态: ${updatedTask.status}")
        Timber.tag("TaskDetailSave").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        viewModelScope.launch {
            try {
                taskUseCases.updateTask(updatedTask).fold(
                    onSuccess = {
                        Timber.tag("TaskDetailSave").d("✅ 任务保存成功")
                        _uiState.value = _uiState.value.copy(
                            task = updatedTask,
                            isEditMode = false,
                            successMessage = "任务修改成功"
                        )
                    },
                    onFailure = { error ->
                        Timber.tag("TaskDetailSave").e("❌ 任务保存失败: ${error.message}")
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "任务修改失败: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.tag("TaskDetailSave").e(e, "❌ 任务保存异常")
                _uiState.value = _uiState.value.copy(
                    errorMessage = "任务修改失败: ${e.message}"
                )
            }
        }
    }
} 