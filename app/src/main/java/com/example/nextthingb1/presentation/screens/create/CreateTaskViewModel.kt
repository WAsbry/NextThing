package com.example.nextthingb1.presentation.screens.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextthingb1.domain.model.Task
import com.example.nextthingb1.domain.model.TaskCategory
import com.example.nextthingb1.domain.model.TaskPriority
import com.example.nextthingb1.domain.model.TaskStatus
import com.example.nextthingb1.domain.model.CategoryItem
import com.example.nextthingb1.domain.model.TaskImportanceUrgency
import com.example.nextthingb1.domain.usecase.TaskUseCases
import com.example.nextthingb1.domain.repository.CustomCategoryRepository
import com.example.nextthingb1.domain.service.CategoryPreferencesManager
import com.example.nextthingb1.domain.usecase.LocationUseCases
import com.example.nextthingb1.domain.model.LocationInfo
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
    private val taskUseCases: TaskUseCases,
    private val customCategoryRepository: CustomCategoryRepository,
    private val categoryPreferencesManager: CategoryPreferencesManager,
    private val locationUseCases: LocationUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateTaskUiState())
    val uiState: StateFlow<CreateTaskUiState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryItem>>(emptyList())
    val categories: StateFlow<List<CategoryItem>> = _categories.asStateFlow()

    private val _showCreateCategoryDialog = MutableStateFlow(false)
    val showCreateCategoryDialog: StateFlow<Boolean> = _showCreateCategoryDialog.asStateFlow()

    private val _savedLocations = MutableStateFlow<List<LocationInfo>>(emptyList())
    val savedLocations: StateFlow<List<LocationInfo>> = _savedLocations.asStateFlow()

    init {
        initializeCategories()
        loadSavedLocations()
    }

    private fun initializeCategories() {
        viewModelScope.launch {
            try {
                customCategoryRepository.initializeSystemCategories()

                // 首先获取上次选择的分类
                val lastSelectedCategoryId = categoryPreferencesManager.getLastSelectedCategoryId()

                customCategoryRepository.getAllCategories().collect { categoryList ->
                    // 按使用频率排序分类
                    val sortedCategories = categoryPreferencesManager.sortCategoriesByUsage(categoryList)
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
                ?: _categories.value.find { it.id == TaskCategory.LIFE.name } // 默认生活分类
                ?: CategoryItem.fromTaskCategory(TaskCategory.LIFE) // 备用默认

            _uiState.value = _uiState.value.copy(selectedCategoryItem = categoryItem)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load last selected category")
            // 出错时使用默认分类
            val defaultCategory = CategoryItem.fromTaskCategory(TaskCategory.LIFE)
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

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updatePriority(priority: TaskPriority) {
        _uiState.value = _uiState.value.copy(priority = priority)
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
                val result = customCategoryRepository.createCategory(name, colorHex)
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
                val result = customCategoryRepository.deleteCategory(categoryId)
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
                val result = customCategoryRepository.pinCategory(categoryId, isPinned)
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

    fun updateImportanceUrgency(importanceUrgency: TaskImportanceUrgency?) {
        _uiState.value = _uiState.value.copy(importanceUrgency = importanceUrgency)
    }

    fun updateSelectedImage(imageUri: String?) {
        _uiState.value = _uiState.value.copy(selectedImageUri = imageUri)
    }

    fun clearSelectedImage() {
        _uiState.value = _uiState.value.copy(selectedImageUri = null)
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
                    dueDate = if (currentState.dueDate.isNotBlank()) LocalDateTime.now().plusDays(1) else null,
                    imageUri = currentState.selectedImageUri
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
    val selectedCategoryItem: CategoryItem? = null,
    val dueDate: String = "",
    val isLoading: Boolean = false,
    val selectedLocation: LocationInfo? = null,
    val importanceUrgency: TaskImportanceUrgency? = null,
    val selectedImageUri: String? = null
) {
    // 获取对应的TaskCategory，用于创建任务
    val category: TaskCategory
        get() = selectedCategoryItem?.let { categoryItem ->
            when (categoryItem.id) {
                TaskCategory.WORK.name -> TaskCategory.WORK
                TaskCategory.STUDY.name -> TaskCategory.STUDY
                TaskCategory.LIFE.name -> TaskCategory.LIFE
                TaskCategory.HEALTH.name -> TaskCategory.HEALTH
                TaskCategory.PERSONAL.name -> TaskCategory.PERSONAL
                else -> TaskCategory.OTHER // 自定义分类映射到OTHER
            }
        } ?: TaskCategory.LIFE
} 