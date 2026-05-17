package com.nextthing.app.presentation.screens.categorymanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextthing.app.domain.model.Category
import com.nextthing.app.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class CategoryEditUiState(
    val name: String = "",
    val icon: String = "drawable:life",
    val colorHex: String = "#42A5F5",
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CategoryEditViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryEditUiState())
    val uiState: StateFlow<CategoryEditUiState> = _uiState.asStateFlow()

    private var editingCategory: Category? = null

    fun loadCategory(categoryId: String?) {
        if (categoryId.isNullOrBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val category = categoryRepository.getCategoryById(categoryId)
            if (category != null) {
                editingCategory = category
                _uiState.update {
                    it.copy(
                        name = category.name,
                        icon = category.icon,
                        colorHex = category.colorHex,
                        isEdit = true,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "分类不存在") }
            }
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateIcon(icon: String) {
        _uiState.update { it.copy(icon = icon) }
    }

    fun updateColor(colorHex: String) {
        _uiState.update { it.copy(colorHex = colorHex) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "分类名称不能为空") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = if (state.isEdit && editingCategory != null) {
                val updated = editingCategory!!.copy(
                    name = state.name.trim(),
                    icon = state.icon,
                    colorHex = state.colorHex
                )
                categoryRepository.updateCategory(updated)
            } else {
                categoryRepository.createCategory(
                    name = state.name.trim(),
                    icon = state.icon,
                    colorHex = state.colorHex
                ).map { }
            }

            result.fold(
                onSuccess = {
                    Timber.d("🏷️ ${if (state.isEdit) "编辑" else "新建"}分类成功: ${state.name}")
                    _uiState.update { it.copy(isSaving = false) }
                    onSuccess()
                },
                onFailure = { e ->
                    Timber.e("🏷️ 保存分类失败: ${e.message}")
                    _uiState.update { it.copy(isSaving = false, errorMessage = "保存失败: ${e.message}") }
                }
            )
        }
    }
}
