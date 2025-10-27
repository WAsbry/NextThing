package com.example.nextthingb1.domain.repository

import com.example.nextthingb1.domain.model.Category
import com.example.nextthingb1.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow

/**
 * 分类数据仓库接口
 */
interface CategoryRepository {

    /**
     * 获取所有启用的分类（按排序顺序）
     */
    fun getAllCategories(): Flow<List<Category>>

    /**
     * 根据 ID 获取分类
     */
    suspend fun getCategoryById(categoryId: String): Category?

    /**
     * 根据类型获取分类
     */
    fun getCategoriesByType(type: CategoryType): Flow<List<Category>>

    /**
     * 创建新分类（用户自定义）
     */
    suspend fun createCategory(
        name: String,
        icon: String = "circle",
        colorHex: String = "#9E9E9E"
    ): Result<Category>

    /**
     * 更新分类
     */
    suspend fun updateCategory(category: Category): Result<Unit>

    /**
     * 删除分类（仅限用户自定义分类）
     */
    suspend fun deleteCategory(categoryId: String): Result<Unit>

    /**
     * 启用/禁用分类
     */
    suspend fun toggleCategoryEnabled(categoryId: String, isEnabled: Boolean): Result<Unit>

    /**
     * 更新分类排序
     */
    suspend fun updateCategorySortOrder(categoryId: String, newSortOrder: Int): Result<Unit>

    /**
     * 初始化预置分类（如果不存在）
     */
    suspend fun ensurePresetCategories(): Result<Unit>

    /**
     * 初始化系统分类（别名方法，与ensurePresetCategories相同）
     */
    suspend fun initializeSystemCategories() {
        ensurePresetCategories()
    }

    /**
     * 固定/取消固定分类
     */
    suspend fun pinCategory(categoryId: String, isPinned: Boolean): Result<Unit>
}
