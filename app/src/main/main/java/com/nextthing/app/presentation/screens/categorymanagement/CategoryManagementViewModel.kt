package com.nextthing.app.presentation.screens.categorymanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextthing.app.domain.model.Category
import com.nextthing.app.domain.model.CategoryType
import com.nextthing.app.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class CategoryManagementUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryManagementUiState())
    val uiState: StateFlow<CategoryManagementUiState> = _uiState.asStateFlow()

    private var categoriesJob: Job? = null

    init {
        viewModelScope.launch {
            categoryRepository.ensurePresetCategories()
        }
        loadCategories()
    }

    private fun loadCategories() {
        categoriesJob?.cancel()
        categoriesJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            categoryRepository.getAllCategories().collect { list ->
                _uiState.update { it.copy(categories = list, isLoading = false) }
            }
        }
    }

    fun createCategory(name: String, icon: String, colorHex: String = "#42A5F5") {
        viewModelScope.launch {
            val result = categoryRepository.createCategory(name = name, icon = icon, colorHex = colorHex)
            if (result.isFailure) {
                Timber.e("🏷️ 新建分类失败: ${result.exceptionOrNull()?.message}")
                _uiState.update { it.copy(errorMessage = "新建分类失败") }
            } else {
                Timber.d("🏷️ 新建分类成功: $name")
            }
        }
    }

    fun updateCategory(category: Category, newName: String, newIcon: String, newColorHex: String) {
        viewModelScope.launch {
            val updated = category.copy(name = newName, icon = newIcon, colorHex = newColorHex)
            val result = categoryRepository.updateCategory(updated)
            if (result.isFailure) {
                Timber.e("🏷️ 编辑分类失败: ${result.exceptionOrNull()?.message}")
                _uiState.update { it.copy(errorMessage = "编辑分类失败") }
            } else {
                Timber.d("🏷️ 编辑分类成功: $newName")
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            val result = categoryRepository.deleteCategory(categoryId)
            if (result.isFailure) {
                val msg = result.exceptionOrNull()?.message ?: "删除失败"
                Timber.e("🏷️ 删除分类失败: $msg")
                _uiState.update { it.copy(errorMessage = msg) }
            } else {
                Timber.d("🏷️ 删除分类成功: $categoryId")
            }
        }
    }

    /**
     * 将指定 index 的分类上移或下移：交换两项的 sortOrder
     */
    fun reorderCategory(fromIndex: Int, toIndex: Int) {
        val list = _uiState.value.categories
        if (fromIndex < 0 || toIndex < 0 || fromIndex >= list.size || toIndex >= list.size) return
        val fromCategory = list[fromIndex]
        val toCategory = list[toIndex]
        viewModelScope.launch {
            categoryRepository.updateCategorySortOrder(fromCategory.id, toCategory.sortOrder)
            categoryRepository.updateCategorySortOrder(toCategory.id, fromCategory.sortOrder)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun isPreset(category: Category): Boolean = category.type == CategoryType.PRESET
}
