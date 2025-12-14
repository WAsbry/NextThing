package com.example.nextthingb1.presentation.screens.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextthingb1.domain.model.Task
import com.example.nextthingb1.domain.model.Category
import com.example.nextthingb1.domain.model.CategoryType
import com.example.nextthingb1.domain.model.TaskStatus
import com.example.nextthingb1.domain.model.CategoryItem
import com.example.nextthingb1.domain.model.TaskImportanceUrgency
import com.example.nextthingb1.domain.model.RepeatFrequency
import com.example.nextthingb1.domain.model.RepeatFrequencyType
import com.example.nextthingb1.domain.usecase.TaskUseCases
import com.example.nextthingb1.domain.repository.CategoryRepository
import com.example.nextthingb1.domain.repository.NotificationStrategyRepository
import com.example.nextthingb1.domain.service.CategoryPreferencesManager
import com.example.nextthingb1.domain.usecase.LocationUseCases
import com.example.nextthingb1.domain.model.LocationInfo
import com.example.nextthingb1.domain.model.NotificationStrategy
import com.example.nextthingb1.domain.model.GeofenceLocation
import com.example.nextthingb1.domain.usecase.GeofenceUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class CreateTaskViewModel @Inject constructor(
    private val taskUseCases: TaskUseCases,
    private val categoryRepository: CategoryRepository,
    private val categoryPreferencesManager: CategoryPreferencesManager,
    private val locationUseCases: LocationUseCases,
    private val notificationStrategyRepository: NotificationStrategyRepository,
    private val geofenceUseCases: GeofenceUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateTaskUiState())
    val uiState: StateFlow<CreateTaskUiState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryItem>>(emptyList())
    val categories: StateFlow<List<CategoryItem>> = _categories.asStateFlow()

    private val _showCreateCategoryDialog = MutableStateFlow(false)
    val showCreateCategoryDialog: StateFlow<Boolean> = _showCreateCategoryDialog.asStateFlow()

    private val _savedLocations = MutableStateFlow<List<LocationInfo>>(emptyList())
    val savedLocations: StateFlow<List<LocationInfo>> = _savedLocations.asStateFlow()

    private val _availableGeofenceLocations = MutableStateFlow<List<GeofenceLocation>>(emptyList())
    val availableGeofenceLocations: StateFlow<List<GeofenceLocation>> = _availableGeofenceLocations.asStateFlow()

    init {
        initializeCategories()
        loadSavedLocations()
        loadNotificationStrategies()
        loadAvailableGeofenceLocations()
    }

    private fun initializeCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.initializeSystemCategories()

                // 首先获取上次选择的分类
                val lastSelectedCategoryId = categoryPreferencesManager.getLastSelectedCategoryId()

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

                    // 按使用频率排序分类
                    val sortedCategories = categoryPreferencesManager.sortCategoriesByUsage(categoryItems)
                    _categories.value = sortedCategories

                    // 设置默认选中的分类
                    loadLastSelectedCategory(lastSelectedCategoryId)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize categories")
            }
        }
    }

    private fun loadLastSelectedCategory(categoryId: String) {
        try {
            // 在分类列表中查找对应的CategoryItem
            val categoryItem = _categories.value.find { it.id == categoryId }
                ?: _categories.value.find { it.displayName == "生活" } // 默认生活分类
                ?: _categories.value.firstOrNull() // 使用第一个分类作为备用

            _uiState.value = _uiState.value.copy(selectedCategoryItem = categoryItem)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load last selected category")
            // 出错时使用第一个分类
            val defaultCategory = _categories.value.firstOrNull()
            _uiState.value = _uiState.value.copy(selectedCategoryItem = defaultCategory)
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

    private fun loadNotificationStrategies() {
        viewModelScope.launch {
            try {
                notificationStrategyRepository.getAllStrategies().collect { strategies ->
                    _uiState.value = _uiState.value.copy(availableNotificationStrategies = strategies)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load notification strategies")
            }
        }
    }

    private fun loadAvailableGeofenceLocations() {
        viewModelScope.launch {
            try {
                geofenceUseCases.getGeofenceLocations().collect { locations ->
                    _availableGeofenceLocations.value = locations
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load geofence locations")
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updateSelectedCategory(categoryItem: CategoryItem) {
        // 记录分类使用
        viewModelScope.launch {
            try {
                categoryPreferencesManager.recordCategoryUsage(categoryItem.id)
            } catch (e: Exception) {
                Timber.e(e, "Failed to record category usage")
            }
        }

        // 直接设置选中的CategoryItem
        _uiState.value = _uiState.value.copy(selectedCategoryItem = categoryItem)
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
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to create category")
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            try {
                val result = categoryRepository.deleteCategory(categoryId)
                if (result.isSuccess) {
                    Timber.d("Category deleted successfully: $categoryId")
                } else {
                    Timber.e("Failed to delete category: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete category")
            }
        }
    }

    fun pinCategory(categoryId: String, isPinned: Boolean) {
        viewModelScope.launch {
            try {
                val result = categoryRepository.pinCategory(categoryId, isPinned)
                if (result.isSuccess) {
                    Timber.d("Category pin status updated: $categoryId -> $isPinned")
                } else {
                    Timber.e("Failed to update category pin status: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update category pin status")
            }
        }
    }

    fun updateDueDate(dueDate: String) {
        _uiState.value = _uiState.value.copy(dueDate = dueDate)
    }

    fun selectLocation(location: LocationInfo?) {
        _uiState.value = _uiState.value.copy(selectedLocation = location)
    }

    fun deleteLocation(locationId: String) {
        viewModelScope.launch {
            try {
                locationUseCases.deleteLocation(locationId).fold(
                    onSuccess = {
                        // 删除成功，地点列表会自动更新（通过Flow）
                        // 如果删除的是当前选中的地点，清除选择
                        if (_uiState.value.selectedLocation?.id == locationId) {
                            _uiState.value = _uiState.value.copy(selectedLocation = null)
                        }
                    },
                    onFailure = { error ->
                        // 删除失败的处理
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "删除地点失败: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "删除地点时发生错误: ${e.message}"
                )
            }
        }
    }

    fun updateImportanceUrgency(importanceUrgency: TaskImportanceUrgency?) {
        _uiState.value = _uiState.value.copy(importanceUrgency = importanceUrgency)
    }

    fun updateSelectedImage(imageUri: String?) {
        _uiState.value = _uiState.value.copy(selectedImageUri = imageUri)
    }

    fun clearSelectedImage() {
        _uiState.value = _uiState.value.copy(selectedImageUri = null)
    }

    fun updateRepeatFrequency(repeatFrequency: RepeatFrequency) {
        _uiState.value = _uiState.value.copy(repeatFrequency = repeatFrequency)
    }

    fun updateRepeatFrequencyType(type: RepeatFrequencyType) {
        val currentRepeat = _uiState.value.repeatFrequency
        val newRepeat = when (type) {
            RepeatFrequencyType.NONE, RepeatFrequencyType.DAILY -> {
                RepeatFrequency(type = type)
            }
            RepeatFrequencyType.WEEKLY -> {
                RepeatFrequency(type = type, weekdays = currentRepeat.weekdays)
            }
            RepeatFrequencyType.MONTHLY -> {
                RepeatFrequency(type = type, monthDays = currentRepeat.monthDays)
            }
        }
        _uiState.value = _uiState.value.copy(repeatFrequency = newRepeat)
    }

    fun updateRepeatWeekdays(weekdays: Set<Int>) {
        val currentRepeat = _uiState.value.repeatFrequency
        if (currentRepeat.type == RepeatFrequencyType.WEEKLY) {
            val newRepeat = currentRepeat.copy(weekdays = weekdays)
            _uiState.value = _uiState.value.copy(repeatFrequency = newRepeat)
        }
    }

    fun updateRepeatMonthDays(monthDays: Set<Int>) {
        val currentRepeat = _uiState.value.repeatFrequency
        if (currentRepeat.type == RepeatFrequencyType.MONTHLY) {
            val newRepeat = currentRepeat.copy(monthDays = monthDays)
            _uiState.value = _uiState.value.copy(repeatFrequency = newRepeat)
        }
    }

    fun updatePreciseTime(preciseTime: Pair<Int, Int>?) {
        Timber.tag("NotificationTask").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Timber.tag("NotificationTask").d("【ViewModel】updatePreciseTime 被调用")
        Timber.tag("NotificationTask").d("  传入的值: $preciseTime")
        Timber.tag("NotificationTask").d("  当前值: ${_uiState.value.preciseTime}")

        _uiState.value = _uiState.value.copy(preciseTime = preciseTime)

        Timber.tag("NotificationTask").d("  更新后的值: ${_uiState.value.preciseTime}")
        Timber.tag("NotificationTask").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    fun updateSelectedDate(date: LocalDate?) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
    }

    fun updateNotificationStrategy(strategyId: String?) {
        _uiState.value = _uiState.value.copy(notificationStrategyId = strategyId)
    }

    fun updateGeofenceEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(geofenceEnabled = enabled)
        // 如果禁用，清除选中的地点
        if (!enabled) {
            _uiState.value = _uiState.value.copy(selectedGeofenceLocationId = null)
        }
    }

    fun updateSelectedGeofenceLocation(locationId: String?) {
        _uiState.value = _uiState.value.copy(selectedGeofenceLocationId = locationId)
    }

    fun createTask() {
        val currentState = _uiState.value
        if (currentState.title.isBlank()) {
            Timber.w("Cannot create task with empty title")
            return
        }

        viewModelScope.launch {
            try {
                Timber.tag("NotificationTask").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Timber.tag("NotificationTask").d("【ViewModel】createTask 被调用")
                Timber.tag("NotificationTask").d("任务信息：")
                Timber.tag("NotificationTask").d("  标题: ${currentState.title}")
                Timber.tag("NotificationTask").d("  描述: ${currentState.description}")
                Timber.tag("NotificationTask").d("  分类: ${currentState.category}")
                Timber.tag("NotificationTask").d("  selectedDate: ${currentState.selectedDate}")
                Timber.tag("NotificationTask").d("  preciseTime: ${currentState.preciseTime}")
                Timber.tag("NotificationTask").d("  notificationStrategyId: ${currentState.notificationStrategyId}")

                // 计算截止时间
                val dueDateTime = when {
                    // 情况1：选择了日期
                    currentState.selectedDate != null -> {
                        val baseDate = currentState.selectedDate.atStartOfDay()
                        if (currentState.preciseTime != null) {
                            // 如果设置了精确时间，使用设置的时间
                            val time = baseDate.withHour(currentState.preciseTime.first)
                                .withMinute(currentState.preciseTime.second)
                                .withSecond(0)
                                .withNano(0)
                            Timber.tag("NotificationTask").d("计算得到的dueDateTime (选择日期+精确时间): $time")
                            time
                        } else {
                            // 如果没有设置精确时间，默认为当天23:59
                            val time = baseDate.withHour(23)
                                .withMinute(59)
                                .withSecond(59)
                                .withNano(0)
                            Timber.tag("NotificationTask").d("计算得到的dueDateTime (选择日期+默认23:59): $time")
                            time
                        }
                    }
                    // 情况2：没有选择日期，但设置了精确时间 - 使用今天+精确时间
                    currentState.preciseTime != null -> {
                        val today = java.time.LocalDate.now()
                        val time = today.atTime(
                            currentState.preciseTime.first,
                            currentState.preciseTime.second,
                            0,
                            0
                        )
                        Timber.tag("NotificationTask").d("计算得到的dueDateTime (今天+精确时间): $time")
                        time
                    }
                    // 情况3：都没有设置
                    else -> {
                        Timber.tag("NotificationTask").d("未选择日期和精确时间，dueDateTime = null")
                        null
                    }
                }

                Timber.tag("NotificationTask").d("准备调用 taskUseCases.createTask()...")

                val result = taskUseCases.createTask(
                    title = currentState.title,
                    description = currentState.description,
                    category = currentState.category,
                    dueDate = dueDateTime,
                    imageUri = currentState.selectedImageUri,
                    repeatFrequency = currentState.repeatFrequency,
                    notificationStrategyId = currentState.notificationStrategyId,
                    importanceUrgency = currentState.importanceUrgency
                )

                if (result.isSuccess) {
                    val taskId = result.getOrNull()
                    Timber.tag("NotificationTask").d("✅ 任务创建成功")
                    Timber.tag("NotificationTask").d("  任务ID: $taskId")

                    // 如果启用了地理围栏且选择了地点，创建TaskGeofence关联
                    if (taskId != null && currentState.geofenceEnabled && currentState.selectedGeofenceLocationId != null) {
                        try {
                            val geofenceResult = geofenceUseCases.createTaskGeofence.invoke(
                                taskId = taskId,
                                geofenceLocationId = currentState.selectedGeofenceLocationId
                            )
                            if (geofenceResult.isSuccess) {
                                Timber.tag("TaskGeofence").d("✅ 任务地理围栏关联创建成功")
                            } else {
                                Timber.tag("TaskGeofence").e("❌ 任务地理围栏关联创建失败")
                            }
                        } catch (e: Exception) {
                            Timber.tag("TaskGeofence").e(e, "创建任务地理围栏关联异常")
                        }
                    }

                    Timber.tag("NotificationTask").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    // 重置表单
                    _uiState.value = CreateTaskUiState()
                } else {
                    Timber.tag("NotificationTask").e("❌ 任务创建失败: ${result.exceptionOrNull()?.message}")
                    Timber.tag("NotificationTask").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
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
    val selectedCategoryItem: CategoryItem? = null,
    val dueDate: String = "",
    val selectedDate: LocalDate? = null, // 选择的日期
    val preciseTime: Pair<Int, Int>? = null, // 精确时间（小时, 分钟），null表示未设置
    val isLoading: Boolean = false,
    val selectedLocation: LocationInfo? = null,
    val importanceUrgency: TaskImportanceUrgency? = TaskImportanceUrgency.IMPORTANT_NOT_URGENT,
    val selectedImageUri: String? = null,
    val repeatFrequency: RepeatFrequency = RepeatFrequency(),
    val notificationStrategyId: String? = null, // 通知策略ID
    val availableNotificationStrategies: List<NotificationStrategy> = emptyList(), // 可用的通知策略列表
    val geofenceEnabled: Boolean = false, // 是否启用地理围栏
    val selectedGeofenceLocationId: String? = null, // 选中的地理围栏地点ID
    val errorMessage: String? = null
) {
    // 获取对应的Category，用于创建任务
    val category: Category
        get() = selectedCategoryItem?.let { categoryItem ->
            Category(
                id = categoryItem.id,
                name = categoryItem.displayName,
                type = if (categoryItem.isSystemDefault) CategoryType.PRESET else CategoryType.CUSTOM,
                icon = categoryItem.icon,
                colorHex = categoryItem.colorHex
            )
        } ?: Category(
            id = "LIFE",
            name = "生活",
            type = CategoryType.PRESET,
            icon = "life",
            colorHex = "#4CAF50"
        )
} 